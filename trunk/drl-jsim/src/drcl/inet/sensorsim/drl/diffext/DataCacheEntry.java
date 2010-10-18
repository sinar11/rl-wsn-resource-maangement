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
	
	/** State related to source nodes, one per each stream from which data is received for this task 
	 * Each stream has unique combination of streamId and sourceId*/
	List<DataStreamEntry> dataStreams= new ArrayList<DataStreamEntry>();
	
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
		DataStreamEntry entry= new DataStreamEntry(data.getSourceId(),data.getStreamId());
		int index=dataStreams.indexOf(entry);
		if(index==-1){
			dataStreams.add(entry);			
		}else{
			entry= dataStreams.get(index);
		}
		entry.setNodes(data.getTrace());
		entry.updateStatsOnNewDataPkt(data.getReward(),data.getTimestamp());
		//lastDataTimestamp=data.getTimestamp();
	}
	
	/** Prints the data cache entry */
	public void printDataEntry(){
		System.out.println("DataCacheEntry[taskId="+taskId+",data size="+recentData.size()+"]") ;
		for(DataStreamEntry stream: dataStreams){
			System.out.println(stream);
		}
	}

	public boolean containtsDataPacket(DataPacket dataPkt) {
		return recentData.contains(dataPkt);
	}
	
	public Collection<ReinforcementPacket> getPendingReinforcements(long nid, double currentTime, double margin, InterestPacket interest, double payable){
		Map<Long,ReinforcementPacket> pkts= new HashMap<Long,ReinforcementPacket>();
		if((currentTime-lastReinforcedTime)<DRLDiffApp.REINFORCE_WINDOW)
			return pkts.values();	
		//if(recentData.size()==0) return pkts;
		//DataStreamEntry bestStream= findBestStream(interest);
		for(DataStreamEntry stream: dataStreams){
			if(stream.getSourceId()==nid || stream.getNoOfPackets()==0) continue;
			populateReinforcements(pkts, nid, stream, payable, margin);
			/*if(stream.getAvgWLReward()>0){  //|| stream.equals(bestStream)
					System.out.println(nid+":+VE Reinforcement:"+payable+"-"+stream);
					populateReinforcements(pkts, nid, stream, payable);
				} 
			}else if(stream.getNoOfPackets()>0){
				System.out.println(nid+":-VE Reinforcement:"+stream);//+" bestStream:"+bestStream);
				populateReinforcements(pkts,nid,stream,0);
			}	*/			
		}
		reset(currentTime);
		return pkts.values();
	}

	private void populateReinforcements(Map<Long,ReinforcementPacket> pkts, long nid, DataStreamEntry stream, double payable, double margin){
		ReinforcementPacket reinf=pkts.get(stream.getSourceId());
		double payment=0.0;
		if(stream.getAvgWLReward()>0){
			payment=stream.getCurrPayable()==null?payable:stream.getCurrPayable();
			if(stream.shouldReinforce(payment, margin)){
				System.out.println(nid+":+VE Reinforcement:"+payment+"-"+stream);
			}else{
				return;
			}
		}else{
			System.out.println(nid+":-VE Reinforcement:"+stream);
			payment=0.0;
		}
		if(reinf==null){
			reinf=new ReinforcementPacket(taskId,payable,nid,stream.getSourceId());
			pkts.put(stream.getSourceId(),reinf);
		}
		reinf.addStreamPayment(stream.getStreamId(), payment);				
	}
	
	public List<List<Long>> getRecentDataStreams(){
		List<List<Long>> allStreams= new ArrayList<List<Long>>();
		for(DataStreamEntry stream: dataStreams){
			if(stream.getNoOfPackets()>0)
				allStreams.add(stream.getNodes());			
		}
		return allStreams;
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
		
	public void reset(double currTime){
		this.recentData.clear();		
		this.qualityAchieved=false;
		lastReinforcedTime=currTime;
		for(DataStreamEntry stream: dataStreams){
			stream.resetStatsOnReinforcement(currTime);			
		}
	}

	public Collection<DataStreamEntry> getDataStreams() {
		return dataStreams;
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

	public void updateDataStreams(ReinforcementPacket pkt, double payable) {
		Map<Long,Double> payments= pkt.getStreamPayments();
		for(DataStreamEntry stream: dataStreams){
			Double payment=payments.get(stream.getStreamId());
			if(payment!=null){
				stream.setCurrPayable(Math.min(payment,payable));
			}
		}
	}
	
}
