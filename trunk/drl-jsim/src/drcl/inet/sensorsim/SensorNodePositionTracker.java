// @(#)SensorNodePositionTracker.java   12/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
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
import drcl.inet.sensorsim.MultiHop.NeighborQueryContract;

import java.util.*;


/** This class keeps track of the locations of the sensor and target nodes.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
 *
 * Modified by Nicholas Merizzi May 2005.
*/
public class SensorNodePositionTracker extends drcl.net.Module {    

	/** This class implements the location information of a sensor node. */
    public static class SensorLocationInformation {
		long nid ;
		double X; // actual location coordinates
		double Y; // used for calculating the exact distance between two sensors
		double Z; // and hence decide whether two sensors are neighbors or not
	
		public SensorLocationInformation ()
		{
		}
	
		public SensorLocationInformation (long nid_, double X_, double Y_, double Z_)
		{
			nid = nid_;
			X = X_; Y = Y_; Z = Z_;
		}
    } // end class SensorLocationInformation

    public static final String NODE_PORT_ID    = ".node";       // connect to the sensor mobility model 
    public static final String CONFIG_PORT_ID  = ".config";
    public static final String CHANNEL_PORT_ID = ".channel";    // connect to the sensor channel component
    public static final String MULTI_HOP_PORT_ID = ".multiHop"; // added to support multihop queries for next closest node



    protected Port nodePort         = addPort(NODE_PORT_ID, false);
    protected Port configPort       = addPort(CONFIG_PORT_ID, false);
    protected Port channelPort      = addServerPort(CHANNEL_PORT_ID);
    public Port multihopPort        = addPort(MULTI_HOP_PORT_ID);

    {
        removeDefaultUpPort();
        removeDefaultDownPort();
        removeTimerPort();
    }    

    /** Dimensions of the terrain  */    
    double maxX, maxY, minX, minY;
    int    dim_x_, dim_y_;
    
    Vector g[][];

    public SensorNodePositionTracker( ) {
        super();
    }
        
    public SensorNodePositionTracker(double maxX_, double minX_, double maxY_, double minY_) {
        super();
        maxX = maxX_; minX = minX_;
        maxY = maxY_; minY = minY_;
		dim_x_ = ((int)(maxX - minX)) ; // x-axis of the grid runs from 0 to dim_x_ - 1
        dim_y_ = ((int)(maxY - minY)) ; // y-axis of the grid runs from 0 to dim_y_ - 1

		g = new Vector[dim_x_][dim_y_];
        for (int i = 0; i < dim_x_; i ++ ) 
        	for (int j = 0; j < dim_y_; j ++ ) 
        	        g[i][j] = new Vector();
    }

    /** Sets the dimensions of the terrain */
    public void setGrid(double maxX_, double minX_, double maxY_, double minY_) {
        maxX = maxX_; minX = minX_;
        maxY = maxY_; minY = minY_;
		dim_x_ = ((int)(maxX - minX)) ; // x-axis of the grid runs from 0 to dim_x_ - 1
        dim_y_ = ((int)(maxY - minY)) ; // y-axis of the grid runs from 0 to dim_y_ - 1
        g = new Vector[dim_x_][dim_y_];
        for (int i = 0; i < dim_x_; i ++ ) 
            for (int j = 0; j < dim_y_; j ++ ) 
                g[i][j] = new Vector();
    }    

