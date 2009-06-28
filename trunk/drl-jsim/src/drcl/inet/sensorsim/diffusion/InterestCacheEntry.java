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

package drcl.inet.sensorsim.diffusion ;

import java.util.*;


/** This class implements a cache entry denoting a previously seen interest.
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class InterestCacheEntry
{
	/** The interest */
	public AttributeVector interest = null ;

	/** The last time the node sent a matching interest. This field was added because: not all received interests are resent. A node may suppress a received interest if it recently resent a matching interest */
	public double lastTimeSent ;

	/** The gradient list which contains several gradient fields (each of which is GradientEntry), up to one per neighbor */
	public Vector gradientList ;

	public InterestCacheEntry(InterestCacheEntry iCacheEntry)
	{
		super() ;
		interest = new AttributeVector(iCacheEntry.getInterest()) ;
		lastTimeSent = iCacheEntry.getLastTimeSent() ;
		gradientList = new Vector () ;
		for ( int i = 0 ; i < iCacheEntry.getGradientList().size() ; i++ )
		{
			GradientEntry entry = (GradientEntry)iCacheEntry.getGradientList().elementAt(i) ;
			gradientList.addElement(new GradientEntry(entry)) ;
		}
	}

	public InterestCacheEntry(AttributeVector interest_, double lastTimeSent_, Vector gradientList_)
	{
		super() ;
		interest = interest_ ;
		lastTimeSent = lastTimeSent_ ;
		gradientList = gradientList_ ;
	}

	/** Gets the interest */
	public synchronized AttributeVector getInterest()	{ return interest ; }

	/** Gets the last time the node sent a matching interest */
	public synchronized double getLastTimeSent()		{ return lastTimeSent ; }

	/** Gets the gradient list */
	public synchronized Vector getGradientList()		{ return gradientList ; }

	/** Sets the last time the node sent a matching interest */
	public synchronized void setLastTimeSent(double lastTimeSent_)	
	{ 	lastTimeSent = lastTimeSent_ ; }

	/** Checks whether or not to resend the interest. A node may suppress a received interest if it recently resent a matching interest  */
	public synchronized boolean IsToResendInterest(double currentTime)
	{
		if ( (currentTime - lastTimeSent) > DiffApp.RESEND_INTEREST_WINDOW )
			return true ;
		else
			return false ;
	}

	/** Checks whether or not to forward the POSITIVE REINFORCEMENT packet. If the new data rate is higher (i.e., the interval is smaller) than that of ANY existing gradient, then the node must also reinforce at least one neighbor. The node should use its data cache for this purpose */
	public synchronized boolean IsToForwardPositiveReinforcement(float interval)
	{
		int no = gradientList.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			GradientEntry entry = (GradientEntry)gradientList.elementAt(i);
			if ( interval < entry.getDataRate() )
				return true ;
		}

		return false ;
	}

	/** Checks whether or not to forward the NEGATIVE REINFORCEMENT packet. If ALL the gradients are now exploratory, then the node must also negatively reinforce those neighbors that have been sending data to it */
	public synchronized boolean IsToForwardNegativeReinforcement(float interval)
	{
		int no = gradientList.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			GradientEntry entry = (GradientEntry)gradientList.elementAt(i);
			if ( interval > entry.getDataRate() )
				return false ;
		}

		return true ;
	}

	/** Prints the interest cache entry */
	public synchronized void printInterestEntry()
	{
		// interest.printAttributeVector() ;
		int no = gradientList.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			GradientEntry entry = (GradientEntry)gradientList.elementAt(i);
			entry.printGradientEntry() ;
		}	
	}

	/** Checks if the gradient list is empty */
	public synchronized boolean IsGradientListEmpty()
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
	public synchronized void gradientList_purge(double currentTime)
	{
		for (int i = 0 ; i < gradientList.size() ; ) 
		{
			GradientEntry entry = (GradientEntry)gradientList.elementAt(i);
			if ( entry.IsExpired(currentTime) == true )
			{
				gradientList.removeElementAt(i) ;
			}
			else
			{
				i++ ;
			}
		}
	}

	/** Looks up a gradient in the gradient list */
	public synchronized GradientEntry gradientList_lookup(long previousHop, double currentTime)
	{
		int no = gradientList.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			GradientEntry entry = (GradientEntry)gradientList.elementAt(i);
			if ( 		( entry.getPreviousHop() == previousHop ) 
				&&
					( entry.IsExpired(currentTime) == false )
			   )
				return entry ;
		}

		return null ;
	}

	/** Inserts a gradient in the gradient list */
	public synchronized void gradientList_insert(GradientEntry e)
	{	gradientList.addElement(e) ;	}
}
