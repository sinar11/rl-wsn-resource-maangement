// @(#)BatteryContract.java   1/2004
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
 * This class implements the contract between the CPU model
 * and the energy model contained within the wirelessPhy component.
 *
 * @author Ahmed Sobeih
 * @version 1.0, 12/19/2003
 *
 * Modified by Nicholas Merizzi April 2005
 * CPU Modes:
 *              CPU_IDLE   = 0
 *              CPU_SLEEP  = 1
 *              CPU_ACTIVE = 2
 *              CPU_OFF    = 3
*/

public class BatteryContract extends Contract
{
	public static final BatteryContract INSTANCE = new BatteryContract();

	public BatteryContract()
	{
        super();
    }
	
	public BatteryContract(int role_)
	{
        super(role_);
    }
	
	public String getName()
	{
        return "BatteryContract Contract";
    }

	public Object getContractContent()
	{
        return null;
    }

	public static final int SET_CONSUMER_CURRENT = 1;

	/** This class implements the underlying message of the contract. */
	public static class Message extends drcl.comp.Message
	{
		int type;
		double time;

		// for SET_CONSUMER_CURRENT
		public Message(int type_, double time_)
        {
			type = type_;
			time = time_;
		}

		public int getType()
		{
            return type;
        }

		public double getTime()
		{
            return time;
        }

		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			consumer_id = that_.consumer_id;
			current = that_.current;
		}
		*/
	
		public Object clone()
		{ return new Message(type, time); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return ("BatteryContract: " + type);
		}
	} // end class BatteryContract.Message
} // end class BatteryContract
