package drcl.inet.sensorsim.drl.diffext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.diffusion.ActiveTasksEntry;
import drcl.inet.sensorsim.diffusion.AttributeVector;
import drcl.inet.sensorsim.diffusion.PositiveReinforcementPacket;
import drcl.inet.sensorsim.drl.diffext.InterestPacket.CostParam;

public class MacroLearner {

	private static final double COST_UNIT_ENERGY = 1.0;
	private static final double COST_REMAINING_ENERGY=1;
	private static final double COST_PER_HOP = 0.001;
	private static final double PROFIT_MARGIN=0.05;
	private static final double MAX_DATA_QUALITY=1.0;
	private static final double MIN_DATA_QUALITY=0.0;
	private static final double EXPLORATION_FACTOR = 0.0;
	
	DRLDiffApp diffApp;
	
	public MacroLearner(DRLDiffApp diffApp) {
		this.diffApp=diffApp;
	}

	public String toString(){
		return "MacroLeaner-"+diffApp.nid;
	}
	
	
	/**
	 * Data from micro-learner which needs to be sent out on wireless channel.
	 * If gradients are set-up, it uses neighbor with highest gradient with some exploration 
	 * 
	 * @param outboundMsgs
	 */
	public void dataArriveAtUpPort(DataPacket pkt) {
		if(pkt.getReward()<=0) return;  //ignoring packet as doesn't have any reward for 
		InterestCacheEntry interestEntry=diffApp.interestCacheLookup(pkt.getTaskId());
		DataPacket dataPacket=new DataPacket(pkt);
		
		try{
		if(interestEntry==null || interestEntry.isGradientListEmpty()){  //no entry for task or gradient list empty
			dataPacket.setDestinationId(DRLDiffApp.BROADCAST_DEST);                      //broadcast this data packet to all neighbors
			dataPacket.setSourceId(diffApp.nid);			
			diffApp.sendPacket(dataPacket,diffApp.getDelay());
			return;
		}else{   			
			long destination=-1;
			if (Math.random() < EXPLORATION_FACTOR) { 
				destination = interestEntry.getRandomGradient().getNeighbor(); // exploration choosen
			}else{	
				GradientEntry gradient= interestEntry.getMaxGradient();
				if(gradient.getPayment()<=0){
					diffApp.log(Level.INFO,"Dropping packet as gradient's payment is zero or negative");
					return;
				}
				destination=gradient.getNeighbor();  // using neighbor with max gradient				
			}
			dataPacket.setDestinationId(destination);
			dataPacket.setSourceId(diffApp.nid);			
			diffApp.sendPacket(dataPacket,diffApp.getDelay());			
		}		
		}finally{
			insertDataPktInCache(dataPacket);
		}
	}

	private void insertDataPktInCache(DataPacket pkt){
		if(diffApp.dataPacketExists(pkt)) return;
		if(pkt.getSourceId()==diffApp.nid) return; //locally created data, may still want to store it in data-cache if need to correlated incoming reinforcement
		DataCacheEntry entry=diffApp.dataCache.get(pkt.getTaskId());
		if(entry==null){
			entry=new DataCacheEntry(pkt.getTaskId(), pkt);
			diffApp.dataCacheInsert(entry);
		}
		entry.addDataPacket(pkt);
	}
	/**
	 * Data arriving from underlying MAC layer
	 * @param dataPkt
	 */
	public boolean dataArriveAtDownPort(DataPacket dataPkt) {
		if(diffApp.dataPacketExists(dataPkt)) return false;
		//InterestCacheEntry interestEntry=diffApp.interestCacheLookup(dataPkt.getTaskId());
		//if(interestEntry==null) return true; //no entry for task
		insertDataPktInCache(dataPkt);
		return true;
	}
	
