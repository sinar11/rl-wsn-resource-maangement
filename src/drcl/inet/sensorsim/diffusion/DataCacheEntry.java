// @(#)DataCacheEntry.java   10/2004
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL)
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


/** This class implements a cache entry denoting a previously seen (either received or sent) data packet (i.e., event).
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class DataCacheEntry
{
	/** The description of the previously seen event */
	public AttributeVector event = null ;

	/** The data or exploratory interval */
	public float dataInterval ;

	/** The source from which the data was received. If a positive reinforcement is received, it is forwarded to this source */
	public long source ; 

	/** The last time the event was sent. If the data cache entry expires, it is removed from the cache allowing the node to resend it later */
	public double timestamp ;

	public DataCacheEntry(DataCacheEntry dCacheEntry)
	{
		super() ;
		event = new AttributeVector(dCacheEntry.getEvent()) ;
		dataInterval = dCacheEntry.getDataInterval() ;
		source = dCacheEntry.getSource() ;
		timestamp = dCacheEntry.getTimestamp() ;
	}

	public DataCacheEntry(AttributeVector event_, float dataInterval_, long source_, double timestamp_)
	{
		super() ;
		event = event_ ;
		dataInterval = dataInterval_ ;
		source = source_ ;
		timestamp = timestamp_ ;
	}

	/** Gets the description of the previously seen event */
	public synchronized AttributeVector getEvent()		{ return event ; }

	/** Gets the interval */
	public synchronized float getDataInterval()		{ return dataInterval ; }

	/** Gets the source from which the data was received */
	public synchronized long getSource()			{ return source ; }

	/** Gets the last time the event was sent */
	public synchronized double getTimestamp()		{ return timestamp ; }

	/** Sets the last time the event was sent */
	public synchronized void setTimeStamp(double timestamp_)		
	{ timestamp = timestamp_ ; }

	/** Checks if the data cache entry has expired */
	public synchronized boolean IsExpired(double currentTime)
	{
		if ( (currentTime - timestamp) > dataInterval )
			return true ;
		else
			return false ;
	}

	/** Prints the data cache entry */
	public synchronized void printDataEntry()
	{
		System.out.println("source = " + getSource() + " dataInterval = " + getDataInterval() + " timestamp = " + getTimestamp()) ;
		// event.printAttributeVector() ;
	}
}
