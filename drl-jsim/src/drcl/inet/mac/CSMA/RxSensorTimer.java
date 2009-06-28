package drcl.inet.mac.CSMA;

import drcl.inet.mac.MacTimeoutEvt;

/**
 * User: Nicholas Merizzi
 * Date: May 21, 2005
 * Time: 1:13:28 PM
 */
public class RxSensorTimer  extends Mac_Sensor_Timer {

    public RxSensorTimer(Mac_CSMA h) {
		super(h);
		o_.setType(MacTimeoutEvt.Rx_timeout);
	}

    /**
     * Handles RxTimer timeout event. This method is called in MacSensor class.
    */
    public void handle() {
        busy_ = false;
        paused_ = false;
        stime = 0.0;
        rtime = 0.0;
    }
}
