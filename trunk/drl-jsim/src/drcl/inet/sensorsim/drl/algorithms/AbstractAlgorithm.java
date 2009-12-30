package drcl.inet.sensorsim.drl.algorithms;

import java.util.Hashtable;
import java.util.logging.Level;

import drcl.inet.sensorsim.drl.IDRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.util.random.UniformDistribution;

public abstract class AbstractAlgorithm {
	
	public enum Algorithm{
    	DIRL, COIN, RANDOM, TEAM, SORA, ORACLE
    }
    
	//exploration factors for algorithms using exploration
	public static final double MAX_EPSILON=0.25;    // MAX exploration factor
	public static final double MIN_EPSILON=0.0;    // MIN exploration factor
	
	
	 protected UniformDistribution uniformDist;
	 Hashtable<Integer,SensorTask> taskList;
	 IDRLSensorApp app;
	 
	 
	 
	 protected AbstractAlgorithm(Hashtable<Integer,SensorTask> taskList, IDRLSensorApp app){
		 uniformDist= new UniformDistribution(0,taskList.size());
		 this.taskList=taskList;
		 this.app=app;
	 }
	 
	 public abstract SensorTask getNextTaskToExecute(SensorState currentState, SensorTask currentTask);
	 
	 public abstract Algorithm getAlgorithm();
	
	 protected SensorTask getRandomTaskToExecute(){
	    	SensorTask task=null;
	    	do{
	            int index = uniformDist.nextInt();
	            SensorTask[] tasks= taskList.values().toArray(new SensorTask[taskList.size()]);
	            if (index < taskList.size()) {
	                task = tasks[index]; //(SensorTask) taskList.get(index);
	                if (task != null && task.isAvailable()) {
	                	log(Level.FINEST,"using exploration:" + task);
	                    return task;
	                }
	            }
	        }while(task!=null);
	        return null;
	 }
	 
	 protected void log(Level level, String msg){
		 app.log(level,msg);
	 }
	
	 public void handleTaskUpdate(){
		 uniformDist.setMax(taskList.size());
	 }
	 
	 public static AbstractAlgorithm createInstance(Hashtable<Integer,SensorTask> taskList, IDRLSensorApp app){
		 Algorithm algo=Algorithm.valueOf(System.getProperty("algorithm", Algorithm.DIRL.toString()));
	       
		 if(algo.equals(Algorithm.DIRL)){
			 return new DIRLAlgorithm(taskList, app);
		 }else if(algo.equals(Algorithm.COIN)){
			 return new COINAlgorithm(taskList, app);
		 }else if(algo.equals(Algorithm.RANDOM)){
			 return new RANDOMAlgorithm(taskList, app);
		 }else if(algo.equals(Algorithm.TEAM)){
			 return new TEAMAlgorithm(taskList, app);
		 }else if(algo.equals(Algorithm.SORA)){
			 return new SORAAlgorithm(taskList, app);
		 } 
		 else throw new RuntimeException("No implementation found for alogrithm:"+algo);
	 }

	public abstract void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState);
	 
}
