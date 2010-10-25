package drcl.inet.sensorsim.drl.diffext;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import drcl.comp.ACATimer;
import drcl.inet.mac.EnergyContract;
import drcl.inet.sensorsim.drl.EnergyStats;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm;
import drcl.inet.sensorsim.drl.diffext.DRLDiffApp.NodeState;

public class MicroLearner {
	public static final double TIMER_INTERVAL=5;
	public static final long RECENT_WINDOW=10; // TIMESTEPS
	private static final double HEART_BEAT_INTERVAL = 2*TIMER_INTERVAL;
	public static final double ENERGY_DIFFUSE=((2.45*0.001) + (5.97*0.001));	
	public static final double ENERGY_LISTEN=8.41*0.00001;
	public static final double ENERGY_SAMPLE=8.41*0.00001 + ENERGY_DIFFUSE;
	public static final double ENERGY_SLEEP=8.0*0.000001;
    
    public static final String TrackPortId=".track";
    public static final String ActualPosPortId=".actual";
    
    
    protected static final String DIFFUSE="Diffuse";
    protected static final String SLEEP="Sleep";
    
    protected Hashtable<Integer, SensorTask> taskList= new Hashtable<Integer,SensorTask>();
    
    ACATimer taskTimer;
    DRLDiffApp diffApp= null;
    
    public AbstractAlgorithm algorithm;
    List<DataPacket> outboundMsgs= new ArrayList<DataPacket>();
    
    SensorTask currentTask;
    SensorState currentState;
    MacroLearner mlearner= null;
    List<SensorState> states= new LinkedList<SensorState>();
    int noOfStates=0;
    double totalPrice=0;
    long lastDiffusionParticipation=-RECENT_WINDOW; 
    long lastSourceParticipation=-RECENT_WINDOW;
    int noOfPkts=0;
    int noOfSensedPkts=0;
    double initialEnergy;
    double lastReportedTime;
    int timesteps=0;
    double totalReward=0;
    double totalCost=0;
	
    public MicroLearner(DRLDiffApp app, MacroLearner mLearner){
    	this.mlearner=mLearner;
    	this.diffApp=app;
    	prepareTaskList();
        for(Integer i : taskList.keySet()){
            SensorTask task=taskList.get(i);
            totalPrice+=task.expectedPrice;                        
        }
        diffApp.addPort(TrackPortId,false);
        diffApp.addPort(ActualPosPortId,false); 
    }
    
    public void _start() {
    	this.algorithm=AbstractAlgorithm.createInstance(taskList, diffApp);
    	  
       if (diffApp.nid != diffApp.sink_nid) {
    	    initialEnergy = diffApp.getRemainingEnergy();
			currentTask= taskList.get(0);
			currentState=getMatchingState(false, false);
			taskTimer = diffApp.setTimeout("performTask", TIMER_INTERVAL);
		}else{
			initialEnergy=100;
			diffApp.log(Level.INFO,"********ALGORITHM:" + algorithm + "*******************");
			taskTimer = diffApp.setTimeout("reinforce", TIMER_INTERVAL);
		}			
	}
    
    protected void _stop()  {
		if (taskTimer != null)
	       diffApp.cancelTimeout(taskTimer);
	 }
    
    protected void _resume() {
        if(diffApp.nid!=diffApp.sink_nid)
        	taskTimer=diffApp.setTimeout("performTask", TIMER_INTERVAL);
    }
 
    public void timeout(Object data) {
		if (data.equals("performTask")) {
			double currEnergy=diffApp.getRemainingEnergy();
			if (currentTask != null) {
				currentTask.computeReward();
				totalReward += currentTask.lastReward;
				totalCost += currentTask.getLastCost();
				endTask(currentTask, currentState);
				diffApp.addToTotalCost(currentTask.getLastCost());
			}
			SensorState prevState = currentState;
			currentState = getMatchingState(noOfPkts>0, noOfSensedPkts>0);
			algorithm.reinforcement(currentTask, prevState, currentState);
			
			currentTask=algorithm.getNextTaskToExecute(currentState, currentTask);
			resetForNewTask();
			if (currentTask != null)
				currentTask.executeTask();
			if(currEnergy>0){ //stop timer if out of energy
				taskTimer = diffApp.setTimeout("performTask", TIMER_INTERVAL);
				++timesteps;
			}
		}else if (data.equals("reinforce")){ //reinforcement as used by sink 		
			//if(noOfPkts>0) mlearner.computeReinforcements();  //compute reinforcements for arriving data at this timestep
			diffApp.sendReinforcements();
			++timesteps;
			taskTimer = diffApp.setTimeout("reinforce", TIMER_INTERVAL);
		}
	}

	private void resetForNewTask() {
	}

	private void endTask(SensorTask task, SensorState state) {
		outboundMsgs.clear();
		double currEnergy;
		if (diffApp.nid != diffApp.sink_nid) {
			currEnergy = diffApp.getRemainingEnergy();
		}else
			currEnergy=100;
		EnergyStats.update((int) diffApp.nid, initialEnergy - currEnergy, currEnergy,
				currEnergy > 0, diffApp.getTime());
	
	}

