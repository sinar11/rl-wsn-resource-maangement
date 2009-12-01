// @(#)WirelessPhy.java   1/2004
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

package drcl.inet.mac;

import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import java.util.*;
import drcl.comp.Port;
import drcl.inet.sensorsim.BatteryContract;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.LEACH.LEACH_Data_Packet;
import drcl.inet.sensorsim.LEACH.LEACH_Join_Packet;
import drcl.inet.sensorsim.LEACH.LEACH_SCH_Packet;
import drcl.inet.sensorsim.LEACH.LEACH_CH_Packet;
import drcl.inet.InetPacket;
import drcl.inet.mac.CSMA.Mac_Sensor_Packet;

import java.io.*;
import java.lang.Math;

/**
 * This class simulates many functions of a wireless physical card. It piggy-backs
 * various information (ie. location of the sending node, the transmission power
 * of data frame etc.) to the mac layer data frame and passes that fram to the channel.
 * While receiving a data frame from the channel component, it determines whether 
 * that frame can be decoded correctly by consulting <code>RadioPropagationModel</code>
 * and passes the decodable frame to the mac layer. It also contains a <code>EnergyModel</code>
 * component to track the energy consumption.
 * @author Ye Ge
 *
 *
 * Modified By: Nicholas Merizzi
 * June 2005
 * To take into account a cross-layer design. This module
 * which represents the hardware and control over the radio can now accept input and be
 * controlled from other higher layers (such as the application layer). Since data and
 * communication in sensor networks are all triggered and controlled by the application
 * layer it is our belief that providing more power to it will yield longer lifetime.
 * Future work will include extracting the EnergyModel and making it an independent
 * component.
*/

public class WirelessPhy extends drcl.net.Module implements ActiveComponent {

    /* configurate the ports */
    public static final String CONFIG_PORT_ID       = ".config";
    public static final String CHANNEL_PORT_ID      = ".channel";
    public static final String PROPAGATION_PORT_ID  = ".propagation";
    public static final String MOBILITY_PORT_ID     = ".mobility";
    public static final String ENERGY_PORT_ID       = ".energy";
    public static final String ANTENNA_PORT_ID      = ".antenna";           //Chunyu

    public static final String APP_PORT_ID          = ".appEnergy";         //Nicholas
    public static final String CPU_ENERGY_PORT_ID   = ".cpuEnergyPort";     //Nicholas
    public static final String CHANNEL_CHECK        = ".channelCheck";      //Nicholas

    protected Port configPort      = addPort(CONFIG_PORT_ID, false);
    /** the port receiving packets from the channel */
    protected Port channelPort     = addPort(CHANNEL_PORT_ID, false);  // the port receiving packets from the channel
    /** the port to query the path loss */
    protected Port propagationPort = addPort(PROPAGATION_PORT_ID, false); // the port to query the path loss
    /** the port to query the current position of myself  */
    protected Port mobilityPort    = addPort(MOBILITY_PORT_ID, false); // the port to query the current position of myself
    protected Port energyPort      = addPort(ENERGY_PORT_ID, false);
    /** antenna port  */
    protected Port antennaPort  = addPort(ANTENNA_PORT_ID, false); //Chunyu
    /*Nicholas: The following ports were mainly for designing a cross-layered architecture */
    public Port appPort          = addPort(APP_PORT_ID, false);
    public Port cpuEnergyPort    = addPort(CPU_ENERGY_PORT_ID, false);
    public Port MacSensorPort    = addPort(CHANNEL_CHECK);

    /*Nicholas:
    the following flags allows for the design of one hardware component but
    that can acts in various ways depending on which of the following flags
    are enabled*/
    private boolean     oneHopMode      = false;    //true iff one-hop mode w/out TDMA
    private boolean     oneHopModeTDMA  = false;    //true iff TDMA scheme
    private boolean     multiHopMode    = false;    //true iff Multi-Hop mode
    private boolean     LEACHmode       = false;    //true iff running LEACH mode.
    private boolean     steady_state    = false;    //used only for LEACH mode. Turns true if we are in steady state
    private boolean     isCH            = false;    //true iff this sensor is a Cluster head (Only for LEACH)
    private boolean     MIT_uAMPS       = false;    //Determines which radio model you want to use:
                                                    //MIT_uAMPS is the software controlled version and the non (default)
                                                    //one is the non-software controlled one.

    //This is used when sending out broadcast packets. This value is
    //set to the greatest distance between any two sensors in your
    //sensing area. In the case of the McMaster Nuclear Reactor its roughly
    //30m x 100m. Therefore taking the two extreme points gives a distance of:
    //sqrt(30^2 + 100^2) = 104.4. By setting this very high you prevent
    //the hidden terminal problem and cause all other nodes to back off.
    private double sensingArea = 105.0;

    protected static int dropped_packets = 0;   //keeps track of the number of dropped packets.

    long nid;    //this may be removed later. right now, it is same at the node id.

  	// Different Status of Radio
	public static final int RADIO_IDLE = 0;
	public static final int RADIO_SLEEP = 1;
	public static final int RADIO_OFF = 2;
	public static final int RADIO_TRANSMIT = 3;
	public static final int RADIO_RECEIVE = 4;

    private int radioMode;

    private static int numAODV = 0;
    private static int numACK = 0;
    private static int numRTS = 0;
    private static int numCTS = 0;
    private static int numUDP = 0;
    private static int numOthers = 0;
    
    ACATimer idleenergytimer_ = null;
    ACATimer lockTimer;

    ACATimer txTimer_ = null;
    ACATimer rxTimer_ = null;

    /** The energy model installed */
    EnergyModel em;

