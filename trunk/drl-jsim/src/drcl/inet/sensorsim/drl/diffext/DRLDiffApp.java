
package drcl.inet.sensorsim.drl.diffext ;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import drcl.comp.Port;
import drcl.inet.mac.EnergyContract;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.SensorPositionReportContract;
import drcl.inet.sensorsim.SensorAppAgentContract.Message;
import drcl.inet.sensorsim.drl.CSVLogger;
import drcl.inet.sensorsim.drl.EnergyStats;
import drcl.inet.sensorsim.drl.IDRLSensorApp;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.inet.sensorsim.drl.demo.DRLDemoFactory;
import drcl.inet.sensorsim.drl.demo.IDRLDemo;
import drcl.inet.sensorsim.drl.diffext.InterestPacket.CostParam;


/** This class implements the extension of directed diffusion using distributed RL
*
* @author Kunal Shah
*/
public abstract class DRLDiffApp extends drcl.inet.sensorsim.SensorApp implements drcl.comp.ActiveComponent, IDRLSensorApp
{
 	private static final long serialVersionUID = -8724996734345865474L;

	public static Logger log= Logger.global;
	   
	static {
		Logger.global.setLevel(Level.FINE);
	}
	public static final String MOBILITY_PORT_ID    = ".mobility";

	/** The port used to query the current position of myself */
	public Port mobilityPort    = addPort(MOBILITY_PORT_ID, false); 
	
	/** A node may suppress a received interest if it recently (i.e., within RESEND_INTEREST_WINDOW secs.) resent a matching interest. */	
	public static final double RESEND_INTEREST_WINDOW = 2.0 ;

	static final double MAX_DATA_QUALITY=1.0;
	
	/** Period between two successive times to check if any entries in the interest cache need to be purged. */
	public static final double INTEREST_CACHE_PURGE_INTERVAL = 500.0 ; /* secs. */
	
	/** Decay computed gradients at a rate represented by DECAY_FACTOR **/
	public static final double DECAY_FACTOR =0.0;
	
	/** Name of the target that a sink node is interested in (and a sensor node capable of) detecting */
	public String TargetName ;

	public static final int INTEREST_PKT = 0 ;
	public static final int DATA_PKT = 1 ;
	public static final int REINFORCEMENT_PKT = 2 ;
	public static final int BROADCAST_DEST=-1;
	public static final double REINFORCE_WINDOW=20*MicroLearner.TIMER_INTERVAL; //20 TIMESTEPS
	public static final double REINFORCE_SUPRESS_MARGIN= 0.0;
	public static final boolean TRACE_ON=true;
	
	public static enum NodeState { SLEEPING, AWAKE};
	 
	public static final double DELAY = 10.0; 			/* fixed delay to keep arp happy */
	
	protected static java.util.Random rand = new java.util.Random(7777) ;
	protected static int seqNum=0;
	protected static Map<Integer,Integer> taskExecutions= new HashMap<Integer, Integer>();
	
	int numSubscriptions ;
	double initialEnergy; 

	/** A Vector of previously seen interests. Each cache entry is an interest (i.e., a Vector of Attributes) and possibly other fields */
	public Map<Integer,InterestCacheEntry> interestCache = null ;

	/** A Map of previously seen data packets and corresponding state with taskId as key. Each cache entry contains list of recent data received for that task 
	 * as well as SourceList containing stats and rewards of each source */
	public Map<Integer,DataCacheEntry> dataCache = null ;

	/** A Map of active tasks initiated by a sink node */
	public Map<Integer,TaskEntry> activeTasksList = null;

	/** Timer used to periodically check if there are any entries in the interest cache that need to be purged. */
	public DiffTimer interestCache_purgeTimer ;

	/** Timer used to periodically check if there are any pending reinforcements to be sent */
	public DiffTimer reinforcementTimer ;

	protected MicroLearner microLearner;
	
	protected MacroLearner macroLearner;
	
	NodeState nodeState=NodeState.AWAKE;
    
	protected int noOfReinforcements=0;
	protected int noOfDataPkts=0;
	protected int noOfHeartBeats;
	protected int noOfSrcPkts=0;
	protected int noOfInterests=0;
	protected Double lifetime=null;
	protected String costParam;  
	protected int noOfNodes;
	double lastTrackTime;
	double averageDelay;
	double totalEnergyUsed;
	int totalTrackingPkts=0;
	int totalHeartBeatPkts=0;
	double totalTrackingError= 0;
	int totalTrackCount=0;
	
