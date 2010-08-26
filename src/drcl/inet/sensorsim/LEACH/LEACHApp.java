package drcl.inet.sensorsim.LEACH;

import drcl.comp.Port;
import drcl.comp.ACATimer;
import drcl.inet.data.RTKey;
import drcl.inet.data.RTEntry;
import drcl.inet.contract.RTConfig;
import drcl.inet.mac.EnergyContract;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.data.DoubleObj;

import java.util.Random;
import java.util.Vector;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/29/2005
 *
 * Brief Background on LEACH:
 * The following is an extract from a conference procedings written by the author of LEACH.
 * Ref: W.R. Heinzelman, A. Chandrakasan, and H. Balakrishnan, "Energy-efficient communication
 *      protocol for wireless micro networks," in IEEE Proceedings of the Hawaii International
 *      conference on System Sciences 2000 pp1-10.
 *
 * LEACH (Low-Energy Adaptive Clustering Hierarchy), a clustering-based protocol that utilizes
 * randomized rotation of local cluster base stations (cluster-heads) to evenly distribute the
 * energy load among the sensors in the network. LEACH uses localized coordination to enable
 * scalability and robustness for dynamic networks, and incorporates data fusion into the routing
 * protocol to reduce the amount of information that must be transmitted to the base station.
 *
 * Specific Information Regarding LEACHApp:
 * This j-sim implementation was ported from an implementation the author did on
 * an old version of (ns2.1b5). This class represents the application level that all sensors
 * will have running. It performs all the core duties of turning on and off the radio and
 * determines the course action during the setup phases which continually occur.
*/

public class LEACHApp  extends SensorApp implements drcl.comp.ActiveComponent
{
    //macSensorPort: Port that connects the LEACHApp with the MacSensor layer in order to correlate the SS code.
    public static final String MAC_SENSOR_PORT_ID   = ".macSensor";
    public Port macSensorPort            = addPort(MAC_SENSOR_PORT_ID, false);

    /*To collect and display energy levels in graph form. Used to output to a plotter*/
    public static final String ENERGY_EVENT     = "Remaining Energy";
    public static final String PLOTTER_PORT_ID  = ".plotter";
    protected Port plotterPort = addEventPort(PLOTTER_PORT_ID);

    //Message Constants
    public static final int LEACH_ADV_CH    = 0;
	public static final int LEACH_JOIN_REQ  = 1;
    public static final int LEACH_ADV_SCH   = 2;
    public static final int LEACH_DATA      = 3;

    public static final int BYTES_ID        = 2;

    //opt Variables from ns2
    protected static int nn_;                           //Number of non-BS nodes
    protected static int num_clusters = 3;              //Total number of clusters
    protected static int total_rounds;                  //Total number of rounds
    protected static boolean eq_energy = true;          //if all nodes start with equal energy
    protected static int spreading = (int)(1.5*num_clusters)+1;   //the spreading factor
    protected static double ch_change = 20.0;           //changing clusters every 20 seconds
    protected static double setup_time = 5.0;           //amnt of time spent in setup phase.
    protected final static int bsCode = 0;              //spreading code for BS

    protected final static int hdr_size = 25;         //Bytes for header
    protected final static int sig_size = 170;        // Bytes for data signal
    protected final double slot_time = txtime(hdr_size+sig_size);//Packet transmission time
    protected final double ss_slot_time = slot_time * (double)spreading;  //Spread-spectrum packet transmission time
    protected double frame_time = ss_slot_time * nn_;       //Maximum TDMA frame time (if all nodes in one cluster)


    protected double ra_adv = txtime(hdr_size+ 4);                  //RA Time (s) for CH ADVs
    protected double ra_adv_total = ra_adv*((double)num_clusters*4 + 1);    //Total time (s) for CH ADVs; Assume max 4(nn*%) CHs
    protected double ra_join = 0.01 * (double)nn_;                  //RA Time (s) for nodes' join reqs
    protected double ra_delay = txtime(hdr_size+ 4);                //Buffer time for join req xmittal
    protected double xmit_sch = 0.005 +(txtime(nn_*4+hdr_size));    //Maximum time required to transmit a schedule (n nodes in 1 cluster)
    protected double start_xmit = ra_adv_total+ra_join+xmit_sch;    //Overhead time for cluster set-up

    protected boolean isch_;                    //whether or not it is currenly a cluster head
    protected boolean hasbeench_;               //has it been a cluster head recently
    protected double next_change_time_;         // = ch_change + getTime() -->determines next election time for the current node
    protected int round_;                       //the current round

    protected Vector clusterChoices = new Vector(); //used to keep track of possible CH
    protected Vector joinedNodes = new Vector();    //used to keep track of the IDs of each sensor in the cluster (called clusterNodes in NS2 implementation)
    protected long currentCH;                       //who the current cluster head is for this sensor
    protected double[] CHloc;                       //Location of the CH

    protected double xmitTime_;     //when a nodes TDMA time begins
    protected double end_frm_time_; //when a nodes TDMA time slot is over
    protected Vector TDMAschedule_ = new Vector();  //for a CH to keep track of the sending order (i.e. timeslot ordering)
    protected double dist_;
    protected int code_ = 0;            //Spread Sprectrum code.

    protected int myADVnum_;
    protected Vector receivedFrom_ = new Vector();  //if a CH keep track of who you have received from

    protected Vector CHData = new Vector();     //used for CH to keep track of all the data they have received.
                                                //it will be a vector of Data_Report from all the sensors in its Cluster

