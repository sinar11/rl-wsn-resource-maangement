// @(#)SensorApp.java   12/2003
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

import drcl.data.*;
import drcl.comp.*;
import drcl.comp.Port;
import drcl.inet.mac.PositionReportContract;

import java.util.Vector;


/**
 * This class implements the sensor application layer. It is in charge of receiving information
 * about target nodes through the sensor protocol stack and ultimately sending periodic updates
 * down the wireless protocol stack. As long as the sensor is not the sink (i.e., data collector),
 * it forwards the data as unicast packets to the sink node.
 *
 *
 * @author Ahmed Sobeih
 * @version 1.0, 12/19/2003
 *
 * Modified by Nicholas Merizzi - April 2005.
*/
public class SensorApp extends drcl.net.Module {

    /* configure the ports
       New Port description (added by Nicholas Merizzi)
       1. setRoutePort: The port used to communicate with the routing table to modify routing entries
       2. wirelessPhyPort: Port added to obtain latest energy information from the lower hardware layers
                           (in this case the EnergyModel located in WirelessPhy.java */
    public static final String CONFIG_PORT_ID               = ".config";
    public static final String FROM_SENSOR_AGENT_PORT_ID    = ".fromSensorAgent";
    public static final String FROM_WIRELESS_AGENT_PORT_ID  = ".fromWirelessAgent";
    public static final String CPU_PORT_ID                  = ".cpu";
    public static final String SNR_PORT_ID                  = ".snr";
    public static final String SNR_EVENT                    = "SNR";
    public static final String ROUTE_TABLE_ID               = ".setRoute";
    public static final String WIRELESS_PHY_PORT_ID         = ".energy";

    public Port configPort               = addPort(CONFIG_PORT_ID, false);
    public Port fromSensorAgentPort      = addPort(FROM_SENSOR_AGENT_PORT_ID, false);
    public Port fromWirelessAgentPort    = addPort(FROM_WIRELESS_AGENT_PORT_ID, false);
	public Port cpuPort                  = addPort(CPU_PORT_ID, false);
	public Port setRoutePort             = addPort(ROUTE_TABLE_ID, false);
    public Port wirelessPhyPort          = addPort(WIRELESS_PHY_PORT_ID, false);


    public static int dropped_packets       = 0;    //keeps track of the number of dropped packets.
    public static final int UNICAST_UPDATE  = 2;    //the type of message that can be sent
	public static final int BYTE_FOR_ID 	= 2048; // number of bytes required for identification
	public static final int COHERENT	    = 0;    //if message SNR is above threshold
	public static final int NON_COHERENT	= 1;    //if message SNR is below threshold
	public static final int SUPPRESS	    = 2;    //packet type
	public static final boolean SUPPRESS_ON	= false; //do we buffer and wait on incoming sensor channel events before sending down

    ////////////////////////////
    //localization/directed diffusion related
    public static final int NUM_TARGET_NODES = 2 ;

    double X_shift =  0;
    double Y_shift =  0;

    int targets_LastSeqNum [] ;
    //////////////////////////

	/*if we are running in microAMPS mode where the application layer
    becomes in control of the application layer and transmissions are
    made based on the distance between the T-R (transmitter-Receiver)*/
    public boolean is_uAMPS = false;

	public double cpuEventTime ;
	public int cpuMode ;
	public double coherentThreshold;
	public double lastSeenSNR;
	public int lastSeenDataSize;
	public ACATimer rTimer ;
	public int running_ ;
	public int eID ;

    /* id of the sensor node. added for cosmotic reasons
    only: namely to make System.out.println() statements*/
	public long nid ;

    /* id of the sink node to which collected data about target
    nodes should be sent along the wireless protocol stack */
	public long sink_nid;

    /* added for cosmotic reasons only: namely to make a graph of
    the data pertaining to each target. This assumes that the targets
    are the high-numbered nodes in the simulation starting from first_target_nid */
	public long first_target_nid;

    /*Various Variables to keep track of various information about the sensor*/
    public boolean sensorDEAD = false;          //boolean that is set to true iff RADIO_OFF or CPU_OFF
    public double sensorDEADAT = -1;            //at what time the sensor died at
    public double[] myPos=new double[3];        //The actual position of node
    public double[] sinkPos = new double[3];    //the location of the sink node

    public static int nn_;
    public final static double bandwidth_ = 1e6;       //By default make all radios 1Mb

