package drcl.inet.sensorsim.MultiHop;

import drcl.comp.Port;
import drcl.comp.ACATimer;
import drcl.inet.mac.EnergyContract;
import drcl.inet.data.RTKey;
import drcl.inet.data.RTEntry;
import drcl.inet.contract.RTConfig;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.data.DoubleObj;

import java.util.Random;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/09/2005
 *
 * Brief Background on the Multi-Hop Method:
 * Since sensor nodes have limited radio capabilities some sensors due to their
 * distance will be unable to directly send to the base station.  Therefore, in
 * order to increase the field one wants to monitor multi-hop schemes are recommended
 * for several reasons:
 *      1.	Power dissemination is equal to distance squared therefore keeping the
 *          distance of the traveling packets low will result in greater energy savings.
 *      2.	Due to problems such as line of sight or interference having a node send
 *          simply to close-by neighbors strengthen the signal to noise ratio.
 * Along with these advantages over the direct method a new problem creeps in over time
 * with this model.  It is known as the black hole effect where nodes close to the base
 * station die out from constantly acting as routers.  Due to the nature of the multi-hop
 * scheme all distant nodes eventually forward their traffic to a closer neighbor.  Always
 * using the same lowest energy route will drain the nodes on that route extremely quickly.
 *
 * Specific Information Regarding MultiHopApp:
 * This class is the application layer for a multi-hop capable sensor. This layer
 * allows the sensor to generate packets to be sent to the base station or act
 * as a forwarding agent to pass packets down the chain if it was selected as
 * another sensors neighbor. The neighbor selection and updating takes place from the
 * Tcl script.
 *
*/

public class MultiHopApp extends SensorApp implements drcl.comp.ActiveComponent
{

    public static final String SENSOR_NODE_POS_PORT = ".getNeighbor"; // added to support multihop queries for next closest node
    public Port SensorNodePositionPort     = addPort(SENSOR_NODE_POS_PORT);

    /*To collect and display energy levels in graph form. Created a
    port that will output to a plotter*/
    public static final String ENERGY_EVENT     = "Remaining Energy";
    public static final String REM_ENERGY_PORT_ID  = ".plotter";
    public Port plotterPort = addEventPort(REM_ENERGY_PORT_ID);

    //The Final Destination Information
    private double distanceToSink;      //how far the sink is.

    //The following is to keep track of who and where our neighbor is at all times.
    private double[] neighborPos;       //position of neighbor
    private long neighbor_id;           //who do I send to next on the chain.
    private double neighbor_dist;       //distance to neighbor

    ACATimer sendTimer;                 //timer for periodic sending.
    ACATimer CPUactive_timer;           //timer for how long the CPU should remain active for
    ACATimer CPUidleUpdate_;               //timer to continuously update CPU idle/sleep energy

    protected double bandwidth = 1e6;   //1 Mbps radio speed
    protected double sig_size =  150;   //Bytes for data signal
    protected double hdr_size = 25;     //Bytes for header

    int p_size = 175;                   //final packet size after encapsulations
    final double cpu_electronics =5e-6; //how much extra processing time it takes to send a packet (made up?)
    protected double processingTime = txtime(p_size) + cpu_electronics;

    //Random offset for mte-- needed to make sure nodes do not all transmit at
    //same time.  Otherwise, CSMA fails.
    protected double ra_mte = 0.01;

    /*when the sensor reaches a certain amnt of remaining energy it should stop
    trying to generate new packets but rather finish emptying out its queue and
    successfully delivering the packets that have already been generated-->This
    value should be changed depending on number of sensors in your simulation*/
    private double stop_threshold = 0.09; //joules

    /*The data size of the packet...
    total size is 175 after lower encapsulate it with their headers*/
    int dataSize = 97;

    /*Object to store the latest result the sensor
    has sensed. This object is inserted as the body
    of all outgoing messages*/
    Object phenomenon = new SensorAppAgentContract.Message();

    Random generator = new Random();

    //Latency between when nodes transmit their data.  Each data message takes
    //tmsg seconds and traverses sqrt(N) hops (on average).  There are N
    //messages (1 per node).  Therefore, data_lag = N * sqrt(N) * tmsg.
    // = generator.nextDouble()+((double)nn_ * Math.sqrt((double)nn_)* 8 * sig_size * hdr_size + 75) / bandwidth;
    protected double data_lag = generator.nextDouble()*6 + 7;

    {
		removeDefaultUpPort() ;
	}


    ///////////////////////////////////////////////////////////////////////
    /*The following is strictly for analytically comparing
      simulations. It will be removed in future versions once
      the code has been verified thouroughly -
      only called from Tcl validation scripts */
    public void setSendTime (double data_lag_) {
        data_lag = data_lag_;
        System.out.println("Sensor"+this.nid+ " will now send every: " + data_lag + "seconds");
        return;
    }
    ////////////////////////////////////////////////////////////////////////