    //the following is a variable that will keep track of how often
    //a cluster elects itself a CH throughout its lifetime. (this is optional)
    //used for data analysis
    protected int total_times_CH = 0;

    //a static counter which will keep track of how many CH existed throughout the
    //whole simulation
    static int global_total_CH = 0;

    Random generator = new Random();

    ACATimer packet_send_timer;     //specific timer used for packet sending to CH

    /*Object to store the latest result the sensor
    has sensed. This object is inserted as the body
    of all outgoing messages*/
    Object phenomenon = new SensorAppAgentContract.Message();

    //variable letting the sensor know if we are in steady state or setup
    //phase. True during steady-state of round, false during setup.
    boolean steady_state = false;

    /*timer to keep track of when a setup phase started. Each setup phase is clearly
    defined to be 5.0 seconds*/
    double setupStartTime = 0.0;

    /*this counter will be used to keep track of how many packets that the BS would have actually
    received if it wasn't using Clusters. In other words every CH sends one packet which combines
    the data of all its nodes in the their respective cluster. So just to see by how much traffic
    near the sink is reduced we also graph this plot as well.*/
    protected static int totalBSPackets = 0;

    {
		removeDefaultUpPort() ;
	}

    /********************************************************************************/
    /********************************************************************************/

    /**
     * Constructor
    */
    public LEACHApp()
    {
        super();
        this.myPos = new double[3];
        this.sinkPos = new double[3];
        this.setSinkLocation(0.0,0.0,0.0);  //set the sink's location to the default(0,0,0)
        this.isch_ = false;
        this.hasbeench_ = false;
        this.next_change_time_ = 0;
        this.round_ = 0;
        this.dist_ = 0;
        this.code_ = 0;
        this.joinedNodes.clear();
        this.clusterChoices.clear();
        this.TDMAschedule_.clear();
        this.end_frm_time_ = 0;
        this.myADVnum_ = 0;
        this.CHloc = new double[2];
     }

    /**
     * Allows people to set the desired total # of CH/round
     * @param num
    *
    public void set_num_clusters(int num)
    {
        num_clusters = num;
    } */

    /**
     * This helper function will generate random numbers
     * to help determine the next Cluster Head.
     * If trying to find a number between [a, b] do:
     *  1. generate random number[0,1]
     *  2. multiply it by a
     *  3. add that number to b-a
     * @return The distance
     */
    public double getRandomNumber(double llim, double ulim)
    {
        if (llim >= ulim) {
            System.out.println("RandomNumber Generator Error: Lower Bound is greater then Higher Bound");
            return -1.0;
        }
        Random generator = new Random();
        return((generator.nextDouble()*llim)+ (ulim-llim));
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
     *      Set timeout_ to NaN or negative so that route never gets prunned
    */
    public void addRoute(long src_nid_, long dst_nid_, int timeout_)
    {
        int type = 0;
        RTKey key = new RTKey(src_nid_, dst_nid_, timeout_);
        RTEntry entry = new RTEntry(new drcl.data.BitSet(new int[]{0}));

        /*  connect to the port and send the message based on the RTConfig
            contract settings which are:
                RTConfig.Message (int type_, RTKey key_, RTEntry entry_, double timeout_)  */
        setRoutePort.sendReceive(new RTConfig.Message(type, key, entry, timeout_));
    }

    /**
     * Calculates the distance between 2 points in 3D space.
    */
    protected double EuclideanDist(double X, double Y, double Z,double X2, double Y2, double Z2)
    {
        double dx = X2 - X;
        double dy = Y2 - Y;
        double dz = Z2 - Z;
        return(Math.sqrt((dx*dx) + (dy*dy) + (dz*dz)));
    }

    /**
     * Checks to see if this sensor is currently a
     * cluster head. Returns true if it is, ow. false.
    */
    public boolean isClusterHead()
    {
        return(this.isch_);
    }

    /**
     * Checks to see if it has been cluster head
     * this round already.
    */
    public boolean hasBeenClusterHead()
    {
        return(this.hasbeench_);
    }

    /**
     * Here we set the sensors status as not having been
     * picked as a cluster head yet.
    */
    public void hasnotbeenClusterHead()
    {
        this.hasbeench_ = false;
        return;
    }

    /**
     * Sets this node as a cluster head
    */
    public void setClusterHead()
    {
        this.isch_ = true;
        this.hasbeench_ = true;
    }

    /**
     * Unsets this node from being a cluster head.
    */
    public void unsetClusterHead()
    {
        this.isch_ = false;
    }

    /**
     *Sets both the CPU and Radio Components to sleep.
    */
    public void GoToSleep()
    {
        //set the CPU to sleep
        if (this.cpuMode != 1) {
            this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        }

        //Contract type: SET_RADIO_MODE = 1  &  Radio Modes: RADIO_SLEEP=1
        EnergyContract.Message temp = (EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0,1));
        if (temp.getRadioMode() != 1) {
            System.out.println("Unable to radio to sleep. Its mode is: " + temp.getRadioMode());
        }
        return;
    }

