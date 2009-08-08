
package drcl.inet.sensorsim.drl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import drcl.comp.ACATimer;
import drcl.comp.Port;
import drcl.data.DoubleObj;
import drcl.inet.sensorsim.CPUBase;
import drcl.inet.sensorsim.CurrentTargetPositionTracker;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.drl.GlobalRewardManager.WLReward;
import drcl.util.random.UniformDistribution;


/*
 * @author Kunal
 */

public class DRLSensorApp extends SensorApp implements drcl.comp.ActiveComponent{

    private static final long serialVersionUID = 1933018614040188069L;

    public static final int REWARD= 5;
    
    private static final double TIMER_INTERVAL =10;
    private static final double MAX_EPSILON=0.3;    // MAX exploration factor
    private static final double MIN_EPSILON=0.1;    // MIN exploration factor
    
    private static final String QPortId=".Qval";
    private static final String ExecutionsPortId=".executions";
    
    protected static final String SAMPLE="Sample";
    protected static final String ROUTE="Route";
    protected static final String SLEEP="Sleep";
    
    protected Hashtable<Integer,SensorTask> taskList= new Hashtable<Integer,SensorTask>();
    
    ACATimer myTimer;
    
    //public static String algorithm="RANDOM";
    //public static String algorithm="STATIC";
    //public static String algorithm="DIRL";
    public static String algorithm="COIN";
    //public static String algorithm="FIXED";
    //public static String algorithm="SORA";
    protected List<SensorAppWirelessAgentContract.Message> outboundMsgs= new LinkedList<SensorAppWirelessAgentContract.Message>();
    protected UniformDistribution uniformDist;
    protected Port batteryPort = addPort(".battery", false);
    protected Port energyPort = addPort(".energy",false);
    long destId=-1;
    protected SensorTask currentTask;
    protected SensorState currentState;
    MacroLearner mlearner= new MacroLearner();
    protected List states= new LinkedList();
    protected int noOfStates=0;
    protected double totalPrice=0;
    protected int totalTrackingPkts=0;
    int noOfRx;
    int noOfTx;
    int noOfSensedEvents;
    int noOfSamplesAggregated;
    int noOfPktsDropped=0;
    int totalPkts=0;
    int noOfEventsMissed=0;
    int totalEvents=0;
    double lastMeasuredEnergy;
    double lastReportedTime;
    double initialEnergy;
    int totalExecutions=0;
    double totalReward=0;
    double totalCost=0;
    String[] tasks;
    GlobalRewardManager globalRewardManager;
    //List<Double> globalRewards= new ArrayList<Double>(500);
    
	private long currStream=-1;

	private static boolean globalLogged=false;
        
