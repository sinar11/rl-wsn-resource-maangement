package drcl.inet.sensorsim.drl;

import java.util.logging.Level;

public interface IDRLSensorApp {

	public void log(Level level, String s);
	public int getNoOfStates();
	public int getNid();
}
