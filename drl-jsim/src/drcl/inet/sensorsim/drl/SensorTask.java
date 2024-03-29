/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim.drl;


/*
 * @author Kunal
 */

public abstract class SensorTask {
    
    public final double ALPHA=0.5; // LEARNING RATE PARAMETER
    public final double GAMMA=0.3; //DISCOUNT FACTOR 
    
    public String taskId;  // String id 
    public int id;
    protected double[] Qvalues= new double[SensorState.MAX_STATES]; // value of Q-learning parameter Q for different states
    public double expectedPrice;
    //double[] expectedPrices= new double[SensorState.MAX_STATES]; //expected price obtained for performing this action in different states
    public double lastReward; // immediate reward of last time action was taken
    public double[] getQvalues() {
		return Qvalues;
	}

	public double getLastReward() {
		return lastReward;
	}

	public double getLastCost() {
		return lastCost;
	}

	double lastCost; //cost of last time action was taken
    public int noOfExecutions=0;
    
    protected SensorTask(int id, String taskId, double price){
        this.id=id;
        this.taskId=taskId;
        /*for(int i=0;i<expectedPrices.length;i++){
        	this.expectedPrices[i]=price;
        }*/
        this.expectedPrice=price;
    }
    
    public double updateQValue(SensorState lastState, double QforBestAction){
    	double prevQ= Qvalues[lastState.getStateId()];
        Qvalues[lastState.getStateId()]= (1-ALPHA)*prevQ+ ALPHA*(lastReward + GAMMA * QforBestAction);
        return (Qvalues[lastState.getStateId()]-prevQ);
    }
    
    public void executeTask(){
        noOfExecutions++; 
        execute();
    }
    protected abstract void execute();

    public void computeReward(){
    	this.lastCost=computeCost();
    	this.lastReward=computePrice()-lastCost;
    }
    
    public abstract double computePrice();
    public abstract double computeCost();
    public abstract boolean isAvailable();
    
    public String toString(){
        return taskId+"- Q:["+printQValues()+"] noofExecutions:"+noOfExecutions;
    }
    
    public String printQValues(){
        String q="";
        for(int i=0;i<Qvalues.length;i++){
            q+=Qvalues[i]+",";
        }
        return q;
    }
    public String printExpPrices(){
       /* String q="";
        for(int i=0;i<expectedPrices.length;i++){
            q+=expectedPrices[i]+",";
        }
        return q;*/
    	return expectedPrice+"";
    }
    public double getQvalue(SensorState s) {
        return Qvalues[s.getStateId()];
    }
   
    public String getTaskId() {
        return taskId;
    }

   
    public double getExpectedPrice() {
        return expectedPrice;
    }
    
    public int getNoOfExecutions() {
        return noOfExecutions;
    }

    public int getId() {
        return id;
    }

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SensorTask){
			SensorTask other=(SensorTask)obj;
			if(this.id==other.id) return true;			
		}
		return false;
	}

	public void resetQValues() {
		for(int i=0;i<Qvalues.length;i++){
			Qvalues[i]=0;
		}
		
	}    
    
    
}
