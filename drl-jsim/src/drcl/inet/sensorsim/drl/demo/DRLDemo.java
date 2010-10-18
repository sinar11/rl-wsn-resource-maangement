/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */
package drcl.inet.sensorsim.drl.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ChainedTransformer;

import drcl.inet.sensorsim.drl.EnergyStats.NodeStat;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractModalGraphMouse;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.GradientVertexRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.renderers.BasicVertexLabelRenderer.InsidePositioner;


/**
 * Shows a graph overlaid on a world map image.
 * Scaling of the graph also scales the image background.
 * @author Tom Nelson
 * 
 */
@SuppressWarnings("serial")
public class DRLDemo extends JApplet implements IDRLDemo {

    /**
     * the graph
     */
    Graph<Integer, Number> graph;

    /**
     * the visual component and renderer for the graph
     */
    VisualizationViewer<Integer, Number> vv;
    
	Map<Integer,double[]> map = new HashMap<Integer,double[]>();
	
	Map<Integer,String> nodeTypes= new HashMap<Integer,String>();
	
   	NodeStat[] nodeStats= new NodeStat[20];

	private Layout<Integer,Number> layout;
	
	List<List<Long>> activeStreams= new ArrayList<List<Long>>();
    
    /**
     * create an instance of a simple graph with controls to
     * demo the zoom features.
     * 
     */
    public DRLDemo() {
    	// create a frome to hold the graph
        final JFrame frame = new JFrame();
        Container content = frame.getContentPane();
        content.add(this);
        initDemo();
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    
    private void initDemo(){
    	setLayout(new BorderLayout());
    
        /*map.put("0",new double[]{550.0,350.0});
        map.put("1",new double[]{450.0,250.0});
        map.put("2",new double[]{450.0,450.0});
        map.put("3",new double[]{350.0,350.0});
        map.put("4",new double[]{360.0,340.0});
        map.put("5",new double[]{300.0,300.0});
        map.put("6",new double[]{250.0,450.0});
        map.put("7",new double[]{150.0,350.0});
        map.put("8",new double[]{270.0,325.0});
        map.put("9",new double[]{251.0,270.0});*/
       
   		// create a simple graph for the demo
        graph = new DirectedSparseMultigraph<Integer, Number>();
        
        Dimension layoutSize = new Dimension(600,600);
        
        layout = new StaticLayout<Integer,Number>(graph,
        		new ChainedTransformer(new Transformer[]{
        				new NodeTransformer(),
        				new LatLonPixelTransformer()
        		}));
        	
        layout.setSize(layoutSize);
        vv =  new VisualizationViewer<Integer,Number>(layout,
        		new Dimension(400,200));
       
        vv.getRenderer().setVertexRenderer(
        		new GradientVertexRenderer<Integer,Number>(
        				Color.white, Color.red, false));
        //vv.getRenderContext().setVe
        vv.getRenderContext().setVertexShapeTransformer(new VertexShapeSizeAspect(graph));
        // add my listeners for ToolTips
        vv.setVertexToolTipTransformer(new Transformer<Integer,String>(){
        	public String transform(Integer v) {
				return nodeTypes.get(v)+"-"+v+",Energy="+nodeStats[v].getCurrEnergy();
		}});
        vv.setEdgeToolTipTransformer(new Transformer<Number,String>() {
			public String transform(Number edge) {
				return "E"+graph.getEndpoints(edge).toString();
			}});
        
        vv.getRenderContext().setVertexLabelTransformer(new Transformer<Integer,String>(){
        	public String transform(Integer v) {
				return nodeTypes.get(v)+"-"+v;
		}});
        vv.getRenderer().getVertexLabelRenderer().setPositioner(new InsidePositioner());
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.E);
        
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        add(panel);
        final AbstractModalGraphMouse graphMouse = new DefaultModalGraphMouse();
        vv.setGraphMouse(graphMouse);
        
        vv.addKeyListener(graphMouse.getModeKeyListener());
        vv.setToolTipText("<html><center>Type 'p' for Pick mode<p>Type 't' for Transform mode");
        
        final ScalingControl scaler = new CrossoverScalingControl();
        
//        vv.scaleToLayout(scaler);


        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1/1.1f, vv.getCenter());
            }
        });

        JButton reset = new JButton("reset");
        reset.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
				vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
			}});

        JPanel controls = new JPanel();
        controls.add(plus);
        controls.add(minus);
        controls.add(reset);
        add(controls, BorderLayout.SOUTH);
    }
   

    /**
     * create edges for this demo graph
     * @param v an array of Vertices to connect
     */
    void createEdges() {
     	
    	/*for(int i=0; i<map.keySet().size()*1.3; i++) {
    		graph.addEdge(new Double(Math.random()), randomCity(), randomCity(), EdgeType.DIRECTED);
    	}*/
    }
   
    double getEnergy(Integer nid){
    	NodeStat stat=(nodeStats.length>=nid)?nodeStats[nid]:null;
    	if(stat!=null){
    		return stat.getCurrEnergy();
    	}
    	return 20;
    }
    class NodeTransformer implements Transformer<Integer,double[]> {

    	public NodeTransformer() {
    	}

    	/**
    	 * transform airport code to latlon string
    	 */
		public double[] transform(Integer city) {
			return map.get(city);
		}
    }
    
    static class LatLonPixelTransformer implements Transformer<double[],Point2D> {
    	
    	public LatLonPixelTransformer() {
    	}
    	/**
    	 * transform a lat
    	 */
		public Point2D transform(double[] latlon) {
			double latitude = latlon[0];
			double longitude = latlon[1];
			return new Point2D.Double(longitude,latitude);
		}
    	
    }

    private final class VertexShapeSizeAspect
    extends AbstractVertexShapeTransformer 
    implements Transformer  {
    	protected Graph<Integer,Number> graph;
//        protected AffineTransform scaleTransform = new AffineTransform();
        
        public VertexShapeSizeAspect(Graph<Integer,Number> graphIn)
        {
        	this.graph = graphIn;
            setSizeTransformer(new Transformer() {

				public Integer transform(Object o) {
					if(o instanceof Integer){
						return Math.min((int)getEnergy((Integer)o)/2,25);
					}else
		                return 10;

				}});
           /* setAspectRatioTransformer(new Transformer<V,Float>() {

				public Float transform(V v) {
		            if (stretch) {
		                return (float)(graph.inDegree(v) + 1) / 
		                	(graph.outDegree(v) + 1);
		            } else {
		                return 1.0f;
		            }
				}});*/
        }
    
        public Shape transform(Object v)
        {
        	Integer id= (Integer)v;
        	String type= nodeTypes.get(id);
        	if(type.equals(TYPE_SINK))
        		return factory.getRegularStar(v,6);
        	else if(type.equals(TYPE_SENSOR))
        		return factory.getEllipse(v);
        	else if(type.equals(TYPE_TARGET))
        		return factory.getRegularPolygon(v, 5);
        	else return factory.getRoundRectangle(v);
        }
    }
    /**
     * a driver for this demo
     */
    public static void main(String[] args) {
        
    }

	public void addNode(int id, double[] latlong, double energy, String type) {
		layout.lock(id,true);
        //add a vertex
       /* Relaxer relaxer = vv.getModel().getRelaxer();
        relaxer.pause();*/
        map.put(id,latlong);
		graph.addVertex(id);
		NodeStat stat= new NodeStat(id,0);
		stat.setCurrEnergy(energy);
		nodeStats[id]=stat;
		nodeTypes.put(id,type);
		System.err.println("added node " + id);
       
        layout.initialize();
        //relaxer.resume();
        layout.lock(id,false);
		
	}

	public void updateNodePosition(Integer nid, double[] loc){
		double[] currLoc= map.get(nid);
		double dist = Math.sqrt(Math.pow(Math.abs(loc[0]- currLoc[0]), 2)
                  + Math.pow(Math.abs(loc[1] - currLoc[1]), 2));
		if(dist<5) return;
		map.put(nid,loc);
		layout.lock(nid, true);
		layout.setLocation(nid, new Point2D.Double(loc[0],loc[1]));
		/*graph.removeVertex(nid);
		*/
		layout.initialize();
		layout.lock(nid, false);
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
		
		/*layout.lock(0, true);
		graph.addVertex(nid);
		layout.initialize();
		layout.lock(0, false);
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();*/
	}
	
	public void markActiveStream(List<List<Long>> streams) {
		boolean diffFound = (streams.size() != activeStreams.size());
		if (!diffFound) {
			for (List<Long> stream : streams) {
				if (!activeStreams.contains(stream)) {
					diffFound = true;
					break;
				}
			}
		}
		if (!diffFound) {
			for (List<Long> stream : activeStreams) {
				if (!streams.contains(stream)) {
					diffFound = true;
					break;
				}
			}
		}

		if (!diffFound)
			return;
		activeStreams.clear();
		Number[] edges = graph.getEdges().toArray(new Number[0]);
		layout.lock(0, true);
		for (Number e : edges) {
			graph.removeEdge(e);
		}
		for (List<Long> s : streams) {
			activeStreams.add(s);
			for (int i = 0; i < s.size() - 1; i++) {
				Double id = new Double(s.get(i).toString()
						+ s.get(i + 1).toString());
				if (!graph.containsEdge(id))
					graph.addEdge(id, Integer.parseInt(s.get(i).toString()),
							Integer.parseInt(s.get(i + 1).toString()));
			}
		}
		layout.initialize();
		layout.lock(0, false);
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(
				Layer.LAYOUT).setToIdentity();
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(
				Layer.VIEW).setToIdentity();
	}

	public void markCurrentEdge(int fromNode, int toNode) {
		// TODO Auto-generated method stub
		
	}

	public void updateNodes(NodeStat[] stats) {
		this.nodeStats=stats;		
	}
}
