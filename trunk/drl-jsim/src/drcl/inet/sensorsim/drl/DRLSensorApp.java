
package drcl.inet.sensorsim.drl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import drcl.comp.ACATimer;
import drcl.comp.Port;
import drcl.data.DoubleObj;
import drcl.data.XYData;
import drcl.inet.mac.EnergyContract;
import drcl.inet.sensorsim.CPUBase;
import drcl.inet.sensorsim.CurrentTargetPositionTracker;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.drl.GlobalRewardManager.WLReward;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm.Algorithm;
import drcl.util.random.UniformDistribution;


/*
 * @author Kunal
 */

public class DRLSensorApp extends SensorApp implements drcl.comp.ActiveComponent, IDRLSensorApp{

    private static final long serialVersionUID = 1933018614040188069L;

    public static Logger log= Logger.global;
    private static final double TIMER_INTERVAL =10;
    private static final double MANAGE_REWARD_INTERVAL=10;
    private static final double ENERGY_SAMPLE=8.41*0.00001;
    private static final double ENERGY_ROUTE=((2.45*0.001) + (5.97*0.001));	
    private static final double ENERGY_SLEEP=8.0*0.000001;
    
    public static final String TrackPortId=".track";
    public static final String ActualPosPortId=".actual";
    
    private static final String QPortId=".Qval";
    private static final String ExecutionsPortId=".executions";
    
    protected static final String SAMPLE="Sample";
    protected static final String ROUTE="Route";
    protected static final String SLEEP="Sleep";
    
    protected Hashtable<Integer,SensorTask> taskList= new Hashtable<Integer,SensorTask>();
    
    ACATimer myTimer;
    
    public AbstractAlgorithm algorithm;
    protected List<SensorAppWirelessAgentContract.Message> outboundMsgs= new LinkedList<SensorAppWirelessAgentContract.Message>();
    protected Port batteryPort = addPort(".battery", false);
    protected Port energyPort = addPort(".energy",false);
    long destId=-1;
    protected SensorTask currentTask;
    protected DIRLSensorState currentState;
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
    double currentEnergy;
    double lastReportedTime;
    int totalExecutions=0;
    double totalReward=0;
    double totalCost=0;
    String[] tasks;
    GlobalRewardManager globalRewardManager;
    int noOfNodes=0;
    //List<Double> globalRewards= new ArrayList<Double>(500);
    
	public int getNoOfNodes() {
		return noOfNodes;
	}

	public void setNoOfNodes(int noOfNodes) {
		this.noOfNodes = noOfNodes;
		CSVLogger.noOfNodes=noOfNodes;
	}

	private long currStream=-1;

	private static boolean globalLogged=false;
        
    public DRLSensorApp() throws Exception{
        super();
        globalRewardManager= Factory.getGlobalRewardManagerInstance();
       
        prepareTaskList();
        for(Integer i : taskList.keySet()){
            SensorTask task=taskList.get(i);
            totalPrice+=task.expectedPrice;
            addPort(QPortId+i,false);           
            addPort(ExecutionsPortId+i,false);            
        }
        addPort(TrackPortId,false);
        addPort(ActualPosPortId,false);        
    }
    
    public void setDestId(long destid){
        this.destId=destid;
    }
    
    public void setTasks(String[] tasks){
        this.tasks=tasks;
    }
   
