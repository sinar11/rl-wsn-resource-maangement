package drcl.inet.sensorsim.drl.diffext ;

import java.util.List;

/** This class implements an interest packet carrying the description of a task.
*
* @author Kunal Shah
* 
*/

public class InterestPacket
{
	/** Unique task id. Can be created using sinkId+no of tasks created by that sink **/
	private final int taskId;
	/** unique sink node's id **/ 
	private final long sinkId;
	/** Source who sends the current packet **/
	private long sourceId; 
	
	/** Optional id of current destination node if targetted to one particular node or -1 for broadcast **/ 
	private long destinationId=DRLDiffApp.BROADCAST_DEST;
	
	/** The list of attributes that sink is interested in */
	private List<Tuple> attributes = null ;
	/** time at this interest was published **/
	private final double timestamp;
	/** Optional List of QoS contraints associated with this task **/
	private List<Tuple> qosConstraints=null;
	/**List of cost parameters associated with this task **/
	private List<CostParam> costParameters=null;
	
	private double dataInterval;
	
	private double duration;
	
	/** Payment associated with this task, initial value set by application/sink and each node
	 * updates the value after deducting its cost and profit.
	 */
	private double payment;
	
	
	public InterestPacket(int taskId, long sinkId, List<Tuple> attributes, double payment, double timestamp, double dataInterval, List<CostParam> costParameters)
	{
		this.taskId=taskId;
		this.sinkId=sinkId;
		this.attributes=attributes;
		this.timestamp=timestamp;
		this.costParameters=costParameters;		
		this.dataInterval=dataInterval;
		this.payment=payment;
	}

	public InterestPacket(InterestPacket pkt){
		this.taskId=pkt.taskId;
		this.sinkId=pkt.sinkId;
		this.sourceId=pkt.sourceId;
		this.attributes=pkt.attributes;
		this.timestamp=pkt.timestamp;
		this.costParameters=pkt.costParameters;
		this.qosConstraints=pkt.qosConstraints;
		this.payment=pkt.payment;
		this.dataInterval=pkt.dataInterval;
		this.duration=pkt.duration;
	}
	
	public String toString(String separator_){
		return "InterestPacket[taskId="+taskId+",sinkId="+sinkId+",attributes="+attributes+",timestamp="+timestamp+"]";		
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InterestPacket other = (InterestPacket) obj;
		if (sinkId != other.sinkId)
			return false;
		if (taskId != other.taskId)
			return false;
		if (Double.doubleToLongBits(timestamp) != Double
				.doubleToLongBits(other.timestamp))
			return false;
		return true;
	}

	public void updatePayment(double amtToDeduct){
		this.payment-=amtToDeduct;
	}

	public long getSourceId() {
		return sourceId;
	}

	public void setSourceId(long sourceId) {
		this.sourceId = sourceId;
	}

	public List<Tuple> getQosConstraints() {
		return qosConstraints;
	}

	public void setQosConstraints(List<Tuple> qosConstraints) {
		this.qosConstraints = qosConstraints;
	}

	public void setPayment(double payment) {
		this.payment = payment;
	}

	public int getTaskId() {
		return taskId;
	}

	public long getSinkId() {
		return sinkId;
	}

	public List<Tuple> getAttributes() {
		return attributes;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public List<CostParam> getCostParameters() {
		return costParameters;
	}

	public double getPayment() {
		return payment;
	}


	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	public double getDataInterval() {
		return dataInterval;
	}

	public static class CostParam{
		public static enum Type{Energy,NoOfHops,Lifetime};
		Type type;
		double weight;
		
		public CostParam(Type type, double weight){
			this.type=type;
			this.weight=weight;
		}
	}

	/**
	 * Checks if two interest packets are similar and hence can be replaced by one single interest packet
	 * @param interest
	 * @return
	 */
	public boolean isMatching(InterestPacket interest) {
		if(!this.attributes.equals(interest.attributes)) return false;
		if(!this.costParameters.equals(interest.costParameters)) return false;
		if(this.qosConstraints!=null && !this.qosConstraints.equals(interest.qosConstraints)) return false;
		return true;
	}

	public long getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(long destinationId) {
		this.destinationId = destinationId;
	}	
	
	public String toString(){
		return "InterestPacket[taskId="+taskId+",sinkId="+sinkId+",sourceId="+sourceId+",destinationId="+destinationId+",payment="+payment;
	}
}
