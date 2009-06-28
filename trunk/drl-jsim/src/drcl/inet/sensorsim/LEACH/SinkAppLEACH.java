package drcl.inet.sensorsim.LEACH;

import drcl.data.DoubleObj;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.comp.Port;

import java.util.Vector;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 04/20/2005
 *
 * This class acts as the application layer for the sink when using the LEACH scheme.
 * It simply listens on the channel for incomming data and plots graphs to update
 * results for the end operators. It will only accept LEACH related packets and
 * will ignore other packets. If running in LEACH mode a flag exists in
 * ({@link drcl.inet.mac.WirelessPhy}) which will make the base station only
 * listen for a specific spreading code.
 *
 * Since this is also considered the base station we do not worry about
 * energy and assume that this node is plugged into an outlet with
 * infite energy resources.
 *
*/

public class SinkAppLEACH extends SensorApp implements drcl.comp.ActiveComponent
{
    /*To collect and display Total packets received by sink node in graph form. Created a
    port that will output to a plotter*/
    public static final String PACKETS_REC_EVENT     = "Total Packets Received by Sink";
    public static final String PLOTTER_PORT_ID  = ".PacketsReceivedPlot";
    public Port packetsPlotPort = addEventPort(PLOTTER_PORT_ID); //for total packets received.

    /*To collect and display Total packets received by sink node in graph form. Created a
    port that will output to a plotter*/
    public static final String LATENCY_GRAPH     = "Avg. Latency between CH-Sink";
    public static final String LATENCY_PORT_ID  = ".latencyPlot";
    protected Port latencyPlotPort = addEventPort(LATENCY_PORT_ID); //for total packets received.

    //Message Constants for the LEACH protocol.
    public static final int LEACH_ADV_CH    = 0;
    public static final int LEACH_JOIN_REQ  = 1;
    public static final int LEACH_ADV_SCH   = 2;
    public static final int LEACH_DATA      = 3;
    public static final int BYTES_ID        = 2;

    //to keep track of the total number of received packets
    private int totalINpackets = 0;

    private double avgLatency = 0.0;
    private int latencyCount = 0;
    private double sumLatency = 0.0;

    //this counter will be used to keep track of how many packets that the BS would have actually
    //received if it wasn't using Clusters. In other words every CH sends one packet which combines
    //the data of all its nodes in the their respective cluster. So just to see by how much traffic
    //near the sink is reduced we also graph this plot as well.
    private int totalVirtualPackets = 0;

    public static final String VIRTUAL_PACKETS_REC_EVENT     = "Total Theoretical Packets Received";
    public static final String PLOTTER_PORT_ID_2  = ".theo_PacketsReceivedPlot";
    public Port packetsPlotPort2 = addEventPort(PLOTTER_PORT_ID_2); //for total theoretical packets received.

    {
        removeDefaultUpPort() ;
        removeTimerPort() ;
    }

    /**
     * Constructs a Sink specifically for the use in LEACH networks.
     */
    public SinkAppLEACH()
    {
        super();
        this.myPos = new double[3];
        this.myPos[0]=0.0; this.myPos[1]=0.0; this.myPos[2]=0.0;
        totalINpackets = 0;
    }

    /**
     * Handles information received over the wireless channel.
     * This function overrides its parent from SensorApp and
     * is called whenever a packet (SensorPacket) is received
     * through the wireless protocol stack
    */
    public synchronized void recvSensorPacket(Object data_)
    {
        if ( data_ instanceof SensorPacket ) {
            SensorPacket spkt = (SensorPacket)data_ ;

            //*****Cluster-Head Advertisement was Received *********************/
            if ((spkt.getPktType() == LEACH_ADV_CH)  ||
                (spkt.getPktType() == LEACH_JOIN_REQ)||
                (spkt.getPktType() == LEACH_ADV_SCH)){
                    //simply overheard...
                    //BS does not take place in clustering discard.
                    return;
            } else {

                LEACH_Data_Packet msg = (LEACH_Data_Packet) spkt.getBody();

                //***********************
                //Step 1. Always calculate latency whenever BS receives a packet
                //***********************
                //how long the packet took to get back to the Sink
                double latency = getTime() - msg.getSendTime();

                if (latencyPlotPort.anyOutConnection()) {
                    latencyPlotPort.exportEvent(LATENCY_GRAPH, latency, null);
                }
                if (latencyCount == 0) {
                    sumLatency = latency;
                    latencyCount = 1;
                }
                else {
                    latencyCount ++;
                    sumLatency = sumLatency + latency;
                }

                avgLatency = sumLatency/latencyCount;

                //***********************
                // Step 2. Update the number of total packets that have been received. This means
                // only the packets received and processed from the CHs throughout the simulation.
                //***********************
                this.totalINpackets = this.totalINpackets + 1;
                if (packetsPlotPort.anyOutConnection()) {
                    packetsPlotPort.exportEvent(PACKETS_REC_EVENT, new DoubleObj(this.totalINpackets), null);
                }
                //**********************
                // Step 3. Theoretical packets received-Since CH combined packets from all their sensors
                // into one... here we actually display the theoretical number of packets that should have
                // been received by the base station.
                //**********************
                int cluster_size = ((Vector)msg.getBody()).size();
                this.totalVirtualPackets = this.totalVirtualPackets + cluster_size;
                if (packetsPlotPort2.anyOutConnection()) {
                    packetsPlotPort2.exportEvent(VIRTUAL_PACKETS_REC_EVENT, new DoubleObj(totalVirtualPackets), null);
                }
                //**********************
                // Step 4. Extract the sensed data which is contained within the body
                // of the incomming packet and plot the resulting value
                //**********************
                if (cluster_size == 0) {
                    //there was no data in the packet
                }
                else {
                    for (int i = 0; i < cluster_size; i ++) {
                        long target_nid = ((SensorAppAgentContract.Message)((Vector)msg.getBody()).get(i)).getTargetNid();
                        lastSeenSNR = ((SensorAppAgentContract.Message)((Vector)msg.getBody()).get(i)).getSNR();
                        //System.out.println("The sink received info from sensor" + spkt.getSrc_nid() + " regarding an event from " + target_nid);
                        //System.out.println("The received signal of the phenomena is: " + lastSeenSNR);

		                Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(target_nid - first_target_nid));
		                if ( snrPort != null )
			                if ( snrPort.anyOutConnection() )
				                snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)lastSeenSNR), null);
                    }
                }
            }
        } else {
            System.out.println("LEACHSinkApp -> Warning: Received a non LEACH_SensorPacket.");
            super.recvSensorPacket(data_) ;
            return;
        }
    }

    public int getTotalINPackets()
    {
        return (this.totalINpackets);
    }

    public double getAvgLatency() {
        return (this.avgLatency);
    }

    public String getName()
    {
        return "SinkAppLEACH";
    }
}

