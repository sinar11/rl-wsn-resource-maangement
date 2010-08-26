package visualSensorNet;

import drcl.comp.Component;
import drcl.inet.sensorsim.SensorMobilityModel;
import drcl.inet.sensorsim.SensorPhy;
import drcl.inet.sensorsim.TargetAgent;


/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
 * This class is used to create all the inner components in a target node.
 *
*/

public class Target {

	Component targ;
	
	double xmin =100.0;
	double xmax = 600.0;
	double ymin = 100.0;
	double ymax = 500.0;
	double dx = 60.0;
	double dy = 60.0;
	
	protected SensorMobilityModel mobility;
	
	void setTopo(double xmi,double xma,double ymi,double yma,double dx_,double dy_){
		// spécification de la topologie du grid
			xmin =xmi;
			xmax = xma;
			ymin = ymi;
			ymax = yma;
			dx = dx_;
			dy = dy_;
			
			mobility.setTopologyParameters( xmax,ymax,0.0,xmin,ymin,0.0,dx,dy,0.0);
//			TODO: prendre en compte la 3eme dim
	
	}

	void setTopo(Environnement env){
		mobility.setTopologyParameters( env.xmax,env.ymax,0.0,env.xmin,env.ymin,0.0,env.dx,env.dy,0.0);
	}
	
	public Target(int nid, Environnement env){
	

	System.out.println("Creation du Target n°"+nid);
	
	targ = new Component("targ"+nid);
	TargetAgent agent = new TargetAgent();agent.setID("agent");
	targ.addComponent( agent);
	agent.setBcastRate( 20.0);
	agent.setSampleRate( 1.0);
	
	
	// interface avec sensorchannel
	SensorPhy phy = new SensorPhy();phy.setID("phy");
	targ.addComponent( phy);
	phy.setNid(nid);
	phy.setRadius(250.0);
	phy.setRxThresh( 0.0);
	
	mobility = new SensorMobilityModel();mobility.setID("mobility");
	targ.addComponent( mobility);
	
	//connection agent -> sensorphy
	agent.downPort .connectTo( phy.upPort );
	//connection externe : sensorphy -> sensorchannel
	phy.downPort .connectTo( env.chan.getPort(env.chan.NODE_PORT_ID ));
	// connection externe au modèle de propagation
	phy.getPort( phy.PROPAGATION_PORT_ID ).connect(env.seismic_prop.getPort(".query"));
	
	// config du terrain
	mobility.setNid( nid);


	//ajout à l'environnement 
	env.root.addComponent( targ);
	setTopo(env);
	
	//bonus : cf en dessous
	mobility.getPort( mobility.REPORT_SENSOR_PORT_ID ).connect( env.nodetracker.getPort( env.nodetracker.NODE_PORT_ID ));
	phy.getPort( phy.MOBILITY_PORT_ID ).connect( mobility.getPort( ".query"));
	}
}
