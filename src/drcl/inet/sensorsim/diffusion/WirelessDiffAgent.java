// @(#)WirelessDiffAgent.java   10/2004
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

package drcl.inet.sensorsim.diffusion ;

import drcl.comp.Port;
import drcl.inet.InetPacket;
import drcl.inet.sensorsim.*;

/** This class implements the directed diffusion agent.
*
* @author Ahmed Sobeih
* @version 1.0, 10/19/2004
*/
public class WirelessDiffAgent extends drcl.inet.sensorsim.WirelessAgent
{
	public WirelessDiffAgent()
	{ super(); }

	public WirelessDiffAgent(String id_)
	{ super(id_); }

	/** Sends a broadcast packet over the wireless channel  */
	public synchronized void sendBcastPkt(int type_, Object body_)
	{
		SensorPacket sensorPkt = new SensorPacket(type_, body_);
		switch ( type_ )
		{
			case DiffApp.INTEREST_PKT :
				//InterestPacket interestPkt = (InterestPacket)sensorPkt.getBody() ;
				//long source = interestPkt.source ;
				broadcast(sensorPkt, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 1, 0);
				break ;
			case DiffApp.DATA_PKT :
				broadcast(sensorPkt, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 1, 0);
				break ;
			case DiffApp.POSITIVE_REINFORCEMENT_PKT :
				broadcast(sensorPkt, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 1, 0);
				break ;
			case DiffApp.NEGATIVE_REINFORCEMENT_PKT :
				broadcast(sensorPkt, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 1, 0);
				break ;
		}
	}

	/** Handles data arriving at the UP port--> meaning info being sent down from DiffApp
     *  is received at this port. */
	protected synchronized void dataArriveAtUpPort(Object data_, Port upPort_)
	{
		SensorAppWirelessAgentContract.Message msg = (SensorAppWirelessAgentContract.Message)data_ ;
		
		if (msg.getFlag() == SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET)
			sendBcastPkt(msg.getType(), msg.getBody()) ;
		else
			super.dataArriveAtUpPort(data_, upPort_) ;
	}

	/** Handles data arriving at the DOWN port */
	/* received packet from the routing layer, needs to forward to the sensor application layer */
	protected synchronized void dataArriveAtDownPort(Object data_, Port downPort_)
	{
		InetPacket ipkt_ = (InetPacket)data_;
		if ( ipkt_.getBody() instanceof SensorPacket )
		{
			SensorPacket pkt_ = (SensorPacket)ipkt_.getBody();
			switch ( pkt_.pktType )
			{
				case DiffApp.INTEREST_PKT :
					toSensorAppPort.doSending(new SensorPacket(pkt_.pktType, pkt_.getBody()));
					break ;
				case DiffApp.DATA_PKT :
					toSensorAppPort.doSending(new SensorPacket(pkt_.pktType, pkt_.getBody()));
					break ;
				case DiffApp.POSITIVE_REINFORCEMENT_PKT :
					toSensorAppPort.doSending(new SensorPacket(pkt_.pktType, pkt_.getBody()));
					break ;
				case DiffApp.NEGATIVE_REINFORCEMENT_PKT :
					toSensorAppPort.doSending(new SensorPacket(pkt_.pktType, pkt_.getBody()));
					break ;
				default :
					super.dataArriveAtDownPort(data_, downPort_) ;
			}
		}
		else
		{
			super.dataArriveAtDownPort(data_, downPort_) ;
		}
	}
}
