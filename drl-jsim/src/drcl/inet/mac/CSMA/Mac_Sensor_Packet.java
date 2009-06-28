package drcl.inet.mac.CSMA;

import drcl.inet.mac.Mac_802_11_Packet;
import drcl.inet.mac.Mac_802_11_Frame_Control;

/**
 * User: Nicholas Merizzi
 * Date: May 21, 2005
 * Time: 2:22:05 PM


 Data frame

----------------------------------------------------------------------------------------------------------------------------------
|    FC (2B)    | Duration (2B) |      DA (6B)   |    SA (6B)   |   BSSID (6B)   |  Seq Control (2B)  |  N/A (6B)  |  FCS (4B)   |
----------------------------------------------------------------------------------------------------------------------------------
 */

/**
 * This class defines the IEEE802.11 Data frame stucture.
 *
 * @see drcl.inet.mac.Mac_802_11
 * @see drcl.inet.mac.Mac_802_11_Packet
 * @see drcl.inet.mac.Mac_802_11_CTS_Frame
 * @see drcl.inet.mac.Mac_802_11_RTS_Frame
 * @see drcl.inet.mac.Mac_802_11_ACK_Frame
 * @see drcl.inet.mac.Mac_802_11_Frame_Control
 * @author Ye Ge
 */
public class Mac_Sensor_Packet extends Mac_802_11_Packet
{
    static final int Mac_Sensor_Packet_Header_Length = 34;

    public String getName()  { return "MAC-802.11_Data_Frame"; }

    /*  define the structure of Data frame, refer to Figure 22   */
	long    da;        // 6 bytes    // the receiver's address
	long    sa;        // 6 bytes    // the sender's address
	long    bssid;     // 6 bytes
	int     scontrol;  // 2 bytes
    int     code_;              //NICHOLAS: I ADDED for SS
    int     type;               //what type of packet it is (i.e.

   	/** Construct an uncorrupted 802_11 data packet
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param da_ - destination MAC address
	  * @param sa_ - source MAC address
	  * @param bssid_ - id of basic service set
	  * @param fcs_ - frame check sequence
	  * @param hsize_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
	  */
	public Mac_Sensor_Packet(Mac_802_11_Frame_Control fc_, int duration_, long da_, long sa_, long bssid_, int fcs_,
								 int hsize_, int bsize_, int type_, Object body_) {
		super(hsize_, bsize_, body_, fc_, duration_, fcs_, false);
		da = da_;
		sa = sa_;
        type = type_;  //THE TYPE OF THE MESSAGE
	}

   	/** Construct a 802_11 data packet
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param da_ - destination MAC address
	  * @param sa_ - source MAC address
	  * @param bssid_ - id of basic service set
	  * @param fcs_ - frame check sequence
      * @param ferror_ - indicating if the packet is corrupted
	  * @param hsize_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
	  */
   	public Mac_Sensor_Packet(Mac_802_11_Frame_Control fc_, int duration_, long da_, long sa_, long bssid_, int fcs_, boolean ferror_,
								 int hsize_, int bsize_, Object body_) {
		super(hsize_, bsize_, body_, fc_, duration_, fcs_, ferror_);
		da = da_;
		sa = sa_;
	}

   	/** Construct a 802_11 data packet with sequence control
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param da_ - destination MAC address
	  * @param sa_ - source MAC address
	  * @param bssid_ - id of basic service set
      * @param ferror_ - indicating if the packet is corrupted
	  * @param hsize_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
	  * @param fcs_ - frame check sequence
	  * @param scontrol_ - sequence control
	  */
   	public Mac_Sensor_Packet(Mac_802_11_Frame_Control fc_, int duration_, long da_, long sa_, long bssid_, int fcs_, boolean ferror_,
								 int hsize_, int bsize_, Object body_, int scontrol_) {
		super(hsize_, bsize_, body_, fc_, duration_, fcs_, ferror_);
		da = da_;
		sa = sa_;
        scontrol = scontrol_;
	}

    public int getType() { return type; }

	/** Get destination MAC address */
    public long getDa( ) { return da; }
	/** Set destination MAC address */
	public void setDa(long da_) { da = da_; }

	/** Get source MAC address */
	public long getSa( ) { return sa; }
	/** Set source MAC address */
	public void setSa(long sa_) { sa = sa_; }

	public Object clone()	{
	    return new Mac_Sensor_Packet(
						(Mac_802_11_Frame_Control)fc.clone(),
						duration,
						da,
						sa,
						bssid,
						fcs,
						forcedError,
						headerSize,
						size-headerSize,
						body instanceof drcl.ObjectCloneable? ((drcl.ObjectCloneable)body).clone(): body,
						scontrol);
	}

	public String _toString(String separator_)	{
		return "Mac_Sensor_Packet" + separator_ + "duration:" + duration + separator_ + "da:" + da + separator_ + "sa:" + sa + separator_ + "forcedError:" + forcedError + separator_;
    }
}