	private InterestPacket pendingInterest;

	private double[] myLoc;


	
	protected DRLDiffApp ()
	{
		super();
		numSubscriptions = 0 ; 
		interestCache = new HashMap<Integer,InterestCacheEntry>() ;
		dataCache = new HashMap<Integer,DataCacheEntry>();
		activeTasksList=new HashMap<Integer,TaskEntry> ();
		interestCache_purgeTimer = null ;
		reinforcementTimer = null ;
		macroLearner= new MacroLearner(this);
		microLearner=new MicroLearner(this,macroLearner);		
	}
	
	 protected void _start() {
		 if(nid==sink_nid){
				EnergyStats.init(noOfNodes);
				this.initialEnergy=100;
				EnergyStats.update((int) nid, 0, initialEnergy,
						initialEnergy > 0, getTime());
		 }else{
			 this.initialEnergy=getRemainingEnergy();	
			 SensorPositionReportContract.Message positionMsg = new SensorPositionReportContract.Message();
			 positionMsg = (SensorPositionReportContract.Message) mobilityPort
					.sendReceive(positionMsg);
			 myLoc= new double[]{positionMsg.getX(),positionMsg.getY()};
		 }
		 
		 DRLDemoFactory.getDRLDemo().addNode((int)nid,myLoc,initialEnergy, getMyType());		 
		 microLearner._start();		 
	 }

	 protected String getMyType(){
		 if(nid==sink_nid) return IDRLDemo.TYPE_SINK;
		 else return IDRLDemo.TYPE_SENSOR;
	 }
	 
	 protected void _stop()  {
		 microLearner._stop();
	 }

	 protected void _resume() {
		 microLearner._resume();
	 }
	    
	public double getMaxEnergy(){
	   return initialEnergy;
	}

	public void setNoOfNodes(int noOfNodes) {
		this.noOfNodes = noOfNodes;
		CSVLogger.noOfNodes=noOfNodes;
	}
	public void setPosition(double speed, double x, double y, double z){
		myLoc= new double[]{x,y};
	}
	
	public String getName() { return "DRLDiffApp"; }

	public void setTargetName(String name)
	{	TargetName = new String(name) ;	}

	/** Looks up an event in the data cache */
	/*public DataCacheEntry dataCache_lookup(InterestPacket interest)
	{
		for(DataCacheEntry entry: dataCache){
			if(interest.equals(entry.getData()))
				return entry ;
		}

		return null ;
	}*/

	//checks if passed data packet already exists in data cache
	public boolean dataPacketExists(DataPacket dataPkt){
		DataCacheEntry entry= dataCache.get(dataPkt.getTaskId());
		if(entry==null) return false;
		else return entry.containtsDataPacket(dataPkt);		
	}
	
	/** Looks up an event (with a specified interval) in the data cache *//*
	public DataCacheEntry dataCacheLookup(List<Tuple> event, int taskId)
	{
		for(DataCacheEntry entry: dataCache){
			if (event.equals(entry.getData().getAttributes()) && entry.getData().getTaskId()==taskId){
			   return entry ;
			}
		}
		return null;
	}*/

	/** Inserts an event description in the data cache */
	public void dataCacheInsert(DataCacheEntry e)
	{
		if ( reinforcementTimer == null )
		{
			reinforcementTimer = new DiffTimer(DiffTimer.TIMEOUT_SEND_REINFORCEMENT, null) ;
			if ( reinforcementTimer != null )
			{
				reinforcementTimer.handle = setTimeout(reinforcementTimer, REINFORCE_WINDOW) ;
			}
		}
		dataCache.put(e.getTaskId(),e) ;
	}

	/** Prints all entries in the data cache */	
	public void dataCachePrint()
	{
		System.out.println("DiffApp " + nid + ": Printing the data cache: " + dataCache.size() + " entries.");
		for(DataCacheEntry entry: dataCache.values()){
			entry.printDataEntry() ;
		}
	}

	public void sendReinforcements(){
		macroLearner.computeReinforcements();
		for(DataCacheEntry entry: dataCache.values()){
			double payable;
			InterestPacket interest;
			if(nid==sink_nid){
				TaskEntry tentry=activeTasksList.get(entry.getTaskId());
				payable= tentry.getPayment();
				interest=tentry.getInterest();
				if(microLearner.timesteps%5==0)
					DRLDemoFactory.getDRLDemo().markActiveStream(entry.getRecentDataStreams());
			}else{
				InterestCacheEntry icentry=interestCacheLookup(entry.getTaskId());
				payable= icentry.getPayable();
				interest=icentry.getInterest();
			}
			sendReinforcements(entry,payable,interest);
		}
	}
	
