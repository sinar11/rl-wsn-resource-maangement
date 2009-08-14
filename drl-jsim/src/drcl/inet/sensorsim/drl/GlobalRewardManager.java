package drcl.inet.sensorsim.drl;

import java.util.List;

import drcl.inet.sensorsim.drl.DRLSensorApp.TrackingEvent;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm.Algorithm;

public interface GlobalRewardManager {

	static class WLReward{
		long pktId;
		double reward;
		WLReward(long pkt, double rwd){
			this.pktId=pkt;
			this.reward=rwd;
		}
	}

	public List<Double> getGlobalRewards();

	public String stats();

	public void dataArrived(long totalExecutions, TrackingEvent tevent);

	public double getEffectiveCost();

	public List<WLReward> getPendingRewards(long nid);

	public void manage(long totalExecutions, Algorithm algorithm);
	
	public double getTotalCost();
	
	public void addToTotalCost(double cost);
}
