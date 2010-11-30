package drcl.inet.sensorsim.drl;

import java.util.ArrayList;
import java.util.List;

class Stream{
	List<Long> nodes;
	double cost=0;
	long streamId;
	List<Long> pktIds= new ArrayList<Long>();
	double reward=0;
	double pktsReward=0;
	double avgWLReward;
	/**Cumulative Avg Pkt reward from last reinforcement**/
	double avgPktReward;	
	double minCost=Integer.MAX_VALUE;
	
	/**no. of data packets from last reinforcement**/
	int noOfPackets;
	
	/** no of WL reward updates from last reinforcement **/
	int noOfWLRewards;
	
	Stream(long streamId, List<Long> nodes){
		this.streamId=streamId;
		this.nodes=nodes;
	}
	@Override
	public boolean equals(Object o){
		if(o instanceof Stream){
			Stream other = (Stream)o;
			if(this.streamId!=other.streamId) return false;
			if(this.nodes.size()!=other.nodes.size()) return false;
			for(int i=0; i<nodes.size(); i++){
				if(nodes.get(i)!=other.nodes.get(i)) return false;
			}
			return true;
		}
		return false;
	}
	public void addPkt(long pktId) {
		pktIds.add(pktId);
	}
	public void addCost(double cost) {
		this.cost+=cost;
		if(minCost>cost) minCost=cost;		
	}
	
	public void addReward(double reward) {
		this.reward+=reward;
	}
	public void addPktsReward(double pktReward) {
		avgPktReward= (avgPktReward*noOfPackets+pktReward)/(noOfPackets+1);
		noOfPackets++;				
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
	
}
