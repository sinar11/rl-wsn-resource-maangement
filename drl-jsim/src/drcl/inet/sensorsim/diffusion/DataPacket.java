// @(#)DataPacket.java   10/2004
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


/** This class implements a data packet carrying the description of a detected event.
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class DataPacket
{
	/** The description of the detected event */
	public AttributeVector event = null ;

	/** The source of the packet */
	public long source ;

	/** The destination of the packet */
	public long destination ;

	/** The data or exploratory interval */
	public float dataInterval ;

	public DataPacket(long source_, long destination_, AttributeVector event_, float dataInterval_)
	{
		super() ;
		source = source_ ;
		destination = destination_ ;
		event = event_ ;
		dataInterval = dataInterval_ ;
	}

	/** Gets the source of the packet */
	public synchronized long getSource()			{	return source ; }

	/** Gets the destination of the packet */
	public synchronized long getDestination()		{	return destination ; }

	/** Gets the description of the detected event */
	public synchronized AttributeVector getEvent() 		{	return event ; }

	/** Gets the interval */
	public synchronized float getDataInterval()		{	return dataInterval ; }

	public String toString(String separator_)
	{
		String str;
		str = "Data Packet source =" + separator_ + source + separator_ + "destination =" + separator_ + destination + separator_ + "dataInterval =" + separator_ + dataInterval ;
		return str;
	}
}
