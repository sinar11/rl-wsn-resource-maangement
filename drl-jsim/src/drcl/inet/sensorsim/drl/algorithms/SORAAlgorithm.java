package drcl.inet.sensorsim.drl.algorithms;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import drcl.inet.sensorsim.drl.DRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm.Algorithm;

public class SORAAlgorithm extends AbstractAlgorithm{
	double[] betaValues= new double[5];     // for SORA
    
	protected SORAAlgorithm(Hashtable<Integer, SensorTask> taskList,
			DRLSensorApp app) {
		super(taskList, app);
	}
	 
	public SensorTask getNextTaskToExecute(SensorState currentState) {
		SensorTask nextTask=null;
		 if (Math.random() <= 0.05) { //exploration choosen
			 nextTask = getRandomTaskToExecute();
         } else {
        	 nextTask=getBestSORATaskToExecute();
         }
		return nextTask;
	}

	 private SensorTask getBestSORATaskToExecute() {
	        double maxQ=Double.NEGATIVE_INFINITY;
	        SensorTask bestTask=null;
	        for(Iterator it=taskList.values().iterator();it.hasNext();){
	            SensorTask task= (SensorTask)it.next();
	            double utility= betaValues[task.getId()]*task.getExpectedPrice();
	            if(task.isAvailable()){
	                if(utility>maxQ){
	                    maxQ=utility;
	                    bestTask=task;
	                }
	            }
	        }
	        return bestTask;
	        
	    }
	 
	protected double calcExplorationFactor() {
        double e=MIN_EPSILON+MAX_EPSILON*(SensorState.MAX_STATES-app.getNoOfStates())/SensorState.MAX_STATES;
        return (e<MAX_EPSILON)?e:MAX_EPSILON;
    }

	@Override
	public Algorithm getAlgorithm() {
		return Algorithm.SORA;
	}

	@Override
	public void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState) {
		if(currentTask!=null){
            if(currentTask.getLastReward()>=0){
                betaValues[currentTask.getId()]=0.2+0.8*betaValues[currentTask.getId()];
            }else{
                betaValues[currentTask.getId()]=0.8*betaValues[currentTask.getId()];
            }
        }	
	}
}
