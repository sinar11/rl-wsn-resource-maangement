
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
	private double timestamp ; 
	
	public GradientEntry(long neighbor, double payment, double duration, double timestamp)
	{
		this.neighbor = neighbor ;
		this.payment = payment;
		this.duration = duration ;
		this.timestamp = timestamp ;
	}

	public GradientEntry(GradientEntry e)
	{
		neighbor = e.neighbor ;
		this.payment = e.payment;
		duration = e.duration ;
		timestamp = e.timestamp;
	}

	public double getPayment() {
		return payment;
	}

	public void setPayment(double payment) {
		this.payment = payment;
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

	public double getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	/** Checks if the lifetime of the gradient has expired */
	public boolean IsExpired(double currentTime)
	{
		return false;
		/*if ( (currentTime - timestamp) > duration )
			return true ;
		else
			return false ;*/
	}

	public String toString(){
		return "GradientEntry[neighbor="+neighbor+",payment="+payment+",duration="+duration+"timestamp="+timestamp+"]";
	}
}