    public String getName() { return "SensorNodePositionTracker"; }
    
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
		SensorNodePositionTracker that_ = (SensorNodePositionTracker)source_;
		maxX = that_.maxX; minX = that_.minX;
		maxY = that_.maxY; minY = that_.minY;
	  	dim_x_ = ((int)(maxX - minX)) ; // x-axis of the grid runs from 0 to dim_x_ - 1
       	dim_y_ = ((int)(maxY - minY)) ; // y-axis of the grid runs from 0 to dim_y_ - 1
        g = new Vector[dim_x_][dim_y_];
        for (int i = 0; i < dim_x_; i ++ )
        	for (int j = 0; j < dim_y_; j ++ )
           		g[i][j] = new Vector();
	}

	int aligngrid(int a, int b)
	{
		if ( a == b )
			return (b - 1);
		else
			return a;
	}

	/** Returns square of the distance between two nodes */
	double distance(double x1, double x2, double y1, double y2)
	{
		 return ((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}

	/** Handles location reports and queries  */
  	protected synchronized void processOther(Object data_, Port inPort_) {
		String portid_ = inPort_.getID();
    
        if (portid_.equals(NODE_PORT_ID)) {
           		processReport(data_, inPort_);
        } else if (portid_.equals(CHANNEL_PORT_ID)) {
            processQuery(data_, inPort_);
        } else if (portid_.equals(MULTI_HOP_PORT_ID)) {
            closestNeighbor(data_, inPort_);
        } else
            super.processOther(data_, inPort_);
	}

    //Nicholas:
    /*The following two functions were added to obtain neighbor distances for the multi-hop
      scheme*/
    /**
     * Calculates distance between two sensors.
     * @param X
     * @param Y
     * @param Z
     * @param X2
     * @param Y2
     * @param Z2
     * @return
    */
    protected double EuclideanDist(double X, double Y, double Z,double X2, double Y2, double Z2)
    {
        double dx = X2 - X;
        double dy = Y2 - Y;
        double dz = Z2 - Z;
        return(Math.sqrt((dx*dx) + (dy*dy) + (dz*dz)));
    }

    /**
     * Finds the closest neighbor.
     * @param data_
     * @param inPort_
    */
    protected synchronized void closestNeighbor(Object data_, Port inPort_) {

        //boolean newNeighbor = false;         //If we found a closer node then set this flag
        long ClosestNeighbor = 0;              //no closest neighbor found make it the sink by default
        double X, Y, Z;                        //the location of the original node
        double MaxDist;                        //reset all nodes to have next hop pointing to BS
	    long id ;                              //the ID of the original node
        double neighborDistSink = -1;          //the distance of the neighbor to the sink
        double current_seperation = -1;        //the distance between this and neighbor
        SensorLocationInformation current = new SensorLocationInformation() ;
        double[] neighborLoc = new double[3];
        double neighborSeperation;      //distance between itself and its closest neighbor.

        if ( !(data_ instanceof NeighborQueryContract.Message) ) {
            error(data_, "processReport()", inPort_, "unknown object");
            return;
        }

        NeighborQueryContract.Message msg = (NeighborQueryContract.Message) data_;

        id = msg.getNode_nid(); X  = msg.getNode_nidX(); Y  = msg.getNode_nidY(); Z  = msg.getNode_nidZ();
        MaxDist = msg.getSinkDistance();
        neighborSeperation = msg.getSinkDistance();

        for (int n = 0; n<dim_x_; n++) {
            for (int m=0; m<dim_y_; m++){
                for(int l = 0; l< g[n][m].size(); l ++){
                    current = (SensorLocationInformation)(g[n][m].get(l));

                    neighborDistSink = EuclideanDist(msg.getSink_X(), msg.getSink_Y(), msg.getSink_Z(), current.X, current.Y, current.Z);

                    //a Potential neighbor must not be the node itself and it must be closer to the BS
			        if (id != current.nid){
                        if (neighborDistSink < MaxDist )  {
                            current_seperation = EuclideanDist(current.X, current.Y, current.Z, X, Y, Z);
                           // System.out.println("The distance is between the two is: "+ current_seperation);
                            if (current_seperation < neighborSeperation) {
                                //System.out.println("We have a new neighbor!");
    		      	            ClosestNeighbor = current.nid;
                                neighborLoc[0] = current.X; neighborLoc[1] = current.Y; neighborLoc[2] = current.Z;
                                neighborSeperation = current_seperation;
		    			    }
                        }
                    }
                }
            }
        }
        //System.out.println("*****ClosestNeighbor is: "+ClosestNeighbor + " Shortest Dist: " + shortestDist);
        multihopPort.doSending(new NeighborQueryContract.Message (ClosestNeighbor, neighborSeperation, neighborLoc));
    }


	/** Handles location reports */
    protected synchronized void processReport(Object data_, Port inPort_) {
        	if ( !(data_ instanceof SensorPositionReportContract.Message) ) {
            		error(data_, "processReport()", inPort_, "unknown object");
            		return;
        	}    

        	SensorPositionReportContract.Message msg = (SensorPositionReportContract.Message) data_;
        
            long id;
            double X, Y, Z, X0, Y0; // Z0;
            int i, j, i0, j0;
            int gindex ;
            boolean found;
	        SensorLocationInformation current = new SensorLocationInformation() ;

            id = msg.getNid();
            X  = msg.getX();
            Y  = msg.getY();
            Z  = msg.getZ();
            X0  = msg.getX0();
            Y0  = msg.getY0();
            //Z0  = msg.getZ0();

            if ( X == X0 && Y == Y0 )  {  // first time or not moved yet
                i = aligngrid((int)X - (int)minX, dim_x_);
                j = aligngrid((int)Y - (int)minY, dim_y_);

                found = false ;
                gindex = 0 ;

                while ( (found == false) && (gindex < g[i][j].size()) ) {

                    current = (SensorLocationInformation)(g[i][j].get(gindex));
                    if ( current.nid == id )
                        found = true ;
                    else
                        gindex++ ;
                }

                if ( found == false ) {
                        g[i][j].insertElementAt(new SensorLocationInformation(id, X, Y, Z), 0);
                }
            } else {
                i = aligngrid((int)X - (int)minX, dim_x_);
                j = aligngrid((int)Y - (int)minY, dim_y_);

                i0 = aligngrid((int)X0 - (int)minX, dim_x_);
                j0 = aligngrid((int)Y0 - (int)minY, dim_y_);

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

                if ( found == true ) {
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
        double X, Y; //Z;    ->Z is never used
	    double Radius; double sqmnr ;
        long[] nodeList;
	    SensorLocationInformation current = new SensorLocationInformation() ;

        X  = msg.getX();
        Y  = msg.getY();
        //Z  = msg.getZ();  never used.
	    nid = msg.getNid();
	    Radius = msg.getRadius();
        
        int grid_x, grid_y, i, j, ulx, uly, lly, llx, adj ;

        grid_x = aligngrid((int)X - (int)minX, dim_x_); 
        grid_y = aligngrid((int)Y - (int)minY, dim_y_); 

	    sqmnr = Radius * Radius ;
	    adj = (int)(Math.ceil(Radius));

	    ulx = Math.min(dim_x_-1, grid_x + adj);
	    uly = Math.min(dim_y_-1, grid_y + adj);
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
				nn = nn + 1;
			} // end if id != nid
		} // end for kv
        nodeList = new long[nn];

	    int kk = 0 ;
        for (i = llx; i <= ulx; i++) {
	        for (j = lly; j <= uly; j++) {
                for ( kv = 0; kv < g[i][j].size(); kv ++ ) {
                    current = (SensorLocationInformation)(g[i][j].get(kv));
			        id = current.nid ;
			        if ( id != nid ) { // only if the id of the potential neighbor is different.
				        if ( distance(current.X, X, current.Y, Y) < sqmnr ) {
    		      	        nodeList[kk] = id ;
	          	      	    kk = kk + 1;
		    			} // end if d2
			        }  // end if id != nid
                }  // end for kv
            } // end for j
	    } // end for i
        
        channelPort.doSending(new SensorNeighborQueryContract.Message(nodeList));
    }       
}
