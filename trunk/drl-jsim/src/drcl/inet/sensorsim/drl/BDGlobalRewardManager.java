package drcl.inet.sensorsim.drl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import drcl.inet.sensorsim.drl.DRLSensorApp.TrackingEvent;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm.Algorithm;

public class BDGlobalRewardManager implements GlobalRewardManager{
	static final double REWARD_PER_TRACK=0.01;
	private static final double SNR_WEIGHT = 0.01;
	private static final double MAX_SNR = 100;
	private static final double MIN_REWARD = 0.25;
	
	Hashtable<Long,List<WLReward>> pendingRwdsForNodes= new Hashtable<Long,List<WLReward>>();
	List<TrackingEvent> pendingData= new ArrayList<TrackingEvent>();
	int rewardUpdates=0;
	private static int positiveUpdates=0;
	List<Double> globalRewards=new ArrayList<Double>(1000);
	double totalReward=0;
	double effectiveCost=0;
	double totalCost=0;

	public Hashtable<Long, List<WLReward>> getPendingRwdsForNodes() {
		return pendingRwdsForNodes;
	}

	public int getRewardUpdates() {
		return rewardUpdates;
	}

	public int getPositiveUpdates() {
		return positiveUpdates;
	}

	public List<Double> getGlobalRewards() {
		return globalRewards;
	}

	public double getTotalReward() {
		return totalReward;
	}

	public double getEffectiveCost() {
		return effectiveCost;
	}

	public double getTotalCost(){
		return totalCost;
	}
	
	public void addToTotalCost(double cost){
		totalCost+=cost;
	}
	
	public  synchronized void dataArrived(long timestep, TrackingEvent trEvent){
		pendingData.add(trEvent);
	}
	
	public synchronized void manage(long timestep, Algorithm algorithm){
		if(pendingData==null || pendingData.size()==0){
			globalRewards.add(totalReward);
			return;
		}
		
		//multiple events received for same target (only 1 target)
		//only encouraging one with least cost
		double totalCost=0;
		ArrayList<Stream> allStreams= new ArrayList<Stream>();
		for(TrackingEvent event : pendingData){
			totalCost+=event.cost;
			Stream stream=new Stream(event.streamId,event.nodes);
			int index=allStreams.indexOf(stream);
			if(index==-1){
				allStreams.add(stream);
			}else{
				stream=allStreams.get(index);
			}
			stream.addPkt(event.pktId);
			stream.addCost(event.cost);
			if(event.snr>MAX_SNR) event.snr= MAX_SNR; 
			stream.addReward((REWARD_PER_TRACK+event.snr*SNR_WEIGHT-event.cost)/(REWARD_PER_TRACK+MAX_SNR*SNR_WEIGHT));
			stream.addPktsReward(event.reward);
		}
		Stream bestStream=null;
		double bestStrReward=Double.MIN_VALUE;
		for(Stream stream : allStreams){
			if(stream.reward>bestStrReward){
				bestStream=stream;
				bestStrReward=stream.reward;
			}
		}
		//global reward is reward_per_track-totalCost into no of pkts and SNR value of the event
		//double glReward=bestStrReward-totalCost+bestStream.cost;
		double glReward=(bestStrReward*bestStream.cost)/totalCost;
		
		totalReward+=glReward;
		globalRewards.add(totalReward/this.totalCost);
		effectiveCost+=bestStream.cost;
		//calc WL based on best stream 
		for (Stream stream : allStreams) {
			if (algorithm.equals(Algorithm.COIN)) {
				if (stream.streamId == bestStream.streamId) {
					addToPendingRwds(stream, (glReward-stream.pktsReward)/stream.nodes.size());
				} else {
					addToPendingRwds(stream, -stream.cost/totalCost);
				}
			} else if (algorithm.equals(Algorithm.TEAM)) {
				addToPendingRwds(stream, (glReward-stream.pktsReward)/stream.nodes.size());
			}
			stream.pktIds.clear();
			stream.pktsReward=0;
			stream.cost=0;
			stream.reward=0;
		}
		pendingData.clear();
	}
	
	//private static double GlobalReward()
	
	private void addToPendingRwds(Stream stream, double d) {
		if(Math.abs(d)<MIN_REWARD) return;   //not significant change
		else rewardUpdates++;
		DRLSensorApp.log.info("Reward:"+d+" to stream:"+stream.nodes);
		if(d>0) positiveUpdates++;
		else{
			System.out.println("-ve reward:"+d+" to stream:"+stream.nodes);
		}
	
		for(long nid : stream.nodes){
			List<WLReward> rwds= pendingRwdsForNodes.get(nid);
			if(rwds==null) rwds= new ArrayList<WLReward>();
			for(long pkt : stream.pktIds)
				rwds.add(new WLReward(pkt,d));
			pendingRwdsForNodes.put(nid,rwds);
		}
		
	}

	public synchronized List<WLReward> getPendingRewards(long nid){
		List<WLReward> rwds= pendingRwdsForNodes.get(nid);
		pendingRwdsForNodes.remove(nid);
		return rwds;
	}
	
	public String stats(){
		return "RewardUpdates="+rewardUpdates+",positiveUpdates="+positiveUpdates
			+",effectiveCost="+effectiveCost+",totalCost="+totalCost+",globalReward="+totalReward/totalCost;
	}

}
