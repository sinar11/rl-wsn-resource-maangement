package drcl.inet.mac.CSMA;


/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/29/2005
 *
 * This class implements the MAC non-persistent CSMA protocol. This class
 * was ported from an old version of ns-2 (ns-2.1) which was the authors
 * original work.
 *
*/

import drcl.net.Packet;
import drcl.comp.ActiveComponent;
import drcl.comp.ACATimer;
import drcl.comp.Port;
import drcl.inet.sensorsim.*;
import drcl.inet.sensorsim.LEACH.*;
import drcl.inet.InetPacket;
import drcl.inet.mac.LLPacket;
import drcl.data.IntObj;
import drcl.inet.mac.*;

import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

public class Mac_CSMA extends drcl.inet.mac.Mac implements ActiveComponent {

    //Port that connects the LEACHApp with the MacSensor layer in order to correlate
    //the SS code.
    public static final String APP_PORT_ID   = ".sensorApp";
    public Port AppPort                    = addPort(APP_PORT_ID, false);

    //Port that will connect to the wirelessPhy.java component which allows us to
    //check if the channel is free
    public static final String WIRELESS_PHY_PORT_ID = ".wphyRadioMode";
    public Port radioModePort                    = addPort(WIRELESS_PHY_PORT_ID);

    //timers
    MacSensor_DeferSensorTimer	df_timer_;
	MacSensor_TxSensorTimer     tx_timer_;
	MacSensor_RxSensorTimer     rx_timer_;

	int code_;       // the code this node is currently receiving
	int node_num_;   // this node's ID number
	int ss_ = 8;     // max number of overlapping ss transmissions
	int CHheard_;    // Number of other CH ADVs heard
	int myADVnum_;   // Position of node's ADV among all ADVs

    int collision = 0; //to keep track of the total number of collisions experienced
                       //by this sensor.

    //the following three fields are only if running LEACH mode
    private boolean     LEACHmode       = false;    //if running leach mode.
    //private boolean     isCH            = false;    //true iff it is a CH
    protected static final int ADV_TYPE = 0;

    // Duplicate Detection state
    int sta_seqno_;	    // next seqno that I'll use
    Hashtable cache_;   //hashtable is used to record the recently received mac frame sequence number to detect the duplicate frames
    int cache_node_count_;

    private int  ETHER_HDR_LEN; //Size of the header portion of the message
    int    DSSS_PreambleLength		  = 144;	    // 144 bits
    int    DSSS_PLCPHeaderLength	  = 48;	        // 48 bits

    Mac_Sensor_Packet pktRx_;  //Received IEEE802.11 Data frame.
    Mac_Sensor_Packet pktTx_;  //IEEE802.11 Data frame to be transmited.

    boolean txinfo_pktRx_error;     //Whether this received packet is of error.
    double  txinfo_pktRx_RxPr;      //The received power of this packet.
    double  txinfo_pktRx_CPThresh;  //Capture threshhold of the wireless physical layer.

    ACATimer queueCheck;            //Timer to continously check queue for new packets when idle

    //protected static int dropped_packets = 0;   //keeps track of the number of dropped packets.

    //keeps track of how many dropped packets have occured and which ones
    //they were.
    protected static Vector dropped_packets = new Vector();
    /**
     * Constructor
     * Note Make sure TCL script calls  setNode_num!
    */
    public Mac_CSMA()
    {
        super();
        df_timer_ = new MacSensor_DeferSensorTimer(this, 0.005);
        tx_timer_ = new MacSensor_TxSensorTimer(this);
        rx_timer_ = new MacSensor_RxSensorTimer(this);
        bandwidth_  = 1e6;        //make sure same as wirelessPhy!!
        SET_RX_STATE(MAC_IDLE);
        SET_TX_STATE(MAC_IDLE);

        setEtherHdrLen();

        sta_seqno_ = 1;
        cache_ = new Hashtable();
        cache_node_count_ = 0;
        code_ = 0;
        CHheard_ = 0;
        myADVnum_ = 0;
        pktTx_ = null;
        pktRx_ = null;
    }

    /**
     * Is this sensor running in LEACH mode or not.
     * @return
    */
    public boolean isLEACHmode()
    {
        return LEACHmode;
    }