    double Pt;                          //Transmitting power
    double last_send_time;              //The last time the node sends somthing.
    double channel_become_idle_time;    //When the channel be idle again.
    double last_energy_update_time;     //The last time we update energy.
    double freq;                        // frequency
    double Lambda;	                    // wavelength (m)
    double L_ = 1.0;   	                // system loss factor
    double RXThresh;	                // receive power threshold (W)
    double CSThresh;	                // carrier sense threshold (W)
    double CPThresh;	                // capture threshold (db)
    double Gt = 1.0;   // transmitting antenna gain, should be moved to attenna component later
    double Gr = 1.0;   // receiving antenna gain, should be moved to attenna component later
    double Ht = 1.5;    //The Height of the transmitting antenna (meters)
    double Hr = 1.5;    //The height of the receiving antenna (meters)
    double bandwidth;   //Bandwidth

    /** Broadcast mac address. */
    public static final long  MAC_BROADCAST	 = -1;

    //NICHOLAS -- the following are needed for LEACH
    double Efriss_amp_ = 2.408e-10; 				// determined by the receiving threshold
	double EXcvr_ = 50 * 1e-9;				        // Energy consumed to run the radio electronics
    double Pfriss_amp_ = Efriss_amp_ * bandwidth;	// Friss base transmission power (W/m^2)
	double PXcvr_ = EXcvr_ * bandwidth;      		// Xcvr Power (W)
    int ss_ = 1;						            // amount of spreading
    int code_;                                      // the current spreading code used by this radio

    /* antenna -- Chunyu*/
    Antenna antenna = new Antenna();

    /** use this cache to speed up the simulation by avoiding unnecessary propagation loss calculation  */
    private Hashtable pathLossCache;    // use this cache to speed up the simulation by avoiding unnecessary propagation loss calculation
    private double Xc = 0.0;            // my previous position 
    private double Yc = 0.0;
    private double Zc = 0.0; 
    private double tc = -1.0;           // my cached position and the last time I consult the mobility model.
    private static double tp = -1.0;    //last time I print the statistics 

    /** Below are the tolerance of coordinate for hashing,
      * i.e., if any distance change (in any one dimension) exceeds the 
      * tolerance below, the path loss will be recalculated,
      * otherwise, it will be just pick from the cache.  
      * Notice the tolerance for Cardesian system and longitude-latitude
      * system is different.  */ 
    // At the equator, the circumference of the earth is 40,003 kilometers
    // (10.0/40003000.0)*360.0 = 9e-5
    private double xyz_tol = 10.0; 
    private double long_lat_tol = 0.00009;

    /**
      Experimental values... Similar to the ones used
      in Wendi Henzeilman PhD thesis for simulating LEACH.
    */
    static class MIT_Amp_Specs
    {
        static double freq       = 914e+6;   //914 MHz
        static double bandwidth  = 1e6;      //1000000 === 1Mb
        static double RXThresh   = 6e-9;     //watts
        static double CSThresh   = 1e-9;     //watts
        static double CPThresh   = 10;       //db
        MIT_Amp_Specs() {};
    }

    /**
     * Constructor. Sets some parameters according to a simple card. 
     */
    public WirelessPhy()
    {
        super();
        pathLossCache = new Hashtable();
        freq       = MIT_Amp_Specs.freq;
        em = new EnergyModel();
        Pt         = em.getPt();
        bandwidth  = MIT_Amp_Specs.bandwidth;
        RXThresh   = MIT_Amp_Specs.RXThresh;
        CSThresh   = MIT_Amp_Specs.CSThresh;	    // carrier sense threshold (W)
        CPThresh   = MIT_Amp_Specs.CPThresh;       // capture threshold (db)
        bandwidth  = MIT_Amp_Specs.bandwidth;
        code_ = 0;

        Lambda = 300000000.0 / freq;        // wavelength (m)
        last_send_time = 0.0;	            // the last time the node sends somthing.
        channel_become_idle_time = 0.0;	    // channel idle time.
        last_energy_update_time = 0.0;	    // the last time we update energy.
        radioMode = RADIO_IDLE;             //idle mode at startup RADIO_IDLE = 0
        this.MIT_uAMPS = false;             //NICHOLAS: dont assume LEACH model
    }


    /**
     * When this component is actived begin the idle energy updates and if
     * not running in microAmps mode then put the radio in IDLE mode.
     */
    public void _start()
    {
        if (!this.MIT_uAMPS) {
            this.radioMode = RADIO_IDLE;
        }
        idleenergytimer_ = setTimeout("IdleEnergyUpdateTimeout", 0.01);
    }

    /**
     * _stop()
    */
	protected void _stop()
    {
        if (oneHopMode) {
            radioMode = RADIO_SLEEP;
        }
	}

    /**
     * If the Component was stopped this will resume its periodic sending
    */
	protected void _resume() { }

    public void duplicate(Object source_) {
        super.duplicate(source_);
        WirelessPhy that_ = (WirelessPhy) source_;
        Pt = that_.Pt;
        RXThresh = that_.RXThresh;
        CSThresh = that_.CSThresh;
        CPThresh = that_.CPThresh;
        freq     = that_.freq;
        Lambda   = that_.Lambda;
        bandwidth  = that_.bandwidth;
        // need to duplicate the energy model?
    }

    /**
     * Sets the frequency
     * wavelenght(meters) = speed of light(which is 3x10^8meters/sec) / frequency (Mhz)
    */
    public void setFreq(double freq_ )
    {
        freq = freq_; Lambda = 300000000.0 / freq;
    }


