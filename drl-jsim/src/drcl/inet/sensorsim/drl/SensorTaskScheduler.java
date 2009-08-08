/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim.drl;

import java.util.List;

/*
 * @author Kunal
 */

public class SensorTaskScheduler {

    protected static SensorTaskScheduler schedulerInstance;
    
    
    public static synchronized SensorTaskScheduler getInstance(){
        if(schedulerInstance==null){
            schedulerInstance=new SensorTaskScheduler();
        }
        return schedulerInstance;
    }
    
}
