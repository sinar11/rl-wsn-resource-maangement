package drcl.inet.sensorsim.LEACH;

import drcl.net.Packet;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 06/15/2005
 *
 *  Class which defines the body of a message for a unicast packet
 *  that is either being sent from a sensor to a CH or from a
 *  CH to the base station.
 * 
*/

public class LEACH_Data_Packet extends Packet{

    long sender_id;
    long CH_id;
    double timeStamp;               //what time the packet was sent at
    int code;

    public LEACH_Data_Packet(long sender_id_, long CH_id_, Object sensedData_, double timeStamp_, int code_, int size_){
        this.sender_id = sender_id_;
        this.CH_id = CH_id_;
        body = sensedData_;
        this.timeStamp = timeStamp_;
        this.code = code_;
        size = size_;
    }

    public long getSender_id()
    {
        return sender_id;
    }

    public long getCH_id()
    {
        return CH_id;
    }

    public double getSendTime()
    {
        return (timeStamp);
    }
    public int getCode()
    {
        return code;
    }

    public String getName()
	{
        return "LEACH_Data_Packet";
    }

	public Object clone()
	{
		return new LEACH_Data_Packet(this.sender_id, this.CH_id, body, this.timeStamp, this.code, size);
	}
}
