package drcl.inet.sensorsim.drl.algorithms;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import drcl.inet.sensorsim.drl.CSVLogger;
import drcl.inet.sensorsim.drl.DRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.inet.sensorsim.drl.SensorState.BestTask;

public class DIRLAlgorithm extends AbstractAlgorithm{
	
	protected DIRLAlgorithm(Hashtable<Integer, SensorTask> taskList,
			DRLSensorApp app) {
		super(taskList, app);
	}
	 
	public SensorTask getNextTaskToExecute(SensorState currentState) {
		SensorTask nextTask=null;
		double expFactor=calcExplorationFactor();
		//CSVLogger.log("Exp"+app.getNid(),""+expFactor,false,Algorithm.DIRL);
		//double delta=0.0;
		if (Math.random() < expFactor) { // exploration choosen
			nextTask = getRandomTaskToExecute();
		//	CSVLogger.log("Delta"+app.getNid(), ""+diff,false,Algorithm.DIRL);
		}else{	
			nextTask = determineBestTaskToExecute(currentState);				
		}
			
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
	 
	protected double calcExplorationFactor() {
        double e=MIN_EPSILON+MAX_EPSILON*(SensorState.MAX_STATES-app.getNoOfStates())/SensorState.MAX_STATES;
        return (e<MAX_EPSILON)?e:MAX_EPSILON;
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
			SensorTask currBTask= determineBestTaskToExecute(prevState);
			BestTask prevBTask= prevState.getBestTask();
			double delta=0.0;
			if(currBTask!=null && prevBTask!=null /*&& currentState.getStateId()==prevState.getStateId()*/){
				if(currBTask.getId()==prevBTask.id) delta=0.0;
				else{
					delta=currBTask.getQvalue(prevState)-prevBTask.qValue;//)?1.0:-1.0;
				}
			}
			prevState.setBestTask(currBTask, currBTask.getQvalue(prevState));
			CSVLogger.log("Delta"+app.getNid(),delta+","+(prevBTask!=null?prevBTask.id:"N")+","+currBTask.getId()+","+prevState+","+currentState,true,Algorithm.DIRL);
		}		
	}
}
