/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim;

import java.util.HashMap;

/*
 * @author Kunal
 */

public class CurrentTargetPositionTracker {

    protected static CurrentTargetPositionTracker instance= new CurrentTargetPositionTracker();
    protected HashMap targetPositions= new HashMap();
    
    private CurrentTargetPositionTracker(){}
    
    public static synchronized CurrentTargetPositionTracker getInstance(){
        return instance;
    }
    
    public void setTargetPosition(long nodeId, double[] loc){
        targetPositions.put(new Long(nodeId),loc);
    }
    
    public double[] getTargetPosition(long nodeId){
        return (double[]) targetPositions.get(new Long(nodeId));
    }

	public long getTargetNid() {
		return ((Long[]) targetPositions.keySet().toArray(new Long[1]))[0];
	}
}
