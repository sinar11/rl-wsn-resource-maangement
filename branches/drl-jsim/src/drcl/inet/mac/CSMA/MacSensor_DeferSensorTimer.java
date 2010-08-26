package drcl.inet.mac.CSMA;

import drcl.inet.mac.MacTimeoutEvt;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/29/2005
 *
 *
 */
public class MacSensor_DeferSensorTimer extends Mac_Sensor_Timer
 {

    public MacSensor_DeferSensorTimer(Mac_CSMA h, double s)
    {
        super(h, s);
		o_.setType(MacTimeoutEvt.Defer_timeout);
    }

    /**
    * Contructor.
    */
	public MacSensor_DeferSensorTimer(Mac_CSMA h)
    {
		super(h, 0);
		o_.setType(MacTimeoutEvt.Defer_timeout);
	}

    /**
     * Handles DfTimer timeout event. This method is called in MacSensor class.
    */
    public void handle()
    {
        busy_ = false;
        paused_ = false;
        stime = 0.0;
        rtime = 0.0;
    }

    /**
     * Starts the timer.
     */
    public void start(double time)
    {
        _assert("DeferTimer start()", "busy_ == false", (busy_ == false));
     	busy_ = true;
		paused_ = false;
		stime = host_.getTime();
		rtime = time;

        _assert("DeferTimer start()", "rtime >= 0.0", (rtime >= 0.0));
		host_.setTimeout(o_, rtime);
	}
}