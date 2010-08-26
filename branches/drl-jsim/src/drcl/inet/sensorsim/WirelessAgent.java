// @(#)WirelessAgent.java   12/2003
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

package drcl.inet.sensorsim;

import drcl.comp.Port;
import drcl.inet.InetPacket;

/** This class implements the middleware between the sensor protocol stack and the wireless protocol stack.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
 *
 * Modified by Nicholas Merizzi May 2005
*/
public class WirelessAgent extends drcl.inet.Protocol {

    //NICHOLAS: used to be 50 i know 600 is large but for testing purposes it will do for now
    public static final int SLOT_SIZE = 600 ;

    public static final String TO_SENSOR_APP_PORT_ID = ".toSensorApp";

    protected Port toSensorAppPort = addPort(TO_SENSOR_APP_PORT_ID, false);


    public WirelessAgent()
    {
        super();
    }


    public WirelessAgent(String id_)
    {
        super(id_);
    }

	/**
     * Sends a unicast packet over the wireless channel
    */
	protected synchronized void sendPkt(long dst_, long src_, double[] dst_loc, int size_, int type_, int eventID_, long target_nid_, Object body_)
    {
		int bytesLeft = size_;
		int bytesSent = 0;

		// send the packet in SLOT_SIZE byte chunk for tdma
		while ( bytesLeft > 0 ) {
			bytesSent = (bytesLeft>=SLOT_SIZE)?SLOT_SIZE:bytesLeft;
            //this constructor is for unicast packets.
	        //SensorPacket(int pktType_, long dst_nid_, long src_nid_, int dataSize_,double [] dst_loc_,int eventID_, long target_nid_, Object body_)
			SensorPacket sensorPkt = new SensorPacket(type_, dst_, src_, bytesSent, dst_loc, eventID_, target_nid_, body_);
			forward(sensorPkt,bytesLeft,dst_loc, drcl.net.Address.NULL_ADDR, dst_, false, 255, 0);
			bytesLeft -= bytesSent;
		} // end while
	}

    /**
     * Directed Difussion packet:
     * Sends a unicast packet over the wireless channel
    */
    protected synchronized void sendPkt(long dst_, int size_, int type_, double snr_, int eventID_, long target_nid_, double target_X_, double target_Y_, double target_Z_, int target_SeqNum_, Object body_)
    {
        int bytesLeft = size_;
        int bytesSent = 0;

        // send the packet in SLOT_SIZE byte chunk for tdma
        while ( bytesLeft > 0 ) {
            bytesSent = (bytesLeft>=SLOT_SIZE)?SLOT_SIZE:bytesLeft;
            SensorPacket sensorPkt = new SensorPacket(type_, bytesSent, snr_, eventID_, 0, target_nid_, target_X_, target_Y_, target_Z_, target_SeqNum_, body_);
            forward(sensorPkt, drcl.net.Address.NULL_ADDR, dst_, false, 255, 0);
            bytesLeft -= bytesSent;
        } // end while
    }

    /**
     * Directed Diffusion
     * Sends a broadcast packet over the wireless channel
    */
    protected synchronized void sendBcastPkt(int type_, double snr_, int eventID_, Object body_)
	{
		SensorPacket sensorPkt = new SensorPacket(type_, 0 /*understood to be sizeof SensorPacket*/, snr_, eventID_, 1, Integer.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Integer.MIN_VALUE, body_); /* since in sending the broadcast packet, the target_nid is lost, a temporary solution is to use Integer.MIN_VALUE. */

		broadcast(sensorPkt, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 255, 0);
	}

	/**
     * Sends a broadcast packet over the wireless channel
    */
	protected synchronized void sendBcastPkt(int type_, long src_, int eventID_, Object body_, int body_size_)
    {
        //since in sending the broadcast packet, the target_nid is lost, a temporary solution is to use Integer.MIN_VALUE.
		SensorPacket sensorPkt = new SensorPacket(type_, src_, eventID_, body_);
        broadcast(sensorPkt, body_size_, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 1, 0);
        //the following line was what was here before!
        //forward(sensorPkt, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, false, 255, 0);
	}

	/**
     * Handles data arriving at UP port (i.e. This is the information coming
     * from the SensorApp layer)
    */
	protected synchronized void dataArriveAtUpPort(Object data_, Port upPort_)
    {
        if (!(data_ instanceof SensorAppWirelessAgentContract.Message)) {
	        error(data_, "processOther()", upPort_, "unknown object");
        	return;
        }
		SensorAppWirelessAgentContract.Message msg = (SensorAppWirelessAgentContract.Message)data_ ;
		
		if (msg.getFlag() == SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET){
			sendPkt(msg.getDst(),msg.getSize(), msg.getType(),msg.getSNR(), msg.getEventID(), msg.getTargetNid(),msg.getTargetX(),msg.getTargetY(),msg.getTargetZ(),msg.getTargetSeqNum(),msg.getBody());
//			sendPkt(msg.getDst(), msg.getSrc(), msg.getDst_loc(), msg.getSize(), msg.getType(), msg.getEventID(), msg.getTargetNid(), msg.getBody()) ;
        }
        else if (msg.getFlag() == SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET) {
			sendBcastPkt(msg.getType(), msg.getSrc(), msg.getEventID(), msg.getBody(),msg.getSize());			
        }
	}

	/**
     * Handles data arriving at DOWN port (i.e. the information coming
     * from the PacketDispatcher). When a received packet from the
     * wireless protocol stack, needs to forward to the sensor application layer
    */
	protected synchronized void dataArriveAtDownPort(Object data_, Port downPort_)
    {
		InetPacket ipkt_ = (InetPacket)data_;
		SensorPacket pkt_ = (SensorPacket)ipkt_.getBody();

        //after getting the data from the packet dispatcher break it apart and store the appropriate
        //parts in a SensorPacket which is then forwarded up to SensorApp for processing
		//toSensorAppPort.doSending(new SensorPacket(pkt_.getPktType(), pkt_.getDst_nid(), pkt_.getSrc_nid(),pkt_.getPacketSize(), pkt_.getEventID(), pkt_.getTarget_nid(), pkt_.getBody()));
		toSensorAppPort.doSending(pkt_);
    }
}
