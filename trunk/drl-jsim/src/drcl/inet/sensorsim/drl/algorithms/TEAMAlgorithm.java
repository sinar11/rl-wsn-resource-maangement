package drcl.inet.sensorsim.drl.algorithms;

import java.util.Hashtable;
import java.util.Iterator;

import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.IDRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;

public class TEAMAlgorithm extends AbstractAlgorithm{
	
	protected TEAMAlgorithm(Hashtable<Integer, SensorTask> taskList,
			IDRLSensorApp app) {
		super(taskList, app);
	}
	 
	public SensorTask getNextTaskToExecute(SensorState currentState) {
		SensorTask nextTask=getBestTeamGameBasedTask();
		if (Math.random() < calcExplorationFactor()) { // exploration choosen
			nextTask = getRandomTaskToExecute();
		}else	
			nextTask = getBestTeamGameBasedTask();
		return nextTask;
	}

	private SensorTask getBestTeamGameBasedTask() {
    	double maxQ=Double.MIN_VALUE;
    	SensorTask bestTask=null;
    	for(Iterator it=taskList.values().iterator();it.hasNext();){
            SensorTask task= (SensorTask)it.next();
            double utility= task.getExpectedPrice();
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
		return Algorithm.TEAM;
	}

	@Override
	public void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState) {		
	}
}