    /**
     * To set whether or not this sensor is running LEACH.
     * @param LEACHmode
     */
    public void setLEACHmode(boolean LEACHmode)
    {
        this.LEACHmode = LEACHmode;
    }

    /**
     * called from Tcl to inform the layer how many sensors exists
     * ID.
     * @param node_num_
    */
    public void setNode_num_(int node_num_)
    {
        this.node_num_ = node_num_;
    }

    /**
     * Calculates and sets the value of what the header lenght of a
     * Mac_Sensor_Packet will be.
    */
    private void setEtherHdrLen()
    {
        ETHER_HDR_LEN = (DSSS_PreambleLength>>3)+(DSSS_PLCPHeaderLength>>3)
        + Mac_Sensor_Packet.Mac_Sensor_Packet_Header_Length;
    }

    /**
     * Used to set update the receiving state of the NIC card.
     * @param x the state (ie. MAC_IDLE, MAC_RECV..)
    */
    protected void SET_RX_STATE(int x)
    {
        rx_state_ = x;
    }


    /**
     * Used to set update the receiving state of the NIC card.
     * @param x the state (ie. MAC_IDLE, MAC_SEND..)
    */
    protected void SET_TX_STATE(int x)
    {
        tx_state_ = x;
    }

    /**
     * Returns true iff both the receiving and transmitting states
     * are set to IDLE o.w. false
     * @return
    */
    private boolean is_idle()
    {
        if (rx_state_ != MAC_IDLE)
            return false;
        if (tx_state_ != MAC_IDLE)
            return false;
        return true;
    }


    /**
     * while calling DATA_Time(), len_ has already counted the extra
     * overhead bits in. Note 1 byte = 8 bits.
     * @param len_ the length of the packet
     * @return the time required to transmit/receive the data (in seconds)
    */
    private double DATA_Time(int len_)
    {
        return 8 * len_ / bandwidth_;
    }


    /**
     * Used to get the number collisions that this
     * radio has experienced so far.
     * @return
    */
    public int getCollision()
    {
        return collision;
    }


    /**
     * This method is called upon to determine the transmission
     * time that is required for this packet (size dependent)
     * @param p packet to be transmitted
     * @return the time t needed for the transfer.
    */
    protected double TX_Time(Packet p)
    {
        double t = DATA_Time(p.size);
        return t;
    }


    /**
     * Prints out Friendly Error Message is
     * @param where
     * @param why
     * @param continue_
    */
    private void _assert(String where, String why, boolean continue_)
    {
        if ( continue_ == false )
            drcl.Debug.error(where, why, true);
        return;
    }


    /**
     * Processing the frames arriving from the up port. In other words this method is
     * called when information has been fetched from the Queue. The incoming packet
     * type which binds these two components together is an LLPacket object.
     * @param data_
     * @param upPort_
    */
    protected synchronized void dataArriveAtUpPort(Object data_, drcl.comp.Port upPort_)
    {
        //attempt to send it off
        send((LLPacket) data_);
        return;
    }


