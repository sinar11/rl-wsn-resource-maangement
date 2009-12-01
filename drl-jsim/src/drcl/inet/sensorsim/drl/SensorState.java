package drcl.inet.sensorsim.drl;

public interface SensorState {
	public static final int MAX_STATES = 4;
	public static final double THRESHOLD_HAMMING = 1;
    
	 public int getStateId();
	 
	 public void setStateId(int stateId);
	 
	 public double calcExplorationFactor();
	 
}
