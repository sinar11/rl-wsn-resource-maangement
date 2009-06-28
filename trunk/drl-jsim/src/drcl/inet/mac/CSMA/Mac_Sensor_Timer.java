package drcl.inet.mac.CSMA;

import drcl.comp.ACATimer;
import drcl.inet.mac.MacTimeoutEvt;


/**
 * User: Nicholas Merizzi
 * Date: May 16, 2005
 * Time: 12:08:01 AM
 */

public class  Mac_Sensor_Timer {


	public MacTimeoutEvt   o_;

	public boolean     busy_;      //true iff timer is running
	public boolean  	paused_;    //true if timer was paused.
	public double		stime;	    // Start time
	public double		rtime;      // Remaining time
	public double		slottime_;

	Mac_CSMA  host_;       // to which MAC component this timer belongs to.
    ACATimer timer_;

    public static final Mac_Sensor_Timer INSTANCE = new Mac_Sensor_Timer();

    public Mac_Sensor_Timer(Mac_CSMA h, double s) {
		busy_    = false;
		paused_  = false;
		stime    = 0;
		rtime    = 0.0;
		slottime_ = s;
		host_     = h;
		o_ = new MacTimeoutEvt(MacTimeoutEvt.Testing_timeout);
	}

	public Mac_Sensor_Timer(Mac_CSMA h) {
		this(h, 1.0);
	}
    public Mac_Sensor_Timer() { }

    public void handle() {
        //to be overridden
    }

	/**
	  * Start the timer
	  * @param time - duration
	  */
	public void start(double time) {
        _assert("Mac_802_11_Timer start()", "busy_ == false", (busy_ == false));
		busy_ = true;
		paused_ = false;
	    stime = host_.getTime();
		rtime = time;
        _assert("Mac_802_11_Timer start()", "rtime >= 0.0", (rtime >= 0.0));
		timer_ = host_.setTimeout(o_, rtime);
	}

	public void stop( ) {
        _assert("Mac_802_11_Timer stop()", "busy_ == true", (busy_ == true));

		if (paused_ == false && timer_ != null)
		    host_.cancelTimeout(timer_);

		busy_ = false;
		paused_ = false;
		stime = 0.0;
		rtime = 0.0;
	}

	public void pause( ) {}
	public void resume( ) {}

	public boolean busy( )
    {
        return busy_;
    }

	public boolean paused( )
    {
        return paused_;
    }

    public double slottime()
    {
        return this.slottime_;
    }

	public double expire()
    {
		return ((stime + rtime) - host_.getTime());
	}

    protected void _assert(String where, String why, boolean continue_)
    {
        if ( continue_ == false )
            drcl.Debug.error(where, why, true);
        return;
    }
}

