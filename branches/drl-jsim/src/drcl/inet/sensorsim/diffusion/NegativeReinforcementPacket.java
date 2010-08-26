// @(#)NegativeReinforcementPacket.java   10/2004
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


/** This class implements a negative reinforcement packet in the directed diffusion paradigm.
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class NegativeReinforcementPacket
{
	/** The interest whose gradient is being negatively reinforced */
	public AttributeVector interest = null ;

	/** The source of the packet */
	public long source ;

	/** The destination of the packet */
	public long destination ;

	/** The exploratory interval */
	public float negativeReinforcementInterval ;

	public NegativeReinforcementPacket(long source_, long destination_, AttributeVector interest_, float negativeReinforcementInterval_)
	{
		super() ;
		source = source_ ;
		destination = destination_ ;
		interest = interest_ ;
		negativeReinforcementInterval = negativeReinforcementInterval_ ;
	}

	/** Gets the source of the packet */
	public synchronized long getSource()			{	return source ; }

	/** Gets the destination of the packet */
	public synchronized long getDestination()		{	return destination ; }

	/** Gets the interest whose gradient is being negatively reinforced */
	public synchronized AttributeVector getInterest() 		{	return interest ; }

	/** Gets the exploratory interval */
	public synchronized float getNegativeReinforcementInterval()		{	return negativeReinforcementInterval ; }

	public String toString(String separator_)
	{
		String str;
		str = "Negative Reinforcement Packet source =" + separator_ + source + separator_ + "destination =" + separator_ + destination + separator_ + "negativeReinforcementInterval =" + separator_ + negativeReinforcementInterval ;
		return str;
	}
}
