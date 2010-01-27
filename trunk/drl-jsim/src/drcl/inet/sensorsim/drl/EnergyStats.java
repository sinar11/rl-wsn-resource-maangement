package drcl.inet.sensorsim.drl;


public class EnergyStats {
	static int noOfNodes;
	static double totalEnergyUsed;
	static double totalEnergyWhileReporting;
	
	public static class NodeStat{
		public int getNid() {
			return nid;
		}
		public double getEnergyUsed() {
			return energyUsed;
		}
		public boolean isAlive() {
			return alive;
		}
		public double getLifetime() {
			return lifetime;
		}
		int nid;
		double energyUsed;
		boolean alive;
		double lifetime;
		NodeStat(int nid, double energyUsed){
			this.nid=nid;
			this.energyUsed=energyUsed;
			this.alive=true;
		}
		public String toString(){
			return nid+":"+lifetime+":"+energyUsed;
		}
	}
	
	static NodeStat[] stats;
	
	public static void init(int nn){
		noOfNodes=nn;
		stats= new NodeStat[nn];
		for(int i=0;i<nn;i++){
			stats[i]= new NodeStat(i,0);			
		}
	}
	
	public static void update(int nid, double energy, boolean isAlive, double time){
		stats[nid].alive=isAlive;
		if(isAlive){
			stats[nid].lifetime=time;
			totalEnergyUsed=totalEnergyUsed-stats[nid].energyUsed+energy;
			stats[nid].energyUsed=energy;			
		}
	}

	public static void markAsReporting(){
		totalEnergyWhileReporting=totalEnergyUsed;
	}
	
	public static double getTotalEnergy(){
		return totalEnergyWhileReporting;
	}
	
	public static NodeStat getNodeWithLowestLifetime(){
		NodeStat lowest=null;
		for(int i=0;i<stats.length;i++){
			if(stats[i].lifetime==0) continue;
			if(lowest==null){
				lowest=stats[i];
				continue;
			}else if(stats[i].lifetime<lowest.lifetime){
				lowest=stats[i];
			}
		}
		return lowest;
	}
}