    /* Global information required for One-Hop TDMA scheduling. Note that
    the size of the packet here is actually larger then what is really sent. This
    allows us to have guards between sends so that no information is loss due to
    overlapping scheduling*/
    public static Vector schedule = new Vector(nn_);
    public static double new_schedule_adv_TDMA;
    public static double frame_time_TDMA;
    public static final double  gap_time_TDMA     = 10.0;
    public static int    hdr_size_TDMA            = 25;   //Bytes for header
    public static int    sig_size_TDMA            = 175;  // Bytes for data signal
    public final  double  slot_time_TDMA          = txtime(hdr_size_TDMA+sig_size_TDMA);//Packet transmission time
    public static double start_time_TDMA;
    public static double sensor_wakeup_time_TDMA;
    public static double buffer_TDMA = 0.5;

   /*******************************************************************************/

	/**
     * Creates ports on which the sink node generates collected information
    */
	public void createSnrPorts(int node_num, int target_node_num) 
	{
        first_target_nid = node_num - target_node_num ;

	    for ( int i = 0; i < target_node_num; i ++ ) {
		    addEventPort(SNR_PORT_ID + i);
        }
    }

    /**
     * Main constructor for application layer of any sensor.
    */
	public SensorApp ()
    {
		super();
		cpuEventTime = 0.0 ;
		cpuMode = 1 ;
		coherentThreshold = 20.0 ; 
		lastSeenSNR = 0.0;
		lastSeenDataSize = 0;
		nid = Integer.MIN_VALUE ; // assuming no node has id Integer.MIN_VALUE
		sink_nid = Integer.MIN_VALUE ; // assuming no node has id Integer.MIN_VALUE
		first_target_nid = Integer.MIN_VALUE ; // assuming no node has id Integer.MIN_VALUE
		rTimer = null ;
		running_ = 0 ;
		eID = 0 ;
		setEventExportEnabled(true);
        targets_LastSeqNum = new int [NUM_TARGET_NODES] ;
		for ( int i = 0 ; i < NUM_TARGET_NODES ; i++ )
		{
			targets_LastSeqNum[i] = -1 ;
		}
	}

    /**
     * Gets whether or not we are running in uAMPS mode. If so
     * then we will allow the application layer to have control
     * over the hardware layers.
     * @return
    */
    public boolean isIs_uAMPS() { return is_uAMPS; }


    /**
     * Sets whether or not we are running in uAMPS mode. If so
     * then we will allow the application layer to have control
     * over the hardware layers.
     * @param is_uAMPS
    */ 
    public void setIs_uAMPS(boolean is_uAMPS) { this.is_uAMPS = is_uAMPS; }


    /**
     * Sets the total number of nodes in Network
    */
    public void setNn_(int nn) { nn_ = nn;  }


    /**
     * Getter for obtaining the total number of nodes in network
     * @return number of nodes in network
    */
    public static int getNn_(){ return nn_; }


    /**
     * Calculate the transmission time of given bytes.
     * while calling DATA_Time(), len_ has already counted the extra
     * overhead bits in. Note 1 byte = 8 bits.
     * @param len_ the length of the packet
     * @return the time required to transmit/receive the data (in seconds)
    */
    public double txtime(int len_){ return 8 * len_ / bandwidth_; }


    /**
     * Determines the Nodes exact location.
    */
    public void getLocation()
    {

        // get most up-to-date location from SensorMobilityModel
		PositionReportContract.Message msg = new PositionReportContract.Message();
		msg = (PositionReportContract.Message) wirelessPhyPort.sendReceive(new DoubleObj(-1));

        //update my position info
        this.myPos[0] = msg.getX();
        this.myPos[1] = msg.getY();
        this.myPos[2] = msg.getZ();
    }

    /**
     * Obtain the X coordinate of the Sensor
     * @return  X coordinate
    */
    public double getX() { return(this.myPos[0]); }

    /**
     * Obtain the Y coordinate of the Sensor
     * @return Y coordinate
    */
    public double getY(){ return(this.myPos[1]); }

    /**
     * Obtain the Z coordinate of the Sensor
     * @return  Z coordinate
    */
    public double getZ(){ return(this.myPos[2]); }

    /**
     * Gets the number of Packets that were sent from this Sensor
     * @return
    */
    public int geteID(){ return eID; }

    /**
     * Increments the event ID which is the unique identifier that
     * is associated with every packet transmission.
    */
    public void incrementEID() { eID ++; }


    public void setXshift(double x_shift_) { X_shift = x_shift_; }

    public void setYshift(double y_shift_) { Y_shift = y_shift_; }

    protected double round_digit(double x, int digit)
    {
        int Y = 1;
        for (int i =0; i < digit; i++) Y = Y * 10;
        return ((double)Math.round((x ) * Y))/Y;
    }

    /**
     * A method that strictly prints this sensors location
    */
    public void printNodeLoc()
    {
        //Obtaining Most up-to-date location
        this.getLocation();
        System.out.println("Sensor"+this.nid+" Location: ("+this.myPos[0]+", " + this.myPos[1]+", "+this.myPos[2]+")");
    }


