package drcl.inet.sensorsim.drl.algorithms;

import java.util.Hashtable;

import drcl.inet.sensorsim.drl.DRLSensorApp;
import drcl.inet.sensorsim.drl.SensorTask;

public class COINAlgorithm extends DIRLAlgorithm{

	protected COINAlgorithm(Hashtable<Integer, SensorTask> taskList,
			DRLSensorApp app) {
		super(taskList, app);
	}

	@Override
	public Algorithm getAlgorithm() {
		return Algorithm.COIN;
	}

}
