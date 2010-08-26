package drcl.inet.sensorsim.OneHopTDMA;

import drcl.comp.ACATimer;
import drcl.comp.Port;
import drcl.data.DoubleObj;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.inet.mac.EnergyContract;

import java.util.Random;
import java.text.DecimalFormat;

/**
 *
 * @author Nicholas Merizzi
 * @version 1.0, 06/06/2005
 *
 * This class acts as the application layer for a base station when simulating a
 * One-hop TDMA scheme. All sensors will run {@link OneHopAppTDMA} as their application
 * layer protocol. This class takes care of sending out the updated periodic
 * schedule that determines when each sensor will send.
 * Otherwise its task is to simply listen for incomming data and to update aggregate
 * results by use of plotters.
*/

public class SinkAppTDMA extends SensorApp implements drcl.comp.ActiveComponent
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
    Random generator = new Random();

    private double avgLatency = 0.0;
    private int latencyCount = 0;
    private double sumLatency = 0.0;

    private static int SINK_SCH_BCAST = 1;

    ACATimer adv_sch_timer ;

	{
		removeDefaultUpPort() ;
	}

    public SinkAppTDMA()
    {
        super();
        this.myPos = new double[3];
        this.myPos[0]=0.0; this.myPos[1]=0.0; this.myPos[2]=0.0;
        totalINpackets = 0;
        adv_sch_timer = null ;
    }

    /**
     * Used to create the global schedule to be used by all
     * sensors.
    */
    public void adv_schedule()
    {
        DecimalFormat timeFormat = new DecimalFormat("#0.00");

        System.out.println("**** Sink sending out new schedule at time " + timeFormat.format(getTime()) + " ****");

        /*The data size of the broadcast packet...
          total size is  after lower layers
          encapsulate it with their headers*/
        int dataSize = 12;

        frame_time_TDMA = nn_ * slot_time_TDMA;
        new_schedule_adv_TDMA = ((5.0*frame_time_TDMA) + (4.0*gap_time_TDMA))+ 2.0 + buffer_TDMA;

        if(nn_ == 1) {
            System.out.println("Warning: There are no Sensors in Reactor");
            this.stop();
        }
        else {
            //Set the TDMA schedule and send it to all nodes in the cluster.

            //send out a bcast to all nodes if it has the energy
            if (!(this.sensorDEAD)) {
                downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, SINK_SCH_BCAST, this.nid, dataSize, schedule));
                eID = eID + 1;
            } else {
                System.out.println("You are trying to send with dead sensor"+this.nid);
                return;
            }
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
        //the next frame is schedule to start in 2 seconds from now.
        //start_time_TDMA = getTime() + 2.0;
        start_time_TDMA = 2.0 - getTime();

        //Broadcast out the TDMA schedule to all nodes at some point between [0,1]
        //adv_sch_timer = setTimeout("SCH_ADV",generator.nextDouble()+0.1);
        adv_sch_timer = setTimeout("SCH_ADV",0.2);

        //since its the BS keep the energy to the highest level      ---  SET_ENERGY_LEVEL = 4
        wirelessPhyPort.doSending(new EnergyContract.Message(4, 1000.0,-1));
    }

    /**
     * _stop()
     *  This cancels the components timer ultimately stopping the periodic
     *  sending back to the sink node.
    */
	protected void _stop()
    {
        if (adv_sch_timer != null)
            cancelTimeout(adv_sch_timer);
	}

    /**
     * If the Component was stopped this will resume its periodic sending
    */
	protected void _resume() {
        adv_sch_timer = setTimeout("SCH_ADV", generator.nextDouble());
    }


    /**
     *  This method is invoked when the components timer reaches zero.
     *  @param data_
    */
    protected synchronized void timeout(Object data_)
    {
        if(data_.equals("SCH_ADV")) {
            this.adv_schedule();
            //sensor_wakeup_time_TDMA = getTime() + new_schedule_adv_TDMA;
            sensor_wakeup_time_TDMA = new_schedule_adv_TDMA-buffer_TDMA;
            adv_sch_timer = setTimeout("SCH_ADV", new_schedule_adv_TDMA);

            return;
        }
        else {
            return;
        }
    }


    /**
     * This function is called whenever a packet (SensorPacket) is
     * received through the wireless protocol stack
    */
	public synchronized void recvSensorPacket(Object data_)
    {

		if ( data_ instanceof SensorPacket ) {
			SensorPacket spkt = (SensorPacket)data_ ;

            /************************
             * Step 1. Always calculate latency whenever BS receives a packet
             ***********************/
            //how long the packet took to get back to the Sink
            double latency = getTime() - ((OH_TDMA_Packet)spkt.getBody()).getSendTime();

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
            /************************
             * Step 2. Update the number of total packets that have been received.
            ***********************/
            this.totalINpackets = this.totalINpackets + 1;
            if (packetsPlotPort.anyOutConnection()) {
                packetsPlotPort.exportEvent(PACKETS_REC_EVENT, new DoubleObj(this.totalINpackets), null);
            }

            //**********************
            // Step 3. Extract the sensed data which is contained within the body
            // of the incomming packet and plot the resulting value
            //**********************
            long target_nid = ((SensorAppAgentContract.Message)((OH_TDMA_Packet)spkt.getBody()).getBody()).getTargetNid();
            lastSeenSNR = ((SensorAppAgentContract.Message)((OH_TDMA_Packet)spkt.getBody()).getBody()).getSNR();
            //System.out.println("The sink received info from sensor" + spkt.getSrc_nid() + " regarding an event from " + target_nid);
            //System.out.println("The received signal of the phenomena is: " + lastSeenSNR);

		    Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(target_nid - first_target_nid));
		    if ( snrPort != null )
			    if ( snrPort.anyOutConnection() )
				    snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)lastSeenSNR), null);
		} else
			super.recvSensorPacket(data_) ;
    }

    /**
     *
     * @return
    */
    public int getTotalINPackets()
    {
        return (this.totalINpackets);
    }

    public double getAvgLatency() {
        return (this.avgLatency);
    }

    /**
     *
     * @return
    */
    public String getName()
    {
        return "SinkAppTDMA";
    }
}
