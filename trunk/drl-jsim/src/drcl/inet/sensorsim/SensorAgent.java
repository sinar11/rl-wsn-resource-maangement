// @(#)SensorAgent.java   12/2003
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
import drcl.comp.Port;

/**
 * This class implements the sensor layer.
 *
 * @author Ahmed Sobeih
 * @version 1.0, 12/19/2003
*/
public class SensorAgent extends drcl.net.Module {

	public static final double WAIT_TIME	= 10.0; // waiting time before sending the byte to the application

	int bufIndex;
	double wait_time;
	/*
		Caution: 
		If wait_time is non-zero, SensorAgent does not forward the 
		received data (which carry information generated by the target node target_nid) 
		immediately to SensorApp. Instead, during the wait_time, SensorAgent aggregates 
		the received information into numByteRcvd and maxSNR (refer to dataArriveAtFromPhyPort).

		After wait_time elapses, SensorAgent passes the aggregated information 
		to SensorApp. Hence, if there are more than one target node in the simulation 
		AND wait_time is non-zero, when wait_time elapses target_nid will be equal to
		the first target node about which SensorAgent receives information during the 
		wait_time. But this is no problem, because target_nid was added only for cosmotic 
		reasons (namely, for System.out.println statements only). However, numByteRcvd 
		and maxSNR might be based on data received from different target nodes. Depending 
		on the application, the designer should decide whether numByteRcvd and/or maxSNR 
		make sense in that case.

		If wait_time is zero, then no problem at all. But keep in mind that in that 
		case, SensorAgent forwards the received data (which carry information generated 
		by the target node target_nid) immediately to SensorApp.
	*/

	int numByteRcvd;
	double maxSNR;
	long target_nid ;     //which target generated the message originally

    double target_X ;
	double target_Y ;
	double target_Z ;
	int target_SeqNum ;

	ACATimer rtimer ;

    /* configure the ports */
    public static final String CONFIG_PORT_ID           = ".config";
    public static final String FROM_PHY_PORT_ID         = ".fromPhy";
    public static final String TO_SENSOR_APP_PORT_ID    = ".toSensorApp";
    public static final String FORK_PORT_ID             = "forkPort";

    protected Port configPort       = addPort(CONFIG_PORT_ID, false);
    protected Port fromPhyPort      = addPort(FROM_PHY_PORT_ID, false);
    protected Port toSensorAppPort  = addPort(TO_SENSOR_APP_PORT_ID, false);
    protected Port forkPort         = addForkPort(FORK_PORT_ID);

	{
		removeDefaultUpPort() ;
		removeDefaultDownPort() ;
		removeTimerPort() ;
	}

	public SensorAgent () {
		super();
		wait_time = 0.0 ;
		numByteRcvd = 0 ;
		maxSNR = 0.0 ;
		target_nid = Integer.MIN_VALUE ; /* assuming no node has id Integer.MIN_VALUE */
        target_X = Double.MIN_VALUE;
		target_Y = Double.MIN_VALUE;
		target_Z = Double.MIN_VALUE;
		target_SeqNum = Integer.MIN_VALUE ;
		bufIndex = 0 ;
		rtimer = null ;
	}

    public String getName() { return "SensorAgent"; }

	/** Sets the waiting time  */
    public void setWaitTime(double wait_time_) { wait_time = wait_time_; }

    public void duplicate(Object source_) {
        super.duplicate(source_);
        SensorAgent that_ = (SensorAgent) source_;
	    wait_time = that_.wait_time ;
        numByteRcvd = that_.numByteRcvd;
        target_nid = that_.target_nid;
        target_X = that_.target_X ;
	    target_Y = that_.target_Y ;
	    target_Z = that_.target_Z ;
        maxSNR = that_.maxSNR;
        bufIndex = that_.bufIndex;
    }

    protected synchronized void ForwardDataToSensorApp()
    {
                                //Depending on the applicatoin, the application designer may replace the null with other application-specific data */
        toSensorAppPort.doSending(new SensorAppAgentContract.Message(numByteRcvd, maxSNR, target_nid, target_X, target_Y, target_Z, target_SeqNum, null));
        numByteRcvd = 0 ;
        maxSNR = 0.0 ;
        target_nid = Integer.MIN_VALUE ; /* assuming no node has id Integer.MIN_VALUE */
        target_X = Double.MIN_VALUE;
        target_Y = Double.MIN_VALUE;
        target_Z = Double.MIN_VALUE;
        target_SeqNum = Integer.MIN_VALUE ;
    }

