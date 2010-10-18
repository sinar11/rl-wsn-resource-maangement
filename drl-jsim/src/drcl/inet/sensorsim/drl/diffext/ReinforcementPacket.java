package drcl.inet.sensorsim.drl.diffext ;

import java.util.HashMap;
import java.util.Map;


/** This class implements a reinforcement packet
*
* @author Kunal Shah
* 
*/

public class ReinforcementPacket
{
	/** The task/interest for which this provides reinforcement (+ve or -ve) */
	private int taskId;

	/** Payment associated with the task **/
	private double payment;
	
	/** The source of the packet */
	private long sourceId;
	
	/** Destination for this packet **/
	private long destinationId;
	
	private Map<Long,Double> streamPayments= new HashMap<Long,Double>();
	
	public ReinforcementPacket(int taskId, double payment, long sourceId, long destinationId){
		this.taskId=taskId;
		this.sourceId = sourceId ;
		this.destinationId=destinationId;
		this.payment=payment;
	}

	public ReinforcementPacket(ReinforcementPacket pkt){
		this.taskId=pkt.taskId;
		this.sourceId = pkt.sourceId ;
		this.streamPayments=pkt.streamPayments;		
		this.destinationId=pkt.destinationId;
		this.payment=pkt.payment;
	}
	
	@Override
	public String toString(){
		return "ReinforcementPacket[taskId="+taskId+",sourceId="+sourceId+",destinationId="+destinationId+",payment="+payment+",streamPayments="+streamPayments+"]";
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
	
	public void addStreamPayment(Long streamId, Double payment){
		streamPayments.put(streamId, payment);
	}

	public Map<Long, Double> getStreamPayments() {
		return streamPayments;
	}
	
	public double getPayment(){
		return payment;
	}
}