    /**
     * Processes data frame coming from MAC component.
     */
    protected synchronized void dataArriveAtUpPort(Object data_,  drcl.comp.Port upPort_)
    {
        if ((radioMode == RADIO_SLEEP) && (!oneHopMode) && (!oneHopModeTDMA)){
            System.out.println("Attemping to send when Sensor"+this.nid + " is in sleep mode (WirelessPhy.java)");
            return;     //  packet can not be transmitted, drop siliently
        }

        if (radioMode == RADIO_OFF){
            System.out.println("Attemping to send when Sensor"+this.nid + " is off. Dropping outgoing packet at wirelessPhy.java");
            dropped_packets ++;
            return;     //  packet can not be transmitted, drop siliently
        }

        //Decreases node's energy
        if ( em.energy > 0 ) {
            double txtime 		 = ((Packet) data_).size * 8.0 / bandwidth;
            double start_time 	 = Math.max(channel_become_idle_time, getTime());
            double end_time 	 = Math.max(channel_become_idle_time, getTime()+txtime);
            double actual_txtime = end_time - start_time;
            
            // decrease the energy consumed during the period from the last energy updating 
            // time to the start of the this transmission
            if ((start_time > last_energy_update_time)&&(!MIT_uAMPS)) {
                em.RadioUpdateIdleEnergy(start_time - last_energy_update_time);
                last_energy_update_time = start_time;
            }

            double temp = Math.max(getTime(), last_send_time);
            double begin_adjust_time = Math.min(channel_become_idle_time, temp);
            double finish_adjust_time = Math.min(channel_become_idle_time, getTime()+txtime);
            double gap_adjust_time = finish_adjust_time - begin_adjust_time;

            if (gap_adjust_time < 0.0) {
                drcl.Debug.error("Negative gap time. Check WirelessPhy.java! \n");
            }

            if ((gap_adjust_time > 0.0) && (radioMode == RADIO_RECEIVE) && (!MIT_uAMPS)) {
                em.RadioUpdateTxEnergy(-gap_adjust_time);
                em.RadioUpdateRxEnergy(gap_adjust_time);
            }

            //Nicholas - Set the transmission energy to the minimum
            //required energy which is inversely based on the distance squared.
            if (MIT_uAMPS) {

                double dest_Xcoord;
                double dest_Ycoord;
                double dest_Zcoord;

                //this is for removing the energy when the radio is in
                //idle mode or sleep mode.
                if (oneHopMode){
                    em.RadioUpdateSleepEnergy(getTime()- last_energy_update_time);
                    last_energy_update_time = start_time;
                }

                updateEnergy();
                radioMode = RADIO_TRANSMIT;
                txTimer_ = setTimeout("handleTxTimeout", txtime);


                //If it is a broadcast packet then send it out to everyone
                if (((Mac_Sensor_Packet)data_).getDa() == MAC_BROADCAST) {
                    /*In order to send it out to everyone you set the
                      transmission power strong enough so that all nodes
                      can hear in the sensing field.*/

                    Pt = Efriss_amp_ * bandwidth * sensingArea * sensingArea;
                    PXcvr_ = EXcvr_ * bandwidth;    //determine the radio electronics energy that will be consumed

                    //remove the appropriate amount of energy
                    if (this.nid != 0) { //only remove energy if its not the BS which has unlimited power supply
                        em.remove(pktEnergy(Pt, PXcvr_, ((Packet)data_).size));
                        em.specialRemoveTX(pktEnergy(Pt, PXcvr_, ((Packet)data_).size)); //line not required... for Logging purposes
                    }
                } else {
                    LLPacket msg = (LLPacket)((Mac_Sensor_Packet)data_).getBody();

                    //determine where the recipient location from the LLPacket
                    dest_Xcoord = msg.getDst_Xcoord();
                    dest_Ycoord = msg.getDst_Ycoord();
                    dest_Zcoord = msg.getDst_Zcoord();

                    //determine the transmission power required
                    Pt = calculateTxPower(dest_Xcoord,dest_Ycoord,dest_Zcoord);
                    //determine the radio electronics energy that will be consumed
                    PXcvr_ = EXcvr_ * bandwidth;

                    if (this.nid != 0) { //only remove energy if its not the BS which has unlimited power supply
                        em.remove(pktEnergy(Pt, PXcvr_, ((Packet)data_).size));
                        em.specialRemoveTX(pktEnergy(Pt, PXcvr_, ((Packet)data_).size)); //line not required... for Logging purposes
                    }
                }
            }
            else {
                em.RadioUpdateTxEnergy(actual_txtime);
                if (end_time > channel_become_idle_time) {
                    radioMode = RADIO_TRANSMIT;  //Nicholas
                }
            }

            last_send_time = getTime() + txtime;
            channel_become_idle_time = end_time;
            last_energy_update_time = end_time;
        }
        else {
            // siliently discards the packet
            dropped_packets ++;
            System.out.println("NODE"+this.nid+" is dead therefore dropping outgoing packet");
            return;
        }

        double t;
        t = this.getTime();
        if ( Math.abs(t - tc) > 1.0 ) {  // the least position check interval is one second to speed up simulation
            PositionReportContract.Message msg = new PositionReportContract.Message();
            msg = (PositionReportContract.Message) mobilityPort.sendReceive(msg);
            Xc = msg.getX();
            Yc = msg.getY();
            Zc = msg.getZ();
            tc = t;

        }

        //System.out.println("Sensor " +this.nid+": out power is: " + Pt);/*\nOutgoing packet: " + ((Packet)data_).toString());
        //System.out.println("time: " + getTime()+" its size is: " + ((Packet)data_).size + "\n");*/

        downPort.doSending(new NodeChannelContract.Message(nid, Xc, Yc, Zc, Pt, Gt, data_));

        //Add by Honghai for debugging
        if (isDebugEnabled()  ) {
        	if (t - tp > 1.0)  {
         		printPktStat();
        		tp = t;
        	}
        	String pktType = ((Packet)data_).getPacketType();
        	if (pktType.equals("AODV") ) numAODV ++;
        	else if (pktType.equals("MAC-802.11_ACK_Frame") ) numACK++;
        	else if (pktType.equals("MAC-802.11_RTS_Frame") ) numRTS++;
        	else if (pktType.equals("MAC-802.11_CTS_Frame") ) numCTS++;
        	else if (pktType.equals("UDP") ) numUDP++;
        	else {
        		numOthers++;
        		System.out.println("type <" + pktType + ">" );
			}
		}
    }


