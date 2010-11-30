package drcl.inet.sensorsim.drl.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import drcl.inet.sensorsim.drl.CSVLogger;
import drcl.inet.sensorsim.drl.DRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;

public class DIRLWoLFAlgorithm extends AbstractAlgorithm{
	Map<Integer,double[]> policy;
	Map<Integer,double[]> avgPolicy;
	int[] stateCount= new int[SensorState.MAX_STATES];
	double deltaW=0.5;
	double deltaL=0.9;
	double currentDelta=0;
	
	protected DIRLWoLFAlgorithm(Hashtable<Integer, SensorTask> taskList,
			DRLSensorApp app) {
		super(taskList, app);
		this.policy= new HashMap<Integer, double[]>();//SensorState.MAX_STATES][this.taskList.size()];
		this.avgPolicy= new HashMap<Integer, double[]>();
		for(int i=0;i<SensorState.MAX_STATES;i++){
			double[] values= new double[taskList.size()];
			for(int j=0;j<taskList.size();j++){
				values[j]= 1.0/taskList.size();
			}
			this.policy.put(i, values);
			this.avgPolicy.put(i, new double[taskList.size()]); //values.clone()); //
			this.stateCount[i]=0;			
		}
		//System.out.prinln("Policy:"+policy+" Avg Policy:"+avgPolicy);
	}
	 
	public SensorTask getNextTaskToExecute(SensorState currentState) {
		SensorTask nextTask=null;
		if (Math.random() < calcExplorationFactor()) { // exploration choosen
			nextTask = getRandomTaskToExecute();
		}else	
			nextTask = determineBestTaskToExecute(currentState);
		return nextTask;
	}

	 protected SensorTask determineBestTaskToExecute(SensorState currentState) {
	        double max=Double.NEGATIVE_INFINITY;
	        List<SensorTask> bestTasks=new ArrayList<SensorTask>();
	        double[] policyValues= policy.get(currentState.getStateId());
	        //choose best task as an available task with highest policy value for current state
	        for(int i=0;i<policyValues.length;i++){
	        	SensorTask task= this.taskList.get(i);
	        	if(task==null) continue;
	        	if(task.isAvailable()){
	                if(policyValues[i]>max){
	                    max=policyValues[i];
	                    bestTasks.clear();
	                    bestTasks.add(task);
	                }else if(policyValues[i]==max){
	                	bestTasks.add(task);
	                }
	            }
	        }
	        if(bestTasks.size()==1) return bestTasks.get(0);
	        int taskId= (int) (Math.random()*bestTasks.size());
	        return bestTasks.get(taskId);        
	    }
	 
	protected double calcExplorationFactor() {
	   double e=MIN_EPSILON+MAX_EPSILON*(SensorState.MAX_STATES-app.getNoOfStates())/SensorState.MAX_STATES;
	   return (e<MAX_EPSILON)?e:MAX_EPSILON;
	}

	@Override
	public Algorithm getAlgorithm() {
		return Algorithm.DIRLWoLF;
	}
	
	//@Override
	public String getStats(){
		String stats="[DIRLWoLF-Stats]";
		for(int i=0;i<app.getNoOfStates();i++){
			stats+="\n[State:"+i+",policy="+print(policy.get(i))+",avgPolicy="+print(avgPolicy.get(i));
		}
		return stats;
	}
	
	/**
	 * Called after a task finishes execution and system moves on to next state (currentState) from the prevState
	 */
	@Override
	public void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState) {
		if (currentTask != null) {
			currentTask.updateQValue(prevState,
					determineBestTaskToExecute(currentState).getQvalue(currentState));
			stateCount[prevState.getStateId()]+=1;
			double[] policyValues= policy.get(prevState.getStateId());
			double[] avgPolicyValues= avgPolicy.get(prevState.getStateId());
			for(int i=0;i<taskList.size();i++){
				avgPolicyValues[i]= avgPolicyValues[i] + (1.0/stateCount[prevState.getStateId()])*(policyValues[i] - avgPolicyValues[i]);
			}
			//set currentDelta
			boolean isWinning=false;
			if(calcPIQ(prevState, policyValues)>calcPIQ(prevState, avgPolicyValues)){
				isWinning=true;
				currentDelta=deltaW/(1+stateCount[prevState.getStateId()]);
			}else{
				currentDelta=deltaL/(1+stateCount[prevState.getStateId()]);
			}
			double delta=determineDelta(prevState,currentTask);
			policyValues[currentTask.getId()]= policyValues[currentTask.getId()] + delta;
			CSVLogger.log("Delta"+app.getNid(), ""+delta,false, Algorithm.DIRLWoLF);
			log(Level.FINE,"isWinning:"+isWinning+", ***task:"+currentTask.getTaskId()+" ***state:"+prevState.getStateId()+" ***policy="+print(policyValues)+" and ***avgPolicy="+print(avgPolicyValues)+" ***delta="+delta);
		}		
	}

	private String print(double[] values) {
		String s="";
		for(int i=0;i<values.length;i++){
			s+=values[i]+",";
		}
		return s;
	}

	private double determineDelta(SensorState s, SensorTask a){
		if(!hasMaxQValue(s,a)) return -determineSmallDelta(s,a);
		else{
			double sum=0;
			for(SensorTask t: this.taskList.values()){
				if(t.getId()!=a.getId()){
					double delta=determineSmallDelta(s, t);
					log(Level.FINE,"Task:"+t.getTaskId()+" Small Delta:"+delta);
					sum+= delta;
				}
			}
			return sum;
		}
	}
	
	//returns true if given task has highest Q value for given state 
	private boolean hasMaxQValue(SensorState s, SensorTask a) {
		double maxQ = Double.NEGATIVE_INFINITY;
		List<SensorTask> bestTasks = new ArrayList<SensorTask>();

		for (Iterator<SensorTask> it = taskList.values().iterator(); it.hasNext();) {
			SensorTask task = it.next();
			double utility = task.getQvalue(s);
			if (task.isAvailable()) {
				if (utility > maxQ) {
					maxQ = utility;
					bestTasks.clear();
					bestTasks.add(task);
				} else if (utility == maxQ) {
					bestTasks.add(task);
				}
			}
		}
		return bestTasks.contains(a);
	}

	private double determineSmallDelta(SensorState s, SensorTask a) {
		double PI= policy.get(s.getStateId())[a.getId()];
		return Math.min(PI, currentDelta/(taskList.size()-1.0));
	}

	private double calcPIQ(SensorState s, double[] policyValues) {
		double result=0;
		for(int i=0;i<policyValues.length;i++){
			result=result+ policyValues[i]*taskList.get(i).getQvalue(s);			
		}
		return result;
	}
}
