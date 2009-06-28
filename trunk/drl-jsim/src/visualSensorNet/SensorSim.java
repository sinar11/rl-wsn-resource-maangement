package visualSensorNet;

import drcl.comp.tool.Plotter;
import drcl.inet.sensorsim.OneHop.*;
import drcl.inet.sensorsim.OneHopTDMA.*;
import drcl.inet.sensorsim.MultiHop.SinkAppMH;
import drcl.inet.sensorsim.MultiHop.MultiHopApp;
import drcl.inet.sensorsim.LEACH.SinkAppLEACH;
import drcl.inet.sensorsim.LEACH.LEACHApp;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.AliveSensors;
import drcl.inet.mac.Mac_802_11;
import drcl.inet.mac.WirelessPhy;
import drcl.inet.mac.CSMA.Mac_CSMA;

import java.util.Random;
import java.util.Vector;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import parser.FileParser;
import parser.ParseEntry;

import javax.swing.*;


/**
 * @author Nicholas Merizzi
 * @version 1.0, 07/24/2005
 *
 * Main Testing Module.
 *
 * This class allows the user to interactively run a sensor network simulation
 * with various protocols. To run just enter `java visualSensorNet.SensorSim`
 * you will be prompted with a GUI that requests various information from you.
 * When you click on 'Run' the simulation will commence. From there multiple
 * graphs will be displayed for your simulation and it will run till the
 * user specified run time or till all the sensors run out of energy.
 *
 */
public class SensorSim extends JFrame implements ActionListener, Runnable {

    static int total_nodes;
	static int total_target_nodes;
	static int total_sensor_nodes;// = total_nodes - total_target_nodes;
	static int sink_id = 0;

    static Random generator = new Random();
    static SensorSimple[] tabsens;
    static Sink sink0;
    static int simChoice;

    static Plotter sinkPlot1_ = new Plotter("sinkPlot1_");
    static Plotter sinkPlot2_ = new Plotter("sinkPlot2_");
    static Plotter plot3 = new Plotter("plot3");
    static Plotter plot4 = new Plotter("plot4");
    static AliveSensors liveSensors = new AliveSensors();

    //The various Routing Combinations Possible
	public static final int OH_80211    = 0;
	public static final int OH_CSMA     = 1;
	public static final int OH_TDMA     = 2;
	public static final int MH_CSMA     = 3;
    public static final int MH_80211    = 4;
    public static final int LEACH       = 5;


    //Create the root panels
    private JPanel rootNorthPanel;
    private JPanel rootCenterPanel;
    private JPanel rootSouthPanel;

    //******************************************************************
    //Declare the Swing components needed
    //******************************************************************

    //North Panel components
    JLabel routingCaptionLabel = new JLabel("Routing Selections: " );
    JRadioButton OH_80211Button = new JRadioButton("One-Hop & IEEE 802.11");
    JRadioButton OH_CSMAButton = new JRadioButton("One-Hop & CSMA");
    JRadioButton OH_TDMAButton = new JRadioButton("One-Hop & TDMA");
    JRadioButton MH_CSMAButton = new JRadioButton("Multi-Hop & CSMA");
    JRadioButton MH_80211Button = new JRadioButton("Multi-Hop & IEEE 802.11");
    JRadioButton LEACHButton = new JRadioButton("LEACH");

    JPanel radioPanel = new JPanel(new GridLayout(0, 1));

    //Group the radio buttongs together
    ButtonGroup group = new ButtonGroup();

    JLabel readTopologyFromLabel = new JLabel("1. Read from a topology File: ");
    JTextField filePath = new JTextField();
    JButton browseButton = new JButton("Browse");

    JLabel randomGeneNumberLabel = new JLabel("2. How many sensors would you like generated: " );
    JTextField randomSensorResult = new JTextField();

    JLabel userHelp = new JLabel ("(Note: Select Option 1 or 2 not both)");

    JFileChooser fc = new JFileChooser();

    //the center pannel components
    JLabel targetNodeLabel = new JLabel("Number of Target Nodes: " );
    JTextField targetNodeResult = new JTextField();
    JLabel simTimeLabel = new JLabel("Enter Total Simulation Time: " );
    JTextField simTimeResult = new JTextField();

    //the south panel components
    JButton startButton = new JButton("Start");
    JButton quitButton = new JButton("Quit");

    static boolean EXIT = true;
    static double simulationTime = 0.0;
    drcl.comp.ACARuntime sim;
	long starttime;


    //static EntryGUI newGUI = new EntryGUI();


    //the following two vectors are used only for running multi-hop schemes
    Vector neighbor_list = new Vector();
    Vector del_list = new Vector();

    //for file parsing
    static FileParser fileparse;

    //for GUI
    static Environnement test;
    static Placer topo;
    /////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor.
    */
    public SensorSim()
    {
         //Title of Window (calling Frame's Construtor--the Parents Constructor
        super("SensorSim: GUI Menu");

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add("North", getNorthernPanel());
        this.getContentPane().add("Center", getCenterPanel());
        this.getContentPane().add("South", getSouthernPanel());

        //dont want the user to be able to resize the window
        this.setResizable(false);

        //size the frame up
        this.pack();

        //When set to null it defaults to center of screen
        this.setLocationRelativeTo(null);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Display the window.
        this.setVisible(true);

        return;
    }

    /**
     * Constructor
     * @param sim_
     * @param starttime_
    */
    public SensorSim (drcl.comp.ACARuntime sim_, long starttime_)
    {
        sim = sim_;
        starttime = starttime_;
    }


