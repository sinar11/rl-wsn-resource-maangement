// @(#)SensorNodePositionTracker.java   12/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer. 
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution. 
// 3. Neither the name of "DRCL" nor the names of its contributors may be used
//    to endorse or promote products derived from this software without specific
//    prior written permission. 
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 

package drcl.inet.sensorsim;

import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*; 
import drcl.inet.contract.*;
import java.util.*;
import drcl.comp.Port;
import drcl.comp.Contract;

/** This class keeps track of the locations of the sensor and target nodes.
*
* @author Ahmed Sobeih
* @version 1.1, 10/19/2004
*/
public class SensorNodePositionTracker extends drcl.net.Module {    
    public static final String NODE_PORT_ID    = ".node";       // connect to the sensor mobility model 
    public static final String CONFIG_PORT_ID  = ".config";
    public static final String CHANNEL_PORT_ID = ".channel";    // connect to the sensor channel component
    
    protected Port nodePort    = addPort(NODE_PORT_ID, false);
    protected Port configPort  = addPort(CONFIG_PORT_ID, false); 
    protected Port channelPort = addServerPort(CHANNEL_PORT_ID);

    {
        removeDefaultUpPort();
        removeDefaultDownPort();
        removeTimerPort();
    }    

    /** Dimensions of the terrain  */    
    double maxX, maxY, minX, minY, dX, dY;
    int    dim_x, dim_y;
    
    Vector g[][];

    public SensorNodePositionTracker( ) {
        super();
    }
        
    public SensorNodePositionTracker(double maxX_, double minX_, double maxY_, double minY_, double dX_, double dY_) {
        super();
        maxX = maxX_; minX = minX_; dX = dX_;
        maxY = maxY_; minY = minY_; dY = dY_;
		dim_x = (int)((maxX - minX)/dX) + 1 ; // x-axis of the grid runs from 0 to dim_x - 1
        dim_y = (int)((maxY - minY)/dY) + 1 ; // y-axis of the grid runs from 0 to dim_y - 1

	g = new Vector[dim_x][dim_y];
        for (int i = 0; i < dim_x; i ++ ) 
        	for (int j = 0; j < dim_y; j ++ ) 
        	        g[i][j] = new Vector();
    }

    /** Sets the dimensions of the terrain, with default grid size 1 */
    public void setGrid(double maxX_, double minX_, double maxY_, double minY_) {
		setGrid(maxX_, minX_, maxY_, minY_, 1.0, 1.0);
    }

    /** Sets the dimensions of the terrain */
    public void setGrid(double maxX_, double minX_, double maxY_, double minY_, double dX_, double dY_) {
        maxX = maxX_; minX = minX_; dX = dX_;
        maxY = maxY_; minY = minY_; dY = dY_;
		dim_x = (int)((maxX - minX)/dX) + 1 ; // x-axis of the grid runs from 0 to dim_x - 1
        dim_y = (int)((maxY - minY)/dY) + 1; // y-axis of the grid runs from 0 to dim_y - 1
        g = new Vector[dim_x][dim_y];
        for (int i = 0; i < dim_x; i ++ ) 
            for (int j = 0; j < dim_y; j ++ ) 
                g[i][j] = new Vector();
    }    

