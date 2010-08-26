package drcl.inet.sensorsim.MultiHop;

import drcl.net.Packet;
import drcl.data.DoubleObj;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/15/2005
 *
 * This class defines the data packet which sensors will send back to the base station.
 * It essentially contains 5 pieces of information:
 * Who sent it, the senders location (i.e. its coordinates), a timestamp to keep
 * track of latency, the size, and the latest data sensed (i.e the phenomenon the sensors
 * are sensing).
 *
*/

public class MH_Packet extends Packet
{

    long sender_id;                 //sender ID
    private double[] sender_pos;    //what position the sender is at
    double timeStamp;               //what time the packet was sent at

    public MH_Packet(long sid, double[] s_pos, double timeStamp_, int size_,  Object phenomenon_)
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
        return "MH_Packet";
    }

    public Object clone()
    {
        return new MH_Packet(this.sender_id,this.sender_pos, this.timeStamp, size, (DoubleObj)body);
    }

}