    /**
     * Processes the received frame.
    */
    protected synchronized void dataArriveAtChannelPort(Object data_)
    {
        double Pr;
        double Loss;
        double Pt_received;    // Pt of the received packet
        double Gt_received;    // Gt of the received packet
        double Xs, Ys, Zs;     // position of the sender
        boolean incorrect = false;
        Packet pkt;
        double t = this.getTime();

        if ( Math.abs(t - tc) > 1.0 ) {      // the least position check interval is one second to speed up simulation
            PositionReportContract.Message msg = new PositionReportContract.Message();
            msg = (PositionReportContract.Message) mobilityPort.sendReceive(msg);
            Xc = msg.getX();
            Yc = msg.getY();
            Zc = msg.getZ();
            tc = t;
        }

        NodeChannelContract.Message msg2 = (NodeChannelContract.Message) data_;
        Xs = msg2.getX();
        Ys = msg2.getY();
        Zs = msg2.getZ();
        Gt_received = msg2.getGt();
        Pt_received = msg2.getPt();

        String type = antenna.QueryType();
        Antenna.Orientation incomingOrient = new Antenna.Orientation(0, 0);
        if (!type.equals("OMNIDIRECTIONAL ANTENNA")) {
            // add by Chunyu -- calculate the gain from uni-/omni-directional antenna gain
            // 1. calculate the incoming angle
            incomingOrient = CalcOrient (Xc, Yc, Zc, Xs, Ys, Zs);
            // 2. get the antenna gain in dBi and convert it to absolute value
            Gr = Math.exp (0.1 * antenna.getGain_dBi(incomingOrient) );
        }

        Long sid = new Long(msg2.getNid());


        boolean cacheHit = false;
        Loss = 1.0;        // Loss will be set to proper value below, here I set it to 1.0 to avoid the compiler's complaint

        if ( pathLossCache.containsKey(sid) ) {
            CachedPathLoss c = (CachedPathLoss) (pathLossCache.get(sid));
            if (RadioPropagationModel.isCartesianCoordinates()) {
                if (Math.abs(c.xs - Xs) <= xyz_tol && Math.abs(c.ys - Ys) <= xyz_tol &&
                             Math.abs(c.zs - Zs) <= xyz_tol && Math.abs(c.xr - Xc) <= xyz_tol &&
                             Math.abs(c.yr - Yc) <= xyz_tol && Math.abs(c.zr - Zc) <= xyz_tol )   {
                    cacheHit = true;
                    Loss = c.loss;
                }
            } else {
                if (Math.abs(c.xs - Xs) <= long_lat_tol && Math.abs(c.ys - Ys) <= long_lat_tol &&
                    Math.abs(c.zs - Zs) <= xyz_tol && Math.abs(c.xr - Xc) <= long_lat_tol &&
                    Math.abs(c.yr - Yc) <= long_lat_tol && Math.abs(c.zr - Zc) <= xyz_tol )  	{

                    cacheHit = true;
                    Loss = c.loss;
                }
            }
        }

        if ( cacheHit == false ) {
            RadioPropagationQueryContract.Message msg3 = (RadioPropagationQueryContract.Message) propagationPort.sendReceive(new RadioPropagationQueryContract.Message(Lambda, Xs, Ys, Zs, Xc, Yc, Zc ));
            Loss = msg3.getLoss();
            CachedPathLoss c = new CachedPathLoss(Xc, Yc, Zc, Xs, Ys, Zs, Loss);
            pathLossCache.put(sid, c);
        }

        /*System.out.println("Pt_received is: " + Pt_received);
        System.out.println("Gt_received is: " + Gt_received);
        System.out.println("Loss factor is: " + Loss); */

        Pr = Pt_received * Gt_received * Gr * Loss;

        //System.out.println("Pr is then: " + Pr);

        //Added by Nicholas: if dead do not continue and just drop the packet
        if ((radioMode == RADIO_OFF)||((em.getEnergy() <= 0.0))){
            //System.out.println("The Packet from sensor" + msg2.getNid() +" has been dropped at sensor" + nid + " because the node is not on");
            return;     //  packet can not be Received, drop siliently
        }

        if (radioMode == RADIO_SLEEP) {
            // if the node is in sleeping mode then it depends what mode you are running.
            if ((oneHopMode) || (LEACHmode) || (oneHopModeTDMA)) {
                //Since every node shut downs when its not its turn to transmit we just ignore
                //overheard packets and drop them.
                return;
            } else {
                //System.out.println("The Packet from sensor" + msg2.getNid() +" has been dropped at sensor" + nid +" because the node is sleeping");
                //silently discard.
            }
            return;
        }

        pkt = (Packet) msg2.getPkt();

        if ( Pr < CSThresh) {
            if ((!multiHopMode) && (!LEACHmode) && (!oneHopMode)){
                System.out.println("The Packet from sensor" + msg2.getNid() +" has been dropped at sensor" + nid +" because Pr is too weak");
                System.out.println("Below CSThresh which is : " + CSThresh);
                System.out.println("Received power was: " + Pr +"\n");
            }
            return;
        }

        if ( Pr < RXThresh) {
            // can detect, but not successfully receive this packet. Mark the packet erro;
            if ((!multiHopMode) && (!LEACHmode)&& (!oneHopMode)){
                System.out.println("The Packet from sensor" + msg2.getNid() +" has been dropped at sensor" + nid + " because it was below threshold");
                System.out.println("Below RXThresh which is : " + RXThresh);
                System.out.println("Received power was: " + Pr+"\n");
            }
            incorrect = true;
        }

        //if modulation is simulated, mark packek decoding error here

        /* The MAC layer must be notified of the packet reception
         * now - ie; when the first bit has been detected - so that
         * it can properly do Collision Avoidance / Detection.
        */

        /*Decrease energy if packet successfully received */
        double rcvtime = (8. * ((Packet) pkt).size) / bandwidth;
        double start_time = Math.max(channel_become_idle_time, getTime());
        double end_time = Math.max(channel_become_idle_time, getTime() + rcvtime);
        double actual_rcvtime = end_time-start_time;


        /*
        * If I am not receiving the code of the incoming packet, drop it.
        * (Used in LEACH only!)
        * I am aware that the following is extremely ugly casting... and should not be used.
        * will be fixed in future revisions. I just keep casting to unwrap the packet completely
        * to see what the sending code was.
        * two cases:
        * Case 1: If i am running leach, and in the steady state and not the sink
        * Case 2: Running LEACHmode and I am the sink (then always discard packets that aren't destined for you
        */
        if (((LEACHmode)&&(steady_state)&&(this.nid != 0)) || ((LEACHmode)&&((this.nid != 0)))) {
            int new_code =-1;

            Mac_Sensor_Packet packet = (Mac_Sensor_Packet)msg2.getPkt();
            //Mac_Sensor_Packet packet = (Mac_Sensor_Packet)((NodeChannelContract.Message)data_).getPkt();

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

            if (new_code != code_) {
                //ignore packet its not your code
                return;
            }
        }

        //NICHOLAS: if using CSMA and LEACH combination then
        //calculate the energy consumed by the electronics for
        //receiving a packet of this size and remove it from the battery
        if (this.MIT_uAMPS) {

            updateEnergy();
            radioMode = RADIO_RECEIVE;
            //rxTimer_ = setTimeout("handleRxTimeout", rcvtime);
            rxTimer_ = setTimeout("handleRxTimeout", actual_rcvtime);

            if (start_time > last_energy_update_time) {
                em.RadioUpdateIdleEnergy(start_time-last_energy_update_time);
                last_energy_update_time = start_time;
            }
            em.RadioUpdateRxEnergy(actual_rcvtime);


            if (end_time > channel_become_idle_time) {
                radioMode = RADIO_RECEIVE;
            }

            /*PXcvr_ = EXcvr_ * bandwidth;

            if (this.nid != 0) { //only remove energy if its not the BS which has unlimited power supply
                em.remove(this.pktEnergy(0, PXcvr_,((Packet) pkt).size));
                em.specialRemoveRX(pktEnergy(0, PXcvr_,((Packet) pkt).size)); //line not required... for Logging purposes
            }*/

        }
        else {

            if (start_time > last_energy_update_time) {
                em.RadioUpdateIdleEnergy(start_time-last_energy_update_time);
                last_energy_update_time = start_time;
            }
            em.RadioUpdateRxEnergy(actual_rcvtime);

            if (end_time > channel_become_idle_time) {
                radioMode = RADIO_RECEIVE;
            }

        }

        channel_become_idle_time = end_time;
        last_energy_update_time = end_time;

        /* added by Chunyu Aug. 05, 2002
         *  1. lock on the signal if the antenna is not locked
         *  2. if lock succeeds, recalculate Graphics= antenna.getGain() and Pr
         *  3. set a timer to unlock the antenna, which times out at end_time
        */
        if ( !type.equals("OMNIDIRECTIONAL ANTENNA") && Pr >= RXThresh && !antenna.isLocked()) {
            antenna.lockAtSignal (incomingOrient);
            Gr = Math.exp (0.1 * antenna.getGain_dBi (incomingOrient) );
            //	incomingOrient.azimuth + ". Gr = " + Gr); //for debug
            Pr = Pt_received * Gt_received * Gr * Loss;
            lockTimer = setTimeout ("AntennaLockSignal_TimeOut", end_time-getTime());
        } //endif

        // MacPhyContract.Message is defined to convey all necessary information to the MAC component.
        MacPhyContract.Message msg4 = new MacPhyContract.Message(incorrect, Pr, CPThresh, CSThresh, RXThresh, pkt);
        upPort.doSending(msg4);
    }


