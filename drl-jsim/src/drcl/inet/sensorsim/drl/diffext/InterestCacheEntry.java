// @(#)InterestCacheEntry.java   10/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 3. Neither the name of "DRCL" nor the names of its contributors may be used
//    to endorse or promote products derived from this software without specific
//    prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//

package drcl.inet.sensorsim.drl.diffext ;

import java.util.*;

import drcl.util.random.UniformDistribution;


/** This class implements a cache entry denoting a previously seen interest.
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class InterestCacheEntry
{
	/** The interest */
	private InterestPacket interest = null ;

	/** The last time the node sent a matching interest. This field was added because: not all received interests are resent. A node may suppress a received interest if it recently resent a matching interest */
	private double lastTimeSent ;

	/** Amount that owning node needs to pay to its source for this task **/
	private double payable;
	
	/** The gradient list which contains several gradient fields (each of which is GradientEntry), up to one per neighbor */
	public List<GradientEntry> gradientList ;

	public InterestCacheEntry(InterestCacheEntry iCacheEntry)
	{
		super() ;
		interest = (InterestPacket) iCacheEntry.getInterest() ;
		lastTimeSent = iCacheEntry.getLastTimeSent() ;
		gradientList = new ArrayList<GradientEntry>() ;
		for (int i = 0 ; i < iCacheEntry.gradientList.size() ; i++ )
		{
			GradientEntry entry = iCacheEntry.gradientList.get(i) ;
			gradientList.add(new GradientEntry(entry)) ;
		}
	}

	public InterestCacheEntry(InterestPacket interest_, List<GradientEntry> gradientList_)
	{
		interest = interest_ ;
		lastTimeSent = 0 ;
		gradientList = gradientList_ ;
	}


	/** Checks whether or not to resend the interest. A node may suppress a received interest if it recently resent a matching interest  */
	public boolean isToResendInterest(double currentTime)
	{
		if ( (currentTime - lastTimeSent) > DRLDiffApp.RESEND_INTEREST_WINDOW )
			return true ;
		else
			return false ;
	}

	/** Checks whether or not to forward the REINFORCEMENT packet. 
	 * returns true if passed neighbor currently helds highest gradient for this interest and passed payment value is different, false otherwise
	 **/
	public boolean shouldForwardReinforcement(long neighbor, double payment){
		GradientEntry entry = getMaxGradient();
		if(entry==null || ((neighbor==entry.getNeighbor()) && payment != entry.getPayment()))
				return true ;
		return false ;
	}
	
	public boolean shouldForwardInterest(long neighbor, double payment){
		GradientEntry entry = getMaxGradient();
		if(entry==null || ((neighbor==entry.getNeighbor()) && payment != entry.getPayment()))
				return true ;
		return false ;
	}
	
	public GradientEntry getMaxGradient() {
		if(gradientList==null || gradientList.size()==0) return null;
		GradientEntry maxEntry=gradientList.get(0);
		for(GradientEntry entry: gradientList){
			if(entry.getPayment()>maxEntry.getPayment()){
				maxEntry=entry;
			}
		}
		return maxEntry;
	}

	public GradientEntry getRandomGradient() {
		if(gradientList==null || gradientList.size()==0) return null;
		UniformDistribution dist= new UniformDistribution(1,gradientList.size());
		GradientEntry entry=gradientList.get(dist.nextInt()-1);
		return entry;
	}

	/** Prints the interest cache entry */
	public void printInterestEntry()
	{
		// interest.printAttributeVector() ;
		int no = gradientList.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			GradientEntry entry = (GradientEntry)gradientList.get(i);
			System.out.println(entry.toString()) ;
		}	
	}

	/** Checks if the gradient list is empty */
	public boolean isGradientListEmpty()
	{
		if ( gradientList == null )
		{	return true ; }
		else
		{
			if ( gradientList.size() == 0 )
				return true ;
			else
				return false ;
		}		
	}

	/** Purges the gradient list */
	public void gradientListPurge(double currentTime)
	{
		for (int i = 0 ; i < gradientList.size() ; ) 
		{
			GradientEntry entry = (GradientEntry)gradientList.get(i);
			if ( entry.IsExpired(currentTime) == true )
			{
				gradientList.remove(i) ;
			}
			else
			{
				i++ ;
			}
		}
	}

	/** Looks up a gradient in the gradient list */
	public GradientEntry gradientListLookup(long previousHop, double currentTime)
	{
		int no = gradientList.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			GradientEntry entry = (GradientEntry)gradientList.get(i);
			if ( 		( entry.getNeighbor() == previousHop ) 
				&&
					( entry.IsExpired(currentTime) == false )
			   )
				return entry ;
		}

		return null ;
	}

	/** Inserts a gradient in the gradient list */
	public void gradientListInsert(GradientEntry e)
	{	gradientList.add(e) ;	}

	public double getLastTimeSent() {
		return lastTimeSent;
	}

	public void setLastTimeSent(double lastTimeSent) {
		this.lastTimeSent = lastTimeSent;
	}

	public double getPayable() {
		return payable;
	}

	public void setPayable(double payable) {
		this.payable = payable;
	}

	public InterestPacket getInterest() {
		return interest;
	}

}
