// @(#)SensorPacket.java   12/2003
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
import drcl.net.Packet;

/** This class implements the packet that a sensor node sends/forwards to the sink node.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorPacket extends Packet {

	public int pktType ;
    public int dataSize ;
	public double maxSNR ;
	protected int eventID ;
	protected long target_nid; // what target NID generated this signal originally
	public int maxProp ;
    protected long src_nid;
    protected long dst_nid;
    protected double[] dst_loc;

    public double target_X ;
	public double target_Y ;
	public double target_Z ;
	public int target_SeqNum ;

    public SensorPacket(){ }


    /**
     * This constructor is for broadcast packets which defines the
     * packet size.
     * @param pktType_
     * @param src_nid_
     * @param eventID_
     * @param body_
     * @param size_
    */
    public SensorPacket(int pktType_, long src_nid_, int eventID_, Object body_, int size_)
    {
		pktType = pktType_ ;
        src_nid = src_nid_;
        eventID = eventID_;
		body = body_ ;
        size = size_;
	}


    /**
     * This constructor is for broadcast packets which does not
     * defines the packet size.
     * @param pktType_
     * @param src_nid_
     * @param eventID_
     * @param body_
    */
    public SensorPacket(int pktType_, long src_nid_, int eventID_, Object body_)
    {
		pktType = pktType_ ;
        src_nid = src_nid_;
        eventID = eventID_;
		body = body_ ;
	}


	/**
     * This constructor is for unicast packets.
     * @param pktType_
     * @param dst_nid_
     * @param src_nid_
     * @param dataSize_
     * @param eventID_
     * @param target_nid_
     * @param body_
     */
    public SensorPacket(int pktType_, long dst_nid_, long src_nid_, int dataSize_,int eventID_, long target_nid_, Object body_)
    {
		pktType = pktType_ ;
        src_nid = src_nid_;
        dst_nid = dst_nid_;
		size = dataSize_ ;
		eventID = eventID_ ;
		target_nid = target_nid_ ;
        body = body_ ;
	}

    //NICHOLAS:
    //this constructor is for unicast packets.
	public SensorPacket(int pktType_, long dst_nid_, long src_nid_, int dataSize_,double [] dst_loc_,int eventID_, long target_nid_, Object body_)
    {
        //NICHOLAS:
        this.dst_loc = new double[2];
        this.dst_loc = dst_loc_;

		pktType = pktType_ ;
        src_nid = src_nid_;
        dst_nid = dst_nid_;
		size = dataSize_ ;
		eventID = eventID_ ;
		target_nid = target_nid_ ;
        body = body_ ;
	}

    /**
     * Used for directed diffusion
     * @param pktType_
     * @param body_
     */
    public SensorPacket(int pktType_, Object body_)
	{
		pktType = pktType_ ;
		body = body_ ;
	}

    //for directed diffusion package
    public SensorPacket(int pktType_, int dataSize_, double maxSNR_, int eventID_, int maxProp_, long target_nid_, double target_X_, double target_Y_, double target_Z_, int target_SeqNum_, Object body_)
    {
            pktType = pktType_ ;
            size = dataSize_ ;
            maxSNR = maxSNR_ ;
            eventID = eventID_ ;
            maxProp = maxProp_ ;
            target_nid = target_nid_ ;
            target_X = target_X_ ;
            target_Y = target_Y_ ;
            target_Z = target_Z_ ;
            target_SeqNum = target_SeqNum_ ;
            body = body_ ;
    }

    public long getTargetNid() {return target_nid;}
    public double getTargetX() {return target_X ; }
	public double getTargetY() {return target_Y ; }
	public double getTargetZ() {return target_Z ; }
	public int getTargetSeqNum() { return target_SeqNum ; }

    	/** Gets the data size.  */
	public int getDataSize()   {return dataSize;}

	/** Gets the MaxSNR.  */
	public double getMaxSnr()  {return maxSNR;}

    public double[] getDst_loc()
    {
        return dst_loc;
    }

    public double getDst_Xcoord()
    {
        return dst_loc[0];
    }

    public double getDst_Ycoord()
    {
        return dst_loc[1];
    }

    public double getDst_Zcoord()
    {
        return dst_loc[2];
    }

    public int getPktType()
    {
        return pktType;
    }

    public int getEventID()
    {
        return eventID;
    }

    /**
     * Gets the ID of the target node to which the enclosed information pertains.
     * @return
     */
    public long getTarget_nid()
    {
        return target_nid;
    }

    public long getSrc_nid()
    {
        return src_nid;
    }

    public long getDst_nid()
    {
        return dst_nid;
    }

    public String getName()
    {
        return "SensorPacket";
    }


    public void duplicate(Object source_)
    {
		SensorPacket that_ = (SensorPacket)source_;
		pktType = that_.pktType ;
		size = that_.size ;
        //maxSNR = that_.maxSNR ;
		eventID = that_.eventID ;
        //maxProp = that_.maxProp ;
		target_nid = that_.target_nid ;
	}
	
	public Object clone()
    {
		return new SensorPacket(pktType, dst_nid, src_nid, size, /*maxSNR,*/ eventID, /*maxProp,*/ target_nid, body);
	}

	public String toString(String separator_)
    {
		String str;
        str = "Sensor Packet dataSize =" + separator_ + size + separator_ +/* "maxSNR=" + maxSNR +*/ separator_ + "target_nid=" + target_nid ;
		return str;
	}
}
