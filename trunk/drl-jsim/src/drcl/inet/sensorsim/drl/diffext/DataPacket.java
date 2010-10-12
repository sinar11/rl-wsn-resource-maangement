// @(#)DataPacket.java   10/2004
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
import java.util.List;


/** This class implements a data packet carrying the description of a detected event.
*
* @author Kunal Shah
* 
*/

public class DataPacket
{
	/** The description of the detected event */
	private List<Tuple> attributes = null ;

	private int taskId;
	
	/** Destination sink **/
	private long sinkId;
	
	/** The source of the packet */
	private long sourceId; 
	
	/** Current destination node for this data packet, -1 if to be broadcasted **/
	private long destinationId=DRLDiffApp.BROADCAST_DEST;
	
	/** Timestamp of data **/
	private double timestamp;
	
	/** VARIABLES UPDATED BY EACH NODE **/
	
	/** Cumulative (running sum) of cost of data acquisition till point.
	 * Updated by each node to add its own cost **/
	private double cost=0;
	
	/**
	 * Reward resetted by each node to reflect the amount of payment it's expecting from this data packet
	 */
	private double reward;
	
	/** This is only for ease of debugging.. not required for protocol. Provides path taken by this data packet **/
	/** though can be useful to detect loop **/
	private List<Long> trace;
	
	private boolean explore=false;
	
	public DataPacket(long sourceId, long sinkId, int taskId, List<Tuple> attributes, double timestamp)
	{
		this.sourceId=sourceId;
		this.sinkId=sinkId;
		this.taskId=taskId;
		this.attributes=attributes;
		this.timestamp=timestamp;
		if(DRLDiffApp.TRACE_ON){
			trace= new ArrayList<Long>();			
		}
	}

	public DataPacket(DataPacket pkt)
	{
		this.sourceId=pkt.sourceId;
		this.sinkId=pkt.sinkId;
		this.destinationId=pkt.destinationId;
		this.taskId=pkt.taskId;
		this.attributes=pkt.attributes;
		this.timestamp=pkt.timestamp;
		this.reward=pkt.reward;
		this.cost=pkt.cost;
		this.explore=pkt.explore;
		if(DRLDiffApp.TRACE_ON){
			this.trace=new ArrayList<Long>(pkt.trace);
		}
	}
	
	public String toString()
	{
		return "DataPacket[sourceId="+sourceId+",destinationId="+destinationId+",sinkId="+sinkId+",taskId="+taskId+",timestamp="+timestamp
			+",cost="+cost+",reward="+reward+",explore="+explore+(DRLDiffApp.TRACE_ON?(",trace="+trace):"");	
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (sinkId ^ (sinkId >>> 32));
		result = prime * result + taskId;
		long temp;
		temp = Double.doubleToLongBits(timestamp);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataPacket other = (DataPacket) obj;
		if (sinkId != other.sinkId)
			return false;
		if (taskId != other.taskId)
			return false;
		if(sourceId != other.sourceId)
			return false;
		if (Double.doubleToLongBits(timestamp) != Double
				.doubleToLongBits(other.timestamp))
			return false;
		return true;
	}

	public void setSourceId(long sourceId) {
		this.sourceId = sourceId;
	}

	public void addCostReward(double reward, double cost, long nodeId) {
		this.reward=reward; //set this as the reward (no sum)
		this.cost+=cost;  //add cost to current running sum
		if(DRLDiffApp.TRACE_ON)
			trace.add(nodeId);
	}
	
	
	public List<Tuple> getAttributes() {
		return attributes;
	}


	public int getTaskId() {
		return taskId;
	}


	public long getSinkId() {
		return sinkId;
	}


	public long getSourceId() {
		return sourceId;
	}


	public double getTimestamp() {
		return timestamp;
	}


	public double getCost() {
		return cost;
	}


	public double getReward() {
		return reward;
	}
	
	public long getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(long destinationId) {
		this.destinationId = destinationId;
	}

	public boolean isExplore() {
		return explore;
	}

	public void setExplore(boolean explore) {
		this.explore = explore;
	}
}
