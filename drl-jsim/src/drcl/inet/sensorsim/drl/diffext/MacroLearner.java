package drcl.inet.sensorsim.drl.diffext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import drcl.inet.sensorsim.drl.diffext.InterestPacket.CostParam;

public class MacroLearner {

	private static final double MAX_LIFETIME=20000;
	private static final double COST_COEF_ENERGY = 1/(MicroLearner.ENERGY_SAMPLE+MicroLearner.ENERGY_DIFFUSE);
	private static final double COST_COEF_LIFETIME=1/MAX_LIFETIME;
	private static final double COST_PER_HOP = 1.0;
	//private static final double PROFIT_MARGIN=0.05;
	//private static final double MIN_DATA_QUALITY=0.0;
	private static final double EXPLORATION_FACTOR = 0.05;
	
	
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
		
		insertDataPktInCache(dataPacket);
		if(interestEntry==null || interestEntry.isGradientListEmpty()){  //no entry for task or gradient list empty
			dataPacket.setDestinationId(DRLDiffApp.BROADCAST_DEST);                      //broadcast this data packet to all neighbors
			dataPacket.setSourceId(diffApp.nid);			
			diffApp.sendPacket(dataPacket,diffApp.getDelay());
			return;
		}else{   			
			long destination=-1;
			if (Math.random() < EXPLORATION_FACTOR || pkt.isExplore()) { 
				//destination = interestEntry.getRandomGradient().getNeighbor(); // exploration choosen
				destination = DRLDiffApp.BROADCAST_DEST;
				//dataPacket.setExplore(true);
			}else{	
				GradientEntry gradient= interestEntry.getMaxGradient();
				/*if(gradient.getPayment()<=0){
					diffApp.log(Level.FINE,"Dropping packet as gradient's payment is zero or negative");
					//diffApp.interestCachePrint();
					return;
				}*/
				destination=gradient.getNeighbor();  // using neighbor with max gradient
				//if(dataPacket.destination)
			}
			dataPacket.setDestinationId(destination);
			dataPacket.setSourceId(diffApp.nid);			
			diffApp.sendPacket(dataPacket,0.0);			
		}		
	}

	private void insertDataPktInCache(DataPacket pkt){
		if(diffApp.dataPacketExists(pkt)) return;
		//if(pkt.getSourceId()==diffApp.nid) return; //locally created data, may still want to store it in data-cache if need to correlated incoming reinforcement
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
		if(diffApp.dataPacketExists(dataPkt)){
			diffApp.log(Level.FINE, "Not processing recently seen data packet:"+dataPkt);
			return false; //prevents loop by not processing data packets already seen earlier
		}
		//InterestCacheEntry interestEntry=diffApp.interestCacheLookup(dataPkt.getTaskId());
		//if(interestEntry==null) return true; //no entry for task
		insertDataPktInCache(dataPkt);
		return true;
	}
	
	public void computeReinforcements(){
		for(DataCacheEntry dataCacheEntry: diffApp.dataCache.values()){
			InterestPacket interest;
			double payment;
			if (diffApp.nid != diffApp.sink_nid) {
				InterestCacheEntry interestEntry = diffApp
						.interestCacheLookup(dataCacheEntry.getTaskId());
				if (interestEntry == null)
					continue; // no entry for task i.e. no gradients present,
								// cannot provide reinforcement
				interest = interestEntry.getInterest();
				payment= interest.getPayment();
			}else{  //this node is the sink
				TaskEntry taskEntry=diffApp.activeTasksList.get(dataCacheEntry.getTaskId());
				interest= taskEntry.getInterest();
				payment= taskEntry.getPayment();
			}
			List<DataPacket> totalPkts=dataCacheEntry.getRecentData();
			if(totalPkts.size()==0){ //If no recently seen data
				continue;
			}
			double payable= calcPayable(interest, payment);
			
			Map<String,List<DataPacket>> dataGroupPkts= classifyDataIntoGroups(totalPkts);
			for (List<DataPacket> pkts : dataGroupPkts.values()) {
				double totalReward = (diffApp.nid == diffApp.sink_nid) ? payable
						: diffApp.calcTotalReward(pkts, interest, payable);
				Collection<DataStreamEntry> dataStreams = dataCacheEntry
						.getDataStreams();
				for (DataStreamEntry stream : dataStreams) {
					if(stream.getNoOfPackets() == 0)
						continue;
					List<DataPacket> clampedPkts = getDataAcceptSource(pkts,stream.getSourceId());
					double clReward = diffApp.calcTotalReward(clampedPkts, interest,
							payable);
					double wlReward = totalReward - clReward;
					stream.updateStatsOnNewWLReward(wlReward);
				}

			}
		}
		
	}
	
	private List<DataPacket> getDataAcceptSource(List<DataPacket> recentData, long sourceId) {
		List<DataPacket> list= new ArrayList<DataPacket>();
		for(DataPacket pkt:recentData){
			if(pkt.getSourceId()!=sourceId)
				list.add(pkt);			
		}
		return list;
	}
	
	private Map<String, List<DataPacket>> classifyDataIntoGroups(
			List<DataPacket> pkts) {
		Map<String,List<DataPacket>> groupData= new HashMap<String, List<DataPacket>>();
		for(DataPacket pkt: pkts){
			List<DataPacket> data= groupData.get(pkt.getGroupId()); 
			if(data==null){
				data= new ArrayList<DataPacket>();
				groupData.put(pkt.getGroupId(), data);
			}
			data.add(pkt);
		}
		return groupData;
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
			interestEntry.setLastRefresh(interestPkt.getTimestamp());			
		}
		GradientEntry gradientEntry=interestEntry.gradientListLookup(sourceId,diffApp.getTime());
		if(gradientEntry==null){
			gradientEntry=new GradientEntry(sourceId,interestPkt.getPayment(),interestPkt.getDuration(),diffApp.getTime());
			interestEntry.gradientListInsert(gradientEntry);
		}else{
			if(gradientEntry.getInterestTimestamp()==interestPkt.getTimestamp()){ //this interest already being processed
				return false;
			}					
			gradientEntry.setPayment(interestPkt.getPayment(),interestPkt.getTimestamp());
			shouldProcess=true;
			gradientEntry.setInterestTimestamp(interestPkt.getTimestamp());
			gradientEntry.setDuration(interestPkt.getDuration());
		}
		//check to see if this gradient entry is maximum among all set gradients, if so we further process this packet otherwise not
		if(!shouldProcess && !interestEntry.shouldForwardInterest(sourceId, interestPkt.getPayment())){
			return false;
		}
		DataCacheEntry entry=diffApp.dataCache.get(interestPkt.getTaskId());
		if(entry!=null)
			entry.reset(diffApp.getTime());
		interestEntry.getInterest().setPayment(interestEntry.getMaxGradient().getPayment());
		interestEntry.setPayable(calcPayable(interestEntry.getInterest(),interestEntry.getMaxGradient().getPayment()));
		diffApp.log(Level.FINE, "After updating gradient from interest:"+interestPkt);
	//	diffApp.interestCachePrint();
		interestPkt.setPayment(interestEntry.getPayable());
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
			gradientEntry.setPayment(reinforcementPkt.getPayment(),diffApp.getTime());
		}
		interestEntry.getInterest().setPayment(interestEntry.getMaxGradient().getPayment());
		interestEntry.setPayable(calcPayable(interestEntry.getInterest(), interestEntry.getMaxGradient().getPayment()));	
		//System.out.println("Node:"+diffApp.nid);
		//interestEntry.printInterestEntry();