	public double getPayable(int taskId){
		if(nid==sink_nid){
			TaskEntry tentry=activeTasksList.get(taskId);
			return tentry.getPayment();
		}else{
			InterestCacheEntry icentry=interestCacheLookup(taskId);
			return icentry.getPayable();
		}	
	}
	
	public void sendReinforcements(DataCacheEntry entry, double payable,
			InterestPacket interest) {
		Collection<ReinforcementPacket> pendingReinforcements = entry
				.getPendingReinforcements(nid, getTime(),
						REINFORCE_SUPRESS_MARGIN , interest, payable);
		for (ReinforcementPacket pkt : pendingReinforcements) {
			sendPacket(pkt, getDelay());
		}
	}

	/** Purges expired entries from the interest cache */
	public void interestCachePurge()
	{
		double currentTime = getTime() ;
		for(InterestCacheEntry entry:interestCache.values()){
			entry.setPayable(macroLearner.calcPayable(entry.getInterest(),entry.getMaxGradient().getPayment()));
			entry.gradientListPurge(currentTime) ;
			//microLearner.updateTaskExpectedPrice(entry.getInterest().getTaskId());
			/*if ( entry.IsGradientListEmpty() == true ){
				it.remove();
			}	*/		
		}
	}

	public boolean isExpectingInterestRefresh(){
		double currentTime = getTime() ;
		for(InterestCacheEntry entry:interestCache.values()){
			double lapseTime=currentTime-entry.getLastRefresh() ;
			if(Math.abs(lapseTime-entry.getInterest().getRefreshInterval())<5*MicroLearner.TIMER_INTERVAL)
				return true;
		}
		return false;
	}
	
	public InterestCacheEntry interestCacheLookup(int taskId) {
		return interestCache.get(taskId);		
	}
	
	/** Performs matching algorithm by checking if given data mathes to any existing InterestCacheEntry */
	public InterestCacheEntry getMatchingInterest(List<Tuple> data)
	{
		for(InterestCacheEntry entry: interestCache.values()){ 
			if (TupleUtils.isMatching(entry.getInterest().getAttributes(), data))
				return entry ;
		}
		return null ;
	}

	/** Inserts an interest cache entry in the interest cache */
	public void interestCacheInsert(InterestCacheEntry e)
	{
		if ( interestCache_purgeTimer == null ){
			interestCache_purgeTimer = new DiffTimer(DiffTimer.TIMEOUT_INTEREST_CACHE_PURGE, null) ;
			if ( interestCache_purgeTimer != null ){
				interestCache_purgeTimer.handle = setTimeout(interestCache_purgeTimer, INTEREST_CACHE_PURGE_INTERVAL) ;
			}
		}
		interestCache.put(e.getInterest().getTaskId(),e) ;
	}

	/** Prints all interest cache entries in the interest cache */
	public void interestCachePrint(){
		log(Level.INFO,"Printing the interest cache.");
		for(InterestCacheEntry entry: interestCache.values()){ 
			entry.printInterestEntry() ;
		}
	}

	/** Looks up an active task description in the active tasks list */
	public TaskEntry activeTasksListLookup(InterestPacket interest)
	{
		for (TaskEntry entry: activeTasksList.values()){
			if ( interest.isMatching(entry.getInterest())) 
				return entry ;
		}
		return null ;
	}

/*	*//** This function is passed the data or exploratory data event. For each neighbor that has a gradient entry, the function creates a timer that generates the data at the "datarate" requested by the specified neighbor. *//*
	public void createDataTimers(InterestCacheEntry intrstEntry, AttributeVector event)
	{
		int no = intrstEntry.gradientList.size() ;
		for (int i = 0 ; i < no ; i++)
		{
			GradientEntry entry = (GradientEntry)intrstEntry.gradientList.elementAt(i);
			 create a timer to periodically generate the data at the "datarate" 
			   requested by the specified neighbor. 
			if ( entry.dataTimer == null )
			{
				 One may also consider adding sendPacket(, rand) here to send the data to the sink promptly instead of waiting until the gradient timer expires 
				entry.dataTimer = new DiffTimer(DiffTimer.TIMEOUT_SEND_DATA, new DataPacket(nid, entry.getPreviousHop(), event, entry.getDataRate())) ;
				entry.dataTimer.handle = setTimeout(entry.dataTimer, (double)(entry.getDataRate())) ;
			}
			else
			{
			
			}
		}
	}*/

