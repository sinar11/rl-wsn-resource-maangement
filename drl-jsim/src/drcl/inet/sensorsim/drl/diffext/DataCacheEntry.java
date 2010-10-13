// @(#)DataCacheEntry.java   10/2004
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 3. Neither the name of "DRCL" nor the names of its contributors may be used
//    to endorse or promote products derived from this software without specific
//    prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//

package drcl.inet.sensorsim.drl.diffext ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**Represents data received for a particular task along with list of source stats (reward, no of packets etc.)
 * 
 * @author Kunal Shah
 *
 */

public class DataCacheEntry
{
	/** Task that this data is associated with **/
	private int taskId;
	
	/** List of recently seen data (in current timestep) for task associated with this cache entry from all sources */
	private List<DataPacket> recentData = new ArrayList<DataPacket>();
	
	/** State related to source nodes, one per each neighbor from which data is received for this task */
	Map<Long,DataStreamEntry> dataStreams= new HashMap<Long,DataStreamEntry>();
	
	/** A flag to track whether expected quality data has been achieved for this entry for current time step */ 
	boolean qualityAchieved=false;
	
	/** Time at which last reinforcement was sent **/ 
	private double lastReinforcedTime;

	
	public DataCacheEntry(int taskId, DataPacket data)
	{
		addDataPacket(data);
		this.taskId=taskId;	
	}

	public int getTaskId() {
		return taskId;
	}

	public void addDataPacket(DataPacket data){
		recentData.add(data);
		DataStreamEntry entry= dataStreams.get(data.getSourceId());
		if(entry==null){
			entry= new DataStreamEntry(data.getSourceId());
			dataStreams.put(data.getSourceId(),entry);
		}
		entry.updateStatsOnNewDataPkt(data.getReward(),data.getTimestamp());
		//lastDataTimestamp=data.getTimestamp();
	}
	
	/** Prints the data cache entry */
	public void printDataEntry(){
		System.out.println("DataCacheEntry[taskId="+taskId+",data size="+recentData.size()+"]") ;
		for(DataStreamEntry stream: dataStreams.values()){
			System.out.println(stream);
		}
	}

	public boolean containtsDataPacket(DataPacket dataPkt) {
		return recentData.contains(dataPkt);
	}
	
	public List<ReinforcementPacket> getPendingReinforcements(long nid, double currentTime, double margin, InterestPacket interest, double payable){
		List<ReinforcementPacket> pkts= new ArrayList<ReinforcementPacket>();
		if((currentTime-lastReinforcedTime)<DRLDiffApp.REINFORCE_WINDOW)
			return pkts;	
		//if(recentData.size()==0) return pkts;
		DataStreamEntry bestStream= findBestStream(interest);
		for(DataStreamEntry stream: dataStreams.values()){
			if(stream.getSourceId()==nid) continue;
			if(stream.getAvgWLReward()>0 || stream.equals(bestStream)){  //
				if(payable>0 && stream.shouldReinforce(payable, margin)){
					System.out.println(nid+":+VE Reinforcement:"+payable+"-"+stream);
					pkts.add(new ReinforcementPacket(taskId,payable,nid,stream.getSourceId()));
				}
			}else if(stream.getNoOfPackets()>0){
				System.out.println(nid+":-VE Reinforcement:"+stream+" bestStream:"+bestStream);
				pkts.add(new ReinforcementPacket(taskId,0,nid,stream.getSourceId()));
			}				
		}
		reset(currentTime);
		return pkts;
	}

	/**
	 * Determine stream with lowest cost and required quality
	 * @param dataCacheEntry
	 * @param interest
	 * @return
	 */
	private DataStreamEntry findBestStream(InterestPacket interest){
		//double minCost=Integer.MAX_VALUE;
		DataStreamEntry bestStream=null;
		for(DataStreamEntry stream: dataStreams.values()){
			if(stream.getNoOfPackets()==0) continue;
			List<DataPacket> pkts=getRecentDataForSource(stream.getSourceId());
			if(calcDataQuality(pkts, interest)==DRLDiffApp.MAX_DATA_QUALITY){
			//	double cost= getAvgCost(pkts);
				if(bestStream==null){
					bestStream=stream;
				}else if(stream.getAvgWLReward()>bestStream.getAvgWLReward()){
					bestStream=stream;
				}/*else if(stream.getAvgPktReward()>=1.1*bestStream.getAvgPktReward()){
					bestStream=stream;
				}else if(stream.getTotalPktReward()>bestStream.getTotalPktReward()){
					bestStream=stream;
				}*/
			}		
		}
		return bestStream;
	}
	
	/**
	 * Matches data attributes to QoS contraints and returns a value between 0 and 1 representing data quality
	 * @param pkts
	 * @param interestEntry
	 * @return
	 */
	private double calcDataQuality(List<DataPacket> pkts,
			InterestPacket interest) {
		List<Tuple> qosConstraints=interest.getQosConstraints();
		if(qosConstraints==null || qosConstraints.size()==0) return DRLDiffApp.MAX_DATA_QUALITY;
		int passCount=0, totalCount=0;
		for(DataPacket pkt:pkts){
			//TODO how to get QoS attributes, should it be just part of normal data attributes?
			if(TupleUtils.isMatching(qosConstraints, pkt.getAttributes())){
				passCount++;
			}
			totalCount++;
		}
		return (passCount*DRLDiffApp.MAX_DATA_QUALITY)/totalCount;
	}
	
	private double getAvgCost(List<DataPacket> pkts){
		if(pkts.size()==0) return 0;
		double totalCost=0;
		for(DataPacket pkt: pkts){
			totalCost+=pkt.getCost();
		}
		return totalCost/pkts.size();
	}
	public List<DataPacket> getRecentData() {
		return recentData;
	}
	
	public double getLastReinforcedTime() {
		return lastReinforcedTime;
	}

	public void setLastReinforcedTime(double lastReinforcedTime) {
		this.lastReinforcedTime = lastReinforcedTime;
	}
	public List<DataPacket> getRecentDataForSource(long sourceId) {
		List<DataPacket> list= new ArrayList<DataPacket>();
		for(DataPacket pkt:recentData){
			if(pkt.getSourceId()==sourceId)
				list.add(pkt);			
		}
		return list;
	}
	
	public void reset(double currTime){
		this.recentData.clear();		
		this.qualityAchieved=false;
		lastReinforcedTime=currTime;
		for(DataStreamEntry stream: dataStreams.values()){
			stream.resetStatsOnReinforcement(currTime);			
		}
	}

	public Collection<DataStreamEntry> getDataStreams() {
		return dataStreams.values();
	}

	public boolean isQualityAchieved() {
		return qualityAchieved;
	}

	public void setQualityAchieved(boolean qualityAchieved) {
		this.qualityAchieved = qualityAchieved;
	}

	class DataPacketWrapper{
		DataPacket pkt;
		String groupId;
		
		DataPacketWrapper(DataPacket pkt, String groupId){
			this.pkt=pkt;
			this.groupId=groupId;
		}		
	}
	
}
