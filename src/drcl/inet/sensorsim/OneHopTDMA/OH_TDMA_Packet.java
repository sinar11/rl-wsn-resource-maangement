package drcl.inet.sensorsim.OneHopTDMA;

import drcl.net.Packet;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 06/03/2005
 *
 * This class represent the body of a packet that is used when sensors want to send their
 * periodic updates back to the base station. It is essentially a data packet which contains
 * 5 pieces of information: Who sent it, the senders location (i.e. its coordinates), a
 * timestamp to keep track of latency, the size, and the latest data sensed (i.e the phenomenon the sensors
 * are sensing).
 *
*/

public class OH_TDMA_Packet  extends Packet {

    long sender_id;                 //sender ID
    private double[] sender_pos;    //what position the sender is at
    double timeStamp;               //what time the packet was sent at


    public OH_TDMA_Packet(long sid, double[] s_pos, double timeStamp_, int size_, Object phenomenon_)
    {
        sender_id = sid;
        this.sender_pos = new double[3];
        this.sender_pos = s_pos;
        this.timeStamp = timeStamp_;
        size = size_;
        body = phenomenon_;
    }

    public long getSID()
    {
        return(sender_id);
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

    public double getSendTime()
    {
        return (timeStamp);
    }

    public String getName()
	{
        return "One Hop TDMA Packet";
    }


	public Object clone()
	{
		return new OH_TDMA_Packet(this.sender_id,this.sender_pos, this.timeStamp, size, body);
	}
}