    public SensorState getMatchingState(boolean successfulDiffusion, boolean successfulSample) {
    	successfulDiffusion=successfulDiffusion || (timesteps-lastDiffusionParticipation)<RECENT_WINDOW;
    	successfulSample=successfulSample || (timesteps-lastSourceParticipation)<RECENT_WINDOW;
        SensorState state= new DiffSensorState(successfulDiffusion,successfulSample);
        
        for (Iterator<SensorState> iter = states.iterator(); iter.hasNext();) {
            SensorState existingState = iter.next();
            if(existingState!=null && state.equals(existingState))
                return existingState;
        }
        
        // no match found, add this new state
        if (noOfStates < DiffSensorState.MAX_STATES) {
			state.setStateId(noOfStates);
			noOfStates++;
			states.add(state);
		}else{
			throw new RuntimeException("Reached max states..");
		}
        return (SensorState) states.get(0);
    }
    
	private synchronized void prepareTaskList() {
        taskList.put(0, new SensorTask(0,DIFFUSE,10*ENERGY_DIFFUSE) {
            public synchronized void execute() {
            	noOfPkts=0;
            	WakeUp();                
                
            }
            public synchronized double computeCost() {
            	double energyConsumed=ENERGY_LISTEN+ noOfPkts*ENERGY_DIFFUSE; 
            	return energyConsumed; //RX +TX
            }
            public synchronized double computePrice() {
            	return (lastDiffusionParticipation==timesteps)?expectedPrice:0;
            }
            public synchronized boolean isAvailable() {
              return true;  
            }
        });
       
        taskList.put(1, new SensorTask(1,SLEEP,0.00) {
            public void execute() {
                noOfPkts=0;
                GoToSleep();
            }

            public synchronized double computeCost() {
            	return ENERGY_SLEEP; 
            }
            public synchronized double computePrice() {
            	return (expectedPrice);
            }
            public synchronized boolean isAvailable() {
                return !diffApp.isExpectingInterestRefresh();  
            }
        });       
    }
    
    class ApplicationTask extends SensorTask{
    	InterestPacket interest=null;
    	
