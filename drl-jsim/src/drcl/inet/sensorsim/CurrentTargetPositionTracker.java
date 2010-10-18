/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim;

import java.util.HashMap;
import java.util.Map;

import drcl.inet.sensorsim.drl.demo.DRLDemo;
import drcl.inet.sensorsim.drl.demo.DRLDemoFactory;
import drcl.inet.sensorsim.drl.demo.IDRLDemo;

/*
 * @author Kunal
 */

public class CurrentTargetPositionTracker {

    protected static CurrentTargetPositionTracker instance;
    protected HashMap<Long, double[]> targetPositions= new HashMap<Long, double[]>();
    
    public static synchronized CurrentTargetPositionTracker getInstance(){
        if(instance==null){
            instance= new CurrentTargetPositionTracker();
        }
        return instance;
    }
    
    public void setTargetPosition(long nodeId, double[] loc){
    	if(!targetPositions.containsKey(nodeId))
    		DRLDemoFactory.getDRLDemo().addNode((int)nodeId, loc, 50, IDRLDemo.TYPE_TARGET);
    	else{
    		DRLDemoFactory.getDRLDemo().updateNodePosition((int)nodeId, loc);
    	}
    		
        targetPositions.put(new Long(nodeId),loc);
    }
    
    public double[] getTargetPosition(long nodeId){
        return  targetPositions.get(nodeId);
    }
    
    public Map<Long, double[]> getTargetPositions() {
		return targetPositions;
	}
}
