package drcl.inet.sensorsim.drl.diffext;

/**
 * Represents a Task entry as maintained by sink
 * 
 * @author Kunal Shah
 *
 */
public class TaskEntry {
	 /** Start time of this task **/
	double startTime; 
	/**Id of this task **/
	int taskId;
	/** Is this task currently active or not **/
	boolean isActive;
	/** Interest packet associated with this task **/
	InterestPacket interest;
	/** Current bid/payment value associated with this task **/
	double payment;
	/** How often the task should be refreshed **/
	double refreshPeriod;

	public TaskEntry(int taskId, InterestPacket interest, double startTime, double refreshPeriod, boolean isActive){
		this.taskId=taskId;
		this.interest=interest;
		this.startTime=startTime;
		this.refreshPeriod=refreshPeriod;
		this.isActive=isActive;
	}

	public double getPayment() {
		return payment;
	}

	public void setPayment(double payment) {
		this.payment = payment;
		this.interest.setPayment(payment);
	}

	public double getStartTime() {
		return startTime;
	}

	public int getTaskId() {
		return taskId;
	}

	public boolean isActive() {
		return isActive;
	}

	public InterestPacket getInterest() {
		return interest;
	}

	public double getRefreshPeriod() {
		return refreshPeriod;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	
}
