/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim.drl.diffext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * @author Kunal
 */

public class NodePositionGroupClassifier {
	protected static final int DISTANCE_THRESHOLD=50;
	
    protected static NodePositionGroupClassifier instance= new NodePositionGroupClassifier();
    protected HashMap<Long, double[]> nodePositions= new HashMap<Long, double[]>();
    protected List<Group> knownGroups= new ArrayList<Group>();
    
    public static synchronized NodePositionGroupClassifier getInstance(){
        return instance;
    }
    
    public String getNodeGroupIdentifier(long nodeId, double[] loc){
        nodePositions.put(new Long(nodeId),loc);
        for(Group group:knownGroups){
        	if(group.isInGroup(loc)) return group.identifier;
        }
        //not found in any existing group
        Group group= new Group(loc);
        knownGroups.add(group);
        return group.identifier;
    }
    
    class Group{
    	double[] position;
    	String identifier;
    	Group(double[] position){
    		this.position= position;
    		this.identifier=position[0]+":"+position[1];
    	}    	
    	public boolean isInGroup(double[] loc){
    		double dist = Math.sqrt(Math.pow(Math.abs(position[0]-loc[0]), 2)
    		                + Math.pow(Math.abs(position[1] - loc[1]), 2));
    		return (dist<DISTANCE_THRESHOLD);
    	}
    }
}