    protected void _start() {
    	this.algorithm=AbstractAlgorithm.createInstance(taskList, this);
    	
        //lastMeasuredEnergy = 
		myTimer = setTimeout("manageReward", MANAGE_REWARD_INTERVAL);
		if (nid != sink_nid) {
			currentEnergy = getRemainingEnergy();
			myTimer = setTimeout("performTask", TIMER_INTERVAL);
		}else{
			log(Level.INFO,"********ALGORITHM:" + algorithm + "*******************");
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
			if (currentTask != null) {
				currentTask.computeReward();
				totalReward += currentTask.lastReward;
				totalCost += currentTask.lastCost;
				endTask(currentTask, currentState);
				globalRewardManager.addToTotalCost(currentTask.lastCost);
			}
			DIRLSensorState prevState = currentState;
			currentState = getMatchingState(lastSeenSNR, destId >= 0,
					noOfRx > 0, noOfSensedEvents > 0);
			algorithm.reinforcement(currentTask, prevState, currentState);
			
			exportvalues();
			
			currentTask=algorithm.getNextTaskToExecute(currentState);
			resetForNewTask();
			if (currentTask != null)
				currentTask.executeTask();
			myTimer = setTimeout("performTask", TIMER_INTERVAL);
			
		} else if (data.equals("manageReward")) {
			setTimeout("manageReward", MANAGE_REWARD_INTERVAL);
			if (nid == sink_nid) {
				globalRewardManager.manage(totalExecutions,algorithm.getAlgorithm());
			}else{
			// update to macro-learners
				List<WLReward> wlrewards = globalRewardManager
					.getPendingRewards(nid);
				mlearner.handleWLReward(wlrewards);
			}
		}
		++totalExecutions;
	}

