
package drcl.inet.sensorsim.drl.diffext ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import drcl.inet.sensorsim.drl.IDRLSensorApp;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm;
import drcl.inet.sensorsim.drl.diffext.InterestPacket.CostParam;
import drcl.inet.sensorsim.drl.diffext.Tuple.Operator;
import drcl.inet.sensorsim.drl.diffext.Tuple.Type;


/** This class implements the extension of directed diffusion using distributed RL
*
* @author Kunal Shah
*/
public class DRLDiffApp extends drcl.inet.sensorsim.SensorApp implements drcl.comp.ActiveComponent, IDRLSensorApp
{
 	private static final long serialVersionUID = -8724996734345865474L;

	public static Logger log= Logger.global;
	   
	static {
		Logger.global.setLevel(Level.FINE);
	}
	public static final String MOBILITY_PORT_ID    = ".mobility";

	/** The port used to query the current position of myself */
	protected Port mobilityPort    = addPort(MOBILITY_PORT_ID, false); 
	
	/** A node may suppress a received interest if it recently (i.e., within RESEND_INTEREST_WINDOW secs.) resent a matching interest. */	
	public static final double RESEND_INTEREST_WINDOW = 2.0 ;

	/** Period between two successive times to check if any entries in the interest cache need to be purged. */
	public static final double INTEREST_CACHE_PURGE_INTERVAL = 120.0 ; /* secs. */
	
	/** Size of the window of N events used for judging whether a neighbor needs to be negatively reinforced or not. Negatively reinforce that neighbor from which no new events have been received within a window of N events */
	public static final int N_WINDOW = 5 ;

	/** Name of the target that a sink node is interested in (and a sensor node capable of) detecting */
	public String TargetName ;

	public static final int INTEREST_PKT = 0 ;
	public static final int DATA_PKT = 1 ;
	public static final int REINFORCEMENT_PKT = 2 ;
	public static final int BROADCAST_DEST=-1;
	public static final double REINFORCE_WINDOW=10*MicroLearner.TIMER_INTERVAL; //10 TIMESTEPS
	public static final double REINFORCE_SUPRESS_MARGIN= 0.10;
	public static final boolean TRACE_ON=true;
	
	public static enum NodeState { SLEEPING, AWAKE};
	 
	public static final double DELAY = 1.0; 			/* fixed delay to keep arp happy */
	
	private static java.util.Random rand = new java.util.Random(7777) ;
	private static int seqNum=0;
	
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

	private MicroLearner microLearner;
	
	private MacroLearner macroLearner;
	
	NodeState nodeState=NodeState.AWAKE;
    
	private int noOfReinforcements=0;
	private int noOfDataPkts=0;
	private int noOfInterests=0;
	   
	public DRLDiffApp ()
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
		 this.initialEnergy=getRemainingEnergy();
		 microLearner._start();		 
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

	/** Purges expired entries from the data cache */
	public void sendReinforcements(){
		for(DataCacheEntry entry: dataCache.values()){
			double payable;
			if(nid==sink_nid){
				payable= activeTasksList.get(entry.getTaskId()).getPayment();
			}else{
				payable= interestCacheLookup(entry.getTaskId()).getPayable();
			}
			List<ReinforcementPacket> pendingReinforcements=entry.getPendingReinforcements(nid, getTime(), REINFORCE_SUPRESS_MARGIN*payable);
			for(ReinforcementPacket pkt: pendingReinforcements){
				sendPacket(pkt, getDelay());
			}
			entry.resetData();
		}	
	}

