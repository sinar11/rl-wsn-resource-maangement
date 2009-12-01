package drcl.inet.sensorsim.drl.diffext ;


/** This class implements a reinforcement packet
*
* @author Kunal Shah
* 
*/

public class ReinforcementPacket
{
	/** The task/interest for which this provides reinforcement (+ve or -ve) */
	private int taskId;

	/** The source of the packet */
	private long sourceId;

	/** New payment value associated with about task **/
	private double payment;
	
	/** Destination for this packet **/
	private long destinationId;
	
	public ReinforcementPacket(int taskId, double payment, long sourceId, long destinationId){
		this.taskId=taskId;
		this.sourceId = sourceId ;
		this.payment=payment;		
		this.destinationId=destinationId;
	}

	public ReinforcementPacket(ReinforcementPacket pkt){
		this.taskId=pkt.taskId;
		this.sourceId = pkt.sourceId ;
		this.payment=pkt.payment;		
		this.destinationId=pkt.destinationId;
	}
	
	@Override
	public String toString(){
		return "ReinforcementPacket[taskId="+taskId+",sourceId="+sourceId+",destinationId="+destinationId+",payment="+payment+"]";
	}
	
	public double getPayment() {
		return payment;
	}

	public void setPayment(double payment) {
		this.payment = payment;
	}

	public long getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(long destinationId) {
		this.destinationId = destinationId;
	}

	public int getTaskId() {
		return taskId;
	}

	public long getSourceId() {
		return sourceId;
	}
}
