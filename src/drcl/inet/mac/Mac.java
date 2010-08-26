package drcl.inet.mac;

import drcl.net.Module;
import drcl.net.Packet;
import drcl.comp.Port;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/29/2005
 *
 * This base class acts as a abstraction which can aid in extending and developing
 * new MAC protocols. The fields in this class are strictly general and can
 * normally apply to various MAC protocols.
 *
 * Still needs work!
 */
public class Mac extends Module {


    /***********************************************************************/
    /** DEFINE MAC LAYER PORTS **/

    public static final String LL_PORT_ID           = ".linklayer";
    public static final String MAC_TRACE_PORT_ID    = ".mactrace";
    public static final String ENERGY_PORT_ID       = ".energy";

    public Port llPort      = addPort(LL_PORT_ID, false);
    public Port tracePort   = addPort(MAC_TRACE_PORT_ID);
    public Port energyPort  = addPort(ENERGY_PORT_ID, false);  //interface to the energy module
    /************************************************************************/


  public static final String[] MAC_STATE = {"MAC_IDLE",
                                              "MAC_POLLING",
                                              "MAC_RECV",
                                              "MAC_SEND",
                                              "MAC_RTS",
                                              "MAC_CTS",
                                              "MAC_ACK",
                                              "MAC_COLL"};

	/** Idle state */
    public static final int MAC_IDLE     = 0x0000;
	/** Polling state */
    public static final int MAC_POLLING  = 0x0001;
	/** Receiving state (which in decimal is 16)*/
    public static final int MAC_RECV     = 0x0010;
	/** Transmitting state */
    public static final int MAC_SEND     = 0x0100;
	/** RTS sent */
    public static final int MAC_RTS      = 0x0200;
	/** CTS sent */
    public static final int MAC_CTS      = 0x0400;
	/** ACK sent */
    public static final int MAC_ACK      = 0x0800;
	/** Collision state (which in decimal is: 4096)*/
    public static final int MAC_COLL     = 0x1000;
	/** Beacon transmitted */
    public static final int MAC_BEACON   = 0x2000;
	/** Inside ATIM window */
    public static final int MAC_ATIM     = 0x4000;

	/** beaconing */
    public static final int MF_BEASON   = 0x0008;
	/** used as mask for control frame */
    public static final int MF_CONTROL  = 0x0010;
	/** Announce slot open for contension */
    public static final int MF_SLOTS    = 0x001a;
	/** Request to send */
    public static final int MF_RTS      = 0x001b;
	/** Clear to send */
    public static final int MF_CTS      = 0x001c;
	/** Acknowledgement */
    public static final int MF_ACK      = 0x001d;
	/** contention free period end */
    public static final int MF_CF_END   = 0x001e;
	/** Polling */
    public static final int MF_POLL     = 0x001f;
	/** Used as a mask for data frame */
    public static final int MF_DATA     = 0x0020;
	/** Ack for data frame */
    public static final int MF_DATA_ACK = 0x0021;


    /** Broadcast mac address. */
    public static final long  MAC_BROADCAST	 = -1;

    //private   int  ETHER_HDR_LEN;
    public long   macaddr_;    //My MAC Address
    public double bandwidth_;  //Channel Bit Rate
    public double delay_;      //MAC Overhead

    /* ============================================================
       Internal MAC State
       ============================================================ */
    public int rx_state_;	    //Incoming state (MAC_RECV or MAC_IDLE)
    public int tx_state_;      //Outgoing state

    public boolean tx_active_; //Transmitter is ACTIVE or not
    public int state_;	        //MAC's current state


    /**
     * Constructor
    */
    public Mac() {
        super();
    }

    /*************************************************************************/
    /* HELPER FUNCTIONS                                                      */
    /*                                                                       */
    /*************************************************************************/

    /**
     * Calculate the transmission time of given bytes.
    */
    double txtime(int bytes) {
        return (8. * bytes / bandwidth_);
    }

    /**
     * Calculates the transmission time of a given packet.
    */
    double txtime(Packet p) {
        return 8. * (p.size) / bandwidth_;
    }

    /**
     * Gets the channel bandwidth.
    */
    public double bandwidth() { return bandwidth_; }

    /**
     * Sets the channel bandwidth.
    */
    public void setBandwidth_(double bandwidth_) {
        this.bandwidth_ = bandwidth_;
    }

    /**
     * Set the MAC address
     *
     *@param addr_  the MAC address
	*/
    public void setMacAddress(long addr_) { macaddr_ = addr_; }

    /**
     * Get the Mac address
	*/
    public long getMacAddress( ) { return macaddr_; }

    /** Set the RTS threshold (size of packet to transmit RTS) */
    public void setRTSThreshold(int rstthreshold_) {
    }

    /**
     * called from Tcl to inform the layer of its ID number
     * ID.
     * @param node_num_
    */
    public void setNode_num_(int node_num_)
    {
        //to be overiden
    }

    /** Disable PSM mode */
    public void disable_PSM()
    {
        //to be overiden
    }

    /** Turns off the MAC_TRACE_ALL_ENABLED flag. */
    public void disable_MAC_TRACE_ALL( )
    {
        //to be overiden
        //
    }

}