    /**
     * Method to check if sensor is dead or not.
    */
    public boolean isSensorDead() { return(sensorDEAD); }


    /**
     * sets the sink position. Default is (0,0,0) which is what the
     * constructor sets it to. Should be called from TCL to be set o.w.
     * @param xLoc
     * @param yLoc
     * @param zLoc
    */
    public void setSinkLocation(double xLoc, double yLoc, double zLoc)
    {
        this.sinkPos[0] = xLoc;
        this.sinkPos[1] = yLoc;
        this.sinkPos[2] = zLoc;
    }


    /**
     * Returns the Name of the object
     * @return name of class
    */
    public String getName(){ return "SensorApp";}


    /**
     * Sets the sensors ID
     * @param nid_ the ID of the sensor
    */
    public void setNid(long nid_) {  nid = nid_; }


    /**
     * Get the ID of the sensor
     * @return sensor ID
     */
    public int getNid()  { return((int)this.nid); }

	/**
     * Sets the ID of the sink node to which information should be forwarded
    */
	public void setSinkNid(long sink_nid_) { sink_nid = sink_nid_; }

    /**
     * Set threshold at which the sensor could no longer comprehend signals
     * @param coherentThreshold_
    */
    public void setCoherentThreshold(double coherentThreshold_) { coherentThreshold = coherentThreshold_; }

    /**
     * Get threshold at which the sensor could no longer comprehend signals
     * @return
    */
    public double getCoherentThreshold() { return  coherentThreshold; }

    /**
     * Allows for duplicating of this object at the current time of call.
     * @param source_ the source object of that you are duplicating
    */
    public void duplicate(Object source_)
    {
        super.duplicate(source_);
        SensorApp that_ = (SensorApp) source_;
	    cpuEventTime = that_.cpuEventTime;
        cpuMode = that_.cpuMode;
	    coherentThreshold = that_.coherentThreshold;
        nid = that_.nid;
        sink_nid = that_.sink_nid;
        first_target_nid = that_.first_target_nid;
        lastSeenSNR = that_.lastSeenSNR ;
        lastSeenDataSize = that_.lastSeenDataSize ;
        running_ = that_.running_ ;
        eID = that_.eID ;
    }

	/**
     *   This Method is called by this.processOther() when
     *   incomming information arrives from the CPU Model. The
     *   only information from the CPU model is the mode that
     *   was changed. So to keep insink with the CPU model
     *   we update our field values.
    */
    protected synchronized void cpuModeChanged(int mode)
    {
        cpuEventTime = this.getTime() ;
        cpuMode = mode ;
    }


    /**
     * Number of packets that were dropped (normally
     * because of lack of energy)
     * @return
    */
    public int getDropped_packets() { return dropped_packets; }


	/**
     * Sets the CPU mode
    */
	public synchronized void setCPUMode(int mode)
    {
        this.cpuMode = ((IntObj)cpuPort.sendReceive(new IntObj(mode))).intValue();
	}