	public void computeReinforcements(){
		for(DataCacheEntry dataCacheEntry: diffApp.dataCache.values()){
			InterestPacket interest;
			double payable;
			if (diffApp.nid != diffApp.sink_nid) {
				InterestCacheEntry interestEntry = diffApp
						.interestCacheLookup(dataCacheEntry.getTaskId());
				if (interestEntry == null)
					continue; // no entry for task i.e. no gradients present,
								// cannot provide reinforcement
				payable = interestEntry.getPayable();
				interest = interestEntry.getInterest();
			}else{  //this node is the sink
				TaskEntry taskEntry=diffApp.activeTasksList.get(dataCacheEntry.getTaskId());
				interest= taskEntry.getInterest();
				payable= taskEntry.getPayment();
			}
			List<DataPacket> totalPkts=dataCacheEntry.getRecentData();
			if(totalPkts.size()==0) continue;
			double totalReward=calcTotalReward(totalPkts, interest, payable);
			Collection<DataStreamEntry> dataStreams=dataCacheEntry.getDataStreams();
			for(DataStreamEntry stream: dataStreams){
				List<DataPacket> pkts=dataCacheEntry.getRecentDataAcceptSource(stream.getSourceId());
				double clReward=calcTotalReward(pkts, interest, payable);
				if(totalReward<stream.getAvgPktReward()){
					diffApp.log(Level.INFO,"Total Reward less than Pkt reward..");
				}
				double wlReward=totalReward-clReward;
				stream.updateStatsOnNewWLReward(wlReward);
			}
		}
		/*if(diffApp.nid==diffApp.sink_nid)
			diffApp.sendReinforcements();*/
	}
	
	private double calcTotalReward(List<DataPacket> pkts,
			InterestPacket interest, double payable){
		if(pkts.size()==0) return 0;
		double quality=calcDataQuality(pkts, interest);
		double minCost=Integer.MAX_VALUE;
		for(DataPacket pkt: pkts){
			if(pkt.getCost()<minCost) minCost=pkt.getCost();
		}
		return quality*payable-minCost;		
	}
	/**
	 * Matches data attributes to QoS contraints and returns a value between 0 and 1 representing data quality
	 * @param pkts
	 * @param interestEntry
	 * @return
	 */
	private double calcDataQuality(List<DataPacket> pkts,
			InterestPacket interest) {
		List<Tuple> qosConstraints=interest.getQosConstraints();
		if(qosConstraints==null || qosConstraints.size()==0) return MAX_DATA_QUALITY;
		int passCount=0, totalCount=0;
		for(DataPacket pkt:pkts){
			//TODO how to get QoS attributes, should it be just part of normal data attributes?
			if(TupleUtils.isMatching(qosConstraints, pkt.getAttributes())){
				passCount++;
			}
			totalCount++;
		}
		return (passCount*MAX_DATA_QUALITY)/totalCount;
	}

	/**
	 * Publish interest out to neighbors over wireless channel
	 * @param interestPkt
	 */
	public void interestArriveAtUpPort(InterestPacket interestPkt) {
		InterestCacheEntry interestEntry = diffApp.interestCacheLookup(interestPkt.getTaskId()) ;
		if(interestEntry==null) throw new RuntimeException("InterestCacheEntry not found for task:"+interestPkt.getTaskId()+" cannot publish interest");
		double currentTime=diffApp.getTime();
		//if(interestEntry.isToResendInterest(currentTime)){
			interestPkt.setSourceId(diffApp.nid);
			interestPkt.setDestinationId(DRLDiffApp.BROADCAST_DEST);
			diffApp.sendPacket(interestPkt, diffApp.getDelay());
			interestEntry.setLastTimeSent(currentTime) ;
		//}
	}
	