	/** Constructs a sensing event */
	public abstract List<Tuple> ConstructSensingEvent(SensorAppAgentContract.Message msg);
	
	public double getRemainingEnergy(){
    	if (nid != sink_nid) {
			double energy = ((EnergyContract.Message) wirelessPhyPort
					.sendReceive(new EnergyContract.Message(0, -1.0, -1)))
					.getEnergyLevel();
			if (energy <= 0 && lifetime==null){
				lifetime=getTime();
				log(Level.WARNING,"Out Of Energy, lifetime="+lifetime);
				microLearner._stop();
			}
			return energy;
		}else
    		return Integer.MAX_VALUE;
    	
    }
	
	public void printEnergy(){
		log(Level.INFO,getRemainingEnergy()+"");
	}
	
	/** Handles information received over the sensor channel  */
	public void recvSensorEvent(Object data_)
	{
		List<Tuple> event = ConstructSensingEvent((Message) data_) ;
		microLearner.handleSensorEvent(event);		
	}

	/** Determines whether a node can satisfy an interest. Specifically, a sensor node can satisfy an interest if (a) the node is within the specified rect in the interest and (b) the type of the detected events (e.g., wheeled vehicle) is the same type as that in the interest */
	public boolean canSatisfyInterest(InterestPacket interest)
	{
		/* construct the event as a list of attribute-value pairs. */
		List<Tuple> event = ConstructSensingEvent(null) ;

		if (TupleUtils.isMatching(interest.getAttributes(), event) ){
			return true ;		
		}else return false ;		
	}

	public String toString(){
		return "DRLDiffApp-"+nid;
	}
	
	/** Handles information received over the wireless channel  */
	protected void recvSensorPacket(Object data_)
	{	
		if ( data_ instanceof SensorPacket )
		{
			SensorPacket spkt = (SensorPacket)data_ ;
			if(nodeState.equals(NodeState.SLEEPING) && spkt.pktType!=DRLDiffApp.INTEREST_PKT){
				log(Level.FINE,"Dropping packet as currenly asleep.."+spkt.getBody());
				log(Level.FINE,"Current task:"+microLearner.currentTask+", current state:"+microLearner.currentState
						+" time:"+microLearner.timesteps+" lastDiff:"+microLearner.lastDiffusionParticipation+" lastS:"+microLearner.lastSourceParticipation);
				return;
			}else{
				log(Level.FINE,"Received sensor packet:"+spkt.getBody());
			}
			boolean process;
			switch ( spkt.pktType )
			{
				case DRLDiffApp.INTEREST_PKT :
					noOfInterests++;
					InterestPacket interestPkt = (InterestPacket)spkt.getBody() ;
					process=macroLearner.interestArriveAtDownPort(interestPkt);
					if(process)
						microLearner.handleInterestPkt(interestPkt);
					interestCachePrint();
					break ;
				case DRLDiffApp.DATA_PKT :
					DataPacket dataPkt = (DataPacket)spkt.getBody() ;
					if(dataPkt.isHeartBeat()){
						noOfHeartBeats++;
					}else{
						noOfDataPkts++;
					}
					process=macroLearner.dataArriveAtDownPort(dataPkt);
					if(process){
						if(nid==sink_nid){
							handleSinkData(dataPkt);							
						}else{
							microLearner.handleDataPkt(dataPkt);
						}
					}
					break;
				case DRLDiffApp.REINFORCEMENT_PKT :
					noOfReinforcements++;
					ReinforcementPacket reinforcementPkt = (ReinforcementPacket)spkt.getBody() ;
					log(Level.FINE,"Received REINFORCEMENT:"+reinforcementPkt);
					//interestCachePrint();
					macroLearner.handleReinforcement(reinforcementPkt);
					microLearner.handleReinforcement(reinforcementPkt);
					break ;
				default :
					super.recvSensorPacket(data_) ;
			}
		}
		else
			super.recvSensorPacket(data_) ;
    	}

	protected abstract void handleSinkData(DataPacket dataPkt);
	

