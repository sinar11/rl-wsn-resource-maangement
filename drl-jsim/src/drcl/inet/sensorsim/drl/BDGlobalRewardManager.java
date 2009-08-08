package drcl.inet.sensorsim.drl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import drcl.inet.sensorsim.drl.DRLSensorApp.TrackingEvent;

public class BDGlobalRewardManager implements GlobalRewardManager{
	static final double REWARD_PER_TRACK=0.05;
	static Hashtable<Long,List<WLReward>> pendingRwdsForNodes= new Hashtable<Long,List<WLReward>>();
	static Hashtable<Long,List<TrackingEvent>> pendingData= new Hashtable<Long,List<TrackingEvent>>();
	static int rewardUpdates=0;
	private static int positiveUpdates=0;
	static List<Double> globalRewards=new ArrayList<Double>(1000);
	static double totalReward=0;
	static double effectiveCost=0;
	
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

	public static double getTotalReward() {
		return totalReward;
	}

	public double getEffectiveCost() {
		return effectiveCost;
	}

	public  synchronized void dataArrived(long timestep, TrackingEvent trEvent){
		List<TrackingEvent> events=pendingData.get(timestep);
		if(events==null) events= new ArrayList<TrackingEvent>();
		events.add(trEvent);
		pendingData.put(timestep, events);
	}
	
	public synchronized void manage(long timestep){
		List<TrackingEvent> events=pendingData.get(timestep);
		if(events==null || events.size()==0){
			globalRewards.add(totalReward);
			return;
		}
		
		//multiple events received for same target (only 1 target)
		//only encouraging one with least cost
		double totalCost=0;
		ArrayList<Stream> allStreams= new ArrayList<Stream>();
		for(TrackingEvent event : events){
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
			stream.addReward(REWARD_PER_TRACK+(event.snr%100)*0.0005-event.cost);
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
		double glReward=bestStrReward-totalCost+bestStream.cost;
		totalReward+=glReward;
		globalRewards.add(totalReward);
		effectiveCost+=bestStream.cost;
		//calc WL based on best stream 
		for (Stream stream : allStreams) {
			if (DRLSensorApp.algorithm.equals("COIN")) {
				if (stream.streamId == bestStream.streamId) {
					addToPendingRwds(stream, glReward);
				} else {
					addToPendingRwds(stream, -glReward);
				}
			} else if (DRLSensorApp.algorithm.equals("TEAM")) {
				addToPendingRwds(stream, glReward*0.1);
			}
		}
		pendingData.remove(timestep);
	}
	
	//private static double GlobalReward()
	
	private static void addToPendingRwds(Stream stream, double d) {
		if(Math.abs(d)<0.001) return;   //not significant change
		else rewardUpdates++;
		if(d>0) positiveUpdates++;
		else{
			System.out.println("-ve reward");
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
			+",effectiveCost="+effectiveCost+",globalReward="+totalReward;
	}

}