    public String getName() { return "SensorNodePositionTracker"; }
    
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
		SensorNodePositionTracker that_ = (SensorNodePositionTracker)source_;
		maxX = that_.maxX; minX = that_.minX; dX = that_.dX;
		maxY = that_.maxY; minY = that_.minY; dY = that_.dY;
	  	dim_x = (int)((maxX - minX)/dX) +1 ; // x-axis of the grid runs from 0 to dim_x - 1
       		dim_y = (int)((maxY - minY)/dY) + 1 ; // y-axis of the grid runs from 0 to dim_y - 1
        	g = new Vector[dim_x][dim_y];
        	for (int i = 0; i < dim_x; i ++ ) 
            		for (int j = 0; j < dim_y; j ++ ) 
                		g[i][j] = new Vector();
	}

	//???
	int aligngrid(int a, int b)
	{
		if ( a == b )
			return (b - 1);
		else
			return a;
	}

	/** Returns square of the distance between two nodes */
	double distanceSq(double x1, double x2, double y1, double y2)
	{
		 return ((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}

	/** Handles location reports and queries  */
  	protected synchronized void processOther(Object data_, Port inPort_)
	{
		String portid_ = inPort_.getID();
    
        	if (portid_.equals(NODE_PORT_ID)) {
            		processReport(data_, inPort_);
        	}    
        	else if (portid_.equals(CHANNEL_PORT_ID)) {
            		processQuery(data_, inPort_);
        	}
        	else 
            		super.processOther(data_, inPort_);
	}  

	/** Handles location reports */
    protected synchronized void processReport(Object data_, Port inPort_) {       
        	if ( !(data_ instanceof SensorPositionReportContract.Message) ) {
            		error(data_, "processReport()", inPort_, "unknown object");
            		return;
        	}    

        	SensorPositionReportContract.Message msg = (SensorPositionReportContract.Message) data_;
        
        long id;  
        double X, Y, Z, X0, Y0, Z0;
        int i, j, i0, j0;	
        int gindex ;
	  boolean found;
	  SensorLocationInformation current ;

        id = msg.getNid();
        X  = msg.getX();
        Y  = msg.getY();
        Z  = msg.getZ();
        X0  = msg.getX0();
        Y0  = msg.getY0();
        Z0  = msg.getZ0();

		i = (int) ((X - minX)/dX);
		j = (int) ((Y - minY)/dY);

        if ( X == X0 && Y == Y0 )  {  // first time or not moved yet 
            //i = aligngrid((int)X - (int)minX, dim_x); 
            //j = aligngrid((int)Y - (int)minY, dim_y); 

			found = false ;
			gindex = 0 ;
			while ( (found == false) && (gindex < g[i][j].size()) )
			{
				current = (SensorLocationInformation)(g[i][j].get(gindex));
				if ( current.nid == id )
					found = true ;
				else
					gindex++ ;
			}

            if ( found == false )
			{
                g[i][j].insertElementAt(new SensorLocationInformation(id, X, Y, Z), 0);
			}
        }
        else {
            //i = aligngrid((int)X - (int)minX, dim_x); 
            //j = aligngrid((int)Y - (int)minY, dim_y); 
            //i0 = aligngrid((int)X0 - (int)minX, dim_x); 
            //j0 = aligngrid((int)Y0 - (int)minY, dim_y); 
            i0 = (int)((X0 - minX)/dX) ; 
            j0 = (int)((Y0 - minY)/dY) ; 

			found = false ;
			gindex = 0 ;
			while ( (found == false) && (gindex < g[i0][j0].size()) )
			{
				current = (SensorLocationInformation)(g[i0][j0].get(gindex));
				if ( current.nid == id )
					found = true ;
				else
					gindex++ ;
			}

			if ( found == true )
			{
	            g[i0][j0].remove(gindex);
	            g[i][j].insertElementAt(new SensorLocationInformation(id, X, Y, Z), 0);
			}
        }
    }

    /** Handles query and replies with a list of neighbors  */    
    protected synchronized void processQuery(Object data_, Port inPort_) {
        if ( !(data_ instanceof SensorNeighborQueryContract.Message) )  {
            error(data_, "processQuery()", inPort_, "unknown object");
            return;
        }
        
        SensorNeighborQueryContract.Message msg = (SensorNeighborQueryContract.Message) data_;
        
        long nid;  
        double X, Y, Z;
	  	double Radius; double sqmnr ;
        long[] nodeList;
		SensorLocationInformation current = null;

        X  = msg.getX();
        Y  = msg.getY();
        Z  = msg.getZ();
  	nid = msg.getNid();
  	Radius = msg.getRadius();
	boolean WithLocations = msg.getWithLocations() ;
	SensorLocationInformation[] neighborsLocation ;
        
        int grid_x, grid_y, i, j, ulx, uly, lly, llx, adj ;

        //grid_x = aligngrid((int)X - (int)minX, dim_x); 
        //grid_y = aligngrid((int)Y - (int)minY, dim_y); 
		grid_x = (int) ((X - minX)/dX);
		grid_y = (int) ((Y - minY)/dY);

	  sqmnr = Radius * Radius ;
	  adj = (int)(Math.ceil(Radius/dX));

	  ulx = Math.min(dim_x-1, grid_x + adj);
	  uly = Math.min(dim_y-1, grid_y + adj);
	  lly = Math.max(0, grid_y - adj);
	  llx = Math.max(0, grid_x - adj);

	  long id ; 
	  int kv ;
        int nn = 0;
	  for (i = llx; i <= ulx; i++) 
	    for (j = lly; j <= uly; j++) 
                for ( kv = 0; kv < g[i][j].size(); kv++ ) 
		{
			current = (SensorLocationInformation)(g[i][j].get(kv));
			id = current.nid ;
			if ( id != nid ) 
			{ // only if id of potential neighbor is different.
				if ( distanceSq(current.getX(), X, current.getY(), Y) < sqmnr )
				{
					nn = nn + 1;
				} // end if d2
			} // end if id != nid
		} // end for kv

        nodeList = new long[nn];
	neighborsLocation = new SensorLocationInformation[nn] ;

	  int kk = 0 ;
	  for (i = llx; i <= ulx; i++) {
	    for (j = lly; j <= uly; j++) {
                for ( kv = 0; kv < g[i][j].size(); kv ++ ) {
              current = (SensorLocationInformation)(g[i][j].get(kv));
			  id = current.nid ;
			  if ( id != nid ) { // only if the id of the potential neighbor is different.
				if ( distanceSq(current.getX(), X, current.getY(), Y) < sqmnr )
					{
			      	        	nodeList[kk] = id ;
						neighborsLocation[kk] = new SensorLocationInformation(current.getNid(), current.getX(), current.getY(), current.getZ()) ;
						kk = kk + 1;
					} // end if d2
			  }  // end if id != nid
                }  // end for kv
    	    } // end for j
	  } // end for i

	if ( WithLocations == false )
	{
	        channelPort.doSending(new SensorNeighborQueryContract.Message(nodeList));
	}
	else
	{
	        channelPort.doSending(new SensorNeighborQueryContract.Message(nodeList, neighborsLocation));
	}
    }       
}