		public ApplicationTask(InterestPacket interest) {
			super(interest.getTaskId(), interest.getTaskId()+"-"+interest.getSinkId(), interest.getPayment());
			this.interest=interest;
		}
		public double computeCost() {
			return mlearner.calcCostOfParticipation(interest);
		}
		public double computePrice() {
			if(noOfSensedPkts==0 && diffApp.supportsHeartBeat()){
				if (diffApp.getTime() - lastSourceParticipation > HEART_BEAT_INTERVAL) {
					diffApp.log(Level.FINE, "Sending heart-beat");
					handleSensorEvent(diffApp.ConstructSensingEvent(null));
				}
			}
			return (noOfSensedPkts>0)?expectedPrice:0;
		}
		protected void execute() {
			noOfSensedPkts=0;
			WakeUp();                
     	}
		public boolean isAvailable() {
			return true;
		}
    	
    }
    /**
     * Set both Radio components in IDLE
     * so that they are ready for either receiving, sending, and/or
     * processing.
    */
    public void WakeUp()
    {
    	//if(diffApp.nodeState.equals(NodeState.AWAKE)) return;
    	
        //set the CPU to ACTIVE
      //  if (diffApp.cpuMode != 2) {
        	diffApp.setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
       // }
        //set the radio to IDLE
        //Contract type: SET_RADIO_MODE = 1 & Radio Modes:RADIO_IDLE=0
        EnergyContract.Message temp = (EnergyContract.Message)diffApp.wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 0));
        if (temp.getRadioMode() != 0) {
            System.out.println("Unable to turn radio back on to Idle mode. Its mode is: " + temp.getRadioMode());
        }
        diffApp.nodeState=NodeState.AWAKE;
        return;
    }
    
    /**
     *Sets both the CPU and Radio Components to sleep.
    */
    public void GoToSleep()
    {
    	//if(diffApp.nodeState.equals(NodeState.SLEEPING)) return;
        //set the CPU to sleep
      //  if (diffApp.cpuMode != 1) {
        	diffApp.setCPUMode(1);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        //}

        //Contract type: SET_RADIO_MODE = 1  &  Radio Modes: RADIO_SLEEP=1
        EnergyContract.Message temp = (EnergyContract.Message)diffApp.wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0,1));
        if (temp.getRadioMode() != 1) {
            System.out.println("Unable to radio to sleep. Its mode is: " + temp.getRadioMode());
        }
        diffApp.nodeState=NodeState.SLEEPING;
        return;
    }
    
	public int getNoOfPkts() {
		return noOfPkts;
	}

	public void setNoOfPkts(int noOfPkts) {
		this.noOfPkts = noOfPkts;
	}

	public int getNoOfSensedPkts() {
		return noOfSensedPkts;
	}

	public void setNoOfSensedPkts(int noOfSensedPkts) {
		this.noOfSensedPkts = noOfSensedPkts;
	}

	/**
	 * Handles incoming interest packet by checking if this node can act as a source for the interest or not..
	 * If it can be a source, creates an application task and adds to its list
	 * @param interestPkt
	 */
	public void handleInterestPkt(InterestPacket interestPkt) {
		int taskId=interestPkt.getTaskId();
		if(taskList.containsKey(taskId)){   //update task
			SensorTask task= taskList.get(taskId);
			task.expectedPrice=diffApp.interestCache.get(taskId).getMaxGradient().getPayment();
			//task.expectedPrice=interestPkt.getPayment();
			//return;  
		}
		lastDiffusionParticipation=timesteps;
		if(diffApp.canSatisfyInterest(interestPkt)){
			ApplicationTask newTask= new ApplicationTask(interestPkt);
			taskList.put(taskId, newTask);			
			algorithm.handleTaskUpdate();
			diffApp.numSubscriptions++;
		}		
		mlearner.interestArriveAtUpPort(interestPkt);
	}

	public String toString(){
		return "MicroLearner-"+diffApp.nid;
	}
	
	/**
	 * Handle reception of data packet from neighbor node, just increments noOfPkts count
	 * @param dataPkt
	 */
	public void handleDataPkt(DataPacket dataPkt) {
		noOfPkts++;
		if(diffApp.nid==diffApp.sink_nid){
			return;
		}
		lastDiffusionParticipation=timesteps;
		
		//filtering of data for same task for this timestep, 
		if(!shouldFilter(dataPkt)){
			outboundMsgs.add(dataPkt);
			dataPkt.addCostReward(diffApp.interestCache.get(dataPkt.getTaskId()).getMaxGradient().getPayment(), currentTask.getLastCost(),diffApp.nid); 
            mlearner.dataArriveAtUpPort(dataPkt); 
		}
		//Any other application specific data aggregation/filtering..
	}

	private boolean shouldFilter(DataPacket inData) {
		if(inData.isExplore())
			return false;
		for(DataPacket data:outboundMsgs){
			if(data.getTaskId()==inData.getTaskId() && data.getSinkId()==inData.getSinkId() && data.getGroupId()==inData.getGroupId() && data.getStreamId()==inData.getStreamId()){
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the payment (expected price) of a task if get a reinforcement for this node
	 * @param reinforcementPkt
	 */
	public void handleReinforcement(ReinforcementPacket reinforcementPkt) {
		if(reinforcementPkt.getDestinationId()==diffApp.nid){  //if this is destined to me
			lastDiffusionParticipation=timesteps;
			updateTaskExpectedPrice(reinforcementPkt);
		}		
	}

	void updateTaskExpectedPrice(ReinforcementPacket reinfPkt) {
		SensorTask task= taskList.get(reinfPkt.getTaskId());
		if(task!=null){
			//task.expectedPrice=reinforcementPkt.getPayment();
			//task.resetQValues();
			Double payment=reinfPkt.getStreamPayments().get(diffApp.nid);
			//if reinforcement has payment for my stream (where I am source), use that to update expected price else use max gradient
			if(payment==null)
				payment=diffApp.interestCache.get(reinfPkt.getTaskId()).getMaxGradient().getPayment();
			task.expectedPrice=(1-task.ALPHA)*task.expectedPrice+ task.ALPHA*payment;
			//task.expectedPrice=
		}		
	}

	/**
	 *  A sensor node that detects a target searches its interest cache for a matching interest entry.  
	 * construct the event as a list of attribute-value pairs. **/
	
	public void handleSensorEvent(List<Tuple> event) {
		if(diffApp.getRemainingEnergy()<=0) return;
		if(diffApp.numSubscriptions==0 || !(currentTask instanceof ApplicationTask)) return;
		
		InterestCacheEntry interestEntry = diffApp.getMatchingInterest(event) ;
		if ( interestEntry != null )	/* if there is a matching interest. */
		{
			InterestPacket interest= interestEntry.getInterest();
			//check if QoS constraints match or not
			if(!TupleUtils.isMatching(interest.getQosConstraints(), event)){
				return;
			}
			noOfSensedPkts++;
			lastSourceParticipation=timesteps;
			DataPacket dataPkt=new DataPacket(diffApp.nid,interest.getSinkId(),interest.getTaskId(),event, diffApp.getTime());
			long targetNid=(Long)TupleUtils.getAttributeValue(event, Tuple.TARGET_NID);
			String groupId=(String)TupleUtils.getAttributeValue(event, Tuple.GROUP_ID);
			dataPkt.setGroupId(groupId);
			if(targetNid<0){
				dataPkt.setHeartBeat(true);
				diffApp.noOfHeartBeats++;
			}else{ 
				diffApp.log(Level.FINE, "Received target packet as source");
				diffApp.noOfSrcPkts++;				
			}
			//filtering of data for same task for this timestep, 
			if(!shouldFilter(dataPkt)){
				outboundMsgs.add(dataPkt);
				dataPkt.addCostReward(currentTask.lastReward, currentTask.getLastCost(),diffApp.nid); 
	            mlearner.dataArriveAtUpPort(dataPkt); 
			}
		}		
	}

}
