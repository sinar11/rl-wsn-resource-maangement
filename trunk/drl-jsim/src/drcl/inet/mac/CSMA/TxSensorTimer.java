package drcl.inet.mac.CSMA;

import drcl.inet.mac.MacTimeoutEvt;

/**
 * User: Nicholas Merizzi
 * Date: May 21, 2005
 * Time: 1:13:13 PM
 */
public class TxSensorTimer extends Mac_Sensor_Timer {

    public TxSensorTimer(Mac_CSMA h) {
		super(h);
		o_.setType(MacTimeoutEvt.Tx_timeout);
	}

    /**
     * Handles TxTimer timeout event. This method is called in MacSensor class.
    */
    public void handle() {
        busy_ = false;
        paused_ = false;
        stime = 0.0;
        rtime = 0.0;
    }
}
