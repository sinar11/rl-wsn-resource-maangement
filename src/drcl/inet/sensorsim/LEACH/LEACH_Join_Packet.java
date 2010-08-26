package drcl.inet.sensorsim.LEACH;

import drcl.net.Packet;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 06/15/2005
 *
 * Class which defines the body of a message that all non
 * cluster heads send back to the CH they have chosen. Sent
 * as a broadcast message.
 * 
*/

public class LEACH_Join_Packet extends Packet
{

    long src_id;    //Who the joining node is. (i.e. who sent the request)
    long chID;      //the cluster head you are joining
    int code;

    public LEACH_Join_Packet(long src_id_, long chID_, int code_, int size_)
    {
        src_id = src_id_;
        chID = chID_;
        this.code = code_;
        size = size_;
    }

    public long getSrc_id()
    {
        return src_id;
    }

    public long getChID()
    {
        return chID;
    }

    public int getCode()
    {
        return code;
    }


    public String getName()
	{
        return "LEACH_Join_Packet";
    }

	public Object clone()
	{
		return new LEACH_Join_Packet(this.src_id,this.chID, this.code, size);
	}

}