    /**
     * This method attempts to simulate CSMA by verifying if this
     * card is not busy and that the channel is free. If so then it
     * sends it down to the physical layer (WirelessPhy.java)
     * otherwise it starts a backoff timer.
     * @param p   The Packet
    */
    protected synchronized void send(Packet p)
    {
        /**********
        print statements for debugging...

        //System.out.println("MacSensor.UpPort(): Sensor"+this.macaddr_+" data arrived or being resent");
        //long dst_macaddr = ((LLPacket) data_).getDstMacAddr();
        //if (((LLPacket)data_).dst_macaddr != -1) {
        //    System.out.println("Sensor"+this.macaddr_+ " at MacSensor.UpPort is getting ready to send Packet to: " + ((LLPacket)data_).dst_macaddr);
        // }
        //if (p instanceof LLPacket) {
        //    System.out.println("Sensor"+this.macaddr_+" Is attempting to send a packet. is pktTx_ set to null: " + (pktTx_ == null));
        //} else{
        //    System.out.println("Sensor"+this.macaddr_+" Is attempting to resend at time: " + getTime());
        //}*/

        /*The Packet may have already been encapsulated into a
        MAC header... This part is only needed if its being
        wrapped ie. just received from LL layer.*/
        if (p instanceof LLPacket) {

            long dst_macaddr = ((LLPacket) p).getDstMacAddr();
            Mac_Sensor_Packet df;
            Mac_802_11_Frame_Control fc;

            /*The FC field consists of the following fields:
            * Protocol Version, Type, Subtype, To DS, From DS, More Fragments, Retry, Power Management,
            * More Data, Wired Equivalent Privacy (WEP), and Order.*/
            fc = new Mac_802_11_Frame_Control(Mac_802_11_Frame_Control.MAC_Subtype_Data, Mac_802_11_Frame_Control.MAC_Type_Data,Mac_802_11_Frame_Control.MAC_ProtocolVersion);
            fc.set_fc_flags(false, false, false, false, false, false, false, false);

            if ( dst_macaddr != MAC_BROADCAST ) {
                //10 is arbitratry should be DATA_DURATION()
                df = new Mac_Sensor_Packet(fc, 10, dst_macaddr, macaddr_, 0, 0, false, ETHER_HDR_LEN, p.size, (LLPacket)p);
                pktTx_ =  (Mac_Sensor_Packet)df.clone();
            } else {
                df = new Mac_Sensor_Packet(fc, 10, dst_macaddr, macaddr_, 0, 0, false, ETHER_HDR_LEN, p.size, (LLPacket)p);
                pktTx_ =  (Mac_Sensor_Packet)df.clone();
            }
            //Assign the data packet a sequence number.
            ((Mac_Sensor_Packet) pktTx_).scontrol = sta_seqno_;
            sta_seqno_ = sta_seqno_ + 1;
        }

        //get channel status :	RADIO_TRANSMIT = 3, RADIO_RECEIVE = 4;
        int channelStatus = ((IntObj)radioModePort.sendReceive(new IntObj(-1))).intValue();

        /*If channelStatus becomes a 2 we know that the radio was turned off by
        the wirelessPhy layer. The only time the radio goes to RADIO_OFF = 2 mode
        is when the sensor is dead. So the outgoing packet must be dropped*/
        if (channelStatus == 2){
            System.out.println("Sensor"+macaddr_ + " had an outgoing packet backoff in the mac layer which has now been dropped!");
            if (p instanceof LLPacket) {
                long dst_macaddr = ((LLPacket) p).getDstMacAddr();
                int event_id =  ((SensorPacket) (((InetPacket) ((LLPacket) p.getBody()).getBody()).getBody())).getEventID();
                droppedPacket newDropped = new droppedPacket(macaddr_, dst_macaddr, event_id);
                dropped_packets.add(newDropped);
            } else {
                int event_id =  ((SensorPacket)(((InetPacket)((LLPacket)((Mac_Sensor_Packet)p).getBody()).getBody()).getBody())).getEventID();
                droppedPacket newDropped = new droppedPacket(macaddr_, pktTx_.getDa(), event_id);
                dropped_packets.add(newDropped);
            }
        }

        //System.out.println("Sensor"+this.macaddr_+ " at MacSensor.send is getting ready to send Packet to: " + pktTx_.getDa() + " current time: " + getTime());
        //Perform carrier sence.  If the channel is busy, backoff.
        //if (IamTransmittting or IamReceiving) or (channelBusy) then
        if (!is_idle() || ((channelStatus == 3) || (channelStatus == 4)) ) {
            System.out.println("Sensor"+this.macaddr_+ " at MacSensor.send has to backoff Channel busy when attempting to send packet " + pktTx_.getDa());
            //System.out.println("rx_state_: " + rx_state_ + "     tx_state_: " + tx_state_ + " Channel Status: " + channelStatus);
            Random generator = new Random();   //Generate a random number to avoid collisions.
            double time =TX_Time(pktTx_) + (generator.nextDouble() /5);
            System.out.println("Sensor"+this.macaddr_+ " has been delayed from sending... will now try again in: "+time);
            df_timer_.start(time);  //defer the transmission
            return;
        }
        /*Determine how many ADV messages have been heard to determine
        * spreading code to use for each cluster. (For LEACH) */
        myADVnum_ = CHheard_;

        SET_TX_STATE(MAC_SEND);             //change the state of the MAC layer
        tx_timer_.start(TX_Time(pktTx_));   //start the TXtimer for the duration of the packet send
        downPort.doSending(pktTx_);         //send the data
        return;
    }

