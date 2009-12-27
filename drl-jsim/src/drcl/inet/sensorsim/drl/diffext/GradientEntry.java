
package drcl.inet.sensorsim.drl.diffext ;



/** This class implements a gradient entry (for a specific neighbor) in an interest cache entry.
*
* @author Kunal Shah
*/

public class GradientEntry
{
	/** The immediately previous hop */
	private long neighbor ; 

	/** The amount of payment that is expected from this neighbor */
	private double payment ;	

	/** The lifetime of the interest. This is derived from the duration (aka range) of the interest */
	private double duration ;	

	/** The timestamp of the last received matching interest */
	private double interestTimestamp ; 
	
	/** Timestamp at which this gradient entry was last updated (either result of interest or reinforcement**/
	private double updateTimestamp;
	
	public GradientEntry(long neighbor, double payment, double duration, double timestamp)
	{
		this.neighbor = neighbor ;
		this.payment = payment;
		this.duration = duration ;
		this.interestTimestamp = timestamp ;
	}

	public GradientEntry(GradientEntry e)
	{
		neighbor = e.neighbor ;
		this.payment = e.payment;
		duration = e.duration ;
		interestTimestamp = e.interestTimestamp;
	}

	public double getPayment() {
		return payment;
	}

	public void setPayment(double payment, double timestamp) {
		this.updateTimestamp=timestamp;
		this.payment = payment;
	}

	public void applyTimeDecay(double currentTime){
		double decay= DRLDiffApp.DECAY_FACTOR*(currentTime - updateTimestamp);
		this.payment=this.payment- decay*0.01*this.payment;
	}
	public long getNeighbor() {
		return neighbor;
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	public double getInterestTimestamp() {
		return interestTimestamp;
	}
	
	public void setInterestTimestamp(double interestTimestamp) {
		this.updateTimestamp=interestTimestamp;
		this.interestTimestamp = interestTimestamp;
	}

	/** Checks if the lifetime of the gradient has expired */
	public boolean IsExpired(double currentTime)
	{
		if ( (currentTime - updateTimestamp) > duration )
			return true ;
		else
			return false ;
	}

	public String toString(){
		return "GradientEntry[neighbor="+neighbor+",payment="+payment+",duration="+duration+"interestTimestamp="+interestTimestamp+",updateTimestamp="+updateTimestamp+"]";
	}
}
