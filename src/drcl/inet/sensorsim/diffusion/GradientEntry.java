// @(#)GradientEntry.java   10/2004
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


/** This class implements a gradient entry (for a specific neighbor) in an interest cache entry.
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class GradientEntry
{
	/** The immediately previous hop */
	public long previousHop ; 

	/** The datarate requested by the specified neighbor. This is derived from the interval (aka frequency) of the interest */
	public float datarate ;	

	/** The lifetime of the interest. This is derived from the duration (aka range) of the interest */
	public float duration ;	

	/** The timestamp of the last received matching interest */
	public double timestamp ; 

	/** The data timer used to periodically send DATA packets */
	public DiffTimer dataTimer ;

	public GradientEntry(long previousHop_, float datarate_, float duration_, double timestamp_)
	{
		previousHop = previousHop_ ;
		datarate = datarate_ ;
		duration = duration_ ;
		timestamp = timestamp_ ;
		dataTimer = null ;
	}

	public GradientEntry(GradientEntry e)
	{
		previousHop = e.getPreviousHop() ;
		datarate = e.getDataRate() ;
		duration = e.getDuration() ;
		timestamp = e.getTimeStamp() ;
		if ( e.dataTimer != null )
			dataTimer = new DiffTimer(e.dataTimer) ;
		else
			dataTimer = null ;
	}

	/** Gets the immediately previous hop */
	public synchronized long getPreviousHop()	{ return previousHop ; } 

	/** Gets the datarate requested by the specified neighbor */
	public synchronized float getDataRate()		{ return datarate ; }

	/** Sets the datarate requested by the specified neighbor */
	public synchronized void setDataRate(float datarate_)
	{	datarate = datarate_ ; }

	/** Sets the lifetime of the interest */
	public synchronized void setDuration(float duration_)
	{	duration = duration_ ; }

	/** Gets the lifetime of the interest */
	public synchronized float getDuration() 	{ return duration ; }

	/** Sets the timestamp of the last received matching interest */
	public synchronized void setTimeStamp(double timestamp_)
	{	timestamp = timestamp_ ; }

	/** Gets the timestamp of the last received matching interest */
	public synchronized double getTimeStamp()
	{	return timestamp ; }

	/** Checks if the lifetime of the gradient has expired */
	public synchronized boolean IsExpired(double currentTime)
	{
		if ( (currentTime - timestamp) > duration )
			return true ;
		else
			return false ;
	}

	/** Prints the gradient */
	public synchronized void printGradientEntry()
	{
		System.out.println(" previousHop = " + previousHop + " datarate = " + datarate + " duration = " + duration + " timestamp = " + timestamp) ;
	}
}
