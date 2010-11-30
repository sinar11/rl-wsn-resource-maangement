package drcl.inet.sensorsim.drl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import drcl.inet.sensorsim.CurrentTargetPositionTracker;
import drcl.inet.sensorsim.drl.DRLSensorApp.TrackingEvent;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm.Algorithm;

public class GlobalRewardManager implements GlobalRewardManagerMBean{
	static final double PAYMENT=1.0;
	private static final double SNR_WEIGHT = 0.00;
	private static final double MAX_SNR = 100;
	private static final double MIN_REWARD = 0.25;
	
	Hashtable<Long,List<WLReward>> pendingRwdsForNodes= new Hashtable<Long,List<WLReward>>();
	List<TrackingEvent> pendingData= new ArrayList<TrackingEvent>();
	Map<Long,List<TrackingEvent>> targetEventMap= new HashMap<Long,List<TrackingEvent>>();
	Map<Long,TrackingEvent> lastTrackingEvent= new HashMap<Long,TrackingEvent>();
	Map<Integer,Integer> taskExecutions= new HashMap<Integer, Integer>();
	
	double totalTrackingError= 0;
	int totalTrackCount=0;
	
	int rewardUpdates=0;
	private static int positiveUpdates=0;
	List<Double> globalRewards=new ArrayList<Double>(1000);
	double avgReward=0;
	double effectiveCost=0;
	double dataCost=0;
	double totalCost=0;
	double costFromLastReinforcement=0;
	private long noOfTracks=0;
	
