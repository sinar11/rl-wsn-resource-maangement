package drcl.inet.sensorsim.OneHop;

import drcl.data.DoubleObj;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.inet.mac.EnergyContract;
import drcl.comp.Port;


/**
 * @author Nicholas Merizzi
 * @version 1.0, 04/20/2005
 *
 * This class acts as the application layer for the sink when using the one-hop scheme.
 * It simply listens on the channel for incomming data and plots graph to update
 * results for the end operators.
 *
*/

public class SinkAppOH extends SensorApp implements drcl.comp.ActiveComponent
{
    /*To collect and display Total packets received by sink node in graph form. Created a
    port that will output to a plotter*/
    public static final String PACKETS_REC_EVENT     = "Total Packets Received by Sink";
    public static final String PLOTTER_PORT_ID  = ".PacketsReceivedPlot";
    public Port packetsPlotPort = addEventPort(PLOTTER_PORT_ID); //for total packets received.

    /*To collect and display Total packets received by sink node in graph form. Created a
    port that will output to a plotter*/
    public static final String LATENCY_GRAPH     = "Avg. Latency between Sensor-Sink";
    public static final String LATENCY_PORT_ID  = ".latencyPlot";
    protected Port latencyPlotPort = addEventPort(LATENCY_PORT_ID); //for total packets received.

    private int totalINpackets = 0; //to keep track of the total number of received packets

    private double avgLatency = 0.0;
    private int latencyCount = 0;
    private double sumLatency = 0.0;
	{
		removeDefaultUpPort() ;
		removeTimerPort() ;
	}

    public SinkAppOH()
    {
        super();
        this.myPos = new double[3];
        this.myPos[0]=0.0; this.myPos[1]=0.0; this.myPos[2]=0.0;
        totalINpackets = 0;
    }

    /**
     * _start()
     *   This method is called when attempting to 'run' the component
     *   in TCL.
    */
    protected void _start ()
    {
        //since its the BS keep the energy to the highest level      ---  SET_ENERGY_LEVEL = 4
        wirelessPhyPort.doSending(new EnergyContract.Message(4, 1000.0,-1));
    }

    /**
     * This function is called whenever a packet (SensorPacket) is
     * received through the wireless protocol stack
    */
	public synchronized void recvSensorPacket(Object data_)
    {
		if ( data_ instanceof SensorPacket ) {
			SensorPacket spkt = (SensorPacket)data_ ;

            //***********************
            // Step 1. Always calculate latency whenever BS receives a packet
            //***********************
            //how long the packet took to get back to the Sink
            double latency = getTime() - ((OH_Packet)spkt.getBody()).getSendTime();

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
            // Step 2. Update the number of total packets that have been received.
            //***********************
            this.totalINpackets = this.totalINpackets + 1;
            if (packetsPlotPort.anyOutConnection()) {
                packetsPlotPort.exportEvent(PACKETS_REC_EVENT, new DoubleObj(this.totalINpackets), null);
            }

            //**********************
            // Step 3. Extract the sensed data which is contained within the body
            // of the incomming packet and plot the resulting value
            //**********************
            long target_nid = ((SensorAppAgentContract.Message)((OH_Packet)spkt.getBody()).getBody()).getTargetNid();
            lastSeenSNR = ((SensorAppAgentContract.Message)((OH_Packet)spkt.getBody()).getBody()).getSNR();
            //System.out.println("The sink received info from sensor" + spkt.getSrc_nid() + " regarding an event from " + target_nid);
            //System.out.println("The received signal of the phenomena is: " + lastSeenSNR);

		    Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(target_nid - first_target_nid));
		    if ( snrPort != null )
			    if ( snrPort.anyOutConnection() )
				    snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)lastSeenSNR), null);

		} else
			super.recvSensorPacket(data_) ;
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
        return "SinkAppOH";
    }

}
