package drcl.inet.sensorsim.drl.algorithms;

import java.util.Hashtable;
import java.util.logging.Level;

import drcl.inet.sensorsim.drl.DRLSensorApp;
import drcl.inet.sensorsim.drl.SensorState;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.util.random.UniformDistribution;

public abstract class AbstractAlgorithm {
	
	public enum Algorithm{
    	DIRL, COIN, RANDOM, TEAM, SORA, ORACLE, DIRLWoLF, SIMPLE
    }
    
	//exploration factors for algorithms using exploration
	public static final double MAX_EPSILON=0.3;    // MAX exploration factor
	public static final double MIN_EPSILON=0.05;    // MIN exploration factor
	
	
	 protected UniformDistribution uniformDist;
	 Hashtable<Integer,SensorTask> taskList;
	 DRLSensorApp app;
	 
	 
	 
	 protected AbstractAlgorithm(Hashtable<Integer,SensorTask> taskList, DRLSensorApp app){
		 long seed=(long)Math.random()*100;
		 if(System.getProperty("mobility.seed")!=null){
			 seed= Long.parseLong(System.getProperty("mobility.seed"));
		 }
		 uniformDist= new UniformDistribution(0,taskList.size(),seed);
		 this.taskList=taskList;
		 this.app=app;
	 }
	 
	 public abstract SensorTask getNextTaskToExecute(SensorState currentState);
	 
	 public abstract Algorithm getAlgorithm();
	
	 protected SensorTask getRandomTaskToExecute(){
	    	SensorTask task=null;
	    	do{
	            int index = uniformDist.nextInt();
	            if (index < taskList.size()) {
	                task = (SensorTask) taskList.get(index);
	                if (task != null && task.isAvailable()) {
	                    log(Level.FINEST,"using exploration:" + task);
	                    return (SensorTask) taskList.get(index);
	                }
	            }
	        }while(task!=null);
	        return null;
	 }
	 
	 protected void log(Level level, String msg){
		 app.log(level,msg);
	 }
	
	 public static AbstractAlgorithm createInstance(Hashtable<Integer,SensorTask> taskList, DRLSensorApp app){
		 Algorithm algo=Algorithm.valueOf(System.getProperty("algorithm", Algorithm.TEAM.toString()));
	       
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
		 }else if(algo.equals(Algorithm.DIRLWoLF)){
			 return new DIRLWoLFAlgorithm(taskList, app);
		 }else if(algo.equals(Algorithm.SIMPLE)){
			 return new SIMPLEAlgorithm(taskList, app);
		 }else if(algo.equals(Algorithm.ORACLE)){
			 return new ORACLEAlgorithm(taskList, app);
		 }
		 else throw new RuntimeException("No implementation found for alogrithm:"+algo);
	 }

	public abstract void reinforcement(SensorTask currentTask, SensorState prevState,
			SensorState currentState);
	 
}
