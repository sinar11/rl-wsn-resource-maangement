package drcl.inet.sensorsim.drl;

public class Factory {
	private static GlobalRewardManager globalRwdManager=null;
	
	
	public static synchronized GlobalRewardManager getGlobalRewardManagerInstance() throws Exception{
		if(globalRwdManager==null){
			String glRewManager= System.getProperty("rewardManager","BD");
			if(glRewManager.equals("BD"))
		    	globalRwdManager= new BDGlobalRewardManager();
			/*else if(glRewManager.equals("WL"))
		    	globalRwdManager= new WLRewardManager();*/
			else{
				throw new Exception("No global reward manager defined for:"+glRewManager);
			}
		}
		return globalRwdManager;
	}
}
