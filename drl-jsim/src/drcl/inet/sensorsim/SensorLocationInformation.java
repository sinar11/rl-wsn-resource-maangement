// @(#)SensorLocationInformation.java   10/2004
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

package drcl.inet.sensorsim;

/** This class implements the location information of a sensor node. 
*
* @author Ahmed Sobeih
* @version 1.1, 10/19/2004
*/

public class SensorLocationInformation
{
	long nid ;
	/** X: actual location coordinates used for calculating the exact distance between two sensors and hence decide whether two sensors are neighbors or not */
	private double X;

	/** Y: actual location coordinates used for calculating the exact distance between two sensors and hence decide whether two sensors are neighbors or not */ 
	private double Y;

	/** Z: actual location coordinates used for calculating the exact distance between two sensors and hence decide whether two sensors are neighbors or not */
	private double Z;

	public SensorLocationInformation ()
	{
	}

	public SensorLocationInformation (long nid_, double X_, double Y_, double Z_)
	{
		nid = nid_;
		X = X_; Y = Y_; Z = Z_;
	}

	public synchronized long getNid()		{	return nid ; }
	public synchronized double getX()		{	return X ; }
	public synchronized double getY()		{	return Y ; }
	public synchronized double getZ()		{	return Z ; }
} // end class SensorLocationInformation
