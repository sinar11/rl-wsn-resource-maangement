package drcl.inet.sensorsim.OneHopTDMA;

import drcl.data.DoubleObj;
import drcl.data.LongObj;
import drcl.inet.mac.EnergyContract;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.comp.ACATimer;
import drcl.comp.Port;

import java.util.Vector;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 06/15/2005
 *
 * Brief Background on the Direct Method:
 * The direct method, otherwise known as the one-hop method, has all sensor
 * nodes communicate back to the base station.  This model is normally not
 * feasible for most applications because of severe restrictions and inefficiencies.
 * It’s main fault is that sensors are traditionally equipped with relatively
 * low power radios limiting the distance to which they can communicate to.
 * Therefore, any sensors that are located at a greater distance or that have
 * more obstacles (i.e. no line of sight) will have higher Signal-to-Noise Ratio (SNR)
 * or possibly not be able to reach the base station.  It is also an unreasonable
 * setup since you are not providing any radio redundancy. So by movement of
 * certain obstacles within the reactor sensors can temporarily be out-of-reach.
 * This setup will create dead spots and his therefore unfeasible.
 *
 * Specific Information Regarding OneHopAppTDMA:
 *  This class supports the direct model in a cross-layered design where the
 *  routing abstraction layers are collapsed and the application layer can have
 *  direct control over the hardware. This allows for the application to setup
 *  a TDMA schedule amongst all nodes such that the radio component is only
 *  on during its allocated transmission time.
*/

public class OneHopAppTDMA extends SensorApp implements drcl.comp.ActiveComponent
{

    /*To collect and display energy levels in graph form. Created a
    port that will output to a plotter*/
    public static final String ENERGY_EVENT     = "Remaining Energy";
    public static final String REM_ENERGY_PORT_ID  = ".plotter";
    public Port plotterPort = addEventPort(REM_ENERGY_PORT_ID);

    //how often to send back to base station.
    protected double interval = 10.0;

    //How far the sink is.
    double sinkDistance;

    //when the sensor can send (based on TDMA scheduling)
    double xmit_time;

    ACATimer send_timer;
    ACATimer wake_up;
    ACATimer CPUactive_timer;           //timer for how long the CPU should remain active for

    int p_size = 175;                   //final packet size after encapsulations
    final double cpu_electronics =5e-6; //how much extra processing time it takes to send a packet (made up?)
    protected double processingTime = txtime(p_size) + cpu_electronics;

    /*The data size of the packet... total size is 175 after lower
      layers encapsulate it with their headers*/
    int dataSize = 97;

    /*Object to store the latest result the sensor
    has sensed. This object is inserted as the body
    of all outgoing messages*/
    Object phenomenon = new SensorAppAgentContract.Message();

    private static int SINK_SCH_BCAST = 1;
    
    {
		removeDefaultUpPort() ;
	}

    /**
     * Constructor
    */
    public OneHopAppTDMA()
    {
        super();
        this.setSinkLocation(0.0,0.0,0.0);  //set the sink's location to the default(0,0,0)
    }

