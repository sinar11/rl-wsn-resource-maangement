package drcl.inet.sensorsim.OneHop;

import drcl.data.DoubleObj;
import drcl.inet.mac.EnergyContract;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.comp.ACATimer;
import drcl.comp.Port;

import java.util.Random;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 04/20/2005
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
 * Specific Information Regarding OneHopApp:
 *  This is the application layer for simulating a one-hop routing scheme
 *  for sensor networks. The route entry is added in the Tcl script and when
 *  this component is started from the script it essentially starts a timer
 *  that when it expirers simply generates a packet and sends down towards
 *  the wireless protocol stack.
 */

public class OneHopApp extends SensorApp implements drcl.comp.ActiveComponent
{

    /*To collect and display energy levels in graph form. Created a
    port that will output to a plotter*/
    public static final String ENERGY_EVENT     = "Remaining Energy";
    public static final String REM_ENERGY_PORT_ID  = ".plotter";
    public Port plotterPort = addEventPort(REM_ENERGY_PORT_ID);

    ACATimer packet_send_timer;         //timer for periodic sends
    ACATimer CPUactive_timer;           //timer for how long the CPU should remain active for
    double sinkDistance;                //How far the sink is.
    Random generator = new Random();    //Generate a random number to avoid collisions.

    /*Sending interval (the first was used to study simulation analytically,
    use the second data_lag which uses a random # in practice this will
    reduce collisions.*/
    protected double data_lag = generator.nextDouble()*6 + 8;

    /*The data size of the packet...
    total size is 175 after lower encapsulate it with their headers*/
    int dataSize = 97;

    /*Object to store the latest result the sensor
    has sensed. This object is inserted as the body
    of all outgoing messages*/
    Object phenomenon = new SensorAppAgentContract.Message();

    int p_size = 175;                   //final packet size after encapsulations
    final double cpu_electronics =5e-6; //how much extra processing time it takes to send a packet (made up?)
    protected double processingTime = txtime(p_size) + cpu_electronics;

    {
		removeDefaultUpPort() ;
	}

    ///////////////////////////////////////////////////////////////////////
    /*The following is strictly for analytically comparing
      simulations. It will be removed in future versions once
      the code has been verified thouroughly -
      only called from Tcl validation scripts */
    public void setSendTime (double data_lag_)
    {
        data_lag = data_lag_;
        System.out.println("Sensor"+this.nid+ " will now send every: " + data_lag + "seconds");
        return;
    }
    ////////////////////////////////////////////////////////////////////////


    /**
     * Constructor
    */
    public OneHopApp()
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
        //set the CPU to sleep till we do something.
        if (this.cpuMode != 1) {
            this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        }
        //set the radio to sleep till its time to send too
        //Contract type: SET_RADIO_MODE = 1 & Radio Modes: RADIO_SLEEP=1
        if (isIs_uAMPS()) {
            wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0,1));
        }

        packet_send_timer = setTimeout("packetSend", data_lag);

        rTimer = setTimeout("getEnergy", 1);  //obtain up-to-date info on the energy of this sensor node.
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
        if (packet_send_timer != null)
          cancelTimeout(packet_send_timer);

        //Contract type: SET_RADIO_MODE = 1 & Radio Modes: RADIO_SLEEP=1
        if (isIs_uAMPS()) {
            wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0,1));
        }

        this.setCPUMode(3);     //turn off CPU when sim stops

	}

    /**
     * If the Component was stopped this will resume its periodic sending
    */
	protected void _resume()
    {
        rTimer = setTimeout("getEnergy", 4);
        packet_send_timer = setTimeout("packetSend", data_lag);
    }

    /**
     * Called periodically every data_lag seconds
     * and is in charge of sending an update back to the Base station.
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
        OH_Packet newPacket = new OH_Packet(this.nid, this.myPos, getTime(), dataSize, phenomenon);

        //do a last minute update check to make sure energy levels are still OK
        //Contract type: ENERGY_QUERY = 0
        double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();

        // CASE 1: Sensor is dead
        if (((energy == 0.0) & (!this.sensorDEAD)) || (this.sensorDEAD)){
            this.sensorDEADAT = getTime();
            System.out.println("Sensor"+this.nid+ " is dead at " + this.sensorDEADAT + " was unable to forward traffic to sink");
            this.sensorDEAD = true;
            this._stop();
            nn_=nn_-1;  //fewer total nodes now
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
        //long target_nid = msg.getTargetNid();

        //System.out.println("Sensor" +this.nid + " has received info over the sensor channel from target node " + target_nid);

        //we received newly sensed information so update our field which holds
        //the current measurements to be next sent to the base station.
        phenomenon = data_;
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
     *  This method is invoked when the components timer reaches zero.
     *  @param data_
    */
    protected synchronized void timeout(Object data_)
    {
        if (!this.sensorDEAD){
            if ( data_.equals("getEnergy")) {
                // Contract type: ENERGY_QUERY = 0
                double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();

                if ((energy == 0.0) & (!this.sensorDEAD)){
                    this.sensorDEADAT = getTime();
                    this.getLocation();
                    this.sinkDistance = this.EuclideanDist(this.getX(),this.getY(), this.getZ(), 0.0,0.0,0.0);
                    System.out.println("Sensor"+this.nid+ " is dead at " + this.sensorDEADAT + " its distance from the sink was: " + this.sinkDistance);
                    this.sensorDEAD = true;
                    this._stop();
                }

                if (plotterPort.anyOutConnection()) {
                    plotterPort.exportEvent(ENERGY_EVENT, new DoubleObj(energy), null);
                }

                rTimer = setTimeout("getEnergy", 1);
                return;
            }

            if(data_.equals("packetSend")) {
                this.packet_send();
                packet_send_timer = setTimeout("packetSend", data_lag);
                return;
            }

            if (data_.equals("CPUactive")) {
                //put CPU back to sleep
                this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
                return;
            }

            else {
                return;
            }
        } else {
            this._stop();
            return;
        }
    }

        /**
     *
     * @return
    */
    public String getName()
    {
        return "OneHopApp";
    }

}
