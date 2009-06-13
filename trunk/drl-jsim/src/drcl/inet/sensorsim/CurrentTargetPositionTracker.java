/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim;

import java.util.HashMap;

/*
 * @author Kunal
 */

public class CurrentTargetPositionTracker {

    protected static CurrentTargetPositionTracker instance;
    protected HashMap targetPositions= new HashMap();
    
    public static synchronized CurrentTargetPositionTracker getInstance(){
        if(instance==null){
            instance= new CurrentTargetPositionTracker();
        }
        return instance;
    }
    
    public void setTargetPosition(long nodeId, double[] loc){
   //     System.out.println("Target:"+loc[0]+"--"+loc[1]);
        targetPositions.put(new Long(nodeId),loc);
    }
    
    public double[] getTargetPosition(long nodeId){
        return (double[]) targetPositions.get(new Long(nodeId));
    }
}