    /**
     * Processing the data from the down port. In other words this method is
     * called when information arrives from the wirelessPhy component (in other
     * words inbound traffic that was sensed from the channel).
     * @param data_
     * @param downPort_
    */
    protected synchronized void dataArriveAtDownPort(Object data_, drcl.comp.Port downPort_)
    {
        MacPhyContract.Message msg = ( MacPhyContract.Message ) data_;

        /*
        * Handle incoming packets.  We just received the 1st bit of a packet on the interface.
        * (Used in LEACH only!)
        */
        if (isLEACHmode()) {
            Mac_Sensor_Packet packet = (Mac_Sensor_Packet)msg.getPkt();
            if ((((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody()) instanceof LEACH_CH_Packet) {
                CHheard_ ++;
            }
        }

        /*
        * If I am not receiving the code of the incoming packet, drop it.
        * (Used in LEACH only!)
        * I am aware that the following is extremely ugly casting... and should not be used.
        * will be fixed in future revisions. I just keep casting to unwrap the packet completely
        * to see what the sending code was.
        */
        if (isLEACHmode()) {
            int new_code =-1;
            Mac_Sensor_Packet packet = (Mac_Sensor_Packet)msg.getPkt();

            //Extract the body of the packet
            if ((((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody()) instanceof LEACH_Data_Packet){
                LEACH_Data_Packet body = (LEACH_Data_Packet)(((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody());
                new_code = body.getCode();

            } else if ((((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody()) instanceof LEACH_Join_Packet) {
                LEACH_Join_Packet body = (LEACH_Join_Packet)(((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody());
                new_code = body.getCode();

            } else if ((((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody()) instanceof LEACH_SCH_Packet){
                LEACH_SCH_Packet body = (LEACH_SCH_Packet)(((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody());
                new_code = body.getCode();

            } else if ((((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody()) instanceof LEACH_CH_Packet){
                LEACH_CH_Packet body = (LEACH_CH_Packet)(((SensorPacket)(((InetPacket)((LLPacket)packet.getBody()).getBody()).getBody())).getBody());
                new_code = body.getCode();

            }else {
                System.out.println("ERROR: SS not set correctly!");
            }
    /*        if ((new_code == 0) && (isCH)){
                //then we must accept it to prevent collisions to the base station
            } else*/
            if (new_code != code_) {
                //ignore packet its not your code
                return;
            }
        }

        /*A node cannot receive a packet while transmitting. If the node is transmitting as a
         packet arrives, the received packet is marked as erroneous. It is not dropped
         because the receiver may be busy receiving this erroneous packet after the transmitter
         has finished. All packets that contain errors are dropped in the LL layer.*/
        if ( this.tx_active_ && msg.getError() == false ) {
            msg.setError(true);
            ((Mac_Sensor_Packet) msg.getPkt()).setForcedError(true);
        }

        /*
        * If more than ss_ simultaneous transmissions occur, there is a
        * collision and I cannot receive the packet. (For LEACH)
        *
        int num_codes = 0;
        //The following is not working!! NEED TO BE FIXED!
        //for (int i = 0; i < 1000; i++)
        // if (netif_->csEnd(i) > Scheduler::instance().clock())
            num_codes++;
        if (num_codes > ss_) {
            System.out.println("I can hear " + num_codes+" different packets...Too many experiencing a collision.");
            collision((Mac_Sensor_Packet) msg.getPkt());
            return;
        } */

        /*
         * The node is neither transmitting or receiving when this new packet arrived
        */
        if ( rx_state_ == MAC_IDLE ) {
            //set the state (i.e. rx_state_) to receiving
            //and start receiving timer.
            SET_RX_STATE(MAC_RECV);
            pktRx_ = (Mac_Sensor_Packet) (msg.getPkt());
            rx_timer_.start(TX_Time(pktRx_));

        } else {
            System.out.println("Sensor"+this.macaddr_ + " had a problem receiving its rx_state is: " + rx_state_);

            /*
             * We get here if this sensor is already receiving information. 2 Cases
             * can occur and either results in a erroneous packet
            */
            if (msg.getError() == false) {
                msg.setError(true);
            }
            /* Case 1: pktRx might be sent with enough power to swamp out the
             * reception of the new packet, p. Therefore a capture occurs and p is dropped
            */
            if ( txinfo_pktRx_RxPr / msg.getRxPr() >= msg.getCPThresh() ) {
                capture((Mac_Sensor_Packet) msg.getPkt());
            }
            else {
                /* Case 2: pktRx does not have enough power to swamp out p, both
                 * packets collide. The packet that will last the longest for reception
                 * is kept and marked as erroneous and the other packet is dropped
                */
                collision((Mac_Sensor_Packet) msg.getPkt());
            }
        }
    }


    /**
     * You call this when you received packet. The defining subclass may
     * want to perform some tasks on the packet (optional) and then proceed
     * to sending it out the llport up to the Link layer.
     * @param p
    */
    protected synchronized void recvDATA(Mac_Sensor_Packet p)
    {

        Packet packet = (Packet) p.getBody();
        // two possible packets: InetPacket or ARPPacket
        //System.out.println("*******************INNER BODY: " + packet.getBody().getClass());
        /*if ( packet.getBody() instanceof InetPacket )
            System.out.println("Sending InetPacket to upper layer");
        else if ( packet.getBody() instanceof ARPPacket )
            System.out.println("Sending ARPPacket To Upper Layer");
        else
            System.out.println("Warning: Unknown Incoming packet: " + packet.getBody().toString());*/
        llPort.doSending(packet.getBody());     // the contract from MAC to LL is really simple here, no Message is defined
    }


    /**
     *
     * @param p
    */
    private synchronized void capture(Mac_Sensor_Packet p)
    {
        System.out.println("MacSensor.capture(): " + p.getName());
        p = null;
        return;
    }

    /**
     * need to explicitly release p from outside out this function
     * set p as null won't release that packet
     * @param p
    */
    protected synchronized void collision(Mac_Sensor_Packet p)
    {
        collision ++;

        switch(rx_state_) {

            case MAC_RECV:
                SET_RX_STATE(MAC_COLL);
                /* fall through */
            case MAC_COLL:
                _assert("MacSensor collision()", "(pktRx_ != null)", (pktRx_ != null));
                _assert("MacSensor collision()", "rx_timer_.busy()", rx_timer_.busy());

                //add to the aggregate dropped packets list if not already done by another sensor
                boolean alreadyDropped = false;
                long src_id = p.getSa();
                long dst_id = p.getDa();
                int event_id =  ((SensorPacket)(((InetPacket)((LLPacket)p.getBody()).getBody()).getBody())).getEventID();
                droppedPacket newDropped = new droppedPacket(src_id, dst_id, event_id);

                if (dropped_packets.size() == 0) {
                    dropped_packets.add(newDropped);
                }
                else {
                    for(int i = 0; i < dropped_packets.size(); i ++) {
                        long cur_src_id = ((droppedPacket)dropped_packets.get(i)).getSenderID();
                        long cur_dst_id = ((droppedPacket)dropped_packets.get(i)).getDestID();
                        long cur_event_id = ((droppedPacket)dropped_packets.get(i)).getEventID();
                        if ((cur_src_id == src_id) &&
                            (cur_dst_id == dst_id) &&
                            (cur_event_id == event_id)){
                            alreadyDropped = true;
                            break;
                        }
                    }
                    if (!alreadyDropped){
                        dropped_packets.add(newDropped);
                    }
                }

                /*
                 *  Since a collision has occurred, figure out
                 *  which packet that caused the collision will
                 *  "last" the longest.  Make this packet,
                 *  pktRx_ and reset the Recv Timer if necessary.
                */
                if (TX_Time(p) > rx_timer_.expire()) {
                    rx_timer_.stop();
                    pktRx_= (Mac_Sensor_Packet)p.clone();
                    rx_timer_.start(TX_Time(pktRx_));
                }
                else {
                    //pktRx_ = null;
                }
                break;
            default:
                _assert("MacSensor Collision()", "invalid rx_state", false);
        }
    }

    /**
     * Handles RxTimer Timeout Event.
    */
    protected synchronized void handleRxTimeout()
    {
        rx_timer_.handle();     //reset timer properties.
        SET_RX_STATE(MAC_IDLE); //transmission is done so reset RxState

        //check to make sure the packet belongs to you (or was a broadcast
        //if so then send it up to LL. o.w free packet.
        if ((pktRx_.getDa() != this.macaddr_) && (pktRx_.getDa() != MAC_BROADCAST)){
            this.pktRx_ = null; //free the packet
        }
        else if (pktRx_.isForcedError()) {
            //the packet is errorneous... so we drop it.
            return;
        }
        else{
            this.recvDATA(pktRx_);  //the packet was received so now send it up to the LL layer
        }
    }

    /**
     * Handles TxTimer Timeout Event.
    */
    protected synchronized void handleTxTimeout()
    {
        tx_timer_.handle();     //reset timer properties.
        SET_TX_STATE(MAC_IDLE); //transmission is done so reset TxState
        this.pktTx_ = null;     //once transmission is complete drop the packet

         upPort.doSending(null); //go get another packet from queue
    }

    /**
     * Handles DfTimer Timeout Event.
    */
    protected synchronized void handleDeferTimeout()
    {
        df_timer_.handle();
        //attempt to send packet again
        this.send(this.pktTx_);
    }

    /**
     *  This method is invoked when the components timer reaches zero.
     *  @param evt_
    */
    protected synchronized void timeout(Object evt_)
    {
        if ( evt_.equals("checkQueue")) {
            //if the MAC layer isn't busy go up and check to see if the queue
            //queue has something waiting.
            if ((tx_state_==MAC_IDLE)&&(!this.df_timer_.busy()) && (!this.tx_timer_.busy()) && (pktTx_ == null)) {
                upPort.doSending(null);
            }
            queueCheck = setTimeout("checkQueue", 1);
            return;
        }

        int type_ = ((MacTimeoutEvt)evt_).evt_type;
        switch(type_) {
            case MacTimeoutEvt.Rx_timeout: //Rx_timeout = 2
                handleRxTimeout();
                return;
            case MacTimeoutEvt.Tx_timeout: //Tx_timeout= 3
                handleTxTimeout();
                return;
            case MacTimeoutEvt.Defer_timeout://Defer_timeout = 4
                handleDeferTimeout();
                return;
        }
        return;
    }

    /**
     * _start()
     *   This method is called when attempting to 'run' the component
     *   in TCL.
    */
    protected void _start ()
    {
        queueCheck = setTimeout("checkQueue", 1);
    }

    /**
    * _stop()
    *  This cancels the components timer ultimately stopping the periodic
    *  sending back to the sink node.
    */
    protected void _stop()
    {
        if (queueCheck != null)
          cancelTimeout(queueCheck);
        if (pktTx_ != null) {
            System.out.println("Warning: MAC Layer still has an outgoing packet being sent off.");
        }
    }

    /**
     * To obtain the total number of dropped packets
     * due to collisions were detected at the MAC layer.
     * @return
     */
    public int getDropped_packets()
    {
        return dropped_packets.size();
    }

    /**
     * Overrides the method in SensorApp and is responsible for accepting events
     * on incomming ports.
     * @param data_
     * @param inPort_
    */
    protected synchronized void processOther(Object data_, Port inPort_)
    {
        String portid_ = inPort_.getID();

        if (portid_.equals(APP_PORT_ID)) {
            LEACHAppMacSensorContract.Message msg = (LEACHAppMacSensorContract.Message)data_;
            /* Types: SETTING_CHheard  = 0, SETTING_myADVnum = 1, GETTING_myADVnum = 2, SETTING_code= 3*/
            if (msg.getType() == 0){
                this.CHheard_ = msg.getValue();
                return;
            }
            if (msg.getType() == 1) {
                this.myADVnum_ = msg.getValue();
                return;
            }
            if (msg.getType() == 2) {
                AppPort.doSending(new LEACHAppMacSensorContract.Message(1,this.myADVnum_));
                return;
            }
            if (msg.getType() == 3) {
                this.code_ = msg.getValue();
                return;
            }
            if (msg.getType() == 4) {
                 //TODO- VERIFY?
                return;
            }
        } else {
            super.processOther(data_, inPort_);
        }
    }

    /**
     * Helper class which is an object that represents dropped packet.
     */
    public class droppedPacket {
        long sender_id;
        int event_id;
        long dest_id;

        public droppedPacket(long sender_id_, long dest_id_, int event_id_) {
            sender_id = sender_id_;
            dest_id = dest_id_;
            event_id = event_id_;
        }

        public long getSenderID() { return sender_id; }
        public long getDestID() { return dest_id; }
        public int getEventID() { return event_id; }
    }
}