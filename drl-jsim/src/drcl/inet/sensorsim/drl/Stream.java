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
		
	}
	public void addReward(double reward) {
		this.reward+=reward;
	}
	public void addPktsReward(double reward2) {
		this.pktsReward+=reward2;		
	}
	
}