    protected synchronized void ForwardDataToSensorApp(Object data_)
    {
        toSensorAppPort.doSending(new SensorAppAgentContract.Message(numByteRcvd, maxSNR, target_nid, target_X, target_Y, target_Z, target_SeqNum, data_));
        numByteRcvd = 0 ;
        maxSNR = 0.0 ;
        target_nid = Integer.MIN_VALUE ; /* assuming no node has id Integer.MIN_VALUE */
        target_X = Double.MIN_VALUE;
        target_Y = Double.MIN_VALUE;
        target_Z = Double.MIN_VALUE;
        target_SeqNum = Integer.MIN_VALUE ;
    }

	/**
     * Handles data arriving at fromPhy port.
    */
    protected synchronized void dataArriveAtFromPhyPort(Object data_)
    {
        double sigPower, /*noisePower,*/ snr;

        SensorAgentPhyContract.Message msg = (SensorAgentPhyContract.Message) data_;
        double lastNoisePower = msg.getlastNoisePower();

        if ( wait_time == 0.0 )
        {
            target_nid = msg.getTargetNid() ;
            target_X = msg.getXTarget();
            target_Y = msg.getYTarget();
            target_Z = msg.getZTarget();
            target_SeqNum = ((TargetPacket)msg.getPkt()).seqNum ;

            int size = ((TargetPacket)msg.getPkt()).size ;
            numByteRcvd = numByteRcvd + size ;

            //noisePower = 0.0 ;
            sigPower = 0.0 ;

            TargetPacket sensorPkt = ((TargetPacket)msg.getPkt());
            //new TargetPacket(size, ((TargetPacket)msg.getPkt()).seqNum, ((TargetPacket)msg.getPkt()).data, ((TargetPacket)msg.getPkt()).getBody());

            for ( int i = 0 ; i < size ; i++ )
            {
                sigPower = sigPower + (sensorPkt.data[i]*sensorPkt.data[i]);
            }

            snr = sigPower / (2 * lastNoisePower) ;

            maxSNR = ( snr > maxSNR ) ? snr : maxSNR ; // get the max SNR

            bufIndex = bufIndex + size ;

            ForwardDataToSensorApp(((TargetPacket)msg.getPkt()).getBody()) ;
        }
        else
        {
            /* It is up to the application designer to determine in this case what to do with the encapsulated data in the TargetPacket */

            if ( numByteRcvd == 0 )
            {
                if ( rtimer != null )
                    cancelFork(rtimer) ;
                target_nid = msg.getTargetNid() ;
                target_X = msg.getXTarget();
                target_Y = msg.getYTarget();
                target_Z = msg.getZTarget();
                target_SeqNum = ((TargetPacket)msg.getPkt()).seqNum ;
                rtimer = fork(forkPort, "SENSOR_EVENT", wait_time);
            }

            int size = ((TargetPacket)msg.getPkt()).size ;
            numByteRcvd = numByteRcvd + size ;

            //noisePower = 0.0 ;
            sigPower = 0.0 ;

            TargetPacket sensorPkt = new TargetPacket(size, ((TargetPacket)msg.getPkt()).seqNum, ((TargetPacket)msg.getPkt()).data, ((TargetPacket)msg.getPkt()).getBody() );

            for ( int i = 0 ; i < size ; i++ )
            {
                sigPower = sigPower + (sensorPkt.data[i]*sensorPkt.data[i]);
            }

            snr = sigPower / (2 * lastNoisePower) ;

            maxSNR = ( snr > maxSNR ) ? snr : maxSNR ; // get the max SNR

            bufIndex = bufIndex + size ;
        }
    } // end dataArriveAtFromPhyPort

    /**
     * Called upon to determine how to process incomming data
     * @param data_
     * @param inPort_
    */
    protected synchronized void processOther(Object data_, Port inPort_)
    {
        String portid_ = inPort_.getID();

        if (portid_.equals(FROM_PHY_PORT_ID)) {
            if (!(data_ instanceof SensorAgentPhyContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            dataArriveAtFromPhyPort(data_);
            return;
        }
        else if (portid_.equals(FORK_PORT_ID))
        {
            String msg = new String ((String) data_);
            if ( msg.equals("SENSOR_EVENT") )
            {
                ForwardDataToSensorApp() ;
            }
            return;
        }
        super.processOther(data_, inPort_);
    }
}