    /**
     * _start()
     *   This method is called when attempting to 'run' the component
     *   in TCL.
    */
    protected void _start ()
    {
        //these need to be calculated here because they depend on nn_
        //which is set after object construction.
        frame_time_TDMA = nn_ * slot_time_TDMA;
        new_schedule_adv_TDMA = ((5.0*frame_time_TDMA) + (4.0*gap_time_TDMA))+ 2.0;

        //set the CPU to sleep till we receive the first advertised schedule
        if (this.cpuMode != 1) {
            this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        }

        //turn the radio on to IDLE since we are waiting for a schedule to be received.
        //Contract type: SET_RADIO_MODE = 1 & Radio Modes: RADIO_IDLE=0
        wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 0));

        getRemainingEng();  //start the periodic querying of the remaining energy

        LongObj nidObject = new LongObj(this.nid);
        schedule.add(nidObject);
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
        if (send_timer != null)
            cancelTimeout(send_timer);

        //Shut RADIO off  & Radio Mode: RADIO_OFF=2
        wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0,2));

        //shut CPU off
        this.setCPUMode(3);     //turn off CPU when sim stops
	}


    /**
     * If the Component was stopped this will resume its periodic sending
    */
	protected void _resume()
    {
        rTimer = setTimeout("getEnergy", 4);

        //Turn radio back on. Radio Mode: RADIO_IDLE = 0
        wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 0));
    }


    /**
     * Starts the timer to obtain up-to-date info on the energy of
     * this sensor node.
    */
    protected synchronized void  getRemainingEng()
    {
        rTimer = setTimeout("getEnergy", 1);
    }


    /**
     * Called once every frame during its pre-determined
     * time slot and is in charge of sending an update
     * back to the Base station.
     *
     * Note: The radio is automatically put from sleep mode to transmit
     * mode in the wirelessphy.java layer to keep it in sleep mode as
     * long as possible. Basically when the MAC layer gets a packet
     * from the queue the radio is turned on once the channel has been
     * deamed clear to send.
    */
    public synchronized void packet_send()
    {
        //make sure the CPU is awake
        if (this.cpuMode != 2) {
            this.setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
            CPUactive_timer = setTimeout("CPUactive", processingTime);
        }

        //create a new bcast object
        OH_TDMA_Packet newPacket = new OH_TDMA_Packet(this.nid, this.myPos, getTime(), dataSize, phenomenon);

        //do a last minute update check to make sure energy levels are still OK
        // Contract type: ENERGY_QUERY = 0
        //Radio Modes:RADIO_IDLE=0,RADIO_SLEEP=1,RADIO_OFF=2,RADIO_TRANSMIT=3,RADIO_RECEIVE=4
        double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();

        // CASE 1: Sensor is dead
        if (((energy == 0.0) & (!this.sensorDEAD)) || (this.sensorDEAD)){
            this.sensorDEADAT = getTime();
            System.out.println("Sensor"+this.nid+ " is dead at " + this.sensorDEADAT + " was unable to forward traffic to sink");
            this.sensorDEAD = true;
            this._stop();
            nn_ = nn_-1;  //fewer total nodes now
        }
        else {  //CASE 2: Sensor is not Dead.
            if ( lastSeenSNR > coherentThreshold ) {
                downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, this.nid, this.sinkPos, dataSize, COHERENT, eID, ((SensorAppAgentContract.Message)phenomenon).getTargetNid(), newPacket));
                eID = eID + 1;          //every message sent is associated with an event ID
            } else {
                downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, this.nid, this.sinkPos, dataSize, NON_COHERENT, eID, ((SensorAppAgentContract.Message)phenomenon).getTargetNid(), newPacket));
                eID = eID + 1 ;
            }
        }
        return;
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

        //we received newly sensed information so update our field which holds
        //the current measurements to be next sent to the base station.
        phenomenon = data_;
        return;
    }


    /**
     * When receiving the broadcast from the base station. Since I received it
     * I am obviously alive and hold a slot time somewhere in the schedule.
     * Find what position I am in the vector and determine my next send time.
    */
    public void calc_send_time(Vector schedule)
    {

        //System.out.println("Sensor " + this.nid+ " RECEIVED new schedule!!!");
        double position = -1;
        boolean found = false;

        //cancel any current running schedules that this sensor might have
        if (send_timer != null)
            cancelTimeout(send_timer);

        //find position in vector
        for (int i = 0; i < schedule.size(); i ++){
            long current = ((LongObj)schedule.get(i)).getValue();
            //System.out.println("Sensor"+this.nid+" Current comparison is with: " + current);
            if (current == this.nid) {
                found = true;
                position = i;
                break;
            }
        }
        if (found) {
            //calculate send time
            xmit_time = start_time_TDMA +(position * slot_time_TDMA);

            /*System.out.println("Sensor"+this.nid+" Frame start_time is: " + start_time_TDMA);
            System.out.println("Sensor"+this.nid+" slot time is: " + slot_time_TDMA);
            System.out.println("Sensor"+this.nid+" will be sending at: " + xmit_time);*/

            //start timer
            send_timer = setTimeout("send_packet", xmit_time);

            //put RADIO to sleep (contract type = 1, radio mode = RADIO_SLEEP=1)
            wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 1));

            //must make sure sensors are awake for periodic update sent out from BS
            //wake_up = setTimeout("wakeup", (start_time_TDMA+getTime() + new_schedule_adv_TDMA));
            wake_up = setTimeout("wakeup", (sensor_wakeup_time_TDMA));
        }
        return;
    }


    /**
     * Prints various Information on the sensor.
    */
    public String toString()
    {
        String nodeStatus = new String();
        nodeStatus = "\n";
        nodeStatus.concat("Sending Node: oneHopApp" + this.nid);
        nodeStatus.concat("Sensor Location: ("+this.myPos[0]+", " + this.myPos[1]+", "+this.myPos[2]+")");
        nodeStatus.concat("Sink Location: ("+this.sinkPos[0]+", " + this.sinkPos[1]+", "+this.sinkPos[2]+")");
        nodeStatus.concat("Packet #: " + eID);
        nodeStatus.concat("CPU Mode: " + this.cpuMode);
        nodeStatus.concat("\n");
        return(nodeStatus);
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

			if (spkt.getPktType() == SINK_SCH_BCAST) {
                //pass on just the body of the message
                this.calc_send_time((Vector)spkt.getBody());
                return;
            }
	        return;
        } else
			super.recvSensorPacket(data_) ;
    }


    /**
     * Calculates the distance between 2 points in 3D space.
     * @param X
     * @param Y
     * @param Z
     * @param X2
     * @param Y2
     * @param Z2
     * @return
    */
    protected double EuclideanDist(double X, double Y, double Z,double X2, double Y2, double Z2)
    {
        double dx = X2 - X;
        double dy = Y2 - Y;
        double dz = Z2 - Z;
        return(Math.sqrt((dx*dx) + (dy*dy) + (dz*dz)));
    }


    /**
     *  This method is invoked when the components timer reaches zero.
     *  @param data_
    */
    protected synchronized void timeout(Object data_)
    {
        if ( data_.equals("getEnergy")) {
            // Contract type: ENERGY_QUERY = 0
            //Radio Modes:RADIO_IDLE=0,RADIO_SLEEP=1,RADIO_OFF=2,RADIO_TRANSMIT=3,RADIO_RECEIVE=4
            double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();
            //netif_.residual_energy();
            if ((energy == 0.0) & (!this.sensorDEAD)){
                this.sensorDEADAT = getTime();
                this.sinkDistance = this.EuclideanDist(this.getX(),this.getY(), this.getZ(), 0.0,0.0,0.0);
                System.out.println("Sensor"+this.nid+ " is dead at " + this.sensorDEADAT + " its distance from the sink was: " + this.sinkDistance);
                this.sensorDEAD = true;
                this._stop();

                //updating global TDMA settings
                //these need to be calculated here because they depend on nn_
                //which is set after object construction.
                nn_ --; //one less less node

                //LongObj currentNode = new LongObj(this.nid);
                int loc = -1;
                System.out.println("Removing Sensor"+this.nid+"from schedule");
                for (int i = 0; i <= schedule.size(); i ++) {
                    long temp = ((LongObj)schedule.get(i)).getValue();
                    if (temp == this.nid){
                        loc = i;
                        break;
                    }
                }
                //System.out.println("Its location was"+loc);
                schedule.remove(loc);
                //System.out.println("schedules new size is: " + schedule.size());
            }
            if (plotterPort.anyOutConnection()) {
                plotterPort.exportEvent(ENERGY_EVENT, new DoubleObj(energy), null);
            }

            rTimer = setTimeout("getEnergy", 1);
            return;
        }

        if(data_.equals("send_packet")) {
            this.packet_send();
            send_timer = setTimeout("send_packet", (frame_time_TDMA+ gap_time_TDMA));
            return;
        }

        if (data_.equals("CPUactive")) {
            //put CPU back to sleep
            this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
            return;
        }

        if (data_.equals("wakeup")) {
            //System.out.println("Sensor"+this.nid+" Calling WakeUP at time: " + getTime());
            //turn the radio on to IDLE since we are waiting for a schedule to be received.
            //Contract type: SET_RADIO_MODE = 1 & Radio Modes: RADIO_IDLE=0
            wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 0));
            return;
        }
        else {
            return;
        }
    }

    /**
     *
     * @return
    */
    public String getName()
    {
        return "OneHopAppTDMA";
    }

}
