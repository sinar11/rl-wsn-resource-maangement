package drcl.inet.sensorsim.drl.algorithms;

import java.util.Hashtable;

import drcl.inet.sensorsim.drl.DRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;

public class SIMPLEAlgorithm extends AbstractAlgorithm {

	protected SIMPLEAlgorithm(Hashtable<Integer, SensorTask> taskList,
			final DRLSensorApp app) {
		super(taskList, app);		
	}

	@Override
	public Algorithm getAlgorithm() {
		return Algorithm.SIMPLE;
	}

	@Override
	public SensorTask getNextTaskToExecute(SensorState currentState) {
		return taskList.get(0); //always use ROUTE
	}

	@Override
	public void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState) {
		//nothing to do
	}

}
