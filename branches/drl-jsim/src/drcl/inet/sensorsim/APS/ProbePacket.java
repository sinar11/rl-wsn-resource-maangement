// @(#)ProbePacket.java   8/2004
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

package drcl.inet.sensorsim.APS;


/**
 * This class implements the probing packet sent by
 * {@link drcl.inet.sensorsim.APS.SensorLocApp anchor nodes}
 * in the Ad hoc Positioning System (APS).
 * This is encapsulated by a {@link drcl.inet.sensorsim.SensorPacket sensor packet}
 * and is delivered to other nodes.
 *
 * @author Hyuk Lim
 * @version 1.0, 8/23/2004
 * @see drcl.inet.sensorsim.APS.SensorLocApp
 */
public class ProbePacket extends Object implements Cloneable
{
    /** The anchor node's ID */
    public long senderID;

    /** The anchor node's position */
    public double senderPos[];

    /** The hop-count from the anchor node */
    public int hopCount;

    /** The average distance per hop */
    public double hopDistance;

    /** The sequence number of this packet */
    public long pktSequence = 0;

    /** The packet type ID for this packet */
    public static final int PACKET_TYPE = 3;

    /**
     * Creates a new ProbePacket object.
     */
    public ProbePacket(SensorLocApp app)
    {
        this.senderID = app.nid;
        this.senderPos = new double[3];
        for (int i = 0; i < 3; i++)
        {
            this.senderPos[i] = app.myPos[i];
        }
        this.hopCount = 0;
        this.hopDistance = app.hopDistance;
        this.pktSequence = app.pktSequence;
    }

    /**
     * Increases <code>hopCount</code> by one.
     */
    public int incHopCount()
    {
        this.hopCount += 1;
        return this.hopCount;
    }

    /**
     * Creates and returns a copy of this object.
     * @throws java.lang.CloneNotSupportedException
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        // don't use the "new" operator.
        ProbePacket x = (ProbePacket)super.clone();

        x.senderID = this.senderID;
        x.senderPos = new double[3];
        for (int i = 0; i < 3; i++)
        {
            x.senderPos[i] = this.senderPos[i];
        }
        x.hopCount = this.hopCount;
        x.hopDistance = this.hopDistance;
        return x;
    }

    /**
     * Returns a string representation of the object.
     */
    public String toString()
    {
        String str;
        str = "probepkt #" + this.pktSequence + " of node" + this.senderID
            + "(" + this.senderPos[0] + "," + this.senderPos[1] + ")"
            + " hi=" + this.hopCount + " ci=" + this.hopDistance;
        return str;
    }
}
