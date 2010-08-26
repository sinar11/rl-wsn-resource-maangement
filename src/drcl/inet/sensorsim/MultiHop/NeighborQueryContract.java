package drcl.inet.sensorsim.MultiHop;

import drcl.comp.Contract;
import drcl.comp.Port;
import drcl.inet.sensorsim.SensorNeighborQueryContract;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 04/27/2005
 *
 * A contract used in the multi-hop mode which allows a sensor to query the SensorNodePosition
 * track to determine its location
 */

public class NeighborQueryContract extends Contract
{

	public static final NeighborQueryContract INSTANCE = new NeighborQueryContract();

	public NeighborQueryContract()
    {
        super();
    }

	public String getName()
    {
        return "MultiHop Neighbor Query Contract";
    }

	public Object getContractContent()
    {
        return null;
    }

	/**
     * Sends a query and receives a reply
    */
	public static Object query(long nid_, double X_, double Y_, double Z_, Port out_)
	{
        return out_.sendReceive(new SensorNeighborQueryContract.Message(nid_, X_, Y_, Z_));
    }

	/**
     * Sends a reply
    */
	public static void reply(long[] nodeList_, Port out_)
	{
        out_.doSending(new SensorNeighborQueryContract.Message(nodeList_));
    }

	/**
     * This class implements the underlying message of the contract.
    */
    public static class Message extends drcl.comp.Message
    {
        long   node_nid;         //the nodes ID
        double [] node_pos;
        double [] sink_pos;
        double sink_dist;

        long neighbor_nid;
        double neighbor_dist;
        double[] neighbor_pos;

        /*Constructor for messages going from MULTIHOP_APP TO SENSORNODEPOSITIONTRACKER */
        public Message (long nid_, double[] node_pos_, double[] sink_pos_, double sink_dist_)
        {
            node_nid = nid_;
            this.node_pos = new double[3];
            this.sink_pos = new double[3];
            this.node_pos[0] = node_pos_[0];  this.node_pos[1] = node_pos_[1];  this.node_pos[2] = node_pos_[2];
            this.sink_pos[0] = sink_pos_[0];  this.sink_pos[1] =sink_pos_[1]; this.sink_pos[2] =sink_pos_[2];
            sink_dist = sink_dist_;
        }

        /*Constructor for messages going from SENSORNODEPOSITIONTRACKER TO MULTIHOP_APP */
        public Message (long neighbor_nid_, double neighbor_dist_, double[] neighbor_loc_)
        {
            neighbor_nid = neighbor_nid_;
            this.neighbor_pos = new double[3];
            this.neighbor_pos[0] = neighbor_loc_[0]; this.neighbor_pos[1] =neighbor_loc_[1]; this.neighbor_pos[2] =neighbor_loc_[2];
            neighbor_dist = neighbor_dist_;
        }

        public long   getNode_nid()         { return node_nid; }
        public double getNode_nidX()        { return this.node_pos[0]; }
        public double getNode_nidY()        { return this.node_pos[1]; }
        public double getNode_nidZ()        { return this.node_pos[2]; }
        public double getSink_X()           { return this.sink_pos[0]; }
        public double getSink_Y()           { return this.sink_pos[1]; }
        public double getSink_Z()           { return this.sink_pos[2]; }
        public double getSinkDistance()     { return sink_dist; }

        public long   getneighbor_nid()     { return neighbor_nid; }
        public double getNeighbor_nidX()    { return this.neighbor_pos[0]; }
        public double getNeighbor_nidY()    { return this.neighbor_pos[1]; }
        public double getNeighbor_nidZ()    { return this.neighbor_pos[2]; }
        public double getNeighbor_dist()    { return neighbor_dist; }
        public double[] getNeighborLoc()    { return neighbor_pos; }

        public Object clone() 	{
            return new NeighborQueryContract.Message(node_nid, this.node_pos, this.sink_pos, this.sink_dist);
        }

        public Contract getContract()
        {
            return INSTANCE;
        }

        public String toString(String separator_)
        {
                String str;
                str = "Neighbor Query";
                return str;
        }
	}
}

