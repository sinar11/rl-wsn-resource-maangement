// @(#)SensorAppAgentContract.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.sensorsim;

import drcl.comp.*;

/**
 * This class implements the contract between the sensor layer and the sensor application layer.
 *
 * @author Ahmed Sobeih
 * @version 1.0, 12/19/2003
*/

public class SensorAppAgentContract extends Contract
{
	public static final SensorAppAgentContract INSTANCE = new SensorAppAgentContract();

	public SensorAppAgentContract()
	{ super(); }
	
	public SensorAppAgentContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Sensor Application Agent Contract"; }
	
	public Object getContractContent()
	{ return null; }

	 /** This class implements the underlying message of the contract. */	
	public static class Message extends drcl.comp.Message
	{
		int dataSize ;
		double snr;
		long target_nid; // the id of the target node to which this data pertains
        double target_X ;
		double target_Y ;
		double target_Z ;
		int target_SeqNum ;
		public Object body ; // the encapsulated data in the TargetPacket

        public Message ()	{ }

        public Message (int dataSize_, double snr_, long target_nid_, double target_X_, double target_Y_, double target_Z_, int target_SeqNum_, Object body_)
		{
			dataSize = dataSize_ ;
			snr = snr_;
			target_nid = target_nid_;
			target_X = target_X_ ;
			target_Y = target_Y_ ;
			target_Z = target_Z_ ;
			target_SeqNum = target_SeqNum_ ;
			body = body_ ;
		}

 		public int getDataSize() { return dataSize; }
 		public double getSNR() { return snr; }
		public long getTargetNid() {return target_nid; }
		public double getTargetX() {return target_X ; }
		public double getTargetY() {return target_Y ; }
		public double getTargetZ() {return target_Z ; }
		public int getTargetSeqNum() { return target_SeqNum ; }
		public Object getBody() { return body ; }

		/*
        public void duplicate(Object source_)
		{
		    Message that_ = (Message)source_;
		    dataSize = that_.dataSize;
		    snr = that_.snr;
		    target_nid = that_.target_nid ;
		}
		*/
	
		public Object clone()
		{ 
			return new Message(dataSize, snr, target_nid, target_X, target_Y, target_Z, target_SeqNum, body); 
		}

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
	            String str;
        	    str = "Sensor-App-Agent Message:" + separator_ + "dataSize=" + dataSize + separator_ + "snr=" + snr + separator_ + "target_nid=" + target_nid; 
			return str;
		}
	}
}
