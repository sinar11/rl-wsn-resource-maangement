package drcl.inet.sensorsim.LEACH;

import drcl.net.Packet;
import java.util.Vector;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 06/16/2005
 *
 * Class which defines the body of a message for a schedule
 * being broadcasted out to all its nodes that are part of the
 * schedule.
*/

public class LEACH_SCH_Packet extends Packet
{

    long cluster_head;  //id of the cluster head in this transaction (i.e. sender)
    Vector schedule = new Vector();
    int code;


    public LEACH_SCH_Packet(long chID_, Vector schedule_,int code_, int size_) {
        cluster_head = chID_;
        schedule = schedule_;
        this.code = code_;
        size = size_;
    }

    public long getchID()
    {
        return cluster_head;
    }

    public Vector getSchedule()
    {
        return schedule;
    }

    public int getCode()
    {
        return code;
    }

    public String getName()
	{
        return "LEACH_SCH_Packet";
    }

	public Object clone()
	{
		return new LEACH_SCH_Packet(this.cluster_head,this.schedule, this.code, size);
	}
}