	/** Purges expired entries from the interest cache */
	public void interestCachePurge()
	{
		double currentTime = getTime() ;
		for(InterestCacheEntry entry:interestCache.values()){
			entry.gradientListPurge(currentTime) ;
			/*if ( entry.IsGradientListEmpty() == true ){
				it.remove();
			}	*/		
		}
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
	public List<Tuple> ConstructSensingEvent(SensorAppAgentContract.Message msg)
	{
		double locX, locY;
		long targetNid;
		if (msg == null) {
			/*
			 * sensorLocX and sensorLocY are the X and Y coordinates of the
			 * sensor node and must be obtained from the mobility model.
			 */
			SensorPositionReportContract.Message positionMsg = new SensorPositionReportContract.Message();
			positionMsg = (SensorPositionReportContract.Message) mobilityPort
					.sendReceive(positionMsg);
			locX = positionMsg.getX();
			locY = positionMsg.getY();
			targetNid=this.nid;
		}else{
			locX = msg.getTargetX() ;
		    locY = msg.getTargetY() ;
		    targetNid=msg.getTargetNid();
		}
		List<Tuple> event = new ArrayList<Tuple>() ;
		event.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.IS, locX)) ;
		event.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.IS, locY)) ;
		event.add(new Tuple(Tuple.TARGET_KEY, Type.STRING_TYPE, Operator.IS, TargetName)) ;
		event.add(new Tuple(Tuple.TARGET_NID, Type.INT32_TYPE, Operator.IS, targetNid)) ;
		return event ;
	}

	public double getRemainingEnergy(){
    	if (nid != sink_nid) {
			double energy = ((EnergyContract.Message) wirelessPhyPort
					.sendReceive(new EnergyContract.Message(0, -1.0, -1)))
					.getEnergyLevel();
			if (energy <= 0)
				throw new RuntimeException("Out of energy..");
			return energy;
		}else
    		return Integer.MAX_VALUE;
    	
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

	/* Implements the data-driven local rule used to determine which neighbor needs to be reinforced. If the node must also reinforce at least one neighbor, it uses its data cache for this purpose. The same local rule choices apply. For example, this node might choose that neighbor from whom it first received the latest event matching the interest. 
	public long CheckToForwardPositiveReinforcement(AttributeVector interest, float newInterval)
	{		
		DataCacheEntry dataEntry = dataCache_lookup(interest) ;
		if ( dataEntry != null )	 if there is a matching data packet, return the index of the node from whom I first received the latest event matching the interest. 
		{
                        long source = dataEntry.getSource() ;                        

			if ( dataCache_lookup(interest, newInterval, source) == null )  Obviously, we do not need to reinforce neighbors that are already sending traffic at the higher data rate. 
			{
				return source ;
			}
			else
			{
				return -1 ;
			}
		}

		return -1 ;
	}*/

	/** Implements the data-driven local rule used to reinforce one particular neighbor in order to draw down real data. One example of such a rule is to reinforce any neighbor from which a node receives a previously unseen event (i.e., an event that does not exist in the data cache. *//*
	public boolean CheckToSendPositiveReinforcement(ActiveTasksEntry taskEntry, DataPacket dataPkt)
	{		
		AttributeVector event = dataPkt.getEvent() ;
		DataCacheEntry dataEntry = dataCache_lookup(event) ;
		if ( dataEntry == null )	 if there is NOT a matching data packet (i.e., this data was not seen). 
		{
			 add the received message to the data cache 
			dataCache_insert(new DataCacheEntry(dataPkt.getEvent(), dataPkt.getDataInterval(), dataPkt.getSource(), getTime())) ;

			taskEntry.newEventsWindow_insert(dataPkt.getSource()) ;

			Vector NeighborsToNegativelyReinforce = taskEntry.getListOfNeighborsToNegativelyReinforce() ;
			for ( int i = 0 ; i < NeighborsToNegativelyReinforce.size() ; i++ )
			{
				Long neighborEntry = (Long)(NeighborsToNegativelyReinforce.elementAt(i)) ;
				sendPacket(new NegativeReinforcementPacket(nid, neighborEntry.longValue(), taskEntry.getInterest(), taskEntry.getInterest().getFrequency()), DELAY * rand.nextDouble()) ;
			}

			if ( dataPkt.getDataInterval() != taskEntry.getDataInterval() )		 Obviously, we do not need to reinforce neighbors that are already sending traffic at the higher data rate. 
			{
				return true ;
			}
			else
			{
				return false ;
			}
		}

		return false ;
	}
*/
	public String toString(){
		return "DRLDiffApp-"+nid;
	}
	
	/** Handles information received over the wireless channel  */
	protected void recvSensorPacket(Object data_)
	{	
		if ( data_ instanceof SensorPacket )
		{
			SensorPacket spkt = (SensorPacket)data_ ;
			if(nodeState.equals(NodeState.SLEEPING)){
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
					break ;
				case DRLDiffApp.DATA_PKT :
					noOfDataPkts++;
					DataPacket dataPkt = (DataPacket)spkt.getBody() ;
					process=macroLearner.dataArriveAtDownPort(dataPkt);
					if(process){
						microLearner.handleDataPkt(dataPkt);
					}
					break;
				case DRLDiffApp.REINFORCEMENT_PKT :
					noOfReinforcements++;
					ReinforcementPacket reinforcementPkt = (ReinforcementPacket)spkt.getBody() ;
					log(Level.INFO,"Received REINFORCEMENT:"+reinforcementPkt);
					interestCachePrint();
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

	/** Initiates a sensing task by sending an INTEREST packet */
	public void subscribe(int taskId, float longMin, float longMax, float latMin, float latMax, float duration, float interval, float dataInterval, double refreshPeriod, double payment)
	{
		/* constructs an interest */
		List<Tuple> interest = new ArrayList<Tuple>() ;
		interest.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.GE, new Double(longMin) ) ) ;
		interest.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.LE,new Double(longMax) ) ) ;
		interest.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.GE, new Double(latMin) ) ) ;
		interest.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.LE, new Double(latMax) ) ) ;
		interest.add(new Tuple(Tuple.TARGET_KEY, Type.STRING_TYPE, Operator.IS, TargetName) ) ;
		interest.add(new Tuple(Tuple.TARGET_NID, Type.INT32_TYPE, Operator.EQ_ANY, TargetName) ) ;
		TaskEntry taskEntry = activeTasksList.get(taskId) ;
		if ( taskEntry == null ) /* if there is NOT a matching task entry */
		{
			List<CostParam> costParams= new ArrayList<CostParam>();
			costParams.add(new CostParam(CostParam.Type.Energy,1.0));
			InterestPacket intPkt = new InterestPacket(taskId,this.nid,interest,payment,getTime(),dataInterval,costParams) ;
			intPkt.setDuration(duration);
			
			taskEntry= new TaskEntry(taskId,intPkt,getTime(),refreshPeriod,true);
			taskEntry.setPayment(payment);
			activeTasksList.put(taskId, taskEntry);
			
			/* create a timer to periodically refresh the interest */
			DiffTimer refresh_EVT = new DiffTimer(DiffTimer.TIMEOUT_REFRESH_INTEREST, new Integer(taskId)); /* the object passed to DiffTimer is the index of the interest to be refreshed in the interest cache. */
			refresh_EVT.handle = setTimeout(refresh_EVT, refreshPeriod) ;

			log(Level.INFO,"Sending initial INTEREST packet at time " + getTime()) ;

			/* sends the interest */
            sendPacket(intPkt, 0.0) ;
		}
		else
		{
           if ( isDebugEnabled() )
        	   log(Level.INFO, "Sending INTEREST packet at time " + getTime()) ;

			/* sends the interest */
			sendPacket(taskEntry.interest, 0.0) ;
		}
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
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, interest);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			} 
			else if(interest.getDestinationId()==BROADCAST_DEST){
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
			log(Level.INFO,"Sending REINFORCEMENT:"+reinforcementPkt);
			
			if (delay != 0.0)
			{
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, reinforcementPkt);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			}
			else if(reinforcementPkt.getDestinationId()==BROADCAST_DEST){
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, REINFORCEMENT_PKT, reinforcementPkt)) ;
			}else{
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET,
						reinforcementPkt.getDestinationId(),nid, 100, REINFORCEMENT_PKT, reinforcementPkt)) ;
			}
		}		
	}

	/** Handles a timeout event */
	protected void timeout(Object data_) 
	{
		if ( data_ instanceof DiffTimer )
		{
			DiffTimer d = (DiffTimer)data_ ;
			int type = d.EVT_Type ;
			switch ( type )
			{
				case DiffTimer.TIMEOUT_SEND_REINFORCEMENT :
					sendReinforcements() ;

					/* if the size of the dataCache has become 0, there is no need for the timer. The timer will be set next time dataCache_insert is called. */
					if ( (dataCache.size() == 0) && (reinforcementTimer.handle != null) )
					{
						cancelTimeout(reinforcementTimer.handle) ;
						reinforcementTimer.setObject(null) ;
						reinforcementTimer.handle = null ; 
						reinforcementTimer = null ;
					}
					else{
						/* reset the timer. */
						reinforcementTimer.handle = setTimeout(reinforcementTimer, REINFORCE_WINDOW) ;
					}
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
					if ( (getTime() - taskEntry.getStartTime()) <= interest.getDuration()) /* depends on getTime() - interest start time */
					{
						if ( isDebugEnabled() )
							System.out.println("DiffApp " + nid + ": Sending INTEREST packet at time " + getTime()) ;

						sendPacket(interest, 0.0 ) ;
						DiffTimer refresh_EVT = new DiffTimer(DiffTimer.TIMEOUT_REFRESH_INTEREST, new Integer(taskId)); 
						refresh_EVT.handle = setTimeout(refresh_EVT, taskEntry.getRefreshPeriod()) ;
					}
					else if ( d.handle != null )
					{
						/* The task state has to be purged from the node after the time indicated by the duration attribute. */
						activeTasksList.remove(taskId) ;
					}
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
    public void collectStats(){
        log(Level.INFO,"*******************STATS**************");
        //nid,noOfEventsMissed,totalEvents,noOfPktsDropped,totalPkts,task1Id,task1,task2Id,task2,task3Id,task3,totalCost,totalReward,trPackets
        String stats=""+nid+",interests="+noOfInterests+",dataPkts="+noOfDataPkts+",reinf="+noOfReinforcements; //+","+totalPkts;
        String QValues=nid+",";
        String expPrices=nid+",";
        for(Integer i : microLearner.taskList.keySet()){
            SensorTask task= (SensorTask)microLearner.taskList.get(i);
            stats+=",task-"+task.taskId+","+task.getNoOfExecutions();
            QValues+="task:"+task.getTaskId()+","+task.printQValues();
            expPrices+="task:"+task.getTaskId()+","+task.printExpPrices();
        }
       // stats+=","+totalCost+","+totalReward+","+globalRewardManager.getEffectiveCost();
        if(microLearner.totalTrackingPkts>0)
            stats+=",totalTracks="+microLearner.totalTrackingPkts;
        CSVLogger.log("stats",stats,microLearner.algorithm.getAlgorithm());
        CSVLogger.log("Qvalues",QValues,microLearner.algorithm.getAlgorithm());
        CSVLogger.log("ExpPrices",expPrices,microLearner.algorithm.getAlgorithm());
        CSVLogger.log("States",microLearner.states.toString(),microLearner.algorithm.getAlgorithm());
       /* if(!globalLogged){
        	
        for(int i=0; i< globalRewardManager.getGlobalRewards().size();i++){
            CSVLogger.log("reward",""+globalRewardManager.getGlobalRewards().get(i),false,algorithm.getAlgorithm());            
        }
        CSVLogger.logGlobal("global-stats",globalRewardManager.stats(),algorithm.getAlgorithm());
        globalLogged=true;
        }*/
    }
	
}
