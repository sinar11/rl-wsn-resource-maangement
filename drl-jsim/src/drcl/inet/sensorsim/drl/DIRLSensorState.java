/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim.drl;

import javax.swing.AbstractAction;

import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm;

/*
 * @author Kunal
 */

public class DIRLSensorState implements SensorState{
    public static final double SNR_WEIGHT = 0.01;
     
    protected int stateId;
    protected double snr=-1;
    protected boolean hasNeighbours=true;
    protected boolean successInRecentSampling=false;
    protected boolean successInRecentRX=false;
    protected long streamId;
    
    public DIRLSensorState(double snr, boolean hasNeighbours, boolean successInRecentSampling, boolean successInRecentRX, long streamId){
        this.snr=snr;
        this.hasNeighbours=hasNeighbours;
        this.successInRecentRX=successInRecentRX;
        this.successInRecentSampling=successInRecentSampling;
        this.streamId=streamId;
    }    
    
    public boolean equals(Object o){
        DIRLSensorState s= (DIRLSensorState)o;
        double hammingDist= getHammingDistance(s);
        if(hammingDist>=THRESHOLD_HAMMING)
            return false;
        else
            return true;
    }

    public String toString(){
        return stateId+":"+snr+","+hasNeighbours+","+successInRecentRX+","+successInRecentSampling+","+streamId;
    }
    private double getHammingDistance(DIRLSensorState s) {
        double dist=0;
        if(s.hasNeighbours!=this.hasNeighbours) dist+=1;
        if(s.successInRecentRX!=this.successInRecentRX) dist+=1;
        if(s.successInRecentSampling!=this.successInRecentSampling) dist+=1;
        //if(s.streamId!=this.streamId) dist+=1;
        dist+= SNR_WEIGHT*Math.abs(s.snr-this.snr);
        return dist;
    }

    public int getStateId() {
        return stateId;
    }

	public void setStateId(int stateId) {
		this.stateId=stateId;
	}



	public double calcExplorationFactor(SensorTask currentTask) {
		if(successInRecentRX || successInRecentSampling)
			return AbstractAlgorithm.MIN_EPSILON;
		else
			return AbstractAlgorithm.MAX_EPSILON;				
	}

}