//		reinforceNeighborsIfRequired(interestEntry);
	}
	
	/*private void reinforceNeighborsIfRequired(InterestCacheEntry interestEntry) {
		DataCacheEntry entry=diffApp.dataCache.get(interestEntry.getInterest().getTaskId());
		if(entry!=null)
			diffApp.sendReinforcements(entry,interestEntry.getPayable(),interestEntry.getInterest());		
	}*/

	public double calcPayable(InterestPacket interest, double payment){
		if(diffApp.nid==diffApp.sink_nid) return payment;
		double costOfParticipation= calcCostOfParticipation(interest);
		diffApp.log(Level.INFO,"Cost of participation:"+costOfParticipation);
		//double profit= (payment-costOfParticipation)*PROFIT_MARGIN;
		return payment-costOfParticipation; //+profit);		
	}
	
	public double calcCostOfParticipation(InterestPacket interest){
		List<CostParam> costParams= interest.getCostParameters();
		double myCost=0;
		for(CostParam param: costParams){
			switch(param.type){
			case Energy:
				myCost+= param.weight*MicroLearner.ENERGY_DIFFUSE/COST_COEF_ENERGY;
				break;
			case NoOfHops:
				myCost+= param.weight*COST_PER_HOP;
				break;
			case Lifetime:
				double diff=MAX_LIFETIME-calcCurrLifetime(diffApp.getRemainingEnergy());
				myCost+= param.weight*COST_COEF_LIFETIME*Math.max(diff,0);
				break;
			default:
				throw new RuntimeException("Type:"+param.type+" not supported");
			}
		}
		return myCost;
	}

	private double calcCurrLifetime(double remainingEnergy) {
		double lambda=1.0; //arrival rate (1 per sec)
		double K_T= MicroLearner.TIMER_INTERVAL;
		double K_E= MicroLearner.ENERGY_DIFFUSE;
		double P_s= diffApp.getSamplingEnergy();
		double lifetime= remainingEnergy*(1+ lambda*K_T)/(P_s+lambda*K_E);
		diffApp.log(Level.FINE,"Current Lifetime="+lifetime);
		return lifetime;
	}
}