    /**
     * added by Chunyu -- 08.08.2002
     * Calculates the orientation of the sender (Xt, Yt, Zt) in regards to 
     * the receiver's position (Xr, Yr, Zr) 
     *
    */
    protected Antenna.Orientation CalcOrient
    (
        double Xr, double Yr, double Zr, 
        double Xt, double Yt, double Zt) 
    {
        double delta_x, delta_y, delta_z, delta_xy;
        double alfa = 0, beta = 0;

        delta_x = Xt-Xr;
        delta_y = Yt-Yr; 
        delta_z = Zt-Zr;
        delta_xy = Math.sqrt(delta_x*delta_x + delta_y*delta_y);
        
        if (delta_x==0) {
            if (delta_y==0) alfa = 0;
            else if(delta_y>0) alfa = 90;
            else alfa = 270;
        }else {
            alfa = Math.toDegrees (Math.abs (Math.atan(delta_y/delta_x)));
            if (delta_x>0 && delta_y>=0) ;
            else if (delta_x<0 && delta_y >=0) alfa = 180-alfa;
            else if (delta_x<0 && delta_y < 0) alfa = 180+alfa;
            else if (delta_x>0 && delta_y < 0) alfa = 360-alfa;
        }

        if (delta_xy==0){
            if (delta_z==0) beta = 0;
            else if (delta_z>0) beta = 90;
            else beta = 270;
        } else {
            beta = Math.toDegrees (Math.abs (Math.atan(delta_z/delta_xy)));
            if (delta_xy>0 && delta_z>=0) ;
            else if (delta_xy<0 && delta_z >=0) beta = 180 - beta;
            else if (delta_xy<0 && delta_z >=0) beta = 180 + beta;
            else if (delta_xy>0 && delta_z < 0) beta = 360 - beta;
        }
                        
        return new Antenna.Orientation ((int)alfa, (int)beta);
    } //end CalcOrient
        