	/**
	 * 
	 * @param interestPkt
	 */
	public boolean interestArriveAtDownPort(InterestPacket interestPkt) {	
		long sourceId = interestPkt.getSourceId();
		boolean shouldProcess=false;
		/* if I have already originated a matching interest then do nothing as this interest should have already propagated from this point onwards */
		if ( diffApp.activeTasksListLookup(interestPkt) != null ){
			return false;
		}
		/* Check to see if the interest already exists in the interest cache */
		InterestCacheEntry interestEntry = diffApp.interestCacheLookup(interestPkt.getTaskId()) ;
		List<GradientEntry> gradientList;
		if (interestEntry == null ) /* if it does not exist */
		{
			/* create an interest entry whose parameters are instantiated from the received interest. */
			gradientList = new ArrayList<GradientEntry>() ;
			/* Insert an entry in the interest cache */
			interestEntry=new InterestCacheEntry(interestPkt, gradientList);
			diffApp.interestCacheInsert(interestEntry) ;
			shouldProcess=true;
		}else{
			gradientList=interestEntry.gradientList;			
		}
		GradientEntry gradientEntry=interestEntry.gradientListLookup(sourceId,diffApp.getTime());	
		if(gradientEntry==null){
			gradientEntry=new GradientEntry(sourceId,interestPkt.getPayment(),interestPkt.getDuration(),diffApp.getTime());
			interestEntry.gradientListInsert(gradientEntry);
		}else{
			if(gradientEntry.getPayment()!=interestPkt.getPayment()){
				gradientEntry.setPayment(interestPkt.getPayment());
				shouldProcess=true;
			}
			gradientEntry.setTimestamp(diffApp.getTime());
			gradientEntry.setDuration(interestPkt.getDuration());
		}
		//check to see if this gradient entry is maximum among all set gradients, if so we further process this packet otherwise not
		if(!shouldProcess && !interestEntry.shouldForwardInterest(sourceId, interestPkt.getPayment())){
			return false;
		}
		double costOfParticipation= calcCostOfParticipation(interestPkt);
		double profit= (interestPkt.getPayment()-costOfParticipation)*PROFIT_MARGIN;
		interestEntry.getInterest().setPayment(interestPkt.getPayment()-(costOfParticipation+profit));
		interestEntry.setPayable(interestPkt.getPayment());
		return true;
	}

	public void handleReinforcement(ReinforcementPacket reinforcementPkt) {
		InterestCacheEntry interestEntry = diffApp.interestCacheLookup(reinforcementPkt.getTaskId()) ;
		if (interestEntry == null ) {
			return;  /* if it does not exist, cannot do reinforcement on that interest*/
		}
		GradientEntry gradientEntry=interestEntry.gradientListLookup(reinforcementPkt.getSourceId(),diffApp.getTime());
		double duration=interestEntry.getInterest().getDuration();
		if(gradientEntry==null){
			gradientEntry=new GradientEntry(reinforcementPkt.getSourceId(),reinforcementPkt.getPayment(),duration,diffApp.getTime());
			interestEntry.gradientListInsert(gradientEntry);
		}else{
			gradientEntry.setPayment(reinforcementPkt.getPayment());
			gradientEntry.setTimestamp(diffApp.getTime());
			gradientEntry.setDuration(duration);
		}
		double costOfParticipation= calcCostOfParticipation(interestEntry.getInterest());
		double profit= (reinforcementPkt.getPayment()-costOfParticipation)*PROFIT_MARGIN;
		interestEntry.getInterest().setPayment(reinforcementPkt.getPayment()-(costOfParticipation+profit));
		interestEntry.setPayable(interestEntry.getInterest().getPayment());	
		reinforceNeighborsIfRequired(interestEntry);
	}
	
	private void reinforceNeighborsIfRequired(InterestCacheEntry interestEntry) {
		// TODO Auto-generated method stub
		
	}

	public double calcCostOfParticipation(InterestPacket interest){
		List<CostParam> costParams= interest.getCostParameters();
		double myCost=0;
		for(CostParam param: costParams){
			switch(param.type){
			case Energy:
				myCost+= param.weight*MicroLearner.ENERGY_SAMPLE*COST_UNIT_ENERGY;
				break;
			case NoOfHops:
				myCost+= param.weight*COST_PER_HOP;
				break;
			case Lifetime:
				myCost+= param.weight*COST_REMAINING_ENERGY*((diffApp.getMaxEnergy()-diffApp.getRemainingEnergy())/diffApp.getRemainingEnergy());
				break;
			default:
				throw new RuntimeException("Type:"+param.type+" not supported");
			}
		}
		return myCost;
	}
}