	private void endTask(SensorTask task, DIRLSensorState state) {
	 	//setCPUMode(CPUBase.CPU_ACTIVE);
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



    public DIRLSensorState getMatchingState(double lastSeenSNR, boolean hasNeighbours, boolean successfulRx, boolean successfulSample) {
        DIRLSensorState state= new DIRLSensorState(lastSeenSNR,hasNeighbours,successfulSample,successfulRx,currStream);
        for (Iterator iter = states.iterator(); iter.hasNext();) {
            DIRLSensorState existingState = (DIRLSensorState) iter.next();
            if(existingState!=null && state.equals(existingState))
                return existingState;
        }
        
        // no match found, add this new state
        if (noOfStates < SensorState.MAX_STATES) {
			state.stateId = noOfStates;
			noOfStates++;
			states.add(state);
		}else{
			throw new RuntimeException("Reached max states..");
		}
        return (DIRLSensorState) states.get(0);
    }
    

  
    
    private SensorTask getTask(String taskId){
        for (int i = 0; i < taskList.size(); i++) {
            SensorTask task = (SensorTask) taskList.get(i);
            if(task.taskId.equals(taskId))
                return task;
        }
        return null;
    }
   
    public double getRemainingEnergy(){
    	if(nid!=sink_nid){
    	double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();
        if(energy<=0)
            throw new RuntimeException("Out of energy..");
        return energy;
    	}else
    		return Integer.MAX_VALUE;
    	
    }

    public void log(Level level, String string) {
        log.log(level,getTime()+"[Node:"+nid+"]["+algorithm.getAlgorithm()+"] "+string);        
    }

    private void exportvalues(){
        /*String executions="";
        for (Integer i : taskList.keySet()) {
            SensorTask element = (SensorTask) taskList.get(i);
            Port execPort=getPort(ExecutionsPortId+i);
            execPort.exportEvent("No. Of Executions Node:"+nid, new DoubleObj((double)element.getNoOfExecutions()), null);
            executions+=element.getNoOfExecutions()+",";
        }
        CSVLogger.log("executions-"+nid,executions,false,algorithm.getAlgorithm());
        double avgRew=(totalReward/totalExecutions);
        CSVLogger.log("reward-"+nid,new Double(avgRew).toString(),false,algorithm.getAlgorithm());*/        
    }
    
  /*  *//**
     *Sets both the CPU and Radio Components to sleep.
    *//*
    public void GoToSleep()
    {
        //set the CPU to sleep
        if (this.cpuMode != 1) {
            this.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        }

        //Contract type: SET_RADIO_MODE = 1  &  Radio Modes: RADIO_SLEEP=1
        EnergyContract.Message temp = (EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0,1));
        if (temp.getRadioMode() != 1) {
            System.out.println("Unable to radio to sleep. Its mode is: " + temp.getRadioMode());
        }
        return;
    }

    *//**
     * Set both Radio components in IDLE
     * so that they are ready for either receiving, sending, and/or
     * processing.
    *//*
    public void WakeUp()
    {
        //set the CPU to ACTIVE
        //if (this.cpuMode != 2) {
        //    this.setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        //}
        //set the radio to IDLE
        //Contract type: SET_RADIO_MODE = 1 & Radio Modes:RADIO_IDLE=0
        EnergyContract.Message temp = (EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 0));
        if (temp.getRadioMode() != 0) {
            System.out.println("Unable to turn radio back on to Idle mode. Its mode is: " + temp.getRadioMode());
        }
        return;
    }*/
    class TrackingEvent{
        long streamId;
        List<Long> nodes= new ArrayList<Long>();   
        double reward=0;
        double cost=0;
        double snr;
        long pktId;
        long targetNid;
        double[] targetLocation;
        
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
		public void setTarget(long targetNid, double[] target_location) {
			this.targetNid=targetNid;
			this.targetLocation=target_location;
		}
    }
    
    private synchronized void prepareTaskList() {
        taskList.put(0, new SensorTask(0,ROUTE,10*ENERGY_ROUTE) {
            public synchronized void execute() {
            //	WakeUp();
                noOfRx=0;
                noOfSensedEvents=0;
                currentEnergy=currentEnergy-ENERGY_ROUTE;
            }
            public synchronized double computeCost() {
            	return ENERGY_ROUTE/currentEnergy; //RX +TX
            }
            public synchronized double computePrice() {
            	/*return (noOfRx>0)?expectedPrices[currentState.stateId]:0;*/
            	return (noOfRx>0)?expectedPrice:0;
            }
            public synchronized boolean isAvailable() {
              return true;  
            }
        });
        
        taskList.put(1, new SensorTask(1,SAMPLE, 10*ENERGY_SAMPLE) {
            public void execute() {
              //  setCPUMode(CPUBase.CPU_ACTIVE);
            	noOfRx=0;
                noOfSensedEvents=0;
                currentEnergy=currentEnergy-ENERGY_SAMPLE;
            }
            public synchronized double computeCost() {
            	return ENERGY_SAMPLE/currentEnergy; 
            }
            public synchronized double computePrice() {
            	//return (noOfSensedEvents>0)?(expectedPrices[currentState.stateId]):0;
            	return (noOfSensedEvents>0)?(expectedPrice):0;
            }
            public synchronized boolean isAvailable() {
                return true;  
            }
        });
        taskList.put(2, new SensorTask(2,SLEEP,0.00) {
            public void execute() {
                noOfRx=0; noOfSensedEvents=0;
                //GoToSleep();
                currentEnergy=currentEnergy-ENERGY_SLEEP;
            }

            public synchronized double computeCost() {
            	return ENERGY_SLEEP/currentEnergy; 
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
        if(lastSeenSNR*DIRLSensorState.SNR_WEIGHT<1 || destId==-1) // snr not strong enough or no destination set
            return;
        totalEvents++;
        if((nid!=sink_nid) && (currentTask==null || !currentTask.getTaskId().equals(SAMPLE))){
            noOfEventsMissed++;
            if(currentTask!=null)
            //log("Sensor event missed, executing task:"+currentTask);
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
			double[] target_location = new double[2];
			target_location[0] = round_digit(msg.getTargetX() - X_shift, 4);
			target_location[1] = round_digit(msg.getTargetY() - Y_shift, 4);
			te.setTarget(msg.getTargetNid(),target_location);
	         
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
			log(Level.FINE,"Dropped pkt..,currently executing:" + currentTask);
			noOfPktsDropped++;
			return;
		}
		SensorPacket spkt = (SensorPacket) ((SensorPacket) data_).clone();
		noOfRx++;
		TrackingEvent tevent = (TrackingEvent) spkt.getBody();
		log(Level.FINER,"Received sensor packet data:" + tevent);

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
			long targetNid= spkt.getTargetNid();
			Port snrPort = (Port) getPort(SNR_PORT_ID
					+ (int) (targetNid - first_target_nid));
			lastSeenSNR = spkt.getMaxSnr();
			lastSeenDataSize = spkt.getDataSize();
			totalTrackingPkts++;
			int TargetIndex = (int) (spkt.getTargetNid() - first_target_nid);
			log(Level.INFO,"Tracking event with pkt:" + spkt.getTargetSeqNum() + " is:"+ tevent);
			globalRewardManager.dataArrived(totalExecutions, tevent);
			if (targets_LastSeqNum[TargetIndex] < spkt.getTargetSeqNum()) {
				double target_location[];
				target_location = new double[2];

				target_location[0] = round_digit(spkt.getTargetX() - X_shift, 4);
				target_location[1] = round_digit(spkt.getTargetY() - Y_shift, 4);
				double[] curr = CurrentTargetPositionTracker.getInstance()
						.getTargetPosition(spkt.getTargetNid());

				/*double currX = round_digit(curr[0], 4);
				double currY = round_digit(curr[1], 4);
			*/	/*
				 * log(("At sink:X-"+target_location[0]+" Y-"+target_location[1])
				 * ); if(curr!=null) log(("Actual X-"+curr[0]+" Y-"+curr[1]));
				 * else log("Actual not found");
				 */
				snrPort.exportEvent(SNR_EVENT, target_location, null);
				/*Port trackPort=getPort(TrackPortId);
				trackPort.exportEvent("Target:"+targetNid, new XYData(targetNid,target_location[0],target_location[1]),"Track");
				Port actualPort=getPort(ActualPosPortId);
				actualPort.exportEvent("Target:"+targetNid, new XYData(targetNid,currX,currY),"Track");
				*/
				/*double dist = Math.sqrt(Math.pow(Math.abs(target_location[0]
						- currX), 2)
						+ Math.pow(Math.abs(target_location[1] - currY), 2));
				CSVLogger.log("target"+targetNid, getTime() + "," + spkt.getMaxSnr()
						+ "," + target_location[0] + "," + target_location[1]
						+ "," + currX + "," + currY + "," + dist, true,algorithm.getAlgorithm());*/
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
        log(Level.INFO,"*******************STATS**************");
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
        CSVLogger.log("stats",stats,algorithm.getAlgorithm());
        CSVLogger.log("Qvalues",QValues,algorithm.getAlgorithm());
        CSVLogger.log("ExpPrices",expPrices,algorithm.getAlgorithm());
        if(!globalLogged){
        	
        for(int i=0; i< globalRewardManager.getGlobalRewards().size();i++){
            CSVLogger.log("reward",""+globalRewardManager.getGlobalRewards().get(i),false,algorithm.getAlgorithm());            
        }
        CSVLogger.logGlobal("global-stats",globalRewardManager.stats(),algorithm.getAlgorithm());
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
			if(wlrewards!=null){
    		for(WLReward reward : wlrewards){
				handleWLReward(reward.pktId,reward.reward);
			}
			}
			//if no WL reward received within 2*MANAGE_INERVAL, consider 0 reward
			/*for(Iterator<Tuple> it=recentTuples.values().iterator();it.hasNext();){
				Tuple tuple=it.next();
				if((getTime()-tuple.timeStep)>2*MANAGE_REWARD_INTERVAL){
					updateExpPrice(tuple, 0);
					it.remove();
				}
			}*/
    		
    		
		}
		public void handleWLReward(long pktId, double reward){
    		Tuple tuple=recentTuples.get(pktId);
    		if(tuple==null){
    			//log("no tuple found for pktId:"+pktId);
    			return;
    		}
    		updateExpPrice(tuple, reward);
    		recentTuples.remove(tuple.pktId);
    	}
		
		private void updateExpPrice(Tuple tuple, double reward){
			SensorTask task=taskList.get(tuple.taskId);
			if(algorithm.getAlgorithm().equals(Algorithm.COIN) || algorithm.getAlgorithm().equals(Algorithm.TEAM)){
    			task.expectedPrice=(1-task.ALPHA)*task.expectedPrice+ task.ALPHA*reward;
    			/*for(int i=0;i<task.Qvalues.length;i++)
    				task.Qvalues[i]=0;*/
    		}
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

	public int getNoOfStates() {
		return noOfStates;
	}
}