    /**
     * Configures the node's antenna from assigned port.
     */
    protected void configAntenna (Object data_)
    {
        String args = ((String)data_).toLowerCase(), value;

        /* create an antenna */
        if (args.startsWith("create")) {
            String ant = args.substring (args.indexOf("create")+ 6);
            ant = ant.trim();

            if (ant.equals("antenna")) {
                //antenna = new Antenna();
                return;
            } 

            if (ant.equals("switchedbeam antenna")) {
                antenna = new SwitchedBeamAntenna();
                return;
            }

            if (ant.equals("adaptive antenna")) {
                antenna = new AdaptiveAntenna();
                return;
            }

            System.out.println ("FORMAT erorr! shall be <create antenna/switchedbeam antenna/adaptive antenna>");
            return;
        } //endif "create"

        /* Query the antenna type*/
        if (args.startsWith ("querytype")) {
            System.out.println (antenna.QueryType());
            return;
        } //endif "QueryType"

        /* initialization work */
        int index;
        if ( (index = args.indexOf ('=')) != -1) {                            
            value = (args.substring(index+1)).trim();            
            if (value.equals(null)) {
            System.out.println (this + ":: pls. use the format such as <height = 1.5>");
            return;               
        } //endif value

        if (args.indexOf ("height")!=-1) {
            float height = Float.parseFloat(value);
            antenna.setHeight (height);
            System.out.println ("set height = " + antenna.setHeight(height));
            return;
        } //endif "height"

        if (args.indexOf ("omnigain_dbi")!=-1) {
            float omniGain_dBi = Float.parseFloat(value);
            antenna.setOmniGain_dBi(omniGain_dBi);
            System.out.println ("set omniGain_dBi = " + antenna.getGain_dBi());
            return;
        } //endif "omniGain_dBi"

        if (args.indexOf ("azimuthpatterns")!=-1) {
            try {
                BufferedReader in = 
                    new BufferedReader (new FileReader(value));
                in.close();
            } catch (java.io.IOException e) {
                System.out.println (this + ":: error in opening " + value);
                return;
            } //endtry

            if (antenna.initAzimuthPatterns (value))
                ;//System.out.println (this + "Successfully initialize the azimuth pattern file!");
            else
                System.out.println (this + "Failure in initializing the azimuth pattern file!");
            return;
        } //endif "azimuthPatterns"

        if (args.indexOf ("elevationpatterns")!=-1) {
            try {
                BufferedReader in = 
                    new BufferedReader (new FileReader(value));
                in.close();
            } catch (java.io.IOException e) {
                System.out.println (this + ":: error in opening " + value);
                return;
            } //endtry

            if (antenna.initElevationPatterns (value))
                System.out.println (this + "Successfully initialize the elevation pattern file!");                    
            else
                System.out.println (this + "Failure in initializing the evlation pattern file!");
            // antenna transmission gain, should be moved to attenna component later
            return;
        } //endif "elevationPatterns"                

        System.out.println (" Wrong format: no such initialization item!");

        } //endif ' ... = ...'

        System.out.println ("Wrong format to communicate with the Antenna component!");
            
    } //end configAntenna