    /**
     * Constructor
    */
    public MultiHopApp()
    {
        super();
        this.myPos = new double[3];
        this.sinkPos = new double[3];
        this.neighborPos = new double[3];

        this.sink_nid = 0;
        this.setSinkLocation(0.0,0.0,0.0);  //set the sink's location to the default(0,0,0)
        this.sensorDEAD = false;            //mark the sensor as being alive
        neighbor_id = sink_nid;             //mark its next hop as the sink ID at startup.
        neighborPos[0] = 0.0; neighborPos[1] = 0.0; neighborPos[2] = 0.0;   //set where the neighbor is
    }

    /**
     * Prints out the neighbors ID to the terminal.
    */
    public void printNeighborID()
    {
        System.out.println("Sensor"+this.nid+" Neighbor is: " + this.neighbor_id);
    }


    /**
     * Method to check if sensor is dead or not.
    */
    public synchronized boolean isSensorDead()
    {
        double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();

        if ((energy == 0.0) & (!this.sensorDEAD)){
            this.sensorDEADAT = getTime();
            System.out.println("Sensor"+this.nid+ " is dead at " + this.sensorDEADAT);
            this.sensorDEAD = true;
            this._stop();
            nn_=nn_-1;  //fewer total nodes now
        }
        return(sensorDEAD);
    }

    /**
     * Calculates the distance between 2 points in 3D space.
     * @return The distance
    */
    protected double EuclideanDist(double X, double Y, double Z,double X2, double Y2, double Z2)
    {
        double dx = X2 - X;
        double dy = Y2 - Y;
        double dz = Z2 - Z;
        return(Math.sqrt((dx*dx) + (dy*dy) + (dz*dz)));
    }

    /**
     * _start()
     * This method is called when attempting to 'run' the component in TCL.
    */
    protected void _start ()
    {
        //set the CPU to sleep till we do something.
        if (this.cpuMode != 1) {
            this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        }

        this.getLocation();     //determine the most up-to-date position

        //calculate how far the sink is
        this.distanceToSink = this.EuclideanDist(this.sinkPos[0], this.sinkPos[1], this.sinkPos[2],this.myPos[0], this.myPos[1],this.myPos[2]);

        //start the periodic querying of the remaining energy
        rTimer = setTimeout("getEnergy", 2);

        //start sending
        sendTimer = setTimeout("SendMyData", data_lag);

        //to continuously update CPU status
        CPUidleUpdate_ = setTimeout("updateCPUEnergy", 1.5);
    }


    /**
     * _stop()
     *  This cancels the components timer ultimately stopping the periodic
     *  sending back to the sink node.
    */
	protected void _stop()
    {
        if (rTimer != null)
            cancelTimeout(rTimer);
        if (sendTimer != null)
            cancelTimeout(sendTimer);
        if (CPUidleUpdate_ != null)
            cancelTimeout(CPUidleUpdate_);
        if (CPUactive_timer != null)
            cancelTimeout(CPUactive_timer);
	}

    /**
     * If the Component was stopped this will resume its periodic sending
    */
	protected void _resume()
    {
        rTimer = setTimeout("getEnergy", 4);
        sendTimer = setTimeout("SendMyData", data_lag);
    }

    /**
     * Prints various Information on the sensor.
    */
    public String toString()
    {
        String nodeStatus = new String();
        nodeStatus = "\n";
        nodeStatus.concat("Sending Node:  " + this.nid);
        nodeStatus.concat("Sensor Location: ("+this.myPos[0]+", " + this.myPos[1]+", "+this.myPos[2]+")");
        nodeStatus.concat("Sink Location: ("+this.sinkPos[0]+", " + this.sinkPos[1]+", "+this.sinkPos[2]+")");
        nodeStatus.concat("Packet #: " + eID);
        nodeStatus.concat("CPU Mode: " + this.cpuMode);
        nodeStatus.concat("Neighbor ID: " + neighbor_id);
        nodeStatus.concat("Neighbor Distance: "+ neighbor_dist);
        nodeStatus.concat("Distance From Sink: "+ distanceToSink);
        nodeStatus.concat("\n");
        return(nodeStatus);
    }

