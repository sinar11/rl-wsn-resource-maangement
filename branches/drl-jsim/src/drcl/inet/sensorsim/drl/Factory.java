package drcl.inet.sensorsim.drl;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class Factory {
	private static GlobalRewardManagerMBean globalRwdManager=null;
	private static MBeanServer mbeanServer=null;
	
	public static synchronized GlobalRewardManagerMBean getGlobalRewardManagerInstance() throws Exception{
		if(globalRwdManager==null){
			String glRewManager= System.getProperty("rewardManager","BD");
			if(glRewManager.equals("BD"))
		    	globalRwdManager= new GlobalRewardManager();
			/*else if(glRewManager.equals("WL"))
		    	globalRwdManager= new WLRewardManager();*/
			else{
				throw new Exception("No global reward manager defined for:"+glRewManager);
			}
			getMBeanServer().registerMBean(globalRwdManager, new ObjectName("sensorsim.drl","GlobalRewardManager","GlobalRewardManager"));			
		}
		return globalRwdManager;
	}
	
	public static MBeanServer getMBeanServer() throws Exception{
		if(mbeanServer==null)
			mbeanServer=ManagementFactory.getPlatformMBeanServer();
		return mbeanServer;
	}
}