	/** Initiates a sensing task by sending an INTEREST packet */
	public void subscribe(int taskId, List<Tuple> interest, List<CostParam> costParams, List<Tuple> qosConstraints, double duration, double interval, double dataInterval, double refreshPeriod, double payment)
	{
		/* constructs an interest */
		TaskEntry taskEntry = activeTasksList.get(taskId) ;
		if ( taskEntry == null ){ /* if there is NOT a matching task entry */
			InterestPacket intPkt = new InterestPacket(taskId,this.nid,interest,payment,getTime(),dataInterval,refreshPeriod, costParams) ;
			intPkt.setDuration(duration);
			intPkt.setQosConstraints(qosConstraints);
			taskEntry= new TaskEntry(taskId,intPkt,getTime(),refreshPeriod,true);
			taskEntry.setPayment(payment);
			activeTasksList.put(taskId, taskEntry);
			
			/* create a timer to periodically refresh the interest */
			DiffTimer refresh_EVT = new DiffTimer(DiffTimer.TIMEOUT_REFRESH_INTEREST, new Integer(taskId)); /* the object passed to DiffTimer is the index of the interest to be refreshed in the interest cache. */
			refresh_EVT.handle = setTimeout(refresh_EVT, refreshPeriod) ;

			log(Level.INFO,"Sending initial INTEREST packet at time " + getTime()) ;

			/* sends the interest */
            sendPacket(intPkt, 0.0) ;
		}else{
            refreshInterest(taskEntry);
		}
	}

	public void refreshInterest(TaskEntry taskEntry) {
		log(Level.INFO, "Sending INTEREST packet at time " + getTime()) ;

		/* sends the interest */
        InterestPacket newInterest= new InterestPacket(taskEntry.getInterest());
        newInterest.setTimestamp(getTime());
        //taskEntry.interest.setTimestamp(getTime());
		sendPacket(newInterest, 0.0) ;		
	}

