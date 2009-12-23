package drcl.inet.sensorsim.drl.diffext;

public class DataStreamEntry {
	/** The neighbor from which the data was received. If a reinforcement is received, it is forwarded to this source */
	private long sourceId ; 
	
	/** timestamp of last data **/ 
	private double timestamp ;

	/**Cumulative avg WL Reward from last reinforcement **/
	private double avgWLReward;
	
	/**Cumulative Avg Pkt reward from last reinforcement**/
	private double avgPktReward;	
	
	/**no. of data packets from last reinforcement**/
	private int noOfPackets;
	
	/** no of WL reward updates from last reinforcement **/
	private int noOfWLRewards;
	
	/** Time at which last reinforcement was sent **/ 
	private double lastReinforcedTime;

	public DataStreamEntry(long sourceId){
		this.sourceId=sourceId;
	}
	
	public String toString(){
		return "sourceId="+sourceId+",noOfPackets="+noOfPackets+",noOfWLRewards="+noOfWLRewards+",avgPktReward="+avgPktReward+",avgWLReward="+avgWLReward;
	}
	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	public double getLastReinforcedTime() {
		return lastReinforcedTime;
	}

	public void setLastReinforcedTime(double lastReinforcedTime) {
		this.lastReinforcedTime = lastReinforcedTime;
	}
	
	public int getNoOfPackets() {
		return noOfPackets;
	}

	public double getAvgWLReward() {
		return avgWLReward;
	}

	public double getAvgPktReward() {
		return avgPktReward;
	}

	public long getSourceId() {
		return sourceId;
	}

	public void updateStatsOnNewDataPkt(double pktReward, double timestamp){
		avgPktReward= (avgPktReward*noOfPackets+pktReward)/(noOfPackets+1);
		noOfPackets++;
		this.timestamp=timestamp;
	}
	
	public void updateStatsOnNewWLReward(double WLReward){
		avgWLReward= (avgWLReward*noOfWLRewards+WLReward)/(noOfWLRewards+1);
		noOfWLRewards++;		
	}
	
	public void resetStatsOnReinforcement(double reinforcedTime){
		lastReinforcedTime= reinforcedTime;
		noOfPackets=0;
		noOfWLRewards=0;
		avgWLReward=0;
		avgPktReward=0;
	}

	public boolean shouldReinforce(double currentTime, double margin) {
		if(noOfWLRewards==0||noOfPackets==0) return false;  //not ready for evaluation yet
		
		if(((currentTime-lastReinforcedTime)>DRLDiffApp.REINFORCE_WINDOW) &&
				Math.abs(avgWLReward-avgPktReward)>margin){
			return true;
		}else return false;
	}
}
