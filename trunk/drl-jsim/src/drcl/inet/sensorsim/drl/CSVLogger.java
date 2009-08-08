/* Copyright 2003 SensorLogic, Inc. All rights reserved.
 * SENSORLOGIC PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package drcl.inet.sensorsim.drl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/*
 * @author Kunal
 */

public class CSVLogger {

    String filename;
    FileOutputStream fout;
    static HashMap loggers= new HashMap();
    static String currentRun=System.currentTimeMillis()+"";
    
    public CSVLogger(String filename){
        this.filename=filename;
        String dir="results"+File.separator+DRLSensorApp.algorithm+File.separator+currentRun;
        
        new File(dir).mkdirs();
        
        try {
            fout= new FileOutputStream(dir+File.separator+filename+".csv");
            loggers.put(filename, this);
        } catch (FileNotFoundException e) {
            System.out.println("Error opening file");
            e.printStackTrace();
        }
    }
    
    public void log(String log, boolean printout){
        try {
            if(printout)
                System.out.println("["+filename+"]"+log);
            fout.write((log+"\n").getBytes());
            fout.flush();
        } catch (IOException e) {
            System.out.println("Error opening file");
            e.printStackTrace();
        }
    }
    
    public static void log(String type, String log){
        log(type,log,true);
    }
    public static synchronized void log(String type, String log, boolean printOut){
        CSVLogger logger=(CSVLogger) loggers.get(type);
        if(logger==null){
            logger= new CSVLogger(type);
        }
        logger.log(log,printOut);
    }
    
}
