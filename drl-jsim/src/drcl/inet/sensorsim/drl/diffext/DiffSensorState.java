/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim.drl.diffext;

import java.util.HashMap;
import java.util.Map;

import drcl.inet.sensorsim.drl.DIRLSensorState;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm;

/*
 * @author Kunal
 */

public class DiffSensorState implements SensorState{
	//public static final int MAX_STATES = 4;
	
    protected int stateId;
    protected boolean successInRecentDiffusion=false;
    protected boolean successInRecentSourceTask= false;
    
    public DiffSensorState(boolean successInRecentDiffusion, boolean successInRecentSourceTask){
        this.successInRecentDiffusion=successInRecentDiffusion;
        this.successInRecentSourceTask=successInRecentSourceTask;       
    }
    
    public boolean equals(Object o){
    	//if(!o instanceof DiffSensorState) return false;
    	DiffSensorState s= (DiffSensorState)o;
        double hammingDist= getHammingDistance(s);
        if(hammingDist>=THRESHOLD_HAMMING)
            return false;
        else
            return true;
    }
    
  /*  public void updateTask(int taskId, boolean success){
    	this.successInRecentSourceTask.put(taskId, success);
    }
    
    public void removeTask(int taskId){
    	successInRecentSourceTask.remove(taskId);
    }*/
    
    public String toString(){
        return stateId+":"+successInRecentDiffusion+","+successInRecentSourceTask;
    }
    private double getHammingDistance(DiffSensorState s) {
        double dist=0;
        if(s.successInRecentDiffusion!=this.successInRecentDiffusion) dist+=1;
        if(s.successInRecentSourceTask!=this.successInRecentSourceTask) dist+=1;
        return dist;
    }

	public int getStateId() {
		return stateId;
	}
	public void setStateId(int stateId) {
		this.stateId=stateId;
	}

	public double calcExplorationFactor(SensorTask currentTask) {
		if(!currentTask.taskId.equals(MicroLearner.SLEEP) && (successInRecentDiffusion || successInRecentSourceTask))
			return AbstractAlgorithm.MIN_EPSILON;
		else
			return AbstractAlgorithm.MAX_EPSILON;
		
	}

}
