// @(#)scheduler_SFQ.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.intserv.scheduler;

import java.util.Vector;
import drcl.net.Packet;
import drcl.intserv.*;
import drcl.util.queue.*;

/**
The Start-time Fair Queueing scheduler.
Accepts any type of Rspec.
 */
public class scheduler_SFQ extends drcl.intserv.Scheduler
{
	Queue pq = QueueAssistant.getBest(); // store packets w/ virtual start time
	Vector vflow = new Vector(3, 3); // handle -> vparam
	double V = 0.0; // virtual time; ticks only when a packet departs in real time
	
	double Vm = 0.0; // Vm records the maximum of finish tag assigned to
	           // any packets that have been serviced.
	           // At the end of a busy period, V is set to Vm.
	
	public scheduler_SFQ()
	{ super(); }
	
	public scheduler_SFQ(String id_) 
	{ super(id_); }
	
	public void reset() 
	{
		super.reset();
		pq.reset();
		vflow.removeAllElements();
		V = 0.0;
		Vm = 0.0;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof scheduler_SFQ)) return;
		super.duplicate(source_);
		scheduler_SFQ that_ = (scheduler_SFQ)source_;
	}
	
	protected void qosEnque(Packet p_, SpecR rspec_)
	{
		vparam vp_ = (vparam)vflow.elementAt(rspec_.handle);
		if (vp_ == null) {
			drcl.Debug.error(this, "no flow info is installed for " + p_);
			return;
		}
		
		if ( pq.isEmpty() ) V = Vm;  // beginning of a busy period
		
		double s_ = Math.max(vp_.F, V);
		vp_.F = s_ + (double)p_.size / vp_.fei;
		
		Vm = Math.max(vp_.F, Vm);   // Vm records the largest finish tag
		
		pq.enqueue(s_, p_);
    }

	protected Packet qosDeque() 
	{ 
		if (pq.isEmpty()) return null;
		// Note: (XX) can check if the packet's rspec still exists, but what the heck.
		
		// virtual clock ticks.
		V = pq.firstKey(); 
	    return (Packet)pq.dequeue();
	}

	
	/**
	 * Set up flow pertaining data structure; Activate flow; 
	 * Reply to Admission. Install reserved information in local database B.
	 * note: index according to the handle #.
	 */
	public synchronized int setFlowspec(int handle_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{	
		handle_ = super.setFlowspec(handle_, tos_, tosmask_, fspec_);
		if (handle_ == -1) return -1;
		vparam vp_ = new vparam(fspec_.rspec.getBW());
		
		// XX: should we discard the packets that are still queued and belong to the discarded flow?
		if (vflow.size() <= handle_) vflow.setSize(handle_ + 1);
		vflow.setElementAt(vp_, handle_);
		return handle_;
	}

		
	/**
	 * Event handler when the receive the FLOW_TEARDOWN message.
	 */
	public synchronized SpecFlow removeFlowspec(int handle_)
	{	
		SpecFlow fspec_ = super.removeFlowspec(handle_);
		if (fspec_ == null) return null;
		
		vparam vp_ = (vparam)vflow.elementAt(handle_);
		vflow.setElementAt(null, handle_);
		
		// packets in the queue still get transmitted, see qosDeque()
		return fspec_;
	}
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer("SFQ info: V=" + V + "\n");
		sb_.append("Packet queue:\n" + pq + "\n");
		return super.info() + sb_;
	}
	
	// for keeping parameters of a flow under GPS
	static class vparam extends drcl.DrclObj
	{
		double	F;				// virtual finishing time
		int	fei;			// service share = rspec.getBW()

		vparam (int fei_)
		{
			F		= 0.0;
			fei		= fei_;
		}

		public String toString()
		{
			return "fei=" + fei + ", F=" + F;
		}
	}
}
