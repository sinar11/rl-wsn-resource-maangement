package drcl.inet.sensorsim.drl.algorithms;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import drcl.inet.sensorsim.drl.IDRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;

public class DIRLAlgorithm extends AbstractAlgorithm{
	
	protected DIRLAlgorithm(Hashtable<Integer, SensorTask> taskList,
			IDRLSensorApp app) {
		super(taskList, app);
	}
	 
	public SensorTask getNextTaskToExecute(SensorState currentState) {
		SensorTask nextTask=null;
		if (Math.random() < calcExplorationFactor(currentState)) { // exploration choosen
			nextTask = getRandomTaskToExecute();
		}else	
			nextTask = determineBestTaskToExecute(currentState);
		return nextTask;
	}

	 protected SensorTask determineBestTaskToExecute(SensorState currentState) {
	        double maxQ=Double.NEGATIVE_INFINITY;
	        List<SensorTask> bestTasks=new ArrayList<SensorTask>();
	        
	        for(Iterator it=taskList.values().iterator();it.hasNext();){
	            SensorTask task= (SensorTask)it.next();
	            double utility= task.getQvalue(currentState);//*task.getExpectedPrice(); 
	            if(task.isAvailable()){
	                if(utility>maxQ){
	                    maxQ=utility;
	                    bestTasks.clear();
	                    bestTasks.add(task);
	                }else if(utility==maxQ){
	                	bestTasks.add(task);
	                }
	            }
	        }
	        if(bestTasks.size()==1) return bestTasks.get(0);
	        int taskId= (int) (Math.random()*bestTasks.size());
	        return bestTasks.get(taskId);        
	    }
	 
	protected double calcExplorationFactor(SensorState currentState) {
		if(currentState==null) return MAX_EPSILON;
		else
			return currentState.calcExplorationFactor();        
    }

	@Override
	public Algorithm getAlgorithm() {
		return Algorithm.DIRL;
	}

	@Override
	public void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState) {
		if (currentTask != null) {
			currentTask.updateQValue(prevState,
					determineBestTaskToExecute(currentState).getQvalue(currentState));
		}		
	}
}