    /**
     *  This method is invoked when the components timer reaches zero.
     *  @param data_
    */
    protected synchronized void timeout(Object data_)
    {
        if ( data_.equals("getEnergy") && (!this.isSensorDead())) {
            // Contract type: ENERGY_QUERY = 0
            double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();
            if ((energy == 0.0) & (!this.sensorDEAD)) {
                this.sensorDEADAT = getTime();
                System.out.println("Sensor"+this.nid+ " is dead at " + this.sensorDEADAT);
                this.sensorDEAD = true;
                this._stop();
                nn_=nn_-1;  //fewer total nodes now
            } else {
                if (plotterPort.anyOutConnection()) {
                    plotterPort.exportEvent(ENERGY_EVENT, new DoubleObj(energy), null);
                }
                rTimer = setTimeout("getEnergy", 2);
            }
            return;
        }

        if (data_.equals("SendMyData") && (!this.isSensorDead())) {
            SendMyData();             //call the handling function
            double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();
            if (energy >= stop_threshold) {
                sendTimer = setTimeout("SendMyData", data_lag);
            }
            return;
        }

        if (data_.equals("CPUactive")) {
            //put CPU back to sleep
            this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
            return;
        }

        if (data_.equals("updateCPUEnergy") && (!this.isSensorDead())) {
            this.setCPUMode(this.cpuMode);
            CPUidleUpdate_ = setTimeout("updateCPUEnergy", 0.1);
            return;
        }

        return;
    }

    /**
     * Find next hop neighbor-- node to whom always send data.
     * Choose closest node that is in the direction of the base station.
     * NOTE!  This algorithm assumes nodes know the location of all nodes
     * near them.  In practice, this would require an initial set-up
     * phase where this information is disseminated throughout the network
     * and that each node has a GPS receiver or other location-tracking
     * algorithms to determine node locations.
    */
    public synchronized void setNeighbor ()
    {
        NeighborQueryContract.Message msg = (NeighborQueryContract.Message)SensorNodePositionPort.sendReceive(new NeighborQueryContract.Message(nid,myPos,sinkPos,distanceToSink));
        this.neighbor_id    = msg.getneighbor_nid();
        this.neighborPos[0] = msg.getNeighbor_nidX();
        this.neighborPos[1] = msg.getNeighbor_nidY();
        this.neighborPos[2] = msg.getNeighbor_nidZ();
        this.neighbor_dist  = msg.getNeighbor_dist();
        return;
    }


    /**
     * Handles information received over the wireless channel
     * @param data_
    */
	public synchronized void recvSensorPacket(Object data_)
    {
		if ( data_ instanceof SensorPacket ) {
		    SensorPacket spkt = (SensorPacket)data_ ;
			SendDataNextHop(spkt); //received a packet from up the chain pass it on so it reaches sink
            return;
		} else {
			super.recvSensorPacket(data_) ;
        }
    }


    /**
     * Handles information received over the sensor channel
     * deals with receiving information from the contract that
     * binds SensorApp with SensorAgent
    */
    protected synchronized void recvSensorEvent(Object data_)
    {
        SensorAppAgentContract.Message msg = (SensorAppAgentContract.Message) data_;
        lastSeenSNR = msg.getSNR();
        lastSeenDataSize = msg.getDataSize();
        //long target_nid = msg.getTargetNid();

        //System.out.println("Sensor" +this.nid + " has received info over the sensor channel from target node " + target_nid);

        //we received newly sensed information so update our field which holds
        //the current measurements to be next sent to the base station.
        phenomenon = data_;
        return;
    }

