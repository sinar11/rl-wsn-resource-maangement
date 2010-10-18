package drcl.inet.sensorsim.drl.diffext;

import java.util.List;

public class DataStreamEntry {
	/** The neighbor from which the data was received. If a reinforcement is received, it is forwarded to this source */
	private long sourceId ; 
	
	/** Stream id that this data stream represents */
	private long streamId;
	
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
	
	/** Current payment value for this stream **/
	private Double currPayable;
	
	/**For tracking only...nodes involved in this stream **/
	private List<Long> nodes;
	
	public DataStreamEntry(long sourceId, long streamId){
		this.sourceId=sourceId;
		this.streamId=streamId;		
	}
	
	public String toString(){
		return "streamId="+streamId+",sourceId="+sourceId+",noOfPackets="+noOfPackets+",noOfWLRewards="+noOfWLRewards+",avgPktReward="+avgPktReward+",avgWLReward="+avgWLReward;
	}
	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
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

	public double getTotalPktReward(){
		return avgPktReward*noOfPackets;
	}
	public long getSourceId() {
		return sourceId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (sourceId ^ (sourceId >>> 32));
		result = prime * result + (int) (streamId ^ (streamId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataStreamEntry other = (DataStreamEntry) obj;
		if (sourceId != other.sourceId)
			return false;
		if (streamId != other.streamId)
			return false;
		return true;
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
		noOfPackets=0;
		noOfWLRewards=0;
		avgWLReward=0;
		avgPktReward=0;
	}

	public boolean shouldReinforce(double payable, double margin) {
		//if(noOfPackets==0) return false;  //not ready for evaluation yet
		if(Math.abs(payable-avgPktReward)>payable*margin){
			return true;
		}else return false;
	}

	public Double getCurrPayable() {
		return currPayable;
	}

	public void setCurrPayable(Double currPayable) {
		this.currPayable = currPayable;
	}

	public List<Long> getNodes() {
		return nodes;
	}

	public void setNodes(List<Long> nodes) {
		this.nodes = nodes;
	}

	public long getStreamId() {
		return streamId;
	}
}
