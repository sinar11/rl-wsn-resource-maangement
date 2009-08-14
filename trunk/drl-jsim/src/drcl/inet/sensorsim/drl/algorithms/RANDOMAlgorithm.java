package drcl.inet.sensorsim.drl.algorithms;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import drcl.inet.sensorsim.drl.DRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;

public class RANDOMAlgorithm extends AbstractAlgorithm{
	
	protected RANDOMAlgorithm(Hashtable<Integer, SensorTask> taskList,
			DRLSensorApp app) {
		super(taskList, app);
	}

	public SensorTask getNextTaskToExecute(SensorState currentState) {
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