	public GlobalRewardManager(){		
	}
	
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
		return avgReward;
	}

	public double getEffectiveCost() {
		return effectiveCost;
	}

	public double getTotalCost(){
		return totalCost;
	}
	
	public void addToTotalCost(double cost, Algorithm algorithm){
		if(algorithm==Algorithm.ORACLE){
			cost=DRLSensorApp.ENERGY_SLEEP;
		}
		totalCost+=cost;
		costFromLastReinforcement+=cost;
	}
	
	public  synchronized void dataArrived(long timestep, TrackingEvent trEvent){
		pendingData.add(trEvent);
		List<TrackingEvent> events=targetEventMap.get(trEvent.targetId);
		if(events==null){
			events= new ArrayList<TrackingEvent>();
		}
		events.add(trEvent);
		targetEventMap.put(trEvent.targetId, events);
		lastTrackingEvent.put(trEvent.targetId, trEvent);
	}
	
	public synchronized void manage(long timestep, Algorithm algorithm) {
		/*if (pendingData == null || pendingData.size() == 0) {
			System.out.println("Zero global reward..");
			updateGlobalReward(0);
			costFromLastReinforcement=0;
			//updateTrackingStats(timestep,algorithm);
			return;
		}*/

		// multiple events received for same target
		// only encouraging one with least cost for each target
		double totalReward=0;
		
		for (Long targetId : targetEventMap.keySet()) {
			List<TrackingEvent> targetEvents = targetEventMap.get(targetId);
			double targetCost = 0;
			ArrayList<Stream> allStreams = new ArrayList<Stream>();
			noOfTracks +=targetEvents.size();
			for (TrackingEvent event : targetEvents) {
				targetCost += event.cost;
				Stream stream = new Stream(event.streamId, event.nodes);
				int index = allStreams.indexOf(stream);
				if (index == -1) {
					allStreams.add(stream);
				} else {
					stream = allStreams.get(index);
				}
				stream.addPkt(event.pktId);
				stream.addCost(event.cost);
				/*if (event.snr > MAX_SNR)
					event.snr = MAX_SNR;
				stream.addReward((REWARD_PER_TRACK + event.snr * SNR_WEIGHT - event.cost)
								/ (REWARD_PER_TRACK + MAX_SNR * SNR_WEIGHT));*/
				stream.addPktsReward(event.reward);
			}
			double globalReward= calcTotalReward(pendingData, PAYMENT);
			Stream bestStream=allStreams.get(0);
			double maxWLReward=0;
			for (Stream stream : allStreams) {
				if(stream.pktIds.size() == 0)
					continue;
				List<TrackingEvent> clampedPkts = getDataAcceptStream(
							pendingData, stream);
				double clReward = calcTotalReward(clampedPkts, PAYMENT);
				double wlReward = globalReward - clReward;
				if(wlReward>maxWLReward){
					maxWLReward=wlReward;
					bestStream=stream;
				}
				stream.updateStatsOnNewWLReward(wlReward);
			}
			
			for (Stream stream : allStreams) {
				if(algorithm==Algorithm.COIN){
				if(stream.avgWLReward>0 || maxWLReward<=0){
					addToPendingRwds(stream, PAYMENT, algorithm);
				}else{
					addToPendingRwds(stream, 0, algorithm);
				}
				}
				if(stream.avgWLReward>bestStream.avgWLReward)
					bestStream=stream;
				stream.resetStatsOnReinforcement(timestep);
			}
					
			if(algorithm==Algorithm.ORACLE){
				this.totalCost+=bestStream.cost;
				this.costFromLastReinforcement+=bestStream.cost;
			}
			totalReward+=globalReward;				
						
			
			//CSVLogger.log("Delta-Macro",timestep+","+toString(streamRewards),false,algorithm);
		}
		System.out.println("Reward:"+(totalReward-costFromLastReinforcement)+",totalReward="+totalReward+",costFromLastReinforcement="+costFromLastReinforcement+",noOfTracks="+pendingData.size());
		updateGlobalReward(totalReward-this.costFromLastReinforcement);
		//updateTrackingStats(timestep,algorithm);
		pendingData.clear();
		targetEventMap.clear();
		this.costFromLastReinforcement=0;
	}
	
	public double calcTotalReward(List<TrackingEvent> pkts,	 double payable){
		if(pkts==null || pkts.size()==0) return 0;
		//int distPkts= getNoOfDistinctPkts(pkts);
		double quality=1.0;// 0.75+ Math.min(0.25, 0.25*(distPkts/5));
		//if(quality>1) quality=1.0;//1.0;
		double minCost=Integer.MAX_VALUE;
		for(TrackingEvent pkt: pkts){
			if(pkt.cost<minCost){
				minCost=pkt.cost;
			}	
		}
		//double avgReward= pktsReward/pkts.size();
		//double avgCost=pktsCost/pkts.size();
		return quality*payable- minCost;//+NO_OF_PKTS_FACTOR*pkts.size();		
	}
	
	private int getNoOfDistinctPkts(List<TrackingEvent> pkts) {
		HashMap<Double,Object> timedPkts= new HashMap<Double, Object>();
		for(TrackingEvent pkt: pkts){
			timedPkts.put(pkt.timestamp, pkt);
		}
		return timedPkts.size();
	}

	private List<TrackingEvent> getDataAcceptStream(List<TrackingEvent> recentData, Stream stream) {
		List<TrackingEvent> list= new ArrayList<TrackingEvent>();
		for(TrackingEvent pkt:recentData){
			if(pkt.streamId!=stream.streamId)
				list.add(pkt);			
		}
		return list;
	}
	
    private void updateGlobalReward(double reward) {
    	//globalRewards.add(totalReward/this.totalCost);
    	int len=globalRewards.size();
    	if(len>0)
    		this.avgReward=(this.avgReward*len+reward)/(len+1);
    	else
    		this.avgReward=reward;
    	globalRewards.add(this.avgReward);		
	}

	private String toString(double[] streamRewards) {
		StringBuffer buff= new StringBuffer();
		for(int i=0;i<streamRewards.length;i++){
			buff.append(i+","+streamRewards[i]+",");
		}
		return buff.toString();
	}

	public void updateTrackingStats(long timestep, Algorithm algorithm){
    	Map<Long,double[]> targetPositions=CurrentTargetPositionTracker.getInstance().getTargetPositions();
    	for (Long targetNid : targetPositions.keySet()) {
			double currX = 0, currY = 0, trackX = 0, trackY = 0, snr = 0;
			double[] curr = targetPositions.get(targetNid);
			currX = round_digit(curr[0], 4);
			currY = round_digit(curr[1], 4);
			TrackingEvent lastTrackEvent = lastTrackingEvent.get(targetNid);
			if (lastTrackEvent != null) {
				trackX = lastTrackEvent.targetLocation[0];
				trackY = lastTrackEvent.targetLocation[1];
				snr = lastTrackEvent.snr;
			}
			double dist = Math.sqrt(Math.pow(Math.abs(trackX - currX), 2)
					+ Math.pow(Math.abs(trackY - currY), 2));
			totalTrackingError+=dist;
			totalTrackCount++;
			CSVLogger.log("target" + targetNid, timestep + "," + snr + ","
					+ trackX + "," + trackY + "," + currX + "," + currY + ","
					+ dist, false, algorithm);
		}
    }

	protected double round_digit(double x, int digit){
        int Y = 1;
        for (int i =0; i < digit; i++) Y = Y * 10;
        return ((double)Math.round((x ) * Y))/Y;
    }
	
	private void addToPendingRwds(Stream stream, double d, Algorithm algorithm) {
		/*if(Math.abs(d)<MIN_REWARD) return;   //not significant change
		else */rewardUpdates++;
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
	
	public String getStats(){
		StringBuffer buff= new StringBuffer();
		buff.append("RewardUpdates,"+rewardUpdates+",positiveUpdates,"+positiveUpdates);
		buff.append(",effectiveCost,"+effectiveCost+",totalCost,"+totalCost+",globalReward,"+this.avgReward);
		buff.append(",avgTrackError,"+(totalTrackingError/totalTrackCount)+",noOfTracks,"+noOfTracks+",dataCost,"+dataCost);
		for(Integer taskId: taskExecutions.keySet()){
			buff.append(",task-"+taskId+","+taskExecutions.get(taskId));
		}
		return buff.toString();
	}

	public void updateTaskExecutions(Hashtable<Integer, SensorTask> taskList) {
		for(Integer id: taskList.keySet()){
			int curr= taskExecutions.containsKey(id)?taskExecutions.get(id):0;
			taskExecutions.put(id, curr+taskList.get(id).noOfExecutions);
		}
	}
	
	public long getNoOfTracks(){
		return noOfTracks;
	}

}
