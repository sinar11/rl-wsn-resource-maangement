// @(#)SensorLocApp.java   8/2004
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

import drcl.data.*;
import drcl.comp.*;
import drcl.inet.sensorsim.SensorApp;
import drcl.inet.sensorsim.SensorPacket;
import drcl.inet.sensorsim.SensorAppWirelessAgentContract;

import java.util.*;
import drcl.comp.Port;
import drcl.inet.mac.PositionReportContract;

/**
 * This class implements the sensor localization algorithm
 * called Ad Hoc Positioning System (APS) proposed
 * by D. Noculescu and B. Nath, Globecom 2001.
 * Anchor nodes send probing packets periodically and
 * computes the hop-distance whenever it receives probe packets originated
 * from other anchor nodes. Non-anchor nodes compute the distance to
 * anchor nodes by using the hop-count and the hop-distance.
 * Once the distances to the anchor nodes are obtained, the non-anchor node
 * computes its location by lateration algorithms. Here, the gradient based 
 * lateration algorithm is implemented.
 *
 * @author Hyuk Lim
 * @version 1.0, 8/23/2004
 * @see drcl.inet.sensorsim.APS.ProbePacket
 */
public class SensorLocApp
    extends SensorApp
    implements drcl.comp.ActiveComponent
{
    /**
     * The flag that indicates whether it is an anchor or not (default: false).
     */
    public boolean isAnchor = false;

    /**
     * An anchor node sends {@link drcl.inet.sensorsim.APS.ProbePacket
     * probing packets} to its neighboring nodes
     * at every this time interval (default: 60 seconds).
     */
    public double probeInterval = 60;

    /**
     * An anchor node sends probing packets to its neighboring nodes
     * whenever this timer expires.
     */
    protected ACATimer timer = null;

    /**
     * The data structure that maintains the information on anchors' location
     * and distance.
     */
    protected Vector anchorList;

    /**
     * The sequence number for {@link drcl.inet.sensorsim.APS.ProbePacket
     * drcl.inet.sensorsim.ProbePacket}.
     */
    public long pktSequence = 0;

    /**
     * The average distance per each hop.
     */
    public double hopDistance = -1;

    /**
     * The actual position of node. Only anchor node knows its position.
     */
    public double[] myPos;

    /**
     * The port name for accepting the node location
     * reports (<code>.report</code>) from {@link drcl.inet.mac.MobilityModel
     * drcl.inet.mac.MobilityModel} (value: <code>.mobility</code>).
     */
    public static final String MOBILITY_PORT_ID = ".mobility";

    /**
     * The port associated with {@link #MOBILITY_PORT_ID}
     */
    protected Port mobilityPort = addPort(MOBILITY_PORT_ID, false);

    /**
     * The estimated position for non-anchor node.
     */
    public double[] estPos;

    /**
     * The updating gain for the iterative lateration algorithm based on
     * the gradient method (default: 0.1).
     */
    public double laterationGain = 0.1;

    /**
     * The threshold to check whether the iterative lateration algorithm
     * is converged or not (default: 0.1).
     * If the change of pos[t] is less than
     * "{@link #laterationGain} * pos[t]", the iteration process stops
     */
    public double laterationThreshold = 0.1;

    /**
     * The port name for reporting estimation error of sensor node
     * (value: <code>.locerr</code>).
     */
    public static final String ESTIMATION_ERROR_ID = ".locerr";

    /**
     * The event name for reporting estimation error of sensor node.
     */
    public static final String ESTIMATION_ERROR_EVENT = "location error";

    /**
     * The port associated with {@link #ESTIMATION_ERROR_ID}.
     */
    protected Port errPort = addEventPort(ESTIMATION_ERROR_ID);

    /**
     * Creates a new SensorLocApp object.
     */
    public SensorLocApp()
    {
        super();
        this.myPos = new double[3];
        this.estPos = new double[3];
        for (int i = 0; i < 3; i++)
        {
            this.myPos[i] = Double.NaN;
            this.estPos[i] = Double.NaN;
        }
        this.anchorList = new Vector();
    }

    /**
     * Sets {@link #probeInterval}.
     */
    public void setProbeInterval(double t)
    {
        this.probeInterval = t;
    }

    /**
     * Determines whether this is an anchor.
     */
    public void setAnchor(boolean isAnchor)
    {
        this.isAnchor = isAnchor;
    }

    /**
     * Sets {@link #laterationGain}.
     */
    public void setLaterationGain(double gain)
    {
        this.laterationGain = gain;
    }

    /**
     * Sets {@link #laterationThreshold}.
     */
    public void setLaterationThreshold(double threshold)
    {
        this.laterationThreshold = threshold;
    }

    public String getName()
    {
        return "Ad Hoc Positioning System (APS)";
    }

    protected void _start()
    {
        this.timer = setTimeout(this.getName(), this.probeInterval);
    }

    protected void _stop()
    {
        for (int i = 0; i < 3; i++)
        {
            this.myPos[i] = Double.NaN;
            this.estPos[i] = Double.NaN;
        }
        cancelTimeout(this.timer);
        this.anchorList.removeAllElements();
    }

    protected void _resume()
    {
        this.timer = setTimeout(this.getName(), this.probeInterval);
    }

    /**
     * Updates {@link #anchorList} using the information in the probe packet.
     * @return Boolean value indicating whether it forwards the packet or not.
     */
    protected synchronized boolean updateAnchorList(ProbePacket pkt)
    {
        ProbePacket arrived;
        boolean forwardPacket = true;

        // if the packet is originated from myself, discard it.
        if (this.nid == pkt.senderID)
        {
            return false;
        }
        // debug("receives " + pkt);

        try
        {
            arrived = (ProbePacket) pkt.clone();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

        Iterator itr = this.anchorList.iterator();
        while (itr.hasNext())
        {
            ProbePacket current = (ProbePacket) itr.next();

            // if found, remove old one and insert new one.
            if (current.senderID == arrived.senderID)
            {
                // if the anchor was not moved & longer path,
                // don't forward packet anymore.
                if ( ( (current.senderPos[0] == arrived.senderPos[0])
                      && (current.senderPos[1] == arrived.senderPos[1])
                      && (current.senderPos[2] == arrived.senderPos[2])
                      && (arrived.hopCount > current.hopCount)))
                {
                    arrived.hopCount = current.hopCount;
                    forwardPacket = false;
                }
                this.anchorList.remove(current);
                this.anchorList.add(arrived);
                return forwardPacket;
            }
        }
        // if not found, add the arrived packet to the list.
        this.anchorList.add(arrived);
        return forwardPacket;
    }

    /**
     * Port handler for WirelessAgentPort. If it is an anchor, it computes
     * its hop-distance, If it is a non-anchor, it estimates its location.
     * @return Boolean value indicating whether it handles the packet or not.
     */
    protected synchronized boolean onProcessingWirelessPort(SensorPacket sensorPacket)
    {
        if (sensorPacket.getBody()instanceof ProbePacket)
        {
            ProbePacket pkt = (ProbePacket) sensorPacket.getBody();
            boolean forwardPacket = this.updateAnchorList(pkt);
            if (forwardPacket)
            {
                ProbePacket p;
                try
                {
                    p = (ProbePacket) pkt.clone();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return true;
                }
                p.incHopCount();
                this.sendProbePacket(p);
                // debug("forwards " + p);
            }
            // If I am an anchor node, compute hop-distance.
            if (this.isAnchor)
            {
                this.hopDistance = this.computeHopDistance();
            }
            // If I am a non-anchor node, estimate my location and report its error.
            else
            {
                this.estimateLocation();

                if ( !Double.isNaN(estPos[0]) && !Double.isNaN(estPos[1])
                      && !Double.isNaN(estPos[2]) )
                {
                    double estError
                        = SensorLocApp.EuclideanDist(this.myPos, this.estPos);
                    if (this.errPort.anyOutConnection())
                    {
                        this.errPort.exportEvent(ESTIMATION_ERROR_EVENT,
                                                 new DoubleObj(estError), null);
                    }

                }
            }
            return true;
        }
        else // I did't processing it.
        {
            return false;
        }
    }

    public synchronized void processOther(Object data_, Port inPort_)
    {
        String portid_ = inPort_.getID();
        if (portid_.equals(FROM_WIRELESS_AGENT_PORT_ID))
        {
            if (! (data_ instanceof SensorPacket))
            {
                error(data_, "processOther()", inPort_, "unrecognized data");
                return;
            }
            if( ! this.onProcessingWirelessPort((SensorPacket)data_) )
            {
                super.processOther(data_, inPort_);
            }
        }
        else if (portid_.equals(MOBILITY_PORT_ID))
        {
            if (! (data_ instanceof PositionReportContract.Message))
            {
                error(data_, "processReport()", inPort_, "unrecognized data");
                return;
            }
            // updating my position information.
            PositionReportContract.Message msg
                = (PositionReportContract.Message) data_;
            if (this.nid == msg.getNid())
            {
				if( isDebugEnabled() )
				{
					debug("set my location " + msg);
				}

                this.myPos[0] = msg.getX();
                this.myPos[1] = msg.getY();
                this.myPos[2] = msg.getZ();

                if (this.isAnchor)
                {
                    for (int i = 0; i < 3; i++)
                    {
                        this.estPos[i] = this.myPos[i];
                    }
                }
                // if I am moved, reset list
                // because the saved hopcount is out of date.
                this.anchorList.removeAllElements();
            }
        }
        else
        {
            super.processOther(data_, inPort_);
        }
    }

    /**
     * The hop-distance is defined as the ratio of the summation of
     * Euclidean distances to the other anchor nodes to that of
     * the hop-counts to the anchor nodes.
     * @return The hop-distance
     */
    protected synchronized double computeHopDistance()
    {
        double di = 0., hi = 0.; // sum of Euclidean distances and hop-counts

        Iterator itr = this.anchorList.iterator();
        while (itr.hasNext())
        {
            ProbePacket anchor = (ProbePacket) itr.next();
            hi += anchor.hopCount;
            di += SensorLocApp.EuclideanDist(this.myPos, anchor.senderPos);
        }
        return (hi == 0) ? -1 : (di / hi);
    }

    /**
     * Finds the nearest anchor nodes.
     * @return ProbePacket originated from the nearest anchor node.
     */
    protected synchronized ProbePacket findNearestAnchor()
    {
        double dist, minValue = Double.MAX_VALUE;

        ProbePacket nearAnchor = null;
        Iterator itr = this.anchorList.iterator();
        while (itr.hasNext())
        {
            ProbePacket p = (ProbePacket) itr.next();
            dist = p.hopCount * p.hopDistance;
            if (dist < 0)
            {
                return null;
            }
            else if (dist < minValue)
            {
                minValue = dist;
                nearAnchor = p;
            }
        }
        return nearAnchor;
    }

    /**
     * The lateration algorithm. Instead of the triangularation method used
     * by D. Noculescu and B. Nath, it uses the gradient method.
     */
    protected synchronized void estimateLocation()
    {
        boolean exitFlag;
        double [] pos = new double[3], dpos = new double[3];
        double measured, computed;

        ProbePacket nearAnchor = this.findNearestAnchor();
		if (nearAnchor == null) // information is not sufficient.
		{
			return;
		}
        for (int i = 0; i < 3; i++) // assigning initial position
        {
            pos[i] = nearAnchor.senderPos[i];
            dpos[i] = 0.;
        }

        while (true)
        {
            exitFlag = true;
            Iterator itr = this.anchorList.iterator();
            while (itr.hasNext()) // computing the gradient value
            {
                ProbePacket p = (ProbePacket) itr.next();
                computed = SensorLocApp.EuclideanDist(pos, p.senderPos);
                measured = p.hopCount * p.hopDistance;

                if (computed != 0)
                {
                    for (int i = 0; i < 3; i++)
                    {
                        dpos[i] += (pos[i] - p.senderPos[i]) *
                            (1 - measured / computed);
                    }
                }
            }
            for (int i = 0; i < 3; i++) // updating position by using dpos.
            {
                pos[i] -= this.laterationGain * dpos[i];
                if (this.laterationGain * dpos[i] > // convergence test
                    this.laterationThreshold * pos[i])
                {
                    exitFlag = false;
                }
            }
            if (exitFlag)
            {
                break;
            }
        }
        for (int i = 0; i < 3; i++)
        {
            this.estPos[i] = pos[i];
        }
		if( isDebugEnabled() )
		{
        	debug("Real: " + this.myPos[0] + ", " + this.myPos[1] +
           	   " Est: " + this.estPos[0] + ", " + this.estPos[1]);
		}
    }

    /**
     * Broadcasts the probing packet to its neighbors.
     */
    protected synchronized void sendProbePacket(ProbePacket pkt)
    {
        SensorAppWirelessAgentContract.Message msg= new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET,ProbePacket.PACKET_TYPE, -1, -1, pkt);
        downPort.doSending(msg);
    }

    protected synchronized void timeout(Object data_)
    {
        if (data_.equals(this.getName()))
        {
            if (this.isAnchor)
            {
                // sending probing packets to its neighbors
                ProbePacket pkt = new ProbePacket(this);
                this.sendProbePacket(pkt);
                this.pktSequence++;
                // debug("sends " + pkt);
            }
            this.timer = setTimeout(this.getName(), this.probeInterval);
        }
        else
        {
            super.timeout(data_);
        }
    }

    /**
     * Computes the Euclidean distance between nodes with the Cartesian
     * coordinates <code>x</code> and <code>y</code>.
     */
    public static double EuclideanDist(double x[], double y[])
    {
        double sum = 0;
        int size = Math.min(x.length, y.length);
        for (int i = 0; i < size; i++)
        {
            sum += Math.pow(x[i] - y[i], 2);
        }
        return Math.sqrt(sum);
    }
}
