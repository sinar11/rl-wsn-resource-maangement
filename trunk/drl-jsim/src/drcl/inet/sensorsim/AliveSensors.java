package drcl.inet.sensorsim;

import drcl.comp.Port;
import drcl.data.DoubleObj;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 04/27/2005
 *
 * This is a very simple component for graphing the number of sensors that remain
 * alive. Check TCL script for usage.
 */

public class AliveSensors extends drcl.comp.Component
 {

   /*To collect and display the number of live nodes in graph form.
     Created a port that will output to a plotter*/
    public static final String LIVE_SENSORS_EVENT     = "Sensors Remaining Alive";
    public static final String PLOTTER_PORT_ID  = ".plotter";
    public Port plotterPort = addEventPort(PLOTTER_PORT_ID);

    public int total_live_nodes;

    public AliveSensors() {
        super();
    }

    public void setLiveNodes(int liveNodes) {
        this.total_live_nodes = liveNodes;
    }

    public void updateGraph(){
        if (plotterPort.anyOutConnection()) {
            plotterPort.exportEvent(LIVE_SENSORS_EVENT, new DoubleObj(total_live_nodes), null);
        }
    }
}
