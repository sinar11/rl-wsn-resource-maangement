package drcl.inet.sensorsim.LEACH;

import drcl.net.Packet;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 06/15/2005
 *
 * This class defines the data packet which LEACH sensors will generate
 * if and only if it has elected itself as a Cluster head and it needs
 * to advertise to the others its new status.
 *
*/

public class LEACH_CH_Packet extends Packet {

    long cluster_head;  //id of the cluster head in this transaction (i.e. who sent the bcast)
    private double[] sender_pos;    //the coordinates (X,Y,Z) of the sender
    int code;

    public LEACH_CH_Packet(long sid, double[] s_pos, int code_, int size_)
    {
        cluster_head = sid;
        this.sender_pos = new double[3];
        this.sender_pos = s_pos;
        this.code = code_;
        this.size = size_;
    }

    public long getCH()
    {
        return(cluster_head);
    }

    public double[] getSenderPos()
    {
        return(sender_pos);
    }

    public double getSenderX()
    {
        return (sender_pos[0]);
    }

    public double getSenderY()
    {
        return (sender_pos[1]);
    }

    public double getSenderZ()
    {
        return (sender_pos[2]);
    }

    public int getCode()
    {
        return code;
    }

    public String getName()
	{
        return "LEACH_CH_Packet";
    }

	public Object clone()
	{
		return new LEACH_CH_Packet(this.cluster_head,this.sender_pos, this.code, this.size);
	}
}