    /**
     * Set both the CPU and Radio components in IDLE
     * so that they are ready for either receiving, sending, and/or
     * processing.
    */
    public void WakeUp()
    {
        //set the CPU to ACTIVE
        //if (this.cpuMode != 2) {
        //    this.setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        //}

        //set the radio to IDLE
        //Contract type: SET_RADIO_MODE = 1 & Radio Modes:RADIO_IDLE=0
        EnergyContract.Message temp = (EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 0));
        if (temp.getRadioMode() != 0) {
            System.out.println("Unable to turn radio back on to Idle mode. Its mode is: " + temp.getRadioMode());
        }
        return;
    }

    /**
     * Sets the SS code that this sensor will use
     * for transmission
    */
    public void setCode(int code)
    {
        this.code_ = code;
        //Inform the MAC layer so that it has the
        //proper spreading code.
        //Contract type: SETTING_code = 3
        macSensorPort.doSending(new LEACHAppMacSensorContract.Message(3, code));

        //also notify wirelessphy layer  SET_SPREAD_CODE = 5
        double confirm = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(5, -1.0, code))).getRadioMode();
        if (confirm < 0) {
            System.out.println("Error!");
        }
        return;
    }

    /**
     * Method to check if sensor is Alive or not.
    */
    public void checkAlive()
    {

        // Contract type: ENERGY_QUERY =0
        double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();

        //check energy model to see if we are dead
        if((energy <= 0.0) && (!this.sensorDEAD)) {
            this.sensorDEADAT = getTime();
            this.sensorDEAD = true;
            System.out.println("Sensor"+this.nid+" is dead at time: " + getTime());
            nn_ = nn_ - 1;  //1 less total node in system
            this._stop();
        }
        else if (this.sensorDEAD) {
            return;
        }
        else {
            //continuously check if Sensor is alive.
            setTimeout("checkAlive", 0.1);
            this.sensorDEAD = false;
        }

        //need to update certain variables because they
        //depended on nn_ which wasn't set at startup
        ra_join = 0.01 * (double)nn_;                  //RA Time (s) for nodes' join reqs
        ra_delay = txtime(hdr_size+ 4);                //Buffer time for join req xmittal
        xmit_sch = 0.005 +(txtime(nn_*4+hdr_size));    //Maximum time required to transmit a schedule (n nodes in 1 cluster)
        start_xmit = ra_adv_total+ra_join+xmit_sch;

        return;
    }

    /**
     *  This method is invoked when the components timer reaches zero.
     *  @param data_
    */
    protected synchronized void timeout(Object data_)
    {
        if (!sensorDEAD) {
            if ( data_.equals("getEnergy")) {
                // Contract type: ENERGY_QUERY = 0
                double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();

                if ((energy == 0.0) & (!this.sensorDEAD)){
                    this.sensorDEADAT = getTime();
                    this.getLocation();
                    this.sensorDEAD = true;
                    this._stop();
                    nn_ = nn_ - 1;  //1 less total node in system
                    System.out.println("Sensor"+this.nid+ " is dead at " + this.sensorDEADAT);
                }

                if (plotterPort.anyOutConnection()) {
                    plotterPort.exportEvent(ENERGY_EVENT, new DoubleObj(energy), null);
                }

                rTimer = setTimeout("getEnergy", 0.25);
                return;
            }

            if ( data_.equals("decideCH")) {
                //clear variables
                this.CHData.clear();
                //determine if its a CH for this upcomming round.
                this.decideClusterHead();
                return;
            }

            if (data_.equals("advertiseCH")) {
                this.advertiseClusterHead();
                return;
            }

            if (data_.equals("createSchedule")) {
                this.createSchedule();
                return;
            }

            if (data_.equals("sendData")) {
                if(steady_state)
                    this.sendData();
                else
                    return;
            }

            if (data_.equals("informClusterHead")) {
                this.informClusterHead();
                return;
            }

            if (data_.equals("findBestCluster")){
                this.findBestCluster();
                return;
            }

            if (data_.equals("checkAlive")){
                this.checkAlive();
                return;
            }

            /* If Sensor did not hear any ADV from possible CH then
            * it is set to directly send periodic updates back to
            * the base station.*/
            if (data_.equals("SendMyDataToBS")) {
                this.SendMyDataToBS();
                return;
            }

            /* If a cluster Head then when time comes you must also
            * gather all the data you have received and send it out
            * to the base station.*/
            if (data_.equals("SendDataToBS")) {
                this.SendDataToBS();
                return;
            }
        }
        else {
            return;
        }
    }

    /**
     * _start()
     *   This method is called when attempting to 'run' the component
     *   in TCL.
    */
    protected void _start ()
    {
        this.checkAlive();
        this.decideClusterHead();
        rTimer = setTimeout("getEnergy", 0.25);
        this.setCPUMode(3);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
    }

    /**
    * _stop()
    *  This cancels the components timer ultimately stopping the periodic
    *  sending back to cluster heads.
    */
    protected void _stop()
    {
        if (rTimer != null)
            cancelTimeout(rTimer);

        if (packet_send_timer != null)
            cancelTimeout(packet_send_timer);

        //turn Radio to sleep mode when sim stops
        // Contract type: SET_RADIO_MODE = 1 & Radio Modes:RADIO_SLEEP=1
        EnergyContract.Message temp = (EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 1));
        if (temp.getRadioMode() != 1) {
            System.out.println("Unable to put radio to sleep when simulation stopped. Its mode is: " + temp.getRadioMode());
        }
        this.setCPUMode(3);     //turn off CPU when sim stops
    }


    /**
     * Distributed Cluster Setup--CH SETUP-PHASE
     *  Go through each node and check to see if its a cluster head or not
     *  Pi(t) = k / (N - k mod(r,N/k))
     *  where k is the expected number of clusters per round
     *  N is the total number of sensor nodes in the network
     *  and r is the number of rounds that have already passed.
    */
    public void decideClusterHead()
    {
        double thresh =-1.0;        //statistical threshold

        //update the start time of a new setup phase.
        setupStartTime = getTime();

        //Update state variable since we are in setup phase now
        steady_state = false;

        //let the wirelessPhy.java know that we are in setup phase... to remain on.
        //Contract type: SET_STEADY_STATE = 3
        wirelessPhyPort.sendReceive(new EnergyContract.Message(3, false, false));

        //if there were any lingering packets make sure to cancel their sendings
        //since the round is over this prevents any sort of overlapping.
        if (packet_send_timer != null)
            cancelTimeout(packet_send_timer);

        //SETTING_CHheard  = 0
        macSensorPort.doSending(new LEACHAppMacSensorContract.Message(0,0));

        //SETTING_myADVnum = 0;
        macSensorPort.doSending(new LEACHAppMacSensorContract.Message(0,0));

        /* Check the alive status of the node.  If the node has run out of
         * energy, it no longer functions in the network.
        */
        this.checkAlive();

        if (this.sensorDEAD)
            return;

        //have to set SS code
        this.setCode(0);

        //make sure the radio and CPU components are active and ready
        this.WakeUp();

        int tot_rounds = (int)(nn_ / num_clusters);

        if (this.round_ >= tot_rounds) {
            this.round_ = 0;
        }
        if (eq_energy == true) {
            /*Pi(t) = k / (N - k mod(r,N/k))
            where k is the expected number of clusters per round
            N is the total number of sensor nodes in the network
            and r is the number of rounds that have already passed.*/
            int num_nodes = nn_;
            if ((num_nodes - (num_clusters * this.round_))< 1) {
                thresh = 1;
            } else {
                thresh = (double)num_clusters / ( (double)num_nodes - (double)num_clusters * (double)this.round_);
                if (round_ == 0) {
                    this.hasnotbeenClusterHead();
                }
            }
            /*If node has been cluster-head in this group of rounds, it will not
              act as a cluster-head for this round. */
            if (this.hasBeenClusterHead()) {
                thresh = 0;
            }

        } else {
            /*Pi(t) = Ei(t) / Etotal(t) * k
            where k is the expected number of clusters per round,
            Ei(t) is the node's current energy, and Etotal(t) is the total
            energy from all nodes in the network.*/
            double Etotal = 0; //total energy from all nodes in the network
            double Ecurrent;
            /*   Note!  In a real network, would need a routing protocol to get this
                information.  Alternatively, each node could estimate Etotal(t) from
                the energy of nodes in its cluster.
            */
            for (int id = 0; id < nn_-1; id++) {
                // Contract type: ENERGY_QUERY = 0
                Ecurrent = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();
                Etotal = Etotal + Ecurrent;
            }
            // Contract type: ENERGY_QUERY = 0
            double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();
            thresh = (energy * num_clusters) / Etotal;
        }

        this.TDMAschedule_.clear();
        this.clusterChoices.clear();
        this.joinedNodes.clear();

        if (generator.nextDouble() < thresh) {
            System.out.println("******** Round "+this.round_+" Sensor"+this.nid+" Is a cluster head at time "+ getTime()+" **********");
            this.setClusterHead();

            total_times_CH ++;  //increment counter on how often this sensor was a CH
            global_total_CH ++; //increment global counter for total number of CH overall

            Random generator = new Random();
            /*if chosen to be a CH it will advertise its new status with a signal strong
              enough so that everyone hears within no longer then 0.166665 seconds*/
            setTimeout("advertiseCH", (generator.nextDouble() / 6));
        } else {
            //System.out.println("Sensor"+this.nid + " is not a cluster head");
            this.unsetClusterHead();
        }

        this.round_ = this.round_ + 1;
        next_change_time_ = ch_change + setup_time;
        setTimeout("decideCH", next_change_time_);
        //setTimeout("findBestCluster", ra_adv_total);
        setTimeout("findBestCluster",2.0); //starts in 2 seconds

        //inform the MAC layer on whether or not you are a CH
        macSensorPort.doSending(new LEACHAppMacSensorContract.Message(4, isClusterHead()));
        return;
    }

    /**
     * advertiseClusterHead is for a newly elected Cluster head
     * node to broadcast to all peers (within its range of communication)
     * that a new cluster head is available.
    */
    public synchronized void advertiseClusterHead()
    {

        //make sure the CPU is awake
        //if (this.cpuMode != 2) {
        //    this.setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        //}

        /*The data size of the broadcast packet...
          total size is  after lower layers
          encapsulate it with their headers*/
        int datasize = -58;

        //create a new bcast object
        LEACH_CH_Packet newPacket = new LEACH_CH_Packet(this.nid, this.myPos, this.code_, datasize);


        //System.out.println("Sensor" +this.nid +" is advertising its CH status at time: "+getTime());

        //send out a bcast to all nodes if it has the energy
        if (!(this.sensorDEAD)) {
            //                                                     Message (int UniBcast_flag_, int type_, long src_, Object body_)
            downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, LEACH_ADV_CH, this.nid, datasize, newPacket));
            eID = eID + 1;
        } else {
            System.out.println("You are trying to send with dead sensor"+this.nid);
            return;
        }

        //put CPU back in sleep mode
        //if (this.cpuMode != 1) {
        //    this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        //}
        return;
    }

    /**
     * For all non Cluster-Heads a choice must be made to which
     * cluster to belong to.  Everytime a LEACH_ADV_CH message was
     * received the CH was entered into the vector clusterChoices. This
     * sensor now has to determine which CH is the closest and then
     * call informClusterHead to send a reply back to it.
    */
    public synchronized void findBestCluster()
    {
        int ClusterCode = -1;
        long temp_ID;
        double temp_distance;
        double min_distance = 100000;

        /*Case 1: This Sensor is a CH therefore determine code and
                   create a TDMA schedule */
        if (this.isClusterHead()) {

            /*Since I am a CH who will be sending directly to base station
            add a route to my table to point to the BS*/
            addRoute(this.nid,this.sink_nid,-1);

            this.currentCH = this.nid;

            //gets advertisement# from MAC component (GETTING_myADVnum = 2 is the msg type)
            this.myADVnum_ = ((LEACHAppMacSensorContract.Message)macSensorPort.sendReceive(new LEACHAppMacSensorContract.Message(2,-1))).getValue();

            // There are opt(spreading) - 1 codes available b/c need 1 code
            //for communication with the base station.
            int numCodesAvail = 2*spreading -1;
            ClusterCode = (myADVnum_ % numCodesAvail) + 1;
            double scheduleCreation = (2+ (generator.nextDouble()/2));
            //System.out.println("I am CH"+this.nid+ " and I will create a schedule in:" + scheduleCreation +" sec. The time is:" + getTime());
            //setTimeout("createSchedule", (ra_adv_total+ra_join));
            setTimeout("createSchedule", scheduleCreation);

        } else {
            /*Case 2: If node is not a CH, find the CH which allows
                   minimum transmit power for communication.
                   Set the code and "distance" parameters accordingly.
            */
            if(clusterChoices.isEmpty()) {
                System.out.println("Sensor"+this.nid+ ": Warning No Cluster Head ADVs were heard at time: "+getTime());
                //make this node send directly to base station
                currentCH = this.sink_nid;
                //this.SendMyDataToBS();

                //Slight modification from original ns code...
                //if no clusters were found for this node... it will have to remain quiet in sleep mode
                this.GoToSleep();
                return;
            }
            //o.w. go through list and determine which is closest.
            for (int i = 0; i < clusterChoices.size(); i ++ ) {
                //get ID of current CH being examined
                temp_ID = ((CHinfo)clusterChoices.get(i)).getNid();
                //calculate the current CH distance
                temp_distance = this.EuclideanDist(this.getX(),  this.getY(), this.getZ(),((CHinfo)clusterChoices.get(i)).getX(),((CHinfo)clusterChoices.get(i)).getY(), ((CHinfo)clusterChoices.get(i)).getZ());

                if (temp_distance < min_distance) {
                    min_distance = temp_distance;
                    this.currentCH = temp_ID;                               //store its ID
                    this.CHloc = ((CHinfo)clusterChoices.get(i)).getLoc();  //store its position
                    int numCodesAvail = 2*spreading-1;
                    ClusterCode = (i % numCodesAvail) + 1;
                }
            }//end for
            this.dist_ = min_distance; //update state variable

            //double random_access = ra_adv_total + (this.getRandomNumber(0.0,(ra_join-ra_delay)));
            //setTimeout("informClusterHead", random_access);
            double random_access = generator.nextDouble()*1.9;
            setTimeout("informClusterHead", random_access);
            //System.out.println("Sensor"+this.nid+ " Current Cluster head is Sensor"+this.currentCH+" whose distance is: " + this.dist_);
        }

        //Set the currents node code that will be used for DSSS
        this.setCode(ClusterCode);

        //clear vectors
        this.clusterChoices.clear();
        return;
    }

    /**
     * For all non-cluster-heads once a decision was made in
     * findBestCluster() a message must be sent back to the CH
     * informing it that it is ready to join.
    */
    public synchronized void informClusterHead()
    {
        //add a route in the table for our reply (25 second timeout)
        addRoute(this.nid,this.currentCH,-1);

        //int spreading_factor = spreading;
        //int datasize = (spreading_factor * BYTES_ID);

        /*The data size of the broadcast packet...
          total size is  after lower layers
          encapsulate it with their headers*/
        int datasize = -58;

        LEACH_Join_Packet newPacket = new LEACH_Join_Packet(this.nid, this.currentCH,this.code_,datasize);

        //System.out.println("Sensor"+this.nid+" is sending a Join-REQ to Sensor"+ this.currentCH+" at distance: " +this.dist_+ " at time "+getTime());
        /*
         * NOTE!!!! Join-Req message sent with enough power so all nodes in
         * the network can hear the message.  This avoids the hidden terminal
         * problem.
        */
        if (!(this.sensorDEAD)) {
            //                                                  (int UniBcast_flag_, long dst_, long src_, int size_, int type_, int eventID_, long target_nid_, Object body_)
            downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, this.currentCH, this.nid, datasize,LEACH_JOIN_REQ, eID, this.nid, newPacket)) ;
            eID = eID + 1;          //every message sent is associated with an event ID
        } else {
            System.out.println("Sensor"+this.nid+" Trying to forward a packet but im dead!");
            dropped_packets ++;
        }
        return;
    }

    /**
     * Check if there are nodes in the cluster, if there is then create a
     * TDMA schedule and send out a broadcast with that information.
    */
    public synchronized void createSchedule()
    {

        //make sure the CPU is awake
        //if (this.cpuMode != 2) {
        //    this.setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        //}

        if(this.joinedNodes.isEmpty()) {
            System.out.println("Warning: There are no nodes in cluster "+this.nid);

            //dont start sending till setupPhase is done
            double delay = 5.0 - (getTime()-setupStartTime);
            packet_send_timer = setTimeout("SendMyDataToBS", delay);
            //this.SendMyDataToBS();
            this.TDMAschedule_.clear(); //make sure its empty
            return;
        }
        else {
            //Set the TDMA schedule and send it to all nodes in the cluster.
            //int spreading_factor = spreading;
            //int datasize = spreading_factor*BYTES_ID*this.joinedNodes.size();

            /*The data size of the broadcast packet...
            total size is  after lower layers
            encapsulate it with their headers is 20bytes*/
            int datasize = -58;

            //create a new bcast object
            LEACH_SCH_Packet newPacket = new LEACH_SCH_Packet(this.nid, this.joinedNodes,this.code_,datasize);

            //send out a bcast to all nodes if it has the energy
            if (!(this.sensorDEAD)) {
                downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, LEACH_ADV_SCH, this.nid, datasize, newPacket));
                eID = eID + 1;
            } else {
                System.out.println("You are trying to send with dead sensor"+this.nid);
                return;
            }

            for (int i = 0; i < this.joinedNodes.size(); i ++) {
                this.TDMAschedule_.add(this.joinedNodes.get(i)); //re-insert the proper elements back in.
            }
        }

        //put CPU back in sleep mode
        //if (this.cpuMode != 3) {
        //    this.setCPUMode(3);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        //}

        //Once the CH sends out the schedule he is now ready to enter the steady state phase.
        //Contract type: SET_STEADY_STATE = 3
        wirelessPhyPort.sendReceive(new EnergyContract.Message(3, true, this.isClusterHead()));

        //also update our state variable
        steady_state = true;

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
            //System.out.println("Sensor"+this.nid+"The received packet is of type: " + spkt.getPktType());

			if ((spkt.getPktType() == LEACH_ADV_CH) && !(this.isClusterHead())) {
                //pass on just the body of the message
                this.recvADV_CH((LEACH_CH_Packet)spkt.getBody());
                return;
            }

            if (spkt.getPktType() == LEACH_JOIN_REQ) {
                LEACH_Join_Packet msg = (LEACH_Join_Packet)spkt.getBody();
                //Check to make sure I am the cluster head that this JOIN_REQ is for
                if (this.nid == msg.getChID()) {
                    this.recvJOIN_REQ(msg);
                }
                return;
            }

            if (spkt.getPktType() ==  LEACH_ADV_SCH) {
                //System.out.println("Sensor"+this.nid+"The received packet is of type LEACH_ADV_SCH");
                LEACH_SCH_Packet msg = (LEACH_SCH_Packet)spkt.getBody();
                //Check to make sure this schedule is for my cluster
                if (msg.getchID() == this.currentCH) {
                    this.recvADV_SCH(msg);
                }
                return;
            }

            if (spkt.getPktType() == LEACH_DATA) {
                //cast the body back into its proper type
                LEACH_Data_Packet msg = (LEACH_Data_Packet) spkt.getBody();
                //Check to see if I am the intended CH recepient of this data message
                if (this.nid == msg.getCH_id()) {
                    this.recvDATA(msg);
                }
                return;
            }
		} else {
            System.out.println("LEACHApp -> Warning: Received a non LEACH_SensorPacket.");
            super.recvSensorPacket(data_) ;
            return;
        }
        return;
    }

    /**
     * This method is called upon when a sensor receives a broadcast from a
     * newly elected Cluster head and it is not a cluster head.
     * @param msg
    */
    protected synchronized void recvADV_CH(LEACH_CH_Packet msg)
    {
        //System.out.println("Sensor"+this.nid+ " has received a CH_ADV msg from CH"+msg.getCH());

        //determine its distance based on the location it gave you
        double distance = this.EuclideanDist(this.getX(), this.getY(), this.getZ(), msg.getSenderX(),msg.getSenderY(),msg.getSenderZ());

        //create a CHinfo object and store it in the possible choices.
        LEACHApp.CHinfo possible_CH = new LEACHApp.CHinfo(msg.getCH(), distance, msg.getSenderPos());
        clusterChoices.add(possible_CH);
        return;
    }


    /**
     * This method is called upon when a Cluster head receives a reply
     * from its broadcast for a sensor to join its cluster. This will only
     * be executed by Cluster heads.
     * @param msg
     */
    protected synchronized void recvJOIN_REQ(LEACH_Join_Packet msg)
    {
        //add the Sensors ID to the clusters list.
        Long newMember = new Long(msg.getSrc_id());
        joinedNodes.add(newMember);
        return;
    }


    /**
     * If I am not a CH then I will be expecting a schedule back
     * from the CH that I selected. This method handles that schedule
     * arrival from my cluster head. Given the schedule that it gives
     * me I calculate when my TDMA time slot is to send and then
     * goto sleep to conserve energy.
     * @param msg
    */
    protected synchronized void recvADV_SCH(LEACH_SCH_Packet msg)
    {
        int index = -1;
        Vector newSchedule = msg.getSchedule();

        for (int i = 0; i < newSchedule.size(); i ++) {
            if (this.nid == ((Long)newSchedule.get(i)).longValue()) {
                index = i;
            }
        }

        if (index < 0) {
            System.out.println("Warning: Sensor"+this.nid+" does not have a transmit time. Sending Directly to BS");
            this.SendMyDataToBS();
            return;
        } else {
            /*Determine time for a single TDMA frame.  Each
            node sends data once per frame in the specified slot.*/
            double frame_time = newSchedule.size() * ss_slot_time;
            this.xmitTime_ = ss_slot_time * index;
            this.end_frm_time_ = frame_time - this.xmitTime_;
            //double xmitat = getTime() + this.xmitTime_;     //At what time to transmit at.

            /*System.out.println("\n-------------------");
            System.out.println("Sensor"+this.nid);
            System.out.println("slot_time: " + slot_time);
            System.out.println("Spreading: " + spreading);
            System.out.println("ss_slot_time: " + ss_slot_time);
            System.out.println("index: " + index);
            System.out.println("xmitTime: " + xmitTime_);
            System.out.println("end_frm_time: " + end_frm_time_);*/

            //System.out.println("Sensor"+this.nid+" Tx in: " + ((5.0 - (getTime()-setupStartTime)) + this.xmitTime_) + "seconds. It is now: "+ getTime());

           // if ((xmitat + this.end_frm_time_) < (this.next_change_time_ - 10 * ss_slot_time)) {
            packet_send_timer = setTimeout("sendData", ((5.0 - (getTime()-setupStartTime)) + this.xmitTime_));//((5.0-getTime()) + this.xmitTime_));
            //}

            //i've received my schedule so now I can goto sleep till
            //its my turn to send to my CH
            this.GoToSleep();

            /*Let the hardware know we are entering the steady state and whether or not
            we are a cluster head for this round
            Contract type: SET_STEADY_STATE = 3*/
            wirelessPhyPort.sendReceive(new EnergyContract.Message(3, true,this.isClusterHead()));

            //update this sensors steady_state flag
            steady_state = true;

            return;
        }
    }


    /**
     * It has been determined that you are the CH recipient of this data message
     * therefore acknowledge that you received it and eventually send it to BS
     * @param msg
    */
    protected synchronized void recvDATA(LEACH_Data_Packet msg)
    {

        //System.out.println("CH"+this.nid+ " received data from Sensor"+msg.getSender_id()+ " at time: " + getTime());

        Long src_id = new Long(msg.getSender_id());
        this.receivedFrom_.add(src_id);

        int last_node = TDMAschedule_.size() -1; //last element

        if (this.nid == ((Long)TDMAschedule_.get(last_node)).longValue()){
            last_node = last_node -1;
        }

        //The CH stores the report sent by each node in its own vector then when
        //its time to send a CH report back to the BS it just sends out the whole
        //vector. -->each object is in fact a DoubleObj which would normally be
        //just a double that represents a value for whatever phenomenon you are
        //simulating.
        CHData.add(msg.getBody());

        if (msg.getSender_id() == ((Long)this.TDMAschedule_.get(last_node)).longValue()){
            /*After an entire frame of data has been received, the cluster-head
            must perform data aggregation functions and transmit the aggregate
            signal to the base station.*/
            this.receivedFrom_.clear(); //clear the received list so that we can start over.
            this.SendDataToBS();        //send the data off
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
     * sendData is called when a sensor that is part of a cluster
     * sends a data update to its CH. This method will only be
     * called in the sensors scheduled slot time to respect TDMA
     * schedules that were allocated during setup phase.
    */
    protected void sendData()
    {
        //Use DS-SS to send data messages to avoid inter-cluster interference.
        /*ns2 version:
        set nodeID [$self nodeID]
        set msg [list [list $nodeID , [$ns_ now]]]
        set datasize [expr $spreading_factor * [expr [expr $BYTES_ID * [llength $msg]] + $opt(sig_size)]]
        */
        int datasize = 37;

        //create the packet to be sent out
        LEACH_Data_Packet newPacket = new LEACH_Data_Packet(this.nid, this.currentCH, phenomenon, getTime(),this.code_,datasize);


        //make sure the radio and CPU components are active and ready
        this.WakeUp();

        if (!(this.sensorDEAD)) {
            downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, this.currentCH, this.nid, this.CHloc, datasize,LEACH_DATA, eID, this.nid, newPacket)) ;
            eID = eID + 1;          //every message sent is associated with an event ID
        } else {
            System.out.println("You are trying to send with dead sensor"+this.nid);
            return;
        }
        //Must transmit data again during slot in next TDMA frame.
        double xmitat = getTime() + this.frame_time;
        this.checkAlive();  //update sensors energy status to see if we send again

        //if ((!this.sensorDEAD) && ((xmitat + this.end_frm_time_) < (this.next_change_time_-10*ss_slot_time))) {
        if (!this.sensorDEAD) {
            packet_send_timer = setTimeout("sendData", xmitat);
        }

        return;
    }

    /**
     * If a cluster Head then when time comes you must also
     * gather all the data you have received and send it out
     * to the base station.
    */
    protected void SendDataToBS() {

        //Use DS-SS to send data messages to avoid inter-cluster interference.
        /*ns2 version:
            set nodeID [$self nodeID]
            set msg [list [list [list $nodeID , [$ns_ now]]]]
            set datasize [expr $spreading_factor * [expr $BYTES_ID * [llength $msg] + $opt(sig_size)]]
        */
        //int datasize = spreading * BYTES_ID * (2 + sig_size);
        int datasize = 67;

        //create the packet to be sent out
        LEACH_Data_Packet newPacket = new LEACH_Data_Packet(this.nid, this.currentCH, this.CHData, getTime(),bsCode, datasize);

        //add route to CH to routing table first (15 second timeout)
        addRoute(this.nid, this.sink_nid, -1);

        System.out.println("CH"+this.nid+ " Is sending data to BS");

        if (!(this.sensorDEAD)) {
            //                                                  (int UniBcast_flag_, long dst_, long src_, int size_, int type_, int eventID_, long target_nid_, Object body_)
            downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, this.sink_nid, this.nid, this.sinkPos, datasize,LEACH_DATA, eID, this.nid, newPacket)) ;
            eID = eID + 1;          //every message sent is associated with an event ID
            totalBSPackets = totalBSPackets + 1;
        } else {
            System.out.println("You are trying to send with dead sensor"+this.nid);
            return;
        }

    }


    /**
     * If Sensor did not hear any ADV from possible CH then
     * it is set to directly send periodic updates back to
     * the base station.
     */
    protected void SendMyDataToBS()
    {
        //add route to CH to routing table first (15 second timeout)
        addRoute(this.nid,this.sink_nid,-1);

        //Use DS-SS to send data messages to avoid inter-cluster interference.
        //int datasize = spreading * BYTES_ID * (2 + sig_size);
        int datasize = 37;

        this.CHData.clear();
        this.CHData.add(phenomenon);

        CHData.clear();
        CHData.add(phenomenon);

        //create the packet to be sent out
        LEACH_Data_Packet newPacket = new LEACH_Data_Packet(this.nid, this.currentCH, CHData, getTime(),bsCode, datasize);

        //now wake up the CPU and radio components
        this.WakeUp();

        //Contract type: SET_RADIO_MODE = 1  & Radio Modes:  RADIO_IDLE=0 RADIO_TRANSMIT=3
        int temp = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0,0))).getRadioMode();
        if (temp != 0){
            System.out.println("Unable to set radio mode to IDLE!");
        }
        System.out.println("CH"+this.nid+ " Is sending data to BS");

        if (!(this.sensorDEAD)) {
            //                                                  (int UniBcast_flag_, long dst_, long src_, int size_, int type_, int eventID_, long target_nid_, Object body_)
            downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, this.sink_nid, this.nid, this.sinkPos, datasize,LEACH_DATA, eID, this.nid, newPacket)) ;
            eID = eID + 1;          //every message sent is associated with an event ID
            totalBSPackets = totalBSPackets + 1;
        } else {
            System.out.println("You are trying to send with dead sensor"+this.nid);
            return;
        }

        //Must transmit data again during slot in next TDMA frame.
        double xmitat = getTime() + this.frame_time;
        if ((!this.sensorDEAD) && ((xmitat + this.end_frm_time_) < (this.next_change_time_-10*ss_slot_time))) {
            setTimeout("SendMyDataToBS", xmitat);
        }

        return;
    }

    /**
     * total number of packets sent by CH to base stations.
     * @return
     */
    public int getBSpackets() {
        return(totalBSPackets);
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

        if (portid_.equals(MAC_SENSOR_PORT_ID)) {
            LEACHAppMacSensorContract.Message msg = (LEACHAppMacSensorContract.Message)data_;
            /* Types: SETTING_CHheard  = 0, SETTING_myADVnum = 1, GETTING_myADVnum = 2, SETTING_code= 3*/
            if (msg.getType() == 1) {
                this.myADVnum_ = msg.getValue();
                return;
            } else {
                System.out.println("LEACHApp on Sensor"+this.nid+ " received an incorrect reply!");
            }
            return;
        } else {
            super.processOther(data_, inPort_);
        }
    }

    /**
     * Sets the total number of sensors in the simulation.
     * @param nn_
    */
    public void setNn_(int nn_)
    {
        LEACHApp.nn_ = nn_;
    }

    /**
     * Sets the total number of clusters that is desired
     * for any given round.
     * @param num_clusters
    */
    public void setNum_clusters(int num_clusters)
    {
        LEACHApp.num_clusters = num_clusters;
    }

    /**
     * Sets the total number of rounds to simulate.
     * @param total_rounds
    */
    public void setTotal_rounds(int total_rounds)
    {
        LEACHApp.total_rounds = total_rounds;
    }

    /**
     * A getter method to obtain total number of times
     * a sensor acted as a cluster head.
     * @return
     */
    public int get_total_times_CH() {
        return (total_times_CH);
    }

    /**
     * a getter method to obtain the total number of CHs
     * that were used in during the whole simulation.
     * @return
    */
    public int get_global_total_CH() {
        return(global_total_CH);
    }

   /**
     *
     * @return
    */
    public String getName()
    {
        return "LEACHApp";
    }


    /**************************************************************************************************
     *Helper Classes.
     **************************************************************************************************/

    /**
     * Used by non-cluster heads to maintain a list of possible
     * clusters that they can join and who the leads of each
     * group is.
     */
    class CHinfo
    {
        long nid;
        double distance;
        double[] loc;

        public CHinfo(long nid_, double distance_, double[] loc_)
        {
            this.nid = nid_;
            this.distance = distance_;
            this.loc = new double[2];
            this.loc = loc_;
        }

        public long getNid()
        {
            return nid;
        }

        public double getDistance()
        {
            return distance;
        }

        public double[] getLoc()
        {
            return loc;
        }
        public double getX() { return loc[0]; }
        public double getY() { return loc[1]; }
        public double getZ() { return loc[2]; }

    }

    /**
     * An object that represents a sensor. These objects will be
     * used by cluster heads to maintain a list of nodes that are
     * part of its group.
     */
    class SensorNode {
        long nid;
        double send_time;
        double[] loc;

        public SensorNode(long nid_, double[] loc_)
        {
            this.nid = nid_;
            this.loc = new double[2];
            this.loc = loc_;
        }

        public double getSend_time()
        {
            return send_time;
        }

        public void setSend_time(double send_time)
        {
            this.send_time = send_time;
        }
    }
}
