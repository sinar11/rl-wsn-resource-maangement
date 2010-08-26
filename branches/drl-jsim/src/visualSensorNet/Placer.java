package visualSensorNet;

import drcl.comp.*;
import drcl.comp.Component;
import drcl.inet.mac.MobilityModel;
import drcl.inet.sensorsim.tracer.Tracelogger;
import javax.swing.*;

/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
*/

public class Placer {

    public static final int MAX_LIGNES = 15; // maximum number of lignes in the text_panel

    Plan plan;
	JFrame frame;
    JFrame textFrame;
	Mouseclick mouse;
	JTextPanel text = new JTextPanel("Click on sensor to get Details","",MAX_LIGNES);
	Tracelogger logger = null;

    /**
     *
     * @param width
     * @param height
     * @param max_sinks
     * @param max_sens
     * @param max_targs
     */
	public Placer(int width, int height, int max_sinks, int max_sens, int max_targs)
    {
    	//creation of the MouseListener
		mouse = new Mouseclick(this);

        //********************************************
        // Main Window
        //********************************************
        frame = new JFrame("SensorSim Visualizer");
        //set the dimensions
        frame.setSize(width, height) ;
        //allow user to be able to resize the window
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        //Display the window.
        frame.setVisible(true);
        plan = new Plan(width, height,  max_sens, max_sinks, max_targs);
        plan.addMouseListener(mouse);
        //add the component
        frame.getContentPane().add(plan);
		frame.getContentPane().addMouseListener(mouse);

        //causes the layout manager to re-layout the components and make them visible
        frame.validate();
        //********************************************
        // Log displayer window
        //********************************************

        textFrame = new JFrame("SensorSim Visualizer: Node Info Window");
		//set the dimensions of the log displayer
		textFrame.setSize(width/2, height/2);
        //allow user to be able to resize the window
        textFrame.setResizable(true);
        textFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        //Display the window.
        textFrame.setVisible( true);
		textFrame.getContentPane().add(text);
        textFrame.pack();
		textFrame.setLocation(0,height);
	}

    
    /**
     *  Adds a sink to be drawn on the canvas. You pass in the position and speed information
     *  of the sink.
     * @param node
     * @param speed
     * @param posX
     * @param posY
     * @param posZ
    */
	public void add_sink(Component node, double speed, double posX, double posY, double posZ )
    {
		//ajoute un sink
		((MobilityModel)node.getComponent("mobility")).setPosition(speed, posX,posY,posZ);
        plan.addSink(posX,posY);
		plan.repaint() ;
	}


    /**
     * Adds the sensors to be drawn on the canvas. You pass in the position and speed information
     * of each sensor here.
     * @param node
     * @param speed
     * @param posX
     * @param posY
     * @param posZ
    */
	public void add_sensor(Component node, double speed, double posX, double posY, double posZ )
    {
		((MobilityModel)node.getComponent("mobility")).setPosition(speed, posX,posY,posZ);
		plan.addSensor(posX,posY);
		plan.repaint() ;	
	}


    /**
     * Add target nodes.
     * @param node
     * @param speed
     * @param posX
     * @param posY
     * @param posZ
    */
	public void add_target(Component node, double speed, double posX, double posY, double posZ )
    {
	    ((MobilityModel)node.getComponent("mobility")).setPosition(speed, posX,posY,posZ);
		plan.addTarg(posX,posY);
		plan.repaint() ;
	}


    /**
     * called by the mouse event listener to react to a mouseclick
     * @param posx
     * @param posy
     */
	public void reactMouseClick(int posx, int posy){

		text.clearText();

        //System.out.println("xCoord: " + posx + "   yCoord: " + posy);

		int pointed = plan.whichPointedNode(posx,posy/*-22*/);

		if(logger != null & pointed != -1){
			//a precise node is pointed
			text.addText( "Log concerning node number "+pointed,"");
			text.addText("        EVENT","    DATE");
			text.addSeparator() ;
			for(int i = 0; i< logger.eventcount( pointed); i++){
				//System.out.println("adding event"+i+" of "+logger.eventcount( pointed));
				text.addText(logger.printEvent( pointed,i), logger.printDate( pointed,i));
			}
		}else if((pointed == -1) && (logger != null)){
			// we print all events ....
			text.addText( "Log concerning ALL nodes","");
			text.addText("        EVENT","    DATE");
			text.addSeparator() ;
			//System.out.println(logger.alleventcount() );
			for(int i = 0; i< logger.alleventcount() ; i++){
				//System.out.println("adding event"+i+" of "+logger.eventcount( pointed));
				text.addText(logger.printGlobalEvent( i), logger.printGlobalEventDate( i));
			}
		}
		text.repaint() ;
	}

    /**
     *
     * @param nbframes
     */
	public void afficher(int nbframes)
    {
		//affiche le plan pendant nbframes fois 0.1 s
	    plan.repaint() ;
    	try{Thread.sleep( 100);}catch(Exception e){}
	}

    /**
     *
     * @param logger
    */
	public void setLogSource(Tracelogger logger)
    {
		this.logger = logger;
	}
}
