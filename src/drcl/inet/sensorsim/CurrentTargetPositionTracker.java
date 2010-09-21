/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim;

import java.util.HashMap;
import java.util.Map;

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
        targetPositions.put(new Long(nodeId),loc);
    }
    
    public double[] getTargetPosition(long nodeId){
        return  targetPositions.get(nodeId);
    }
    
    public Map<Long, double[]> getTargetPositions() {
		return targetPositions;
	}
}
