package drcl.inet.sensorsim.drl.demo;

import java.util.List;

import drcl.inet.sensorsim.drl.EnergyStats.NodeStat;

public interface IDRLDemo {
	public String TYPE_TARGET="Target";
	public String TYPE_SINK="Sink";
	public String TYPE_SENSOR="Sensor";
	public String TYPE_VIDEO="Video";
	
	public void addNode(int id, double[] latlong, double energy, String type);
	
	public void markActiveStream(List<List<Long>> list);
	
	public void markCurrentEdge(int fromNode, int toNode);
	
	public void updateNodes(NodeStat[] stats);
	
	public void updateNodePosition(Integer nid, double[] loc);
	
}