    /**
     * Send data once every data_lag seconds.  This is set so that
     * there are minimal collisions among data messages.  If data_lag is too
     * small, no data is transmitted due to collisions.  If data_lag is
     * too large, the channel is not efficiently used.
    */
    protected synchronized void SendMyData()
    {
        //System.out.println("Sensor"+this.nid + " sending UNICAST Packet to neighbor: Sensor" + this.neighbor_id + " at time: " + getTime() + "Its eID is: " + eID);

        //make sure the CPU is awake
        if (this.cpuMode != 2) {
            this.setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
            CPUactive_timer = setTimeout("CPUactive", processingTime);
        }

        //add a route in the table for our reply (40 second timeout)
        addRoute(this.nid,this.neighbor_id,-1);

        //create a new bcast object
        MH_Packet newPacket = new MH_Packet(this.nid, this.myPos, getTime(), dataSize, phenomenon);


        if (!isSensorDead())  {
            double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();
            if (energy < stop_threshold) {
                cancelTimeout(sendTimer);  //stop it early in the MH so that it can still fwd upstream packets
                return;
            }
            else {
                if ( lastSeenSNR > coherentThreshold ) {
                    downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, neighbor_id, this.nid, this.neighborPos, dataSize, COHERENT, eID, ((SensorAppAgentContract.Message)phenomenon).getTargetNid(), newPacket));
                    eID = eID + 1;          //every message sent is associated with an event ID
                } else {
                    downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, neighbor_id, this.nid, this.neighborPos, dataSize, NON_COHERENT, eID, ((SensorAppAgentContract.Message)phenomenon).getTargetNid(), newPacket));
                    eID = eID + 1 ;
                }
            }
        }
        return;
    }


    /**
     * This method is called upon when a sensor receives a packet up the
     * wireless channel from another sensor in the field. This means that it
     * was selected as someone's neighbor. If energy permitting (i.e. if its not dead)
     * forward the packet on.
     * @param spkt
    */
    protected synchronized void SendDataNextHop(SensorPacket spkt)
    {
        //make sure the CPU is awake
        if (this.cpuMode != 2) {
            this.setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
            CPUactive_timer = setTimeout("CPUactive", processingTime);
        }

        //System.out.println("Sensor" + this.nid + "received a UNICAST packet from sensor"+ spkt.getSrc_nid() + " and is now forwording it to the sink");

        //add a route in the table for our reply (15 second timeout)
        addRoute(this.nid,this.neighbor_id,-1);

        //create a new unicast packet to fwd packet onwards
        MH_Packet newPacket = new MH_Packet(this.nid, this.myPos,((MH_Packet)spkt.getBody()).getSendTime(), ((MH_Packet)spkt.getBody()).getSize(), ((MH_Packet)spkt.getBody()).getBody());

        if (isSensorDead())  {
            //drop the packet that was received since this sensor has no energy to fwd it.
            dropped_packets ++;
        }
        else {
            //Fwd the packet on since this sensor has the energy left to do so.
            downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, this.neighbor_id, spkt.getSrc_nid(), this.neighborPos, ((MH_Packet)spkt.getBody()).getSize(), UNICAST_UPDATE, spkt.getEventID(), this.nid, newPacket)) ;
        }
        return;
    }

    /**
     *  To add a route send a request with the following format:
     *      Command:Int, Key:RTKey, Entry:RTEntry, Lifetime:double
     *       where:
     *           Command  -> 0:add, 1: graft, 2: prune
     *           Key      -> The key of the route entry
     *           Entry    -> The route entry to be added
     *           Lifetime -> The lifetime of the entry. Note if negative
     *                       number will keep entry valid until explicitly
     *                       removed.
     *
     *   RTConfig.add(key_, new RTEntry(nexthop_, new drcl.data.BitSet(new int[]{interfaces}),
	 *		entryExtension_), timeout, rtconfigPort);
     *
    */
    public void addRoute(long src_nid_, long dst_nid_, int timeout_)
    {
        int type = 0;
        int timeout = timeout_; //do not want it to automatically pruned therefore non-positive #
        //RTKey key = new RTKey(this.nid, sink_nid, -1);   -->just goes directly to sink not what we want
        RTKey key = new RTKey(src_nid_, dst_nid_, timeout);
        RTEntry entry = new RTEntry(new drcl.data.BitSet(new int[]{0}));

        /*  connect to the port and send the message based on the RTConfig
            contract settings which are:
                RTConfig.Message (int type_, RTKey key_, RTEntry entry_, double timeout_)  */
        setRoutePort.sendReceive(new RTConfig.Message(type, key, entry, timeout));
    }

    /**
     * Overrides the method in SensorApp and is responsible for accepting events
     * on incomming ports.
     * @param data_
     * @param inPort_
    */
    public synchronized void processOther(Object data_, Port inPort_)
    {
        String portid_ = inPort_.getID();
        if (portid_.equals(SENSOR_NODE_POS_PORT)) {
            //since all sensors connect to the same one port in the nodeTracker the
            //sensors will overhear responses sent by other nodes... So if we aren't in
            //the setNeighbor() method we should be ignoring incomming data as it does
            //not belong to us.
            return;
        }
        else {
            super.processOther(data_, inPort_);
        }
    }

    /*---------------Observers and setters--------------*/
    public long getNeighbor_id()
    {
        return neighbor_id;
    }

    public double[] getNeighborPos()
    {
        return neighborPos;
    }

    public double getNeighbor_dist()
    {
        return neighbor_dist;
    }

    public double getNeighborX()
    {
        return neighborPos[0];
    }

    public double getNeighborY()
    {
        return neighborPos[1];
    }

    public double getNeighborZ()
    {
        return neighborPos[2];
    }

    public void setNewNeighborX(double newX)
    {
        this.neighborPos[0] = newX;
    }

    public void setNewNeighborY(double newY)
    {
        this.neighborPos[0] = newY;
    }

    public void setNewNeighborZ(double newZ)
    {
        this.neighborPos[0] = newZ;
    }

    public void setNewNeighborDist(double setNewNeighborDist)
    {
        this.neighbor_dist = setNewNeighborDist;
    }

    public void setNewNeighborID(int newID)
    {
        this.neighbor_id = newID;
    }

    /**
     *
     * @return
    */
    public String getName()
    {
        return "MultiHopApp";
    }

}

