/*
package drcl.inet.sensorsim;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import drcl.comp.ACATimer;
import drcl.comp.Port;
import drcl.data.DoubleObj;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract.Message;
import drcl.util.random.UniformDistribution;


*//**
 * @author Kunal
 *//*
public class DIRLSensorApp extends SensorApp implements drcl.comp.ActiveComponent{

    private static final long serialVersionUID = 1933018614040188069L;

    private static final double TIMER_INTERVAL =10;
    private static final double MAX_EPSILON=0.3;    // MAX exploration factor
    private static final double MIN_EPSILON=0.1;    // MIN exploration factor
    
    private static final String QPortId=".Qval";
    private static final String ExecutionsPortId=".executions";
    
    protected static final String SAMPLE="Sample";
    protected static final String RX="Rx";
    protected static final String TX="Tx";
    protected static final String SLEEP="Sleep";
    protected static final String AGGREGATE="Aggregate";
    
    
    protected List taskList= new LinkedList();
    
    ACATimer myTimer;
    
    //public static String algorithm="RANDOM";
    //public static String algorithm="STATIC";
    public static String algorithm="DIRL";
    //public static String algorithm="FIXED";
    //public static String algorithm="SORA";
    protected List outboundMsgs= new LinkedList();
    protected UniformDistribution uniformDist;
    protected Port batteryPort = addPort(".battery", false);
    protected Port energyPort = addPort(".energy",false);
    long destId=-1;
    protected SensorTask currentTask;
    protected SensorState currentState;
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
    String[] tasks;
    double[] betaValues= new double[5];     // for SORA
    static double[] globalRewards= new double[705];
        
    public DIRLSensorApp(){
        super();
        prepareTaskList();
        for (int i=0;i<taskList.size(); i++) {
            SensorTask task=(SensorTask)taskList.get(i);
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

    protected synchronized void timeout(Object data) {
        if(data.equals("performTask")){
            double currentEnergy=getRemainingEnergy();
            if(++totalExecutions%100==0)
                log("Current Energy:"+getRemainingEnergy());
            if (algorithm.equals("DIRL")) {
                SensorState prevState = currentState;
                currentState = getMatchingState(lastSeenSNR, destId >= 0,
                        noOfRx > 0, noOfSensedEvents > 0);
                // log("Current State:"+currentState);
                if (currentTask != null) {

                    // log("Energy spent:"+(lastMeasuredEnergy-currentEnergy)+"
                    // Task:"+currentTask);
                    currentTask
                            .computeReward((lastMeasuredEnergy - currentEnergy)
                                    / currentEnergy);
                    totalReward+=currentTask.lastReward;
                    currentTask.updateQValue(prevState,
                            determineBestTaskToExecute()
                                    .getQvalue(currentState));
                    lastMeasuredEnergy = currentEnergy;
                    energyPort.exportEvent("Energy Node:" + nid, new DoubleObj(
                            currentEnergy), null);
                }
                     * else{ currentState= new
                     * SensorState(lastSeenSNR,true,false); }
                     
                if (Math.random() < calcExplorationFactor()) { //exploration choosen
                    currentTask = getRandomTaskToExecute();
                } else {
                    currentTask = determineBestTaskToExecute();
                }
            }else {
                if(currentTask!=null){
                currentTask
                    .computeReward((lastMeasuredEnergy - currentEnergy)
                        / currentEnergy);
                totalReward+=currentTask.lastReward;
                }
                if(algorithm.equals("RANDOM")){
                    currentTask=getRandomTaskToExecute();
                }else if(algorithm.equals("STATIC")){
                    currentTask=getNextStaticTaskToExecute(currentTask!=null?currentTask.getTaskId():null);
                }else if(algorithm.equals("FIXED")){
                    currentTask=getNextFixedTaskToExecute(currentTask!=null?currentTask.getTaskId():null);
                }else if(algorithm.equals("SORA")){
                    if(currentTask!=null){
                        if(currentTask.lastReward>=0){
                            betaValues[currentTask.id]=0.2+0.8*betaValues[currentTask.id];
                        }else{
                            betaValues[currentTask.id]=0.8*betaValues[currentTask.id];
                        }
                    }
                    if (Math.random() <= 0.05) { //exploration choosen
                        currentTask = getRandomTaskToExecute();
                    } else {
                        currentTask=getBestSORATaskToExecute();
                    }
                }
            }
            exportvalues();
            lastMeasuredEnergy = currentEnergy;
            //log("Executing task:"+currentTask);
            if(currentTask!=null)
                currentTask.executeTask();
            myTimer=setTimeout("performTask", TIMER_INTERVAL);
        }
    }
    
    private SensorTask getNextFixedTaskToExecute(String task) {
        if(tasks==null || tasks.length==0)
            return null;
        if(task==null){
            return (SensorTask)getTask(tasks[0]);
        }else{
            for(int i=0;i<tasks.length;i++){
                if(task.equals(tasks[i])){
                    SensorTask nextTask=getTask(tasks[(i+1)%tasks.length]);
                    if(nextTask.isAvailable())
                        return nextTask;
                    else{
                        return getNextFixedTaskToExecute(nextTask.getTaskId());
                    }
                }
            }
        }
        return getTask(tasks[0]);
    }

    public SensorState getMatchingState(double lastSeenSNR, boolean hasNeighbours, boolean successfulRx, boolean successfulSample) {
        SensorState state= new SensorState(lastSeenSNR,hasNeighbours,successfulSample,successfulRx, nid);
        for (Iterator iter = states.iterator(); iter.hasNext();) {
            SensorState existingState = (SensorState) iter.next();
            if(existingState!=null && state.equals(existingState))
                return existingState;
        }
        
        // no match found, add this new state
        state.stateId=noOfStates;
        noOfStates++;
        states.add(state);
        return state;
    }
    

    private SensorTask getNextStaticTaskToExecute(String task){
        if(task==null){
            return (SensorTask)taskList.get((int)(new Random().nextInt(taskList.size())));
        }
        SensorTask nextTask=null;
        if(task.equals(SAMPLE))
            nextTask=getTask(TX);
        if(task.equals(AGGREGATE))
            nextTask= getTask(SAMPLE);
        if(task.equals(TX))
            nextTask= getTask(RX);
        if(task.equals(RX))
            nextTask= getTask(SLEEP);
        if(task.equals(SLEEP))
            nextTask= getTask(SAMPLE);
        
        if(nextTask.isAvailable()) return nextTask;
        
        else return getNextStaticTaskToExecute(nextTask.taskId);
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
        for (int i = 0; i < taskList.size(); i++) {
            int index = uniformDist.nextInt();
            if (index < taskList.size()) {
                SensorTask task = (SensorTask) taskList.get(index);
                if (task != null && task.isAvailable()) {
                    //log("using exploration:" + task);
                    return (SensorTask) taskList.get(index);
                }
            }
        }
        return null;
    }
    
    private SensorTask determineBestTaskToExecute() {
        double maxQ=Double.NEGATIVE_INFINITY;
        SensorTask bestTask=null;
        
        for(Iterator it=taskList.iterator();it.hasNext();){
            SensorTask task= (SensorTask)it.next();
            double utility= task.getQvalue(currentState);//*task.getExpectedPrice();
            if(task.isAvailable()){
                if(utility>maxQ){
                    maxQ=utility;
                    bestTask=task;
                }
            }
        }
        return bestTask;        
    }

    private SensorTask getBestSORATaskToExecute() {
        double maxQ=Double.NEGATIVE_INFINITY;
        SensorTask bestTask=null;
        for(Iterator it=taskList.iterator();it.hasNext();){
            SensorTask task= (SensorTask)it.next();
            double utility= betaValues[task.id]*task.getExpectedPrice();
            if(task.isAvailable()){
                if(utility>maxQ){
                    maxQ=utility;
                    bestTask=task;
                }
            }
        }
        return bestTask;
        
    }
    private double calcExplorationFactor() {
        double e=MIN_EPSILON+0.25*(SensorState.MAX_STATES-noOfStates)/SensorState.MAX_STATES;
        return (e<MAX_EPSILON)?e:MAX_EPSILON;
    }

    public double getRemainingEnergy(){
        double energy=BatteryContract.getRemainingEnergy(batteryPort);
        if(energy<=0)
            throw new RuntimeException("Out of energy..");
        return energy;
    }

    private void log(String string) {
        System.out.println(getTime()+"[Node:"+nid+"] "+string);        
    }

    private void exportvalues(){
        String executions="";
        for (int i=0;i<taskList.size(); i++) {
            SensorTask element = (SensorTask) taskList.get(i);
            //Port port=getPort(QPortId+i);
            //port.exportEvent("Q Values Node:"+nid, new DoubleObj((double)element.getQvalue(currentState)), null);
            Port execPort=getPort(ExecutionsPortId+i);
            execPort.exportEvent("No. Of Executions Node:"+nid, new DoubleObj((double)element.getNoOfExecutions()), null);
            
            executions+=element.getNoOfExecutions()+",";
        }
        CSVLogger.log("executions-"+nid,executions,false);
        double avgRew=(totalReward/totalExecutions);
        synchronized(DIRLSensorApp.class){
            globalRewards[totalExecutions]+=avgRew;
        }
        CSVLogger.log("reward-"+nid,new Double(avgRew).toString(),false);
    }
    
    class TrackingEvent{
        double energySpent;
        int aggregatedSamples;       
    }
    
    private synchronized void prepareTaskList() {
        taskList.add(new SensorTask(0,AGGREGATE, 0.2) {
            public void execute() {
                setCPUMode(CPUBase.CPU_ACTIVE);
                setRadioMode(RadioBase.RADIO_OFF);
                noOfSamplesAggregated=0;
                //simple aggregation function where aggregated value is last value received
                if(destId==-1){
                    return; //no destination node present
                }
                noOfSamplesAggregated=outboundMsgs.size();
                if(noOfSamplesAggregated>1){
                    Message lastMessage=(Message)outboundMsgs.get(outboundMsgs.size()-1);
                    if(lastMessage.body==null || (!(lastMessage.body instanceof TrackingEvent))){
                        lastMessage.body= new TrackingEvent();
                    }
                    ((TrackingEvent)lastMessage.body).aggregatedSamples=noOfSamplesAggregated;
                    if(lastMessage!=null)
                        downPort.doSending(lastMessage);
                    outboundMsgs.clear();
                    outboundMsgs.add(lastMessage);
                }
                
            }
            public void computeReward(double energySpent) {
                this.lastReward=noOfSamplesAggregated*(expectedPrice)-(energySpent);
            }
            public synchronized boolean isAvailable() {
                return outboundMsgs.size()>0 && ((getTime()-lastReportedTime)<TIMER_INTERVAL*50);  
            }
        });
        
        taskList.add(new SensorTask(1,TX,0.1) {
            public synchronized void execute() {
                setCPUMode(CPUBase.CPU_ACTIVE);
                setRadioMode(RadioBase.RADIO_TRANSMIT);
                noOfTx=0;
                if(destId==-1){
                    return; //no destination node present
                }
                for (Iterator iter = outboundMsgs.iterator(); iter.hasNext();) {
                    noOfTx++;
                    Object element = iter.next();
                    downPort.doSending(element);
                    iter.remove();
                }
            }
            public synchronized void computeReward(double energySpent) {
                this.lastReward=noOfTx*(expectedPrice)-(energySpent);
            }
            public synchronized boolean isAvailable() {
              return (outboundMsgs.size()>0);  
            }
        });
        
        taskList.add(new SensorTask(2,RX,0.2) {         
            public synchronized void execute() {
                setCPUMode(CPUBase.CPU_ACTIVE);
                setRadioMode(RadioBase.RADIO_RECEIVE);
                noOfRx=0;                
            }
            public synchronized void computeReward(double energySpent) {
                this.lastReward=noOfRx*(expectedPrice)-(energySpent);
            }
            public synchronized boolean isAvailable() {
              return true;  
            }
        });
        
        taskList.add(new SensorTask(3,SAMPLE, 0.05) {
            public void execute() {
                setCPUMode(CPUBase.CPU_ACTIVE);
                setRadioMode(RadioBase.RADIO_OFF);
                noOfSensedEvents=0;
            }
            public void computeReward(double energySpent) {
                this.lastReward=noOfSensedEvents*(expectedPrice)-(energySpent);
            }
            public synchronized boolean isAvailable() {
                return true;  
            }
        });
        taskList.add(new SensorTask(4,SLEEP,0.001) {
            public void execute() {
                noOfRx=0; noOfSensedEvents=0;
                setCPUMode(CPUBase.CPU_OFF);
                setRadioMode(RadioBase.RADIO_OFF);
            }
            public void computeReward(double energySpent) {
               this.lastReward=(expectedPrice)-(energySpent);
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
            return;
        }
        lastSeenDataSize = msg.getDataSize();
        long target_nid = msg.getTargetNid();
        double target_X = msg.getTargetX() ;
        double target_Y = msg.getTargetY() ;
        double target_Z = msg.getTargetZ() ;
        int target_SeqNum = msg.getTargetSeqNum() ;
        
        noOfSensedEvents++;
        log("Received sensed event:"+noOfSensedEvents+" SNR:"+lastSeenSNR);
        if ( nid == sink_nid )
        {
            Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(target_nid - first_target_nid));
            if ( snrPort != null )
                if ( snrPort.anyOutConnection() )
                    snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)lastSeenSNR), null);
        }

        if ( nid != sink_nid )
        {
            if(msg.getBody()==null){
                msg.body= new TrackingEvent();
            }
            outboundMsgs.add(new SensorAppWirelessAgentContract.Message(
                    SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, destId, lastSeenDataSize,
                    COHERENT, lastSeenSNR, eID, target_nid, target_X, target_Y, target_Z, target_SeqNum, msg.getBody())) ;
            eID = eID + 1 ;
        }      
    }


    // receive over wireless channel
    protected synchronized void recvSensorPacket(Object data_) {
        totalPkts++;
        if((nid!=sink_nid) && (currentTask==null || !currentTask.getTaskId().equals(RX))){
            log("Dropped pkt..,currently executing:"+currentTask.getTaskId());
            noOfPktsDropped++;
            return;
        }
        SensorPacket spkt = (SensorPacket) data_;
        noOfRx++;
        log("Received sensor packet no:"+noOfRx+" data:"+spkt);
        if ((spkt.pktType == COHERENT) || (spkt.pktType == NON_COHERENT)) {
            if (nid != sink_nid) {
                outboundMsgs.add(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, 
                        destId, spkt.getDataSize(),spkt.pktType, spkt.getMaxSnr(), eID, 
                        spkt.getTargetNid(),spkt.getTargetX(),spkt.getTargetY(),spkt.getTargetZ(),
                        spkt.getTargetSeqNum(),spkt.getBody())) ;
                eID++;
            } else {
                Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(spkt.getTargetNid() - first_target_nid));
                lastSeenSNR = spkt.getMaxSnr();
                lastSeenDataSize = spkt.getDataSize();
                if(spkt.getBody()!=null && spkt.getBody() instanceof TrackingEvent)
                    totalTrackingPkts+=((TrackingEvent)spkt.getBody()).aggregatedSamples;
                else
                    totalTrackingPkts++;
                int TargetIndex = (int) (spkt.getTargetNid() - first_target_nid);
                if (targets_LastSeqNum[TargetIndex] < spkt
                          .getTargetSeqNum()) {
                    double target_location[];
                            target_location = new double[2];
                            
                            target_location[0] = round_digit(spkt.getTargetX()
                                    - X_shift, 4);
                            target_location[1] = round_digit(spkt.getTargetY()
                                    - Y_shift, 4);
                            double[] curr=CurrentTargetPositionTracker.getInstance().getTargetPosition(spkt.getTargetNid());
                            double currX= round_digit(curr[0],4);
                            double currY= round_digit(curr[1], 4);
                            log(("At sink:X-"+target_location[0]+" Y-"+target_location[1]));
                            if(curr!=null)
                                log(("Actual X-"+curr[0]+" Y-"+curr[1]));
                            else
                                log("Actual not found");
                            
                            snrPort.exportEvent(SNR_EVENT, target_location, null);
                            double dist= Math.sqrt(Math.pow(Math.abs(target_location[0]-currX),2)+
                                            Math.pow(Math.abs(target_location[1]-currY),2));
                            CSVLogger.log("target",getTime()+","+target_location[0]+","+target_location[1]
                                               +","+currX+","+currY+","+dist);
                            snrPort.exportEvent(SNR_EVENT, target_location,
                                    null); // uncomment this line if you want to display the location of the target node. 

                            //snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)spkt.getMaxSnr()), null);
                            targets_LastSeqNum[TargetIndex] = spkt
                                    .getTargetSeqNum();
                        }
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
        String stats=nid+","+noOfEventsMissed+","+totalEvents+","+noOfPktsDropped+","
                    +totalPkts;
        String QValues=nid+",";
        for(int i=0; i< taskList.size();i++){
            SensorTask task= (SensorTask)taskList.get(i);
            stats+=","+task.getNoOfExecutions();
            QValues+="task:"+task.getTaskId()+","+task.printQValues();
        }
        stats+=","+(initialEnergy-lastMeasuredEnergy)+","+totalReward;
        if(totalTrackingPkts>0)
            stats+=","+totalTrackingPkts;
        CSVLogger.log("stats",stats);
        CSVLogger.log("Qvalues",QValues);
        for(int i=0; i< globalRewards.length;i++){
            CSVLogger.log("reward",""+globalRewards[i],false);
        }
    }
}
*/