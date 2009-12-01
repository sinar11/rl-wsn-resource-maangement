package drcl.inet.sensorsim.drl.algorithms;

import java.util.Hashtable;
import java.util.Iterator;

import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.IDRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;

public class SORAAlgorithm extends AbstractAlgorithm{
	double[] betaValues= new double[5];     // for SORA
    
	protected SORAAlgorithm(Hashtable<Integer, SensorTask> taskList,
			IDRLSensorApp app) {
		super(taskList, app);
	}
	 
	public SensorTask getNextTaskToExecute(SensorState currentState) {
		SensorTask nextTask=null;
		 if (Math.random() <= 0.1) { //exploration choosen
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