    public DRLSensorApp(){
        super();
        algorithm=System.getProperty("algorithm", algorithm);
        String glRewManager= System.getProperty("rewardManager",BDGlobalRewardManager.class.getName());
        try {
			globalRewardManager= (GlobalRewardManager) Class.forName(glRewManager).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
        prepareTaskList();
        for(Integer i : taskList.keySet()){
            SensorTask task=taskList.get(i);
            totalPrice+=task.expectedPrice;
            addPort(QPortId+i,false);           
            addPort(ExecutionsPortId+i,false);
        }
        uniformDist= new UniformDistribution(0,taskList.size());
    }
    
    public void setDestId(long destid){
        this.destId=destid;
    }
    
    public void setTasks(String[] tasks){
        this.tasks=tasks;
    }
    
    public void setAlgorithm(String algo){
        algorithm=algo;
    }
    
    protected void _start ()  {
        if(nid!=sink_nid){
            myTimer=setTimeout("performTask", TIMER_INTERVAL);
            lastMeasuredEnergy=getRemainingEnergy();
            initialEnergy=lastMeasuredEnergy;
        }else{
        	myTimer=setTimeout("manageReward", TIMER_INTERVAL);
            log("********ALGORITHM:"+algorithm+"*******************");
        }
    }

    protected void _stop()  {
        if (myTimer != null)
            cancelTimeout(myTimer);
    }

    protected void _resume() {
        if(nid!=sink_nid)
            myTimer=setTimeout("performTask", TIMER_INTERVAL);
    }

    protected void resetForNewTask(){
    	currStream=-1;
    }
    protected synchronized void timeout(Object data) {
		if (data.equals("performTask")) {
			double currentEnergy = getRemainingEnergy();
			if (currentTask != null) {

				// log("Energy spent:"+(lastMeasuredEnergy-currentEnergy)+"
				// Task:"+currentTask);
				currentTask.computeReward((lastMeasuredEnergy - currentEnergy)
						/ currentEnergy);
				totalReward += currentTask.lastReward;
				totalCost += currentTask.lastCost;
				endTask(currentTask, currentState);
				// update to macro-learners
				List<WLReward> wlrewards = globalRewardManager
						.getPendingRewards(nid);
				mlearner.handleWLReward(wlrewards);
			}
			SensorState prevState = currentState;
			currentState = getMatchingState(lastSeenSNR, destId >= 0,
					noOfRx > 0, noOfSensedEvents > 0);
			// log("Current State:"+currentState);
			if (currentTask != null) {
				currentTask.updateQValue(prevState,
						determineBestTaskToExecute().getQvalue(currentState));
			}
			lastMeasuredEnergy = currentEnergy;

			/*
			 * energyPort.exportEvent("Energy Node:" + nid, new DoubleObj(
			 * currentEnergy), null);
			 */
			/*
			 * else{ currentState= new SensorState(lastSeenSNR,true,false); }
			 */
			if (Math.random() < calcExplorationFactor()) { // exploration
															// choosen
				currentTask = getRandomTaskToExecute();
			} else {
				if(algorithm.equals("TEAM"))
					currentTask= getBestTeamGameBasedTask();
				else	
					currentTask = determineBestTaskToExecute();
			}

			exportvalues();
			lastMeasuredEnergy = currentEnergy;
			// log("Executing task:"+currentTask);

			resetForNewTask();
			if (currentTask != null)
				currentTask.executeTask();
			myTimer = setTimeout("performTask", TIMER_INTERVAL);
		} else if (data.equals("manageReward")) {
			setTimeout("manageReward", TIMER_INTERVAL);
			if (nid == sink_nid) {
				globalRewardManager.manage(totalExecutions);
			}
		}
		++totalExecutions;
	}

    
    private SensorTask getBestTeamGameBasedTask() {
    	double maxQ=Double.MIN_VALUE;
    	SensorTask bestTask=null;
    	for(Iterator it=taskList.values().iterator();it.hasNext();){
            SensorTask task= (SensorTask)it.next();
            double utility= task.getExpectedPrice();
            if(task.isAvailable()){
                if(utility>maxQ){
                    maxQ=utility;
                    bestTask=task;
                }
            }
        }
        return bestTask;
	}

	private void endTask(SensorTask task, SensorState state) {
	 	setCPUMode(CPUBase.CPU_ACTIVE);
        //setRadioMode(RadioBase.RADIO_TRANSMIT);
        noOfTx=0;
        if(destId==-1){
            return; // no destination node present
        }
        for (Iterator<SensorAppWirelessAgentContract.Message> iter = outboundMsgs.iterator(); iter.hasNext();) {
            noOfTx++;
            SensorAppWirelessAgentContract.Message msg = iter.next();
            ((TrackingEvent)msg.getBody()).addReward(nid, currentTask.lastReward, currentTask.lastCost);
            this.currStream=((TrackingEvent)msg.getBody()).streamId;
            mlearner.record(new Tuple(msg.getTargetSeqNum(),state.stateId,task.id,totalExecutions));
            downPort.doSending(msg);
            iter.remove();
        }
	}



    public SensorState getMatchingState(double lastSeenSNR, boolean hasNeighbours, boolean successfulRx, boolean successfulSample) {
        SensorState state= new SensorState(lastSeenSNR,hasNeighbours,successfulSample,successfulRx,currStream);
        for (Iterator iter = states.iterator(); iter.hasNext();) {
            SensorState existingState = (SensorState) iter.next();
            if(existingState!=null && state.equals(existingState))
                return existingState;
        }
        
        // no match found, add this new state
        if (noOfStates < SensorState.MAX_STATES) {
			state.stateId = noOfStates;
			noOfStates++;
			states.add(state);
		}
        return (SensorState) states.get(0);
    }
    

  
    
    private SensorTask getTask(String taskId){
        for (int i = 0; i < taskList.size(); i++) {
            SensorTask task = (SensorTask) taskList.get(i);
            if(task.taskId.equals(taskId))
                return task;
        }
        return null;
    }
    private SensorTask getRandomTaskToExecute(){
    	SensorTask task=null;
    	do{
            int index = uniformDist.nextInt();
            if (index < taskList.size()) {
                task = (SensorTask) taskList.get(index);
                if (task != null && task.isAvailable()) {
                    //log("using exploration:" + task);
                    return (SensorTask) taskList.get(index);
                }
            }
        }while(task!=null);
        return null;
    }
    
    private SensorTask determineBestTaskToExecute() {
        double maxQ=Double.NEGATIVE_INFINITY;
        List<SensorTask> bestTasks=new ArrayList<SensorTask>();
        
        for(Iterator it=taskList.values().iterator();it.hasNext();){
            SensorTask task= (SensorTask)it.next();
            double utility= task.getQvalue(currentState);//*task.getExpectedPrice();
            if(task.isAvailable()){
                if(utility>maxQ){
                    maxQ=utility;
                    bestTasks.clear();
                    bestTasks.add(task);
                }else if(utility==maxQ){
                	bestTasks.add(task);
                }
            }
        }
        if(bestTasks.size()==1) return bestTasks.get(0);
        int taskId= (int) (Math.random()*bestTasks.size());
        return bestTasks.get(taskId);        
    }

    private double calcExplorationFactor() {
        double e=MIN_EPSILON+0.25*(SensorState.MAX_STATES-noOfStates)/SensorState.MAX_STATES;
        return (e<MAX_EPSILON)?e:MAX_EPSILON;
    }

    public double getRemainingEnergy(){
        double energy=100;//BatteryContract.INSTANCE.getContractContent();.getRemainingEnergy(batteryPort);
        if(energy<=0)
            throw new RuntimeException("Out of energy..");
        return energy;
    }

    private void log(String string) {
        System.out.println(getTime()+"[Node:"+nid+"] "+string);        
    }

    private void exportvalues(){
        String executions="";
        for (Integer i : taskList.keySet()) {
            SensorTask element = (SensorTask) taskList.get(i);
            //Port port=getPort(QPortId+i);
            //port.exportEvent("Q Values Node:"+nid, new DoubleObj((double)element.getQvalue(currentState)), null);
            Port execPort=getPort(ExecutionsPortId+i);
            execPort.exportEvent("No. Of Executions Node:"+nid, new DoubleObj((double)element.getNoOfExecutions()), null);
            
            executions+=element.getNoOfExecutions()+",";
        }
        CSVLogger.log("executions-"+nid,executions,false);
        double avgRew=(totalReward/totalExecutions);
        /*synchronized(DRLSensorApp.class){
        	double avgRew=
            globalRewards.[totalExecutions]+=avgRew;
        }*/
        CSVLogger.log("reward-"+nid,new Double(avgRew).toString(),false);
    }
    
    class TrackingEvent{
        long streamId;
        List<Long> nodes= new ArrayList<Long>();   
        double reward=0;
        double cost=0;
        double snr;
        long pktId;
        public void addReward(long node, double reward, double cost){
        	this.reward+=reward;
        	this.nodes.add(node);
        	this.cost+=cost;
        }
        public String toString(){
        	return "R="+reward+",C="+cost+",N="+nodes+",sId="+streamId+",SNR="+snr;
        }
        public int getSize(){
        	return toString().length();
        }
    }
    
    private synchronized void prepareTaskList() {
        taskList.put(1, new SensorTask(1,ROUTE,0.1) {
            public synchronized void execute() {
                noOfRx=0;
            }
            public synchronized double computeCost(double energySpent) {
            	return ((2.45*0.001) + (5.97*0.001)); //RX +TX
            }
            public synchronized double computePrice() {
            	/*return (noOfRx>0)?expectedPrices[currentState.stateId]:0;*/
            	return (noOfRx>0)?expectedPrice:0;
            }
            public synchronized boolean isAvailable() {
              return true;  
            }
        });
        
        taskList.put(2, new SensorTask(2,SAMPLE, 0.02) {
            public void execute() {
                setCPUMode(CPUBase.CPU_ACTIVE);
                //setRadioMode(RadioBase.RADIO_OFF);
                noOfSensedEvents=0;
            }
            public synchronized double computeCost(double energySpent) {
            	return (8.41*0.00001); 
            }
            public synchronized double computePrice() {
            	//return (noOfSensedEvents>0)?(expectedPrices[currentState.stateId]):0;
            	return (noOfSensedEvents>0)?(expectedPrice):0;
            }
            public synchronized boolean isAvailable() {
                return true;  
            }
        });
        taskList.put(3, new SensorTask(3,SLEEP,0.00) {
            public void execute() {
                noOfRx=0; noOfSensedEvents=0;
                setCPUMode(CPUBase.CPU_OFF);
               // setRadioMode(RadioBase.RADIO_OFF);
            }

            public synchronized double computeCost(double energySpent) {
            	return (8.0*0.000001); 
            }
            public synchronized double computePrice() {
            	//return (expectedPrices[currentState.stateId]);
            	return (expectedPrice);
            }
            
            public synchronized boolean isAvailable() {
                return true;  
            }
        });
        
    }


    // receive an event over sensor channel
    protected synchronized void recvSensorEvent(Object data_) {
        SensorAppAgentContract.Message msg = (SensorAppAgentContract.Message) data_;
        lastSeenSNR = msg.getSNR();
        if(lastSeenSNR*SensorState.SNR_WEIGHT<1 || destId==-1) // snr not strong enough or no destination set
            return;
        totalEvents++;
        if((nid!=sink_nid) && (currentTask==null || !currentTask.getTaskId().equals(SAMPLE))){
            noOfEventsMissed++;
            //if(currentTask!=null)
           // log("Sensor event missed, executing task:"+currentTask);
            return;
        }
        lastSeenDataSize = msg.getDataSize();
        long target_nid = msg.getTargetNid();
        double target_X = msg.getTargetX() ;
        double target_Y = msg.getTargetY() ;
        double target_Z = msg.getTargetZ() ;
        int target_SeqNum = msg.getTargetSeqNum() ;
        
        noOfSensedEvents++;
        //log("Received sensed event:"+noOfSensedEvents+" SNR:"+lastSeenSNR);
        if ( nid == sink_nid )
        {
            Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(target_nid - first_target_nid));
            if ( snrPort != null )
                if ( snrPort.anyOutConnection() )
                    snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)lastSeenSNR), null);
        }

        if ( nid != sink_nid )
        {
			TrackingEvent te = new TrackingEvent();
			te.pktId = target_SeqNum;
			te.snr = msg.getSNR();
			te.streamId = nid;
			msg.body = te;
			currStream = nid;

			outboundMsgs.add(new SensorAppWirelessAgentContract.Message(
					SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET,
					destId, lastSeenDataSize, COHERENT, lastSeenSNR, eID,
					target_nid, target_X, target_Y, target_Z, target_SeqNum,
					msg.getBody()));
			eID = eID + 1;
		}      
    }


    // receive over wireless channel
	protected synchronized void recvSensorPacket(Object data_) {
		totalPkts++;
		if ((nid != sink_nid)
				&& (currentTask == null || !currentTask.getTaskId().equals(
						ROUTE))) {
			//log("Dropped pkt..,currently executing:" + currentTask);
			noOfPktsDropped++;
			return;
		}
		SensorPacket spkt = (SensorPacket) ((SensorPacket) data_).clone();
		noOfRx++;
		TrackingEvent tevent = (TrackingEvent) spkt.getBody();
		//log("Received sensor packet data:" + tevent);

		if (nid != sink_nid) {
			
			 /*outboundMsgs.add(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, 
					 destId, spkt.getDataSize(),spkt.pktType,spkt.getMaxSnr(), eID, spkt.getTargetNid(),spkt.getTargetX(),
					 spkt.getTargetY(),spkt.getTargetZ(),spkt.getTargetSeqNum(),spkt.getBody())) ;*/
			 SensorAppWirelessAgentContract.Message sawaMsg=new SensorAppWirelessAgentContract.Message(
						SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET,
						destId, spkt.getSrc_nid(), null, tevent.getSize(),
						UNICAST_UPDATE, spkt.getEventID(), this.nid, tevent);
			 sawaMsg.setTarget_nid(spkt.getTarget_nid());
			 sawaMsg.setTarget_X(spkt.getTargetX());
			 sawaMsg.setTarget_Y(spkt.getTargetY());
			 sawaMsg.setTarget_Z(spkt.getTargetZ());
			 sawaMsg.setTarget_SeqNum(spkt.getTargetSeqNum());
			outboundMsgs.add(sawaMsg);
		} else {
			Port snrPort = (Port) getPort(SNR_PORT_ID
					+ (int) (spkt.getTargetNid() - first_target_nid));
			lastSeenSNR = spkt.getMaxSnr();
			lastSeenDataSize = spkt.getDataSize();
			totalTrackingPkts++;
			int TargetIndex = (int) (spkt.getTargetNid() - first_target_nid);
			//log("Tracking event with pkt:" + spkt.getTargetSeqNum() + " is:"
				//	+ tevent);
			globalRewardManager.dataArrived(totalExecutions, tevent);
			if (targets_LastSeqNum[TargetIndex] < spkt.getTargetSeqNum()) {
				double target_location[];
				target_location = new double[2];

				target_location[0] = round_digit(spkt.getTargetX() - X_shift, 4);
				target_location[1] = round_digit(spkt.getTargetY() - Y_shift, 4);
				double[] curr = CurrentTargetPositionTracker.getInstance()
						.getTargetPosition(spkt.getTargetNid());

				double currX = round_digit(curr[0], 4);
				double currY = round_digit(curr[1], 4);
				/*
				 * log(("At sink:X-"+target_location[0]+" Y-"+target_location[1])
				 * ); if(curr!=null) log(("Actual X-"+curr[0]+" Y-"+curr[1]));
				 * else log("Actual not found");
				 */
				snrPort.exportEvent(SNR_EVENT, target_location, null);
				double dist = Math.sqrt(Math.pow(Math.abs(target_location[0]
						- currX), 2)
						+ Math.pow(Math.abs(target_location[1] - currY), 2));
				CSVLogger.log("target", getTime() + "," + spkt.getMaxSnr()
						+ "," + target_location[0] + "," + target_location[1]
						+ "," + currX + "," + currY + "," + dist, true);
				/*
				 * snrPort.exportEvent(SNR_EVENT, target_location, null); //
				 * uncomment this line if you want to display the location of
				 * the target node.
				 */
				// snrPort.exportEvent(SNR_EVENT, new
				// DoubleObj((double)spkt.getMaxSnr()), null);
				targets_LastSeqNum[TargetIndex] = spkt.getTargetSeqNum();
			}

		}

	}


    public String info() {
        String info="Node:"+nid;
        if(noOfEventsMissed>0) info+=" noOfEventsMissed/totalEvents:"+noOfEventsMissed+"/"+totalEvents;
        if(noOfPktsDropped>0) info+=" noOfPktsDropped/totalPkts:"+noOfPktsDropped+"/"+totalPkts;
        info+=" tasks:"+taskList.toString();
        return info;
    }
    
    public void collectStats(){
        log("*******************STATS**************");
        //nid,noOfEventsMissed,totalEvents,noOfPktsDropped,totalPkts,task1Id,task1,task2Id,task2,task3Id,task3,totalCost,totalReward,trPackets
        String stats=nid+","+noOfEventsMissed+","+totalEvents+","+noOfPktsDropped+","
                    +totalPkts;
        String QValues=nid+",";
        String expPrices=nid+",";
        for(Integer i : taskList.keySet()){
            SensorTask task= (SensorTask)taskList.get(i);
            stats+=","+task.id+","+task.getNoOfExecutions();
            QValues+="task:"+task.getTaskId()+","+task.printQValues();
            expPrices+="task:"+task.getTaskId()+","+task.printExpPrices();
        }
        stats+=","+totalCost+","+totalReward+","+globalRewardManager.getEffectiveCost();
        if(totalTrackingPkts>0)
            stats+=","+totalTrackingPkts;
        CSVLogger.log("stats",stats);
        CSVLogger.log("Qvalues",QValues);
        CSVLogger.log("ExpPrices",expPrices);
        if(!globalLogged){
        	
        for(int i=0; i< globalRewardManager.getGlobalRewards().size();i++){
            CSVLogger.log("reward",""+globalRewardManager.getGlobalRewards().get(i),false);
        }
        
        log("GlobalRewardManager:"+globalRewardManager.stats());
        globalLogged=true;
        }
    }
    
    class MacroLearner{
    	Hashtable<Long, Tuple> recentTuples= new Hashtable<Long,Tuple>();
    	public void record(Tuple tuple){
    		//log("Adding tuple:"+tuple);
    		recentTuples.put(tuple.pktId, tuple);
    	}
    	public void handleWLReward(List<WLReward> wlrewards) {
			if(wlrewards==null) return;
    		for(WLReward reward : wlrewards){
				handleWLReward(reward.pktId,reward.reward);
			}
		}
		public void handleWLReward(long pktId, double reward){
    		Tuple tuple=recentTuples.get(pktId);
    		if(tuple==null){
    			//log("no tuple found for pktId:"+pktId);
    			return;
    		}
    		SensorTask task=taskList.get(tuple.taskId);
    		if(algorithm.equals("COIN") || algorithm.equals("TEAM")){
    			task.expectedPrice+=reward;
    		}
    		recentTuples.remove(pktId);
    	//	log("updating task:"+tuple.taskId+" with reward:"+reward);
    		//task.expectedPrices[tuple.stateId]+=reward;
    		//
    		//log("ExpPrices for task "+task.taskId+":"+task.printExpPrices());
    	}
    }
    
    class Tuple{
    	long pktId;
    	int stateId;
    	int taskId;
    	int timeStep;
    	Tuple(long pktId, int stateId, int taskId, int timestep){
    		this.pktId=pktId;
    		this.stateId=stateId;
    		this.taskId=taskId;
    		this.timeStep=timestep;
    	}
    	public String toString(){
    		return pktId+","+stateId+","+taskId+","+timeStep;
    	}
    }
}
