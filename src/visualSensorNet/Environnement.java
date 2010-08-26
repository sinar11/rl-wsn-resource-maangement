package visualSensorNet;

import drcl.inet.mac.Channel;
import drcl.inet.mac.NodePositionTracker;
import drcl.inet.sensorsim.SeismicProp;
import drcl.inet.sensorsim.SensorChannel;
import drcl.inet.sensorsim.SensorNodePositionTracker;
import drcl.inet.sensorsim.tracer.Tracelogger;
import drcl.inet.sensorsim.tracer.Tracer;
import drcl.comp.*;


/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
 * This class is used to create the visual environment for the sensor network
 * simulation.
*/

public class Environnement {

	double seismic_dmin = 0.2; //distance min entre deux noeuds
	
	double xmin =100.0;
	double xmax = 500.0;
	double ymin = 100.0;
	double ymax = 500.0;
	double dx = 60.0;
	double dy = 60.0;
    int nn_; //total number of sensors

    public Component root;
	public SensorChannel chan ;
	public SeismicProp seismic_prop ;
	public SensorNodePositionTracker nodetracker;
	public Channel channel ;
	public NodePositionTracker tracker ;
    public Tracelogger log;
	public Tracer tracer ;

	/**
     *
     * @param node_num
     * @param target_node_num
    */
	public Environnement(int node_num, int target_node_num){
		
        root = new Component("test");
        chan = new SensorChannel();
        chan.setID("chan");
        root.addComponent( chan);
        chan.setCapacity(node_num);

        seismic_prop = new SeismicProp("seismic_prop");
        root.addComponent( seismic_prop);
        seismic_prop.setD0( seismic_dmin );

        nodetracker = new SensorNodePositionTracker();
        nodetracker.setID("nodetracker") ;
        root.addComponent( nodetracker);

        nn_ = node_num - target_node_num;

        // ??? ca marche ou pas ????
        //		h2.getComponent("csl").getPort("up", "6").connectTo(example2.getComponent("tm").addPort("in"));
        chan.getPort( chan.TRACKER_PORT_ID ).connect(nodetracker.getPort(nodetracker.CHANNEL_PORT_ID ));


        //channel wireless : pour les sensors et les sinks
        channel = new Channel(); channel.setID("channel");
        root.addComponent( channel);
        channel.setCapacity( node_num - target_node_num);

        //Node postion tracker
        tracker = new NodePositionTracker();tracker.setID("tracker");
        tracker.setGrid(30.0, 0.0, 100.0, 0.0, 30.0, 100.0);
        root.addComponent( tracker);

        channel.getPort( ".tracker").connect(tracker.getPort( ".channel" ));

        //Tracer to log events
	    log = new Tracelogger(node_num);
	    tracer = new Tracer(log); tracer.setID("tracer");
	    root.addComponent( tracer);

	}

    /**
     * total number of sensors in simulation
     * @return
     */
    public int getNn_() {
        return nn_;
    }

    /**
     * Create the physical dimensions of the sensing area at hand.
     * French: Spécification de la topologie du grid
     * @param xmi
     * @param xma
     * @param ymi
     * @param yma
     * @param dx_
     * @param dy_
     */
	void setTopo(double xmi,double xma,double ymi,double yma,double dx_,double dy_)
    {
		xmin =xmi;
		xmax = xma;
		ymin = ymi;
		ymax = yma;
		dx = dx_;
		dy = dy_;

		nodetracker.setGrid( xmax, xmin , ymax, ymin);
		tracker.setGrid( xmax, xmin , ymax, ymin , dx,dy);
	}

}
