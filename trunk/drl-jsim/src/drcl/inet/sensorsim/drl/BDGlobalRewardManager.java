package drcl.inet.sensorsim.drl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import drcl.inet.sensorsim.CurrentTargetPositionTracker;
import drcl.inet.sensorsim.drl.DRLSensorApp.TrackingEvent;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm.Algorithm;

public class BDGlobalRewardManager implements GlobalRewardManager{
	static final double REWARD_PER_TRACK=0.005;
	private static final double SNR_WEIGHT = 0.0005;
	private static final double COST_WEIGHT = 5.0;
	private static final double MAX_SNR = 400;
	private static final double MIN_REWARD = 0.25;
	
	Hashtable<Long,List<WLReward>> pendingRwdsForNodes= new Hashtable<Long,List<WLReward>>();
	List<TrackingEvent> pendingData= new ArrayList<TrackingEvent>();
	int rewardUpdates=0;
	private static int positiveUpdates=0;
	List<Double> globalRewards=new ArrayList<Double>(10000);
	List<Double> trackErrors=new ArrayList<Double>(10000);
	double totalReward=0;
	double totalGlobalReward=0; //use to calc average
	double effectiveCost=0;
	double totalCost=0;
	private long noOfTracks=0;
	private TrackingEvent lastTrackEvent;
	
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
		try{
		if(pendingData==null || pendingData.size()==0){
			updateGlobalReward();
			return;
		}
		
		//multiple events received for same target (only 1 target)
		//only encouraging one with least cost
		double totalCost=0;
		ArrayList<Stream> allStreams= new ArrayList<Stream>();
		double maxSNR=Double.MIN_VALUE;
		TrackingEvent maxSNREvent=lastTrackEvent;
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
			if(event.snr>maxSNR){
				maxSNR=event.snr;
				maxSNREvent=event;
			}
			if(event.snr>MAX_SNR) event.snr= MAX_SNR; 
			stream.addReward((REWARD_PER_TRACK+event.snr*SNR_WEIGHT-COST_WEIGHT*event.cost)/(REWARD_PER_TRACK+MAX_SNR*SNR_WEIGHT));
			stream.addPktsReward(event.reward);
		}
		lastTrackEvent=maxSNREvent;
		
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
		noOfTracks++; //bestStream.pktIds.size();
		totalReward+=glReward;
		updateGlobalReward();
		effectiveCost+=bestStream.cost;
		//calc WL based on best stream 
		for (Stream stream : allStreams) {
			if (algorithm.equals(Algorithm.COIN)) {
				double stReward= (glReward-stream.pktsReward)/stream.nodes.size();
				if(stReward<0) stReward=0;
				if (stream.streamId == bestStream.streamId) {
					addToPendingRwds(stream, stReward, algorithm);
				} else {
					addToPendingRwds(stream, -stream.cost/totalCost, algorithm);
				}
			} else if (algorithm.equals(Algorithm.TEAM)) {
				addToPendingRwds(stream, (glReward-stream.pktsReward)/stream.nodes.size(), algorithm);
			}
			stream.pktIds.clear();
			stream.pktsReward=0;
			stream.cost=0;
			stream.reward=0;
		}
		pendingData.clear();
		}finally{
			double currX=0, currY=0, trackX=0, trackY=0, snr=0;
			long targetNid= lastTrackEvent.targetNid;
			double[] curr = CurrentTargetPositionTracker.getInstance().getTargetPosition(targetNid);
			currX = round_digit(curr[0], 4);
			currY = round_digit(curr[1], 4);
			
			if(lastTrackEvent!=null){
				trackX=lastTrackEvent.targetLocation[0];
				trackY=lastTrackEvent.targetLocation[1];
				snr=lastTrackEvent.snr;
			}
			double dist = Math.sqrt(Math.pow(Math.abs(trackX- currX), 2)
	                  + Math.pow(Math.abs(trackY - currY), 2));
			CSVLogger.log("target"+targetNid, timestep + "," + snr
						+ "," + trackX + "," + trackY
						+ "," + currX + "," + currY + "," + dist,false,algorithm);			
		}
	}
	
	private void updateGlobalReward() {
	    int size= globalRewards.size()>0?globalRewards.size():1;
	    totalGlobalReward+=(totalReward-totalCost);
		globalRewards.add(totalGlobalReward/size);		
	}
	
	protected double round_digit(double x, int digit)
    {
        int Y = 1;
        for (int i =0; i < digit; i++) Y = Y * 10;
        return ((double)Math.round((x ) * Y))/Y;
    }
	//private static double GlobalReward()
	
	private void addToPendingRwds(Stream stream, double d, Algorithm algorithm) {
		if(Math.abs(d)<MIN_REWARD) return;   //not significant change
		else rewardUpdates++;
		DRLSensorApp.log.info("Reward:"+d+" to stream:"+stream.nodes);
		if(d>0) positiveUpdates++;
		else{
			System.out.println("-ve reward:"+d+" to stream:"+stream.nodes);
		}
		CSVLogger.log("Delta-Macro",""+d,false,algorithm);
		
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
		/*return "RewardUpdates="+rewardUpdates+",positiveUpdates="+positiveUpdates
			+",effectiveCost="+effectiveCost+",totalCost="+totalCost+",globalReward="+totalReward/totalCost+", noOfTracks="+noOfTracks;*/
		return "rewardUpdates,"+rewardUpdates+",positiveUpdates"+positiveUpdates+",effectiveCost,"+effectiveCost+",totalCost,"+totalCost+",reward,"+totalGlobalReward/globalRewards.size()+",noOfTracks,"+noOfTracks;
	}

}
