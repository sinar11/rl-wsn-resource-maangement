package drcl.inet.sensorsim.drl.algorithms;

import java.util.Hashtable;

import drcl.inet.sensorsim.drl.IDRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;

public class RANDOMAlgorithm extends AbstractAlgorithm{
	
	protected RANDOMAlgorithm(Hashtable<Integer, SensorTask> taskList,
			IDRLSensorApp app) {
		super(taskList, app);
	}

	public SensorTask getNextTaskToExecute(SensorState currentState, SensorTask currentTask) {
		SensorTask nextTask=getRandomTaskToExecute();
		return nextTask;
	}
	@Override
	public Algorithm getAlgorithm() {
		return Algorithm.RANDOM;
	}

	@Override
	public void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState) {
	}
}
