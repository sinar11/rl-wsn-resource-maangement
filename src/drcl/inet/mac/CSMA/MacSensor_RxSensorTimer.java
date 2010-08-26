package drcl.inet.mac.CSMA;

import drcl.inet.mac.MacTimeoutEvt;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 04/29/2005
 *
 *
 */
public class MacSensor_RxSensorTimer  extends Mac_Sensor_Timer {

    public MacSensor_RxSensorTimer(Mac_CSMA h) {
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