	/**
     * Handles information received over the sensor channel
     * deals with receiving information from the contract that
     * binds SensorApp with SensorAgent
     *
     * Should be overridden depending on the task at hand.
     *
    */
    protected synchronized void recvSensorEvent(Object data_)
    {
	    SensorAppAgentContract.Message msg = (SensorAppAgentContract.Message) data_;
 	    lastSeenSNR = msg.getSNR();
	    lastSeenDataSize = msg.getDataSize();
	    long target_nid = msg.getTargetNid();
	    double target_X = msg.getTargetX() ;
	    double target_Y = msg.getTargetY() ;
	    double target_Z = msg.getTargetZ() ;
	    int target_SeqNum = msg.getTargetSeqNum() ;

        //If this sensor is not the sink then do not do the following.
        //remember that both sinks (BS) and sensors have this layer so
        //certain distinguishing factors need to be taken into account.
	    if ( nid != sink_nid ) {
            //silently pass it on down the wireless stack to be sent to the sink
	    }
	    else
	    {     //else this node is the basetation
		    Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(target_nid - first_target_nid));
		    if ( snrPort != null )
			    if ( snrPort.anyOutConnection() )
				    snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)lastSeenSNR), null);
	    }

	    if (SUPPRESS_ON )
	    {
		    /* it is up to the application designer to see if this timer makes
            sense if there are more than one target in the simulation. In that case,
            the data included in a sensor packet might not be pertaining to the target_nid
            of that packet. In future releases, we can solve this problem by keeping a
            buffer of SensorPackets to be transmitted. */
		    rTimer = setTimeout("SendPacket", (5.0*Math.log(10.0))/Math.log(lastSeenSNR));
		    running_ = 1 ;
	    }
	    else
	    {
		    if ( lastSeenSNR > coherentThreshold )
		    {
			    if ( nid != sink_nid )
			    {
				    downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, (int)(100), COHERENT, lastSeenSNR, eID, target_nid, target_X, target_Y, target_Z, target_SeqNum, msg.getBody())) ;
				    eID = eID + 1 ;
			    }
		    }
		    else
		    {
			    if ( nid != sink_nid )
			    {
				    downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, lastSeenDataSize, NON_COHERENT, lastSeenSNR, eID, target_nid, target_X, target_Y, target_Z, target_SeqNum, msg.getBody())) ;
				    eID = eID + 1 ;
			    }
		    } // end if ( lastSeenSNR > coherentThreshold )
	    } // end if ( SUPPRESS_ON )
    }


   	/**
     * Handles information received over the wireless channel
     * This function is called whenever a packet (SensorPacket) is
     * received through the wireless protocol stack
    */
    protected synchronized void recvSensorPacket(Object data_)
    {
	    SensorPacket spkt = (SensorPacket) data_;
        System.out.println("Node" +this.nid + " has received info over the wireless channel (in SensorApp)");

	    if ( spkt.pktType == SUPPRESS )
	    {
		    if ( (running_==1) && (rTimer != null) )
    			cancelTimeout(rTimer);
		    running_ = 0;
	    }
	    else if ( (spkt.pktType == COHERENT) || (spkt.pktType == NON_COHERENT) ) {
		    if ( nid != sink_nid ){
            }
            else {
			    lastSeenSNR = spkt.getMaxSnr();
			    lastSeenDataSize = spkt.getDataSize();

			    Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(spkt.getTargetNid() - first_target_nid));
			    if ( snrPort != null )
				    if ( snrPort.anyOutConnection() )
				    {
					    int TargetIndex = (int)(spkt.getTargetNid() - first_target_nid) ;
					    if ( targets_LastSeqNum[TargetIndex] < spkt.getTargetSeqNum() )
					    {
        	                double target_location [] ;
        	                target_location = new double [2] ;
        	                target_location[0] = round_digit(spkt.getTargetX() - X_shift, 4) ;
        	                target_location[1] = round_digit(spkt.getTargetY() - Y_shift, 4) ;
        	                snrPort.exportEvent(SNR_EVENT, target_location, null); // uncomment this line if you want to display the location of the target node.

						    snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)spkt.getMaxSnr()), null);

						    targets_LastSeqNum[TargetIndex] = spkt.getTargetSeqNum() ;
					    }
				    }
		    }
	    }
    }


    /**
     * To handle incomming information from various ports.
     * @param data_
     * @param inPort_
    */
    public synchronized void processOther(Object data_, Port inPort_)
    {
        String portid_ = inPort_.getID();

        if (portid_.equals(CPU_PORT_ID)) {
            if (!(data_ instanceof IntObj)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            cpuModeChanged(((IntObj)data_).getValue());
            return;
        } else if (portid_.equals(FROM_SENSOR_AGENT_PORT_ID)) {
            if (!(data_ instanceof SensorAppAgentContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            //if the incoming data is of type SensorAppAgentContract then send it
            //to the proper handling method
	        recvSensorEvent(data_);
            return;
        } else if (portid_.equals(FROM_WIRELESS_AGENT_PORT_ID)) {
            if (!(data_ instanceof SensorPacket)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            //if the incoming data is a SensorPacket then its coming up the
            //wireless stack so send it to the appropriate stack.
	        recvSensorPacket(data_);
            return;
        }
        super.processOther(data_, inPort_);
    }


    /**
     * Handles expired timers
     * @param data_
    */
    protected synchronized void timeout(Object data_)
    {
	    if ( data_.equals("SendPacket"))
		{
			running_ = 0 ;

			if ( lastSeenSNR > coherentThreshold )
			{
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, SUPPRESS, lastSeenSNR, eID, null)) ; /* Depending on the application, the application designer may override this function to replace the null with other application-specific data */

				if ( nid != sink_nid )
				{
					downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, (int)(100), COHERENT, lastSeenSNR, eID, Integer.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Integer.MIN_VALUE, null)) ; /* because we set a timer, the target information was lost, a temporary solution to pass Integer.MIN_VALUE. Depending on the application, the application designer may override this function to replace the null with other application-specific data. */
					eID = eID + 1 ;
				}
			}
			else
			{
				if ( nid != sink_nid )
				{
					downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, lastSeenDataSize, NON_COHERENT, lastSeenSNR, eID, Integer.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Integer.MIN_VALUE, null)) ; /* because we set a timer, the target information was lost, a temporary solution to pass Integer.MIN_VALUE. Depending on the application, the application designer may override this function to replace the null with other application-specific data. */
					eID = eID + 1 ;
				}
			} // end if ( lastSeenSNR > coherentThreshold )
	    }
	}
}