    public static void main(String[] args)
    {
         (new SensorSim()).setVisible(true);
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public void run()
	{
		double now_ = sim.getTime();

        /*If the simulation is not done yet then check for possible
          events that may need to run*/
		if (now_ < simulationTime) {

            //print the locations of all sensors to the screen
            if (now_ < 0.001) {
                sensorLocPrintOut();
            }
            //the following is only for running in Multi-hop modes. It sets an initial
            //neighbor for all sensors.
            if (((simChoice == 3) || (simChoice == 4)) && (( now_ < 0.01) /*&& (now_ < 0.1)*/)) {
                setNeighbor();
            }

            //the following is only for running in Multi-hop modes. It displays
            //the current neighbor for every sensor.
            if (((simChoice == 3) || (simChoice == 4)) && (now_ < 0.15))  {
                printNeighborList();
            }

            //the following is only for running in Multi-hop modes. It maintains
            //neighbors for when sensors on the chain die
            if (((simChoice == 3) || (simChoice == 4)) && (now_ > 15.0)) {
                neighborUpdate();
            }

            //Check the status of all sensors constantly
            if (now_ > 15.0) {
                wsnLoop();
                sim.addRunnable(0.25, this);
            }
            else { //o.w. just call back this runnable component again in the future
                sim.addRunnable(0.5, this);
            }
		}
	}


    //***********************************************************
    //Helper Functions
    //***********************************************************


    /**
    * Go throught all sensors and prints their (x,y,z) coordinates
    */
    void sensorLocPrintOut()
    {
        if ((simChoice == 0) || (simChoice == 1)) {
            for(int k= 1; k < total_sensor_nodes; k++) {
                ((OneHopApp)tabsens[k].sens.getComponent("OneHopApp")).printNodeLoc();
            }
        }else if ((simChoice == 2)) {
            for(int k= 1; k < total_sensor_nodes; k++) {
                ((OneHopAppTDMA)tabsens[k].sens.getComponent("OneHopAppTDMA")).printNodeLoc();
            }
        }else if ((simChoice == 3) || (simChoice == 4)) {
            for(int k= 1; k < total_sensor_nodes; k++) {
                ((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).printNodeLoc();
            }
        } else if (simChoice == 5) {
            for(int k= 1; k < total_sensor_nodes; k++) {
                ((LEACHApp)tabsens[k].sens.getComponent("LEACHApp")).printNodeLoc();
            }
        }
    }


    /**
    * This method is called upon periodically and checks
    * to see if all sensors are dead. If so prints off
    * concluding results and provides insight on the routings
    * efficiency.
    */
    void wsnLoop() {

        int totalINpackets = 0;
        double avgLatency = 0.0;
        int BSmacCollisions = 0;
        int packets = -1;
        int totalPackets =0;
        int deadNodes = 0;
        int app_dropped = 0;
        int wphy_dropped = 0;
        int mac_dropped = 0;
        int totalDroppedPackets = 0;
        int total_bs_packets = 0;

        //determine if all the sensors are dead:
        if ((simChoice == 0) || (simChoice == 1)) {
            for(int i= 1; i < total_sensor_nodes; i++) {
                if(((OneHopApp)tabsens[i].sens.getComponent("OneHopApp")).isSensorDead()) {
                    deadNodes++;
                }
            }
        }else if ((simChoice == 2)) {
            for(int i= 1; i < total_sensor_nodes; i++) {
                if(((OneHopAppTDMA)tabsens[i].sens.getComponent("OneHopAppTDMA")).isSensorDead()) {
                    deadNodes++;
                }
            }
        }else if ((simChoice == 3) || (simChoice == 4)) {
            for(int i= 1; i < total_sensor_nodes; i++) {
                if(((MultiHopApp)tabsens[i].sens.getComponent("MultiHopApp")).isSensorDead()) {
                     deadNodes++;
                }
            }
        } else if (simChoice == 5) {
            for(int i= 1; i < total_sensor_nodes; i++) {
                if(((LEACHApp)tabsens[i].sens.getComponent("LEACHApp")).isSensorDead()) {
                    deadNodes++;
                }
            }
        } else {
            deadNodes = -1;
        }

        //************************************************
        // Update Live Node Graph
        //************************************************
        liveSensors.setLiveNodes(total_sensor_nodes - deadNodes);
        liveSensors.updateGraph();

        //if all sensors are dead then show results and halt the simulator
        if ((deadNodes == (total_sensor_nodes-1))/* || (sim.getTime() > 11500.0)*/) {

            System.out.println("----------------------------");
            System.out.println("All nodes dead at " + sim.getTime());
            System.out.println("Simulations Terminated.");
            System.out.println("Results: " );

            //*****************************************************************
            // 1. Determine how many packets the BS Received and
            // 2. Determine the total # of packets sent by all nodes.
            // 3. Determine the total # of dropped packets at the wirelessPhy layer
            // 4. Determine the total # of dropped packets at the application layer
            // 5. Determine the total # of dropped packets at the MAC layer (only when using MacSensor.java)
            // 6. Determine the avg. Latency
            //*****************************************************************

            if ((simChoice == 0) || (simChoice == 1)) {
                totalINpackets = ((SinkAppOH)sink0.sink.getComponent("SinkAppOH")).getTotalINPackets();
                avgLatency = ((SinkAppOH)sink0.sink.getComponent("SinkAppOH")).getAvgLatency();

                for(int i= 1; i < total_sensor_nodes; i++) {
                    packets = ((OneHopApp)tabsens[i].sens.getComponent("OneHopApp")).geteID();
                    System.out.println("Sensor" +i+ " sent out " + packets);
                    totalPackets = totalPackets + packets;
                }
                app_dropped = ((OneHopApp)tabsens[1].sens.getComponent("OneHopApp")).getDropped_packets();
                wphy_dropped= ((WirelessPhy)tabsens[1].sens.getComponent("wphy")).getDropped_packets();

                if ((simChoice != 0) && (simChoice != 4)) {
                    mac_dropped = ((Mac_CSMA)tabsens[1].sens.getComponent("MacSensor")).getDropped_packets();
                }

            }else if ((simChoice == 2)) {
                totalINpackets = ((SinkAppTDMA)sink0.sink.getComponent("SinkAppTDMA")).getTotalINPackets();
                avgLatency = ((SinkAppTDMA)sink0.sink.getComponent("SinkAppTDMA")).getAvgLatency();

                for(int i= 1; i < total_sensor_nodes; i++) {
                    packets = ((OneHopAppTDMA)tabsens[i].sens.getComponent("OneHopAppTDMA")).geteID();
                    System.out.println("Sensor" +i+ " sent out " + packets);
                    totalPackets = totalPackets + packets;
                }

                app_dropped = ((OneHopAppTDMA)tabsens[1].sens.getComponent("OneHopAppTDMA")).getDropped_packets();
                wphy_dropped= ((WirelessPhy)tabsens[1].sens.getComponent("wphy")).getDropped_packets();

                if ((simChoice != 0) && (simChoice != 4)) {
                    mac_dropped = ((Mac_CSMA)tabsens[1].sens.getComponent("MacSensor")).getDropped_packets();
                }

            }else if ((simChoice == 3) || (simChoice == 4)) {
                totalINpackets = ((SinkAppMH)sink0.sink.getComponent("SinkAppMH")).getTotalINPackets();
                avgLatency = ((SinkAppMH)sink0.sink.getComponent("SinkAppMH")).getAvgLatency();
                for(int i= 1; i < total_sensor_nodes; i++) {
                    packets = ((MultiHopApp)tabsens[i].sens.getComponent("MultiHopApp")).geteID();
                    System.out.println("Sensor" +i+ " sent out " + packets);
                    totalPackets = totalPackets + packets;
                }
                app_dropped = ((MultiHopApp)tabsens[1].sens.getComponent("MultiHopApp")).getDropped_packets();
                wphy_dropped= ((WirelessPhy)tabsens[1].sens.getComponent("wphy")).getDropped_packets();

                if ((simChoice != 0) && (simChoice != 4)) {
                    mac_dropped = ((Mac_CSMA)tabsens[1].sens.getComponent("MacSensor")).getDropped_packets();
                }

            } else if (simChoice == 5) {
                totalINpackets = ((SinkAppLEACH)sink0.sink.getComponent("SinkAppLEACH")).getTotalINPackets();
                avgLatency = ((SinkAppLEACH)sink0.sink.getComponent("SinkAppLEACH")).getAvgLatency();

                for(int i= 1; i < total_sensor_nodes; i++) {
                    packets = ((LEACHApp)tabsens[i].sens.getComponent("LEACHApp")).geteID();
                    System.out.println("Sensor" +i+ " sent out " + packets);
                    totalPackets = totalPackets + packets;
                }

                total_bs_packets = ((LEACHApp)tabsens[1].sens.getComponent("LEACHApp")).getBSpackets();
                app_dropped = ((LEACHApp)tabsens[1].sens.getComponent("LEACHApp")).getDropped_packets();
                wphy_dropped= ((WirelessPhy)tabsens[1].sens.getComponent("wphy")).getDropped_packets();

                if ((simChoice != 0) && (simChoice != 4)) {
                    mac_dropped = ((Mac_CSMA)tabsens[1].sens.getComponent("MacSensor")).getDropped_packets();
                }
            }else {
                totalINpackets = -1;
            }

            //*****************************************************************
            // Determine collisions at Base station (MAC layer).
            //*****************************************************************
            if ((simChoice == 0) || (simChoice == 4)) {
                //get from MAC80211 Component
                BSmacCollisions = ((Mac_802_11)sink0.sink.getComponent("mac")).getCollision();
            }else {
                //get from MacSensor component
                BSmacCollisions = ((Mac_CSMA)sink0.sink.getComponent("MacSensor")).getCollision();
            }
            System.out.println("Avg. Overall  Latency: " + avgLatency);
            System.out.println("Base Station Received: " + totalINpackets);
            System.out.println("Collisions at Base Station: " + BSmacCollisions);
            System.out.println("Total packets dropped at Application layer: " + app_dropped);
            System.out.println("Total packets dropped at physical layer: " + wphy_dropped);

            if ((simChoice != 0) && (simChoice != 4)) {
                System.out.println("Drops due to collisions (discovered at MAC layer): " + mac_dropped);
                totalDroppedPackets = mac_dropped + wphy_dropped + app_dropped;
            } else {
                totalDroppedPackets = wphy_dropped + app_dropped;
            }

            if (simChoice == 5) {
                System.out.println("Total Packets sent from CH to BS:" +total_bs_packets);
            }
            System.out.println("Total Packets sent from all nodes: " + totalPackets);

            System.out.println("Number of Dropped Packets: "+ totalDroppedPackets);

            if (simChoice != 5) {
                System.out.println("Success Rate (Reliability): " + (((totalINpackets*1.0)/(totalPackets *1.0))*100));
            } else {
                System.out.println("Success Rate (Reliability): " + (((totalINpackets*1.0)/(total_bs_packets *1.0))*100));
            }

            //*****************************************************************
            // Stop the simulator
            //*****************************************************************
            sim.stop();
	   }
    }


    /**
     * The following is only for MULTI-HOP Modes
     * Find next hop neighbor-- node to whom always send data.
     * Choose closest node that is in the direction of the base station.
     * NOTE!  This algorithm assumes nodes know the location of all nodes
     * near them.  In practice, this would require an initial set-up
     * phase where this information is disseminated throughout the network
     * and that each node has a GPS receiver or other location-tracking
     * algorithms to determine node locations.
    */
    void setNeighbor()
    {
        //call each sensors application layer to determine
        //who its closest initial neighbor is at startup
        if ((simChoice == 3) || (simChoice == 4)) {
            for(int k= 1; k < total_sensor_nodes; k++) {
                ((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).setNeighbor();

                /*We also need to store it in the script
	            to maintain their next neighbor when a
	            node dies.*/
                 neighborInfo newNeighbor = new neighborInfo(((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).getNid(),
                                                             (int)((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).getNeighbor_id(),
                                                             ((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).getNeighborX(),
                                                             ((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).getNeighborY(),
                                                             ((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).getNeighborZ(),
                                                             ((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).getNeighbor_dist());

                 neighbor_list.add(newNeighbor);

            }
        } else {
            System.out.println("This function does not apply to the type of sensors you are using.");
        }
    }


    /**
     * This method is used only for Multi-Hop modes and when called it prints
     * off the current neighbors that each sensor is set to use.
     */
    void printNeighborList()
    {
        if ((simChoice == 3) || (simChoice == 4)) {
            for(int k= 1; k < total_sensor_nodes; k++) {
                ((MultiHopApp)tabsens[k].sens.getComponent("MultiHopApp")).printNeighborID();
            }
        } else {
            System.out.println("This function does not apply to the type of sensors you are using.");
        }
    }


    /**
     * This method is used only for Multi-Hop modes.
     * Maintains neighbor chains over time.
    */
    void neighborUpdate() {
        if ((simChoice == 3) || (simChoice == 4)) {
            for(int k= 0; k < neighbor_list.size(); k++) {

                //extract the current element
                int newSensor = ((neighborInfo)neighbor_list.get(k)).getSelf_id();
                int newNeighbor = ((neighborInfo)neighbor_list.get(k)).getNeighbor_id();
                double newX =((neighborInfo)neighbor_list.get(k)).getNeighborX();
                double newY =((neighborInfo)neighbor_list.get(k)).getNeighborY();
                double newZ =((neighborInfo)neighbor_list.get(k)).getNeighborZ();
                double newDist =((neighborInfo)neighbor_list.get(k)).getNeighbor_dist();

                //determine if the current sensor is still alive
                if (((WirelessPhy)tabsens[newSensor].sens.getComponent("wphy")).getRemEnergy() <= 0) {

                    System.out.println("Sensor"+newSensor+" is dead Updating upstream sensors");

                    //go through list and find whoever is pointing to this dead node
                    //and redirect it to another neighbor.
                    for(int j= 0; j < neighbor_list.size(); j++) {
                        //extract the current element
                        int currentSensor = ((neighborInfo)neighbor_list.get(j)).getSelf_id();
                        int currentNeighbor = ((neighborInfo)neighbor_list.get(j)).getNeighbor_id();

                        if (currentSensor != newSensor){
                            if (currentNeighbor == newSensor) {
                                //then update the nodes neighbor
                                ((MultiHopApp)tabsens[currentSensor].sens.getComponent("MultiHopApp")).setNewNeighborID(newNeighbor);
                                ((MultiHopApp)tabsens[currentSensor].sens.getComponent("MultiHopApp")).setNewNeighborX(newX);
                                ((MultiHopApp)tabsens[currentSensor].sens.getComponent("MultiHopApp")).setNewNeighborY(newY);
                                ((MultiHopApp)tabsens[currentSensor].sens.getComponent("MultiHopApp")).setNewNeighborZ(newZ);
                                ((MultiHopApp)tabsens[currentSensor].sens.getComponent("MultiHopApp")).setNewNeighborDist(newDist);

                                //update the nodes neighbor w/in script
                                ((neighborInfo)neighbor_list.get(j)).setNeighbor_id(newNeighbor);
                                ((neighborInfo)neighbor_list.get(j)).setNeighborX(newX);
                                ((neighborInfo)neighbor_list.get(j)).setNeighborY(newY);
                                ((neighborInfo)neighbor_list.get(j)).setNeighborZ(newZ);
                                ((neighborInfo)neighbor_list.get(j)).setNeighbor_dist(newDist);
                            }
                        }
                     }//end inner for

                    //add the node that was dead to the dead sensor list
                    del_list.add(neighbor_list.get(k));

                }//end if energy ==0
            } //end outer for
            //Now remove the elements from sensorList
            for (int u = 0; u < del_list.size(); u ++) {
                int delNode = ((neighborInfo)del_list.get(u)).getSelf_id();

                //find it in real list and remove it!
                for (int v = 0; v < neighbor_list.size(); v ++) {
                    int cur_del = ((neighborInfo)neighbor_list.get(v)).getSelf_id();
                    if (delNode == cur_del) {
                        neighbor_list.remove(v);
                        break;
                    }
                }
            }
            //clear that temp list
            del_list.clear();
        }
        else {
            System.out.println("This function does not apply to the type of sensors you are using.");
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add the component to the northern panel of the main layout.
     * This includes the routing radio button selection as well as the
     * source file option and total number of sensors textbox.
     * @return
     */
    public JPanel getNorthernPanel() {

        if (rootNorthPanel == null) {
            rootNorthPanel = new JPanel();
            SpringLayout layout = new SpringLayout();
            rootNorthPanel.setLayout(layout);

            //Check box menu settings
            OH_80211Button.setMnemonic(KeyEvent.VK_C);
            OH_80211Button.setSelected(true);
            OH_CSMAButton.setMnemonic(KeyEvent.VK_G);
            OH_TDMAButton.setMnemonic(KeyEvent.VK_H);
            MH_CSMAButton.setMnemonic(KeyEvent.VK_T);
            MH_80211Button.setMnemonic(KeyEvent.VK_T);
            LEACHButton.setMnemonic(KeyEvent.VK_T);

            group.add(OH_80211Button);
            group.add(OH_CSMAButton);
            group.add(OH_TDMAButton);
            group.add(MH_CSMAButton);
            group.add(MH_80211Button);
            group.add(LEACHButton);

            //Register a listener for the check boxes.
            OH_80211Button.addActionListener(this);
            OH_CSMAButton.addActionListener(this);
            OH_TDMAButton.addActionListener(this);
            MH_CSMAButton.addActionListener(this);
            MH_80211Button.addActionListener(this);
            LEACHButton.addActionListener(this);

            //Put the radio buttons in a column in a panel.
            radioPanel.add(OH_80211Button);
            radioPanel.add(OH_CSMAButton);
            radioPanel.add(OH_TDMAButton);
            radioPanel.add(MH_CSMAButton);
            radioPanel.add(MH_80211Button);
            radioPanel.add(LEACHButton);

            routingCaptionLabel.setFont(new Font("Arial", Font.BOLD, 11));
            readTopologyFromLabel.setFont(new Font("Arial", Font.BOLD, 11));
            randomGeneNumberLabel.setFont(new Font("Arial", Font.BOLD, 11));
            userHelp.setFont(new Font("Arial", Font.BOLD, 12));

            filePath.setColumns(15);
            randomSensorResult.setColumns(5);
            browseButton.addActionListener(this);

            rootNorthPanel.add(radioPanel);
            rootNorthPanel.add(routingCaptionLabel);
            rootNorthPanel.add(readTopologyFromLabel);
            rootNorthPanel.add(randomGeneNumberLabel);
            rootNorthPanel.add(filePath);
            rootNorthPanel.add(browseButton);
            rootNorthPanel.add(randomSensorResult);
            rootNorthPanel.add(userHelp);

            //Insert routingCaptionLabel
	        layout.putConstraint(SpringLayout.WEST, routingCaptionLabel,5,SpringLayout.WEST, rootNorthPanel);
	        layout.putConstraint(SpringLayout.NORTH, routingCaptionLabel,25,SpringLayout.NORTH, rootNorthPanel);

			//Insert the radioPanel
			layout.putConstraint(SpringLayout.WEST, radioPanel,5,SpringLayout.WEST, rootNorthPanel);
	        layout.putConstraint(SpringLayout.NORTH, radioPanel,20, SpringLayout.SOUTH, routingCaptionLabel);

	        //Insert the readTopologyFromLabel
	        layout.putConstraint(SpringLayout.WEST, readTopologyFromLabel, 250, SpringLayout.WEST, radioPanel);
	        layout.putConstraint(SpringLayout.NORTH, readTopologyFromLabel, 25, SpringLayout.NORTH, rootNorthPanel);

            //Insert the filePath
	        layout.putConstraint(SpringLayout.WEST, filePath, 250, SpringLayout.WEST, radioPanel);
	        layout.putConstraint(SpringLayout.NORTH, filePath, 5, SpringLayout.SOUTH, readTopologyFromLabel);

            //Insert the browseButton
	        layout.putConstraint(SpringLayout.WEST, browseButton, 10, SpringLayout.EAST, filePath);
	        layout.putConstraint(SpringLayout.NORTH, browseButton, 5, SpringLayout.SOUTH, readTopologyFromLabel);

			//Insert randomGeneNumberLabel
		    layout.putConstraint(SpringLayout.WEST, randomGeneNumberLabel, 250, SpringLayout.WEST, radioPanel);
	        layout.putConstraint(SpringLayout.NORTH, randomGeneNumberLabel, 15, SpringLayout.SOUTH, filePath);

            //Insert randomSensorResult
		    layout.putConstraint(SpringLayout.WEST, randomSensorResult, 250, SpringLayout.WEST, radioPanel);
	        layout.putConstraint(SpringLayout.NORTH, randomSensorResult, 5, SpringLayout.SOUTH, randomGeneNumberLabel);

            //Insert userHelp
            layout.putConstraint(SpringLayout.WEST, userHelp, 250, SpringLayout.WEST, radioPanel);
            layout.putConstraint(SpringLayout.NORTH, userHelp, 10, SpringLayout.SOUTH, randomSensorResult);

			//Insert the lower right and lower boundaries
        	layout.putConstraint(SpringLayout.EAST, rootNorthPanel,20,SpringLayout.EAST, randomSensorResult);
        	layout.putConstraint(SpringLayout.SOUTH, rootNorthPanel,10,SpringLayout.SOUTH, radioPanel);

        }
        return rootNorthPanel;
    }

    /**
     * Creates the components which belong to the center panel
     * of the main GUI window. (Uses SpringLayout to customize layout)
     * @return
    */
    public JPanel getCenterPanel() {
        if (rootCenterPanel == null) {
            rootCenterPanel = new JPanel();
            SpringLayout layout = new SpringLayout();
            rootCenterPanel.setLayout(layout);

            targetNodeLabel.setFont(new Font("Arial", Font.BOLD, 11));
			rootCenterPanel.add(targetNodeLabel);
            rootCenterPanel.add(targetNodeResult);
            targetNodeResult.setColumns(4);

            simTimeLabel.setFont(new Font("Arial", Font.BOLD, 11));
            rootCenterPanel.add(simTimeLabel);
            rootCenterPanel.add(simTimeResult);
            simTimeResult.setColumns(4);

            //Insert targetNodeLabel
	        layout.putConstraint(SpringLayout.WEST, targetNodeLabel, 10,SpringLayout.WEST, rootCenterPanel);
	        layout.putConstraint(SpringLayout.NORTH, targetNodeLabel, 15,SpringLayout.NORTH, rootCenterPanel);

			//Insert the radioPanel
			layout.putConstraint(SpringLayout.WEST, targetNodeResult,25,SpringLayout.EAST, targetNodeLabel);
	        layout.putConstraint(SpringLayout.NORTH, targetNodeResult,15, SpringLayout.NORTH, rootCenterPanel);

	        //Insert the simTimeLabel
	        layout.putConstraint(SpringLayout.WEST, simTimeLabel, 10, SpringLayout.WEST, rootCenterPanel);
	        layout.putConstraint(SpringLayout.NORTH, simTimeLabel, 25, SpringLayout.SOUTH, targetNodeLabel);

			//Insert simTimeResult
		    layout.putConstraint(SpringLayout.WEST, simTimeResult, 25, SpringLayout.EAST, targetNodeLabel);
	        layout.putConstraint(SpringLayout.NORTH, simTimeResult, 10,SpringLayout.SOUTH, targetNodeResult);

			//Insert the lower right and lower boundaries
        	layout.putConstraint(SpringLayout.EAST, rootCenterPanel, 200, SpringLayout.EAST, simTimeResult);
        	layout.putConstraint(SpringLayout.SOUTH, rootCenterPanel, 10,SpringLayout.SOUTH, simTimeResult);

        }
        return rootCenterPanel;
    }

    /***
     * This method creates the southern panel of the main GUI window.
     * This panel consists of simply 2 buttons (start simulation or quit).
     * @return
    */
    public JPanel getSouthernPanel() {
        if (rootSouthPanel == null) {
            rootSouthPanel = new JPanel(new BorderLayout());
            SpringLayout layout = new SpringLayout();
            rootSouthPanel.setLayout(layout);

            startButton.addActionListener(this);
            quitButton.addActionListener(this);

            rootSouthPanel.add(startButton);
            rootSouthPanel.add(quitButton);

            //Insert startButton
	        layout.putConstraint(SpringLayout.WEST, startButton, 150,SpringLayout.WEST, rootSouthPanel);
	        layout.putConstraint(SpringLayout.NORTH, startButton, 10,SpringLayout.NORTH, rootSouthPanel);

			//Insert quitButton
			layout.putConstraint(SpringLayout.WEST, quitButton, 125,SpringLayout.EAST, startButton);
	        layout.putConstraint(SpringLayout.NORTH, quitButton, 10, SpringLayout.NORTH, rootSouthPanel);

			//Insert the lower right and lower boundaries
        	layout.putConstraint(SpringLayout.EAST, rootSouthPanel, 150, SpringLayout.EAST, quitButton);
        	layout.putConstraint(SpringLayout.SOUTH, rootSouthPanel, 2,SpringLayout.SOUTH, quitButton);

        }
        return rootSouthPanel;
    }



    /**
     * Event Handler for GUI components.
     * @param event
     */
    public void actionPerformed(ActionEvent event)
    {
        ////////////////////////////////////////////////////////////////////////////////////

        if (event.getSource() == quitButton) {
            this.dispose();
            System.exit(0);
        }

        /////////////////////////////////////////////////////////////////////////////////////
        if (event.getSource() == browseButton){
            String filePathString = "";

            int returnVal = fc.showOpenDialog(SensorSim.this);

     	    if (returnVal == JFileChooser.APPROVE_OPTION) {
                //try and get the file
                File inputFile = fc.getSelectedFile();
                try {
                    //extract the path
                    filePathString = inputFile.getCanonicalPath();
                }catch(IOException e) {
                    System.err.println("IO Error opening file");
                }
                //store the PATH in the textbox on the GUI
                filePath.setText(filePathString);

            } else {

            }
        }

        //////////////////////////////////////////////////////////////////////////////////////
        if (event.getSource() == startButton) {

            //get which type of routing he wants to use
            if(OH_80211Button.isSelected())
                simChoice = 0;
            else if (OH_CSMAButton.isSelected())
                simChoice = 1;
            else if (OH_TDMAButton.isSelected())
                simChoice = 2;
            else if (MH_CSMAButton.isSelected())
                 simChoice = 3;
            else if (MH_80211Button.isSelected())
                simChoice = 4;
            else if (LEACHButton.isSelected())
                simChoice = 5;
            System.out.println("SimChoice: " + simChoice);



            //determine how many target nodes he requested
            total_target_nodes = Integer.parseInt(targetNodeResult.getText());
            System.out.println("YOU requrested" + total_target_nodes + " target nodes");

            //get how long he wanted to run the simulation for
            simulationTime = Integer.parseInt(simTimeResult.getText());
            System.out.println("sim time: " + simulationTime);

            System.out.println("The text is:"+filePath.getText()+"_end");
            //if he entered a path then go and read it.
            if (!filePath.getText().equals("")){
                fileparse = new FileParser(filePath.getText());

                // parse failed... nothing to do there
		        if(!fileparse.parse_succes )
                    return ;

                System.out.println("Setting the total sensor nodes to: " + fileparse.nbsens);
                total_sensor_nodes = fileparse.nbsens;
                total_nodes = total_sensor_nodes + total_target_nodes;

                topo = new Placer(300, 725, 1, total_sensor_nodes, total_target_nodes);
		        test = new Environnement(total_nodes, total_target_nodes);
                test.setTopo(fileparse.map_xmin,fileparse.map_xmax,fileparse.map_ymin,fileparse.map_ymax,fileparse.map_dx,fileparse.map_dy);
                topo.setLogSource(test.log );
            }
            else {
                total_sensor_nodes = Integer.parseInt(randomSensorResult.getText()) + 1;
                total_nodes = total_sensor_nodes + total_target_nodes;

		        topo = new Placer(300, 725, 1, total_sensor_nodes, total_target_nodes);
		        test = new Environnement(total_nodes, total_target_nodes);
		        test.setTopo(0.0, 30.0, 0.0, 100.0, 100.0, 100.0);
                topo.setLogSource( test.log );
            }


            tabsens = new SensorSimple[total_sensor_nodes];
            long time_ = System.currentTimeMillis();

            //***********************************************************
            //  Creating Sink Node
            //***********************************************************
            sink0 = new Sink(sink_id, test, simChoice);
		    topo.add_sink(sink0.sink, 0.0, 0.0, 0.0, 0.0);

            //***********************************************************
            //  Creating Sensor Nodes
            //***********************************************************
		    double xCoord;
            double yCoord;
            double zCoord = 0.0;

            //if the user entered a topology file go and read the sensor locations from there
            if (!filePath.getText().equals("")){

                for(int i = 1; i < tabsens.length; i++) {
                    //parse that entry to extract the info.
                    ParseEntry sensDef = fileparse.getSensor(i-1);
                    //create the sensor
                    tabsens[i] = new SensorSimple((sensDef.number+1), sink_id, test, simChoice, sensDef.posX, sensDef.posY, sensDef.posZ);	;
                    //add it to the GUI so that it can be drawn
                    topo.add_sensor(tabsens[i].sens, 0.0, sensDef.posX ,sensDef.posY ,sensDef.posZ );
                }
            }
            else { //o.w. no topology file was given so randomly generate the sensor locations.
                for(int i = 1; i < total_sensor_nodes; i++) {
                    //create two random x, y coordinates
                    xCoord = generator.nextDouble()* 30;
                    yCoord = generator.nextDouble()* 100;

                    //create the sensor
                    tabsens[i]=new SensorSimple(i, sink_id, test, simChoice, xCoord, yCoord, zCoord);

                    //add it to the GUI so that it can be drawn
                    topo.add_sensor(tabsens[i].sens,0.0, xCoord, yCoord, zCoord);

                }
            }

            //***********************************************************
            //  Creating Target Nodes
            //***********************************************************
            Target[] tabtarg = new Target[total_target_nodes];


            if(total_target_nodes == 0) {
                System.out.println("No Target Nodes");
            }
            else{
                for(int i= total_nodes - total_target_nodes ; i < total_nodes ;i++){
                    //create two random x, y coordinates
                    xCoord = generator.nextDouble()* 30;
                    yCoord = generator.nextDouble()* 100;

                    tabtarg[i - total_nodes + total_target_nodes] = new Target(i, test);
                    topo.add_target(tabtarg[i - total_nodes + total_target_nodes].targ,0.0,xCoord,yCoord,0.0);
                } //endfor
            }


            //************************************************************
            //Plotters
            //    Plotter # 1:To plot the total number of received packets at the sink
            //    Plotter # 2 Calculate the avg latency when the sink finally receives it
            //    Plotter # 3: Output remaining energy levels of the sensors to a plotter
            //    Plotter # 4: Total # of sensors still alive
            //    Plotter # 5: Plots the monitored phenomenon at hand.
            //***********************************************************
            test.root.addComponent(sinkPlot1_);
            test.root.addComponent(sinkPlot2_);
            test.root.addComponent(plot3);
            test.root.addComponent(plot4);
            //plot # 5-----
            sink0.app.createSnrPorts( total_nodes, total_target_nodes);
		    Plotter plot = new Plotter("plot");
		    test.root.addComponent( plot);

            if ((simChoice == 0) || (simChoice == 1)) {
                ((SinkAppOH)sink0.sink.getComponent("SinkAppOH")).getPort(".PacketsReceivedPlot").connectTo(sinkPlot1_.addPort("0", "0"));
                ((SinkAppOH)sink0.sink.getComponent("SinkAppOH")).getPort(".latencyPlot").connectTo(sinkPlot2_.addPort("0", "0"));
                for(int i= 1; i < total_sensor_nodes; i++) {
                    Integer temp = new Integer(i);
                    ((OneHopApp)tabsens[i].sens.getComponent("OneHopApp")).getPort(".plotter").connectTo(plot3.addPort("0",temp.toString()));
                }
                for(int i = 0; i < total_target_nodes; i++){
			        // connexion au plotter
		    	    ((SinkAppOH) sink0.sink.getComponent( "SinkAppOH")).getPort(".snr"+i).connectTo( plot.addPort(i+"",i+""));
		        }
             }
             else if ((simChoice == 2)) {
                ((SinkAppTDMA)sink0.sink.getComponent("SinkAppTDMA")).getPort(".PacketsReceivedPlot").connectTo(sinkPlot1_.addPort("0", "0"));
                ((SinkAppTDMA)sink0.sink.getComponent("SinkAppTDMA")).getPort(".latencyPlot").connectTo(sinkPlot2_.addPort("0", "0"));
                for(int i= 1; i < total_sensor_nodes; i++) {
                    Integer temp = new Integer(i);
                    ((OneHopAppTDMA)tabsens[i].sens.getComponent("OneHopAppTDMA")).getPort(".plotter").connectTo(plot3.addPort("0",temp.toString()));
                }
                for(int i = 0; i < total_target_nodes; i++){
			        // connexion au plotter
		    	    ((SinkAppTDMA) sink0.sink.getComponent( "SinkAppTDMA")).getPort(".snr"+i).connectTo( plot.addPort(i+"",i+""));
		        }
             }
             else if ((simChoice == 3) || (simChoice == 4)){
                 System.out.println(((SinkAppMH)sink0.sink.getComponent("SinkAppMH")).getNid());
                ((SinkAppMH)sink0.sink.getComponent("SinkAppMH")).getPort(".PacketsReceivedPlot").connectTo(sinkPlot1_.addPort("0", "0"));
                ((SinkAppMH)sink0.sink.getComponent("SinkAppMH")).getPort(".latencyPlot").connectTo(sinkPlot2_.addPort("0", "0"));//connectTo(sinkPlot2_.outport);
                for(int i= 1; i < total_sensor_nodes; i++) {
                    Integer temp = new Integer(i);
                    ((MultiHopApp)tabsens[i].sens.getComponent("MultiHopApp")).getPort(".plotter").connectTo(plot3.addPort("0",temp.toString()));
                }
                for(int i = 0; i < total_target_nodes; i++){
			        // connexion au plotter
		    	    ((SinkAppMH) sink0.sink.getComponent( "SinkAppMH")).getPort(".snr"+i).connectTo( plot.addPort(i+"",i+""));
		        }
             }
             else if (simChoice == 5) {
                ((SinkAppLEACH)sink0.sink.getComponent("SinkAppLEACH")).getPort(".PacketsReceivedPlot").connectTo(sinkPlot1_.addPort("0", "0"));//(sinkPlot1_.outport);
                ((SinkAppLEACH)sink0.sink.getComponent("SinkAppLEACH")).getPort(".theo_PacketsReceivedPlot").connectTo(sinkPlot1_.addPort("0","1"));
                ((SinkAppLEACH)sink0.sink.getComponent("SinkAppLEACH")).getPort(".latencyPlot").connectTo(sinkPlot2_.addPort("0", "0"));
                for(int i= 1; i < total_sensor_nodes; i++) {
                    Integer temp = new Integer(i);
                    ((LEACHApp)tabsens[i].sens.getComponent("LEACHApp")).getPort(".plotter").connectTo(plot3.addPort("0",temp.toString()));
                }
                for(int i = 0; i < total_target_nodes; i++){
			        // connexion au plotter
		    	    ((SinkAppLEACH) sink0.sink.getComponent( "SinkAppLEACH")).getPort(".snr"+i).connectTo( plot.addPort(i+"",i+""));
		        }

             }
             else {
                ((SensorApp)sink0.sink.getComponent("SensorApp")).getPort(".PacketsReceivedPlot").connectTo(sinkPlot1_.addPort("0", "0"));
                ((SensorApp)sink0.sink.getComponent("SensorApp")).getPort(".latencyPlot").connectTo(sinkPlot2_.addPort("0", "0"));

                 for(int i = 0; i < total_target_nodes; i++){
			        // connexion au plotter
		    	    ((SensorApp) sink0.sink.getComponent( "SensorApp")).getPort(".snr"+i).connectTo( plot.addPort(i+"",i+""));
		        }
             }

            liveSensors.setID("liveSensors");
            test.root.addComponent(liveSensors);
            liveSensors.plotterPort.connectTo(plot4.addPort("0", "0"));

            //close the current window
            this.dispose();

            //**********************************************************
            // Running Simulation
            //**********************************************************
            System.out.println("Simulation begins...");

            drcl.comp.ACARuntime sim_ = null;
            sim_ = new drcl.sim.event.SESimulator();

            sim_.takeover(test.root);
            sim_.stop();
            sim_.addRunnable(0.0000001, new SensorSim(sim_, time_));
            sim_.stopAt(simulationTime);

            Object[] object = new Object[1];
            object[0] = test.root;

            drcl.comp.Util.operate(object, "start");
            sim_.resume();

        }
    }


    //***************************************************************
    // Helper Class - Only used for Multi-Hop Modes.
    //***************************************************************
    class neighborInfo {
        private int self_id;
        private int neighbor_id;
        private double neighborX;
        private double neighborY;
        private double neighborZ;
        private double neighbor_dist;

        public neighborInfo(int self_nid_, int nid, double neighborX_, double neighborY_, double neighborZ_, double neighbor_dist_)
        {
            self_id = self_nid_;
            neighbor_id = nid;
            neighborX = neighborX_;
            neighborY = neighborY_;
            neighborZ = neighborZ_;
            neighbor_dist = neighbor_dist_;
        }

        public int getSelf_id() {
            return self_id;
        }

        public void setSelf_id(int self_id) {
            this.self_id = self_id;
        }

        public int getNeighbor_id() {
            return neighbor_id;
        }

        public void setNeighbor_id(int neighbor_id) {
            this.neighbor_id = neighbor_id;
        }

        public double getNeighborX() {
            return neighborX;
        }

        public void setNeighborX(double neighborX) {
            this.neighborX = neighborX;
        }

        public double getNeighborY() {
            return neighborY;
        }

        public void setNeighborY(double neighborY) {
            this.neighborY = neighborY;
        }

        public double getNeighborZ() {
            return neighborZ;
        }

        public void setNeighborZ(double neighborZ) {
            this.neighborZ = neighborZ;
        }

        public double getNeighbor_dist() {
            return neighbor_dist;
        }

        public void setNeighbor_dist(double neighbor_dist) {
            this.neighbor_dist = neighbor_dist;
        }
    }
}
