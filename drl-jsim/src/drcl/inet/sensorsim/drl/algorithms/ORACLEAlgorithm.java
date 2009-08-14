package drcl.inet.sensorsim.drl.algorithms;

import java.util.Hashtable;

import drcl.inet.sensorsim.drl.DRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;

public class ORACLEAlgorithm extends AbstractAlgorithm{
	
	protected ORACLEAlgorithm(Hashtable<Integer, SensorTask> taskList,
			DRLSensorApp app) {
		super(taskList, app);
	}

	 
	public SensorTask getNextTaskToExecute(SensorState currentState) {
		return null; //TODO
	}
	 
	@Override
	public Algorithm getAlgorithm() {
		return Algorithm.ORACLE;
	}

	@Override
	public void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState) {
	}
}
