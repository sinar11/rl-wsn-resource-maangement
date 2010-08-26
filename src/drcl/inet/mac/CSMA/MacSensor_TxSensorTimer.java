package drcl.inet.mac.CSMA;

import drcl.inet.mac.MacTimeoutEvt;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/15/2005
 *
 * Acts as the Transmission timer for MacSensor (i.e. the CSMA Protocol)
 */

public class MacSensor_TxSensorTimer extends Mac_Sensor_Timer {


    public MacSensor_TxSensorTimer(Mac_CSMA h) 
    {
		super(h);
		o_.setType(MacTimeoutEvt.Tx_timeout);
	}

    /**
     * Handles TxTimer timeout event. This method is called in MacSensor class.
    */
    public void handle()
    {
        busy_ = false;
        paused_ = false;
        stime = 0.0;
        rtime = 0.0;
    }
}
