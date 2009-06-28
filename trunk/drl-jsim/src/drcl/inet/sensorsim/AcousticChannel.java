// @(#)AcousticChannel.java   10/2004
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
import drcl.comp.Port;


/** This class implements the acoustic channel in a wireless sensor network.
*
* @author Ahmed Sobeih
* @version 1.0, 10/14/2004
*/
public class AcousticChannel extends SensorChannel 
{
	public static double INDOOR_TEMPERATURE    = 20.0 ;          /* unit: celsius */
	public static double SOUND_SPEED_BASE = 331.4 ; /* unit: m/sec. */
	public static double SOUND_SPEED = SOUND_SPEED_BASE + 0.6 * INDOOR_TEMPERATURE ;  /* unit: m/sec. */
	public static double DEFAULT_D0_VALUE = 0.1 ; /* unit: meter */	

	/** Minimum distance between the sender and the receiver */
	public double d0 ;

	/** Indoor temperature */
	public double indoorTemperature ;

	/** Speed of sound */
	public double soundSpeed ; 

	public AcousticChannel() {
		super();
		setD0(DEFAULT_D0_VALUE) ;
		setIndoorTemperature(INDOOR_TEMPERATURE) ;
	}

	/** Sets the minimum distance between the sender and the receiver */
	public synchronized void setD0(double d0_) 	{	d0 = d0_ ; }

	/** Gets the minimum distance between the sender and the receiver */
	public synchronized double getD0() 		{	return d0 ; }

	/** Sets the indoor temperature */
	public synchronized void setIndoorTemperature(double indoorTemperature_) 	
	{	
		indoorTemperature = indoorTemperature_ ; 
		setSoundSpeed(SOUND_SPEED_BASE + 0.6 * indoorTemperature) ;
	}

	/** Gets the indoor temperature */
	public synchronized double getIndoorTemperature() 		{	return indoorTemperature ; }

	/** Sets the speed of sound */
	public synchronized void setSoundSpeed(double soundSpeed_) 	{	soundSpeed = soundSpeed_ ; }

	/** Gets the speed of sound */
	public synchronized double getSoundSpeed() 			{	return soundSpeed ; }

	/** Gets the propagation delay of the acoustic channel. The propagation delay depends on the distance between the sender and the receiver and on the speed of sound  */
    	public double getPropDelay(double tX, double tY, double tZ, double rX, double rY, double rZ)
	{
		double d = Math.sqrt( (tX-rX) * (tX-rX) + (tY-rY) * (tY-rY) + (tZ-rZ) * (tZ-rZ) ) ;
		if ( d < getD0() )
			d = getD0() ;

		propDelay = d / getSoundSpeed() ;

		return propDelay ; 
	}
    
	public String getName() { return "AcousticChannel"; }
    
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	        AcousticChannel that_ = (AcousticChannel) source_;		
		propDelay = that_.propDelay ;
	}

	/** Receives a packet and forwards it to sensors  */
    protected synchronized void processPacket(Object data_) {

        int i;
        long[] nodeList;
	    SensorLocationInformation[] neighborsLocation ;
        
        SensorNodeChannelContract.Message msg = (SensorNodeChannelContract.Message) data_;
       
        double X, Y, Z;
        long   nid;
	    double Radius ;
        X = msg.getX();
        Y = msg.getY();
        Z = msg.getZ();
        nid = msg.getSenderNid();
	    Radius = msg.getRadius();
        
        SensorNeighborQueryContract.Message msg2 = (SensorNeighborQueryContract.Message) trackerPort.sendReceive(new SensorNeighborQueryContract.Message(nid, X, Y, Z, Radius, true));
        
        nodeList = msg2.getNodeList();
	neighborsLocation = msg2.getNeighborsLocation() ;

        for ( i = 0; i < nodeList.length; i++ ) { 
            Port p_;
            if ( nid != nodeList[i] && vp_flag[(int) nodeList[i]] == true ) {
                p_ = (Port) vp.elementAt((int) nodeList[i]);

		send(p_, msg.clone(), getPropDelay(X, Y, Z, neighborsLocation[i].getX(), neighborsLocation[i].getY(), neighborsLocation[i].getZ()));  
		/* to send immediately, comment out above line and add the following one
		// p_.doSending(msg.clone());  
		*/
            }
        }
    }
}
