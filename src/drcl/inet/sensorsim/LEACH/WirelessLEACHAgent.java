package drcl.inet.sensorsim.LEACH;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/09/2005
 *
 * The WirelessLEACHAgent acts as a transport layer which connects the wireless protocol
 * stack to the LEACH application layer (see ({@link LEACHApp}). 
 *
*/


import drcl.comp.Port;
import drcl.inet.InetPacket;
import drcl.inet.sensorsim.SensorPacket;
//import drcl.inet.sensorsim.SensorAppWirelessAgentContract;

public class WirelessLEACHAgent extends drcl.inet.sensorsim.WirelessAgent
{
    public static final int LEACH_ADV_CH    = 0;
	public static final int LEACH_JOIN_REQ  = 1;
    public static final int LEACH_ADV_SCH   = 2;
    public static final int LEACH_DATA      = 3;

	public WirelessLEACHAgent()
	{
        super();
    }

	public WirelessLEACHAgent(String id_)
	{
        super(id_);
    }

    /**
     * Sends a unicast packet over the wireless channel
    *
	protected synchronized void sendPkt(long dst_, long src_, double[] dst_loc, int size_, int type_, int eventID_, long target_nid_, Object body_)
    {
		int bytesLeft = size_;
		int bytesSent = 0;
		// send the packet in SLOT_SIZE byte chunk for tdma
		while ( bytesLeft > 0 ) {
			bytesSent = (bytesLeft>=SLOT_SIZE)?SLOT_SIZE:bytesLeft;
            //this constructor is for unicast packets.
	        //SensorPacket(int pktType_, long dst_nid_, long src_nid_, int dataSize_,double [] dst_loc_,int eventID_, long target_nid_, Object body_)
			SensorPacket sensorPkt = new SensorPacket(type_, dst_, src_, bytesSent, dst_loc, eventID_, target_nid_, body_);
			forward(sensorPkt,bytesLeft,dst_loc, drcl.net.Address.NULL_ADDR, dst_, false, 255, 0);
			bytesLeft -= bytesSent;
		} // end while
	} */

	/**
     * Sends a broadcast packet over the wireless channel
    *
	public synchronized void sendBcastPkt(int type_, long src_nid_, int eventID_, Object body_, int size_)
	{
        //this constructor is for broadcast packets
        //SensorPacket(int pktType_, long src_nid_, int eventID_, Object body_, size_)
		SensorPacket sensorPkt = new SensorPacket(type_, src_nid_, eventID_, body_, size_);
		switch ( type_ )
		{
			case LEACH_ADV_CH :
				broadcast(sensorPkt, size_, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 1, 0);
				break ;
            case LEACH_JOIN_REQ:
                broadcast(sensorPkt, size_, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 1, 0);
                break;
            case LEACH_ADV_SCH:
                broadcast(sensorPkt, size_, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, true, 1, 0);
                break;
		}
	}  */

	/**
     * Handles data arriving at the UP port--> meaning info being sent down from LEACHApp
     *  is received at this port.
    *
	protected synchronized void dataArriveAtUpPort(Object data_, Port upPort_)
	{

		SensorAppWirelessAgentContract.Message msg = (SensorAppWirelessAgentContract.Message)data_ ;

        if (msg.getFlag() == SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET) {
			sendBcastPkt(msg.getType(), msg.getSrc(), msg.getEventID(), msg.getBody(), msg.getSize()) ;
            return;
        }
        if (msg.getFlag() == SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET) {
            sendPkt(msg.getDst(), msg.getSrc(), msg.getDst_loc(), msg.getSize(), msg.getType(), msg.getEventID(), msg.getTargetNid(), msg.getBody()) ;
            return;
        }
		else {
			System.out.println("Unknown Packet Type arrived at up port in WirelessLEACHAgent!");
            return;
        }
	} */

	/**
     * Handles data arriving at the DOWN port
     * received packet from the routing layer, needs to forward to the sensor application layer
    */
	protected synchronized void dataArriveAtDownPort(Object data_, Port downPort_)
	{

		InetPacket ipkt_ = (InetPacket)data_;

		if ( ipkt_.getBody() instanceof SensorPacket )
		{
			SensorPacket pkt_ = (SensorPacket)ipkt_.getBody();
			switch ( pkt_.getPktType())
			{
				case LEACH_ADV_CH :
                    //this constructor is for broadcast packets
                    //LEACH_SensorPacket(int pktType_, long src_nid_, int eventID_, int dataSize_, Object body_)
					toSensorAppPort.doSending(new SensorPacket(LEACH_ADV_CH, pkt_.getSrc_nid(),pkt_.getEventID(), pkt_.getBody(),pkt_.getPacketSize()));
					break ;
				case LEACH_JOIN_REQ :
                    //this constructor is for broadcast packets
                    //LEACH_SensorPacket(int pktType_, long src_nid_, int eventID_, int dataSize_, Object body_)
					toSensorAppPort.doSending(new SensorPacket(LEACH_JOIN_REQ, pkt_.getSrc_nid(),pkt_.getEventID(), pkt_.getBody(), pkt_.getPacketSize()));
					break ;
				case LEACH_ADV_SCH :
                    //this constructor is for unicast packets.
                    //LEACH_SensorPacket(int pktType_, long dst_nid_, long src_nid_, int dataSize_,int eventID_, targetID,  Object body_)	{
					toSensorAppPort.doSending(new SensorPacket(LEACH_ADV_SCH, pkt_.getDst_nid(), pkt_.getSrc_nid(),pkt_.getPacketSize(), pkt_.getEventID(), -1, pkt_.getBody()));
					break ;
                case LEACH_DATA :
                     //this constructor is for unicast packets.
                    //LEACH_SensorPacket(int pktType_, long dst_nid_, long src_nid_, int dataSize_,int eventID_, Object body_)	{
					toSensorAppPort.doSending(new SensorPacket(LEACH_DATA, pkt_.getDst_nid(), pkt_.getSrc_nid(),pkt_.getPacketSize(), pkt_.getEventID(), -1, pkt_.getBody()));
					break ;
				default :
                    System.out.println("*******The packet is not made for LEACH system its of type: " + pkt_.getPktType() + " going to sensorAPP instead");
					super.dataArriveAtDownPort(data_, downPort_) ;
			}
            return;
		}
		else
		{
            //erroneous message for the LEACH system
            System.out.println("*****Warning: WirelessLEACHAgent Did not receive a SensorPacket instead it received: " + ipkt_.getBody().getClass());
			super.dataArriveAtDownPort(data_, downPort_) ;
		}
        return;
	}
}