    /**
     * Process incomming data
     * @param data_
     * @param inPort_
    */
    protected synchronized void processOther(Object data_, Port inPort_)
    {
        String portid_ = inPort_.getID();
        
        if (portid_.equals(CHANNEL_PORT_ID)) {
            if (!(data_ instanceof NodeChannelContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            dataArriveAtChannelPort(data_);  // a packet is "heard" from the channel
            return;
        }
        /*This allows for the MAC layer to connect to the wirelessPhy layer*/
        if (portid_.equals(ENERGY_PORT_ID)){

            if (!(data_ instanceof EnergyContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            EnergyContract.Message msg = (EnergyContract.Message)data_;
            switch(msg.getType()){
                case 0:     //ENERGY_QUERY
                    energyPort.doSending(new EnergyContract.Message(0,em.getEnergy(),this.radioMode));
                    break;
                case 1:    //SET_RADIO_MODE
                    this.radioMode = msg.getRadioMode();
                    //System.out.println("sorry 2 setting mode to:"+msg.getRadioMode());
                    energyPort.doSending(new EnergyContract.Message(1, em.getEnergy(), this.radioMode));
                    break;
                case 2:      //GET_RADIO_MODE
                    energyPort.doSending(new EnergyContract.Message(2, em.getEnergy(), this.radioMode));
                    break;
                default:
                    System.out.println("wirelessPhy: received incorrect request!");
            }
            return;
        }

        if (portid_.equals(APP_PORT_ID)) {
            /**Query the position of the node **/
            if (data_ instanceof DoubleObj) {
                //this means the application wants to know its position
                PositionReportContract.Message msg = new PositionReportContract.Message();
                msg = (PositionReportContract.Message) mobilityPort.sendReceive(msg);
                appPort.doSending(msg); //sends back the position (PositionReportContract used)
                return;
            }
            else {
                if (!(data_ instanceof EnergyContract.Message)) {
                    error(data_, "processOther()", inPort_, "unknown object");
                    return;
                }
                EnergyContract.Message msg = (EnergyContract.Message)data_;
                switch(msg.getType()){
                    case 0:     //ENERGY_QUERY = 0
                        appPort.doSending(new EnergyContract.Message(0,em.getEnergy(),this.radioMode));
                        break;
                    case 1:     //SET_RADIO_MODE = 1
                        this.radioMode = msg.getRadioMode();
                        //System.out.println("Sensor" +this.nid+" Radio mode was changed at " + getTime() + " to " + radioMode);
                        appPort.doSending(new EnergyContract.Message(1, em.getEnergy(), this.radioMode));
                        break;
                    case 2:     //GET_RADIO_MODE = 2
                        appPort.doSending(new EnergyContract.Message(1, em.getEnergy(), this.radioMode));
                        break;
                    case 3:     //SET_STEADY_STATE = 3
                        steady_state = msg.get_steady_state();
                        isCH = msg.isCH_();
                        appPort.doSending(new EnergyContract.Message(3,this.steady_state,this.isCH));
                        break;
                    case 4:     //SET_ENERGY_LEVEL = 4
                        em.setEnergy(msg.getEnergyLevel());
                        break;
                    case 5:   //setting the spread spectrum code to use
                        this.code_ = msg.getRadioMode();
                        appPort.doSending(new EnergyContract.Message(5,em.getEnergy(),this.code_));
                        break;
                    default:
                        System.out.println("wirelessPhy: received incorrect request!");
                }
            }
            return;
        }
        /*CPU utilization update*/
        if (portid_.equals(CPU_ENERGY_PORT_ID)) {
            if (data_ instanceof BatteryContract.Message) {
                BatteryContract.Message msg = (BatteryContract.Message)data_;

                //CPU_IDLE= 0; CPU_SLEEP= 1;CPU_ACTIVE= 2; CPU_OFF = 3;
                switch(msg.getType()) {
                    case 0:
                        em.CPUupdateIdleEnergy(msg.getTime());
                        break;
                    case 1:
                        em.CPUupdateSleepEnergy(msg.getTime());
                        break;
                    case 2:
                        em.CPUupdateActiveEnergy(msg.getTime());
                        break;
                    case 3:
                        em.CPUupdateOffEnergy(msg.getTime());
                        break;
                }
            }
            return;
        }
        /* antenna -- Chunyu */
        if (portid_.equals (ANTENNA_PORT_ID)) {
            configAntenna (data_);
            return;
        }

        if (portid_.equals(CHANNEL_CHECK)) {
            //if (data_ instanceof IntObj) {
                MacSensorPort.doSending(new IntObj(radioMode));
            //}
            return;
        }
        System.out.println("RECEIVED UNKNOWN ENTRY its type is: " + data_.getClass());
        System.out.println("Its port ID is: " + inPort_.id +"---"+  inPort_.getClass());
        super.processOther(data_, inPort_);
    }

    void printPktStat()
    {
        StringBuffer sb_ = new StringBuffer(toString());
        sb_.append("AODV packet: " + numAODV);
        sb_.append("\tRTS packet: " + numRTS);
        sb_.append("\tCTS packet: " + numCTS);
        sb_.append("\tACK packet: " + numACK);
        sb_.append("\tUDP packet: " + numUDP);
        sb_.append("\tOther packet: " + numOthers);
        debug(sb_.toString() );
    }

    void logEnergy() {
        //drcl.Debug.debug("At time: "+ getTime() + " Node" + nid + " remaining energy = " + em.getEnergy() + "\n");
    }


    /**
     * Handles expired timers
     */
    public synchronized void timeout(Object data_)
    {
        if ( data_ instanceof String ){

            //Preriodically timeout to update energy consumption even if it is in idle state.
            if ( ((String) data_).equals("IdleEnergyUpdateTimeout") && (!oneHopMode) ) {
                if ( em.getEnergy() > 0 ) {
                    logEnergy();
                }
                updateEnergy();

                idleenergytimer_ = setTimeout("IdleEnergyUpdateTimeout", 0.02);
                return;
            }

            if ( ((String) data_).equals("AntennaLockSignal_TimeOut") ) {
                antenna.unlock();
                return;
            }

            if (((String) data_).equals("handleTxTimeout")){
                if ((oneHopModeTDMA) || (oneHopMode)) {
                    //after transmission is complete put back to sleep
                    radioMode = RADIO_SLEEP;
                    updateEnergy();
                    return;
                }
                else if ((LEACHmode) && (!isCH) && (steady_state) && (this.nid != 0)) {
                    //System.out.println("Sensor" + this.nid + " radio has been put to sleep because we are in LEACH mode!!!");
                    radioMode = RADIO_SLEEP;
                    updateEnergy();
                    return;
                }
                else {
                    radioMode = RADIO_IDLE;
                    updateEnergy();
                    return;
                }
            }

            if (((String) data_).equals("handleRxTimeout")){
                radioMode = RADIO_IDLE;
                updateEnergy();
                return;
            }
        }
    }
    
    /**
     * updates energy consumption during the idle state
     * or sleep state.
     */
    protected void updateEnergy()
    {
        if ((!oneHopMode) && (this.nid != 0)) {
            if ( getTime() > last_energy_update_time) {
                if(em.getEnergy()> 0.0)
                {
                    if(radioMode == RADIO_SLEEP) {
                        em.RadioUpdateSleepEnergy(getTime() - last_energy_update_time);
                    }
                    if (radioMode == RADIO_IDLE) {
                        em.RadioUpdateIdleEnergy(getTime() - last_energy_update_time);
                    }
                }
                last_energy_update_time = getTime();
            }
        }
        //if its the base station we always assume it has full energy... so just
        //replenish it.
        if (this.nid == 0) {
            em.setEnergy(100);
        }
        return;
    }

    /*Setters*/
    public void setLEACHMode(boolean LEACHmode_) { this.LEACHmode = LEACHmode_; }
    public void setMultiHopMode(boolean multiHopMode_) { this.multiHopMode = multiHopMode_; }
    public void setOneHopMode(boolean oneHopMode_) { oneHopMode = oneHopMode_; radioMode = RADIO_SLEEP; }
    public void setOneHopModeTDMA(boolean oneHopModeTDMA_) { this.oneHopModeTDMA = oneHopModeTDMA_; }
    public void setMIT_uAMPS(boolean MIT_uAMPS) { this.MIT_uAMPS = MIT_uAMPS; }
    public void setNid(long nid_) { nid = nid_; em.setNid(this.nid); }
    public void setPt(double Pt_){ Pt = Pt_;  em.setPt(Pt_); }
    public void setRxThresh(double RXThresh_) {RXThresh = RXThresh_;  }
    public void setCSThresh(double CSThresh_) { CSThresh = CSThresh_; }
    public void setCPThresh(double CPThresh_) { CPThresh = CPThresh_; }
    public void setInitialEnergy(double energy){
    	em.setEnergy(energy);
    }
    /*Getters*/
    public int getRadioMode() { return radioMode; }
    public long getNid(long nid_) { return nid; }
    public Port getChannelPort() { return channelPort; }
    public String getName() { return "WirelessPhy"; }

    //Obtain how much energy was spent in the various radio modes
    public double getRadioTotalTX() { return( em.getTotalRadioTx());  }
    public double getRadioTotalRX() { return(em.getTotalRadioRx());   }
    public double getRadioTotalidle() { return(em.getTotalRadioIdle());  }
    public double getRadioTotalsleep() { return(em.getTotalRadioSleep()); }

    //find out how much time was spent in each of the radio modes
    public double getTotalRadioSleepTime() { return em.getTotalRadioSleepTime(); }
    public double getTotalRadioActiveTime() { return em.getTotalRadioActiveTime(); }
    public double getTotalRadioIdleTime() { return em.getTotalRadioIdleTime(); }
    public double getTotalRadioRxTime() { return em.getTotalRadioRxTime(); }

    //find out how much energy the CPU spent in the various modes
    public double getTotalCPUsleep() { return em.getTotalCPUsleep(); }
    public double getTotalCPUactive() { return em.getTotalCPUactive();}

    //obtain how much time the CPU was in the various modes
    public double getTotalCPUsleepTime() { return em.getTotalCPUsleepTime(); }
    public double getTotalCPUactiveTime() { return em.getTotalCPUactiveTime(); }

    //obtain aggregate results
    public double getTotalCPU() {  return(em.getTotalCPU()); }
    public double getRemEnergy() { return(em.getEnergy());  }
    public int getDropped_packets() {return dropped_packets; }

    //to know when the last update was done when calculating idle time for accuracy reasons
    public double getLastUpdateTime() { return last_energy_update_time; }


    /**
     * The power for transmission depends on the distance between
     * the transmitter and the receiver.  If this distance is
     * less than the crossover distance:
     *       (c_d)^2 =  16 * PI^2 * L * hr^2 * ht^2
     *               ---------------------------------
     *                           lambda^2
     * the power falls off using the Friss equation.  Otherwise, the
     * power falls off using the two-ray ground reflection model.
     * Therefore, the power for transmission of a bit is:
     *      Pt = Pfriss_amp_*d^2 if d < c_d
     *      Pt = Ptwo_ray_amp_*d^4 if d >= c_d.
     * The total power dissipated per bit is PXcvr_ + Pt.
     *
     * Note: Two-ray model is not implemented in this current
     * version!
    */
    protected synchronized double calculateTxPower(double x_dest, double y_dest, double z_dest)
    {
        double Pt_;
        double distance;

        //get an up-to-date position report
        double t;
        t = this.getTime();
        if ( Math.abs(t - tc) > 1.0 ) {  // the least position check interval is one second to speed up simulation
            PositionReportContract.Message msg = new PositionReportContract.Message();
            msg = (PositionReportContract.Message) mobilityPort.sendReceive(msg);
            Xc = msg.getX();
            Yc = msg.getY();
            Zc = msg.getZ();
            tc = t;
        }
        distance = EuclideanDist(Xc, Yc, Zc, x_dest, y_dest, z_dest);
        Pt_ = Efriss_amp_ * bandwidth * distance * distance;
        return (Pt_);
    }

    /**
     * Energy (in Joules) is power (in Watts=Joules/sec) divided by
     * bandwidth (in bits/sec) multiplied by the number of bytes, times 8 bits.
    */
    public double pktEnergy(double pt, double pxcvr, int nbytes)
    {
        // If data has been spread, power per DATA bit should be the same
        // as if there was no spreading ==> divide transmit power
        // by spreading factor.
        double bits = (double) nbytes * 8;
        pt /= ss_;

        double j = (bits * (pt + pxcvr)) / bandwidth;
        return(j);
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
}