	/** Sends a packet */
	public void sendPacket(Object data_, double delay)
	{
		log(Level.FINE,"Sending packet:"+data_);
		if ( data_ instanceof InterestPacket ) /* Interest Packet */
		{
			InterestPacket interest = (InterestPacket)data_ ;
			if (delay != 0.0)
			{
				//pendingInterest=interest; //work-around to issue of JSim where timer event is sometimes getting lost
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, interest);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			} 
			else if(interest.getDestinationId()==BROADCAST_DEST){
				pendingInterest=null;
				/*try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}*/
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, INTEREST_PKT, interest)) ;
			}else{				
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, 
						interest.getDestinationId(), nid, 100, INTEREST_PKT, interest)) ;				
			}
		}
		else if ( data_ instanceof DataPacket ) /* Data packet */
		{
			DataPacket dataPkt = (DataPacket)data_ ;
			if (delay != 0.0)
			{
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, dataPkt);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			}
			else if(dataPkt.getDestinationId()==BROADCAST_DEST){
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, DATA_PKT, dataPkt)) ;
			}else{
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET,
						dataPkt.getDestinationId(), nid, 100, DATA_PKT, dataPkt)) ;
				/*downPort.doSending(new SensorAppWirelessAgentContract.Message(
						SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET,
						dataPkt.getDestinationId(), 100, COHERENT, lastSeenSNR, eID,
						1, 0, 0, 0, seqNum++,dataPkt));*/
				
			}
		}
		else if ( data_ instanceof ReinforcementPacket ) /* PositiveReinforcement Packet */
		{
			ReinforcementPacket reinforcementPkt = (ReinforcementPacket)data_ ;	
			
			if (delay != 0.0)
			{
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, reinforcementPkt);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			}else if(reinforcementPkt.getDestinationId()==BROADCAST_DEST){
				log(Level.FINE,"Broadcasting REINFORCEMENT:"+reinforcementPkt);
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, REINFORCEMENT_PKT, reinforcementPkt)) ;
			}else{
				log(Level.FINE,"Sending REINFORCEMENT:"+reinforcementPkt);
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET,
						reinforcementPkt.getDestinationId(),nid, 100, REINFORCEMENT_PKT, reinforcementPkt)) ;
			}
		}		
	}

	//work-around to shutdown java
    public void shutdown(){
    	System.out.println("SHUTTING DOWN..");
    	System.exit(0);
    }
    
	/** Handles a timeout event */
	protected void timeout(Object data_) 
	{
		if ( data_ instanceof DiffTimer )
		{
			DiffTimer d = (DiffTimer)data_ ;
			int type = d.EVT_Type ;
			if(pendingInterest!=null && type!=DiffTimer.TIMEOUT_DELAY_BROADCAST){
				sendPacket(pendingInterest, 0.0) ;
			}
			switch ( type )
			{
				case DiffTimer.TIMEOUT_SEND_REINFORCEMENT :
					sendReinforcements() ;

					/*// if the size of the dataCache has become 0, there is no need for the timer. The timer will be set next time dataCache_insert is called. 
					if ( (dataCache.size() == 0) && (reinforcementTimer.handle != null) )
					{
						cancelTimeout(reinforcementTimer.handle) ;
						reinforcementTimer.setObject(null) ;
						reinforcementTimer.handle = null ; 
						reinforcementTimer = null ;
					}
					else{*/
					//	 reset the timer. 
						reinforcementTimer.handle = setTimeout(reinforcementTimer, REINFORCE_WINDOW) ;
					//}
					break ;
				case DiffTimer.TIMEOUT_INTEREST_CACHE_PURGE :
					interestCachePurge() ;

					/* if the size of the interestCache has become 0, there is no need for the timer. The timer will be set next time interestCache_insert is called. */
					if ( (interestCache.size() == 0) && (interestCache_purgeTimer.handle != null) )
					{
						cancelTimeout(interestCache_purgeTimer.handle) ;
						interestCache_purgeTimer.setObject(null) ;
						interestCache_purgeTimer.handle = null ; 
						interestCache_purgeTimer = null ;
					}
					else
					{
						/* reset the timer. */
						interestCache_purgeTimer.handle = setTimeout(interestCache_purgeTimer, INTEREST_CACHE_PURGE_INTERVAL) ;
					}
					break ;
				case DiffTimer.TIMEOUT_DELAY_BROADCAST :
					
					if ( d.getObject() instanceof InterestPacket )
					{
						sendPacket((InterestPacket)(d.getObject()), 0.0) ;
					}
					else if ( d.getObject() instanceof DataPacket )
					{
						DataPacket dataPkt = (DataPacket)(d.getObject()) ;
						sendPacket(dataPkt, 0.0) ;
					}
					else if ( d.getObject() instanceof ReinforcementPacket )
					{
						ReinforcementPacket pstvReinforcementPkt = (ReinforcementPacket)(d.getObject()) ;
						sendPacket(pstvReinforcementPkt, 0.0) ;
					}
					break ;
				case DiffTimer.TIMEOUT_REFRESH_INTEREST :
					
					Integer I = (Integer)(d.getObject()) ;
					int taskId = I.intValue() ;
					TaskEntry taskEntry = activeTasksList.get(taskId) ;
					InterestPacket interest= taskEntry.getInterest();
					//if ( (getTime() - taskEntry.getStartTime()) <= interest.getDuration()) /* depends on getTime() - interest start time */
					//{
						if ( isDebugEnabled() )
							System.out.println("DiffApp " + nid + ": Sending INTEREST packet at time " + getTime()) ;
						interest.setTimestamp(getTime());
						sendPacket(interest, 0.0 ) ;
						DiffTimer refresh_EVT = new DiffTimer(DiffTimer.TIMEOUT_REFRESH_INTEREST, new Integer(taskId)); 
						refresh_EVT.handle = setTimeout(refresh_EVT, taskEntry.getRefreshPeriod()) ;
					/*}
					else if ( d.handle != null )
					{
						// The task state has to be purged from the node after the time indicated by the duration attribute. 
						activeTasksList.remove(taskId) ;
					}*/
					cancelTimeout(d.handle) ;
					d.setObject(null) ;
					d.handle = null ;
					break ;
			}
		}
		else
		{
			microLearner.timeout(data_) ;
		}    
	}
	
	public double calcTotalReward(List<DataPacket> pkts,
			InterestPacket interest, double payable){
		if(pkts==null || pkts.size()==0) return 0;
		double quality=calcDataQuality(pkts, interest);
		double minCost=Integer.MAX_VALUE;
		double maxReward=Integer.MIN_VALUE;
		for(DataPacket pkt: pkts){
			if(pkt.getCost()<minCost){
				minCost=pkt.getCost();
			}
			if(pkt.getReward()>maxReward){
				maxReward=pkt.getReward();
			}			
		}
		//double avgReward= pktsReward/pkts.size();
		//double avgCost=pktsCost/pkts.size();
		return quality*payable- minCost;//+NO_OF_PKTS_FACTOR*pkts.size();		
	}
	
	/**
	 * Matches data attributes to QoS contraints and returns a value between 0 and 1 representing data quality
	 * @param pkts
	 * @param interestEntry
	 * @return
	 */
	protected double calcDataQuality(List<DataPacket> pkts,
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
	
	public double getDelay(){
		return DELAY * rand.nextDouble();
	}
	
	public void log(Level level, String string) {
		if(level.intValue()>=Level.INFO.intValue())
			log.log(Level.INFO,getTime()+"[Node:"+nid+"]["+"] "+string);        
    }

	public int getNoOfStates() {
		return microLearner.noOfStates;
	}
	
    public void addToTotalCost(double lastCost) { 	
			
	}
    
    public void collectGlobalStats() {
		log(Level.INFO, "*******************GLOBAL STATS**************");
		if(nid==sink_nid){
        	EnergyStats.NodeStat lowestLifeNode=EnergyStats.getNodeWithLowestLifetime();
        	String mobility=System.getProperties().getProperty("target.mobile", "true");
        	String stats= noOfNodes+","+nid+","+totalHeartBeatPkts+","+totalTrackingPkts+","+lastTrackTime+","+averageDelay
        		+","+totalEnergyUsed+","+lowestLifeNode.toString()+","+Boolean.parseBoolean(mobility)+","+this.costParam+","+this.macroLearner.utilFunc.toString()
        		+","+(totalTrackingError/totalTrackCount);
        	for(Integer taskId: taskExecutions.keySet()){
    			stats+=",task-"+taskId+","+taskExecutions.get(taskId);
    		}
        	CSVLogger.logGlobal("sinkStats",stats,microLearner.algorithm.getAlgorithm());            
        }
	}
    
    public void collectStats(){
        log(Level.INFO,"*******************STATS**************");
        String QValues=nid+",";
        String expPrices=nid+",";
        String nodeStats=noOfNodes+","+nid+","+noOfInterests+","+noOfHeartBeats+","+noOfSrcPkts+","+noOfDataPkts+","+noOfReinforcements; //+","+totalPkts;
        
        for(Integer i : microLearner.taskList.keySet()){
            SensorTask task= (SensorTask)microLearner.taskList.get(i);
            if (nid != sink_nid) {
            	nodeStats += ",task-" + task.taskId + ","
						+ task.getNoOfExecutions();
				QValues += "task:" + task.getTaskId() + ","
						+ task.printQValues();
				expPrices += "task:" + task.getTaskId() + ","
						+ task.printExpPrices();
			}
        }
        updateTaskExecutions(microLearner.taskList);
       // stats+=","+totalCost+","+totalReward+","+globalRewardManager.getEffectiveCost();
        
        CSVLogger.log("Qvalues",QValues,microLearner.algorithm.getAlgorithm());
        CSVLogger.log("ExpPrices",expPrices,microLearner.algorithm.getAlgorithm());
        CSVLogger.log("States",microLearner.states.toString(),microLearner.algorithm.getAlgorithm());
        if(nid!=sink_nid){
        	nodeStats+=","+getRemainingEnergy()+","+lifetime;  
        	CSVLogger.logGlobal("nodeStats", nodeStats, microLearner.algorithm.getAlgorithm());            
        	interestCachePrint();            
        }
        /* if(!globalLogged){
        	
        for(int i=0; i< globalRewardManager.getGlobalRewards().size();i++){
            CSVLogger.log("reward",""+globalRewardManager.getGlobalRewards().get(i),false,algorithm.getAlgorithm());            
        }
        CSVLogger.logGlobal("global-stats",globalRewardManager.stats(),algorithm.getAlgorithm());
        globalLogged=true;
        }*/
    }

	public double getSamplingEnergy() {
		return MicroLearner.ENERGY_SAMPLE; //default
	}
	
	public boolean supportsHeartBeat(){
		return false;
	}
	
	public static String doubleArrToString(double[] arr){
		String s="[";
		for(double d:arr) s+=d+" ";
		s+="]";
		return s;
	}
	
	public static void updateTaskExecutions(Hashtable<Integer, SensorTask> taskList) {
		for(Integer id: taskList.keySet()){
			int curr= taskExecutions.containsKey(id)?taskExecutions.get(id):0;
			taskExecutions.put(id, curr+taskList.get(id).noOfExecutions);
		}
	}
}
