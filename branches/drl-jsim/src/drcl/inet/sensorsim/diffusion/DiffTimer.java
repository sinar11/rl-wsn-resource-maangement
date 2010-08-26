// @(#)DiffTimer.java   10/2004
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


/** This class implements a timer handling timeout event. 
* This timeout event is able to carry an object when it is set.
* When the timeout happens, we can process the object carried in the timeout event.
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class DiffTimer { 
	public static final int TIMEOUT_DELAY_BROADCAST		= 0 ; 
	public static final int TIMEOUT_REFRESH_INTEREST	= 1 ; 
	public static final int TIMEOUT_SEND_DATA		= 2 ; 
	public static final int TIMEOUT_INTEREST_CACHE_PURGE	= 3 ; 
	public static final int TIMEOUT_DATA_CACHE_PURGE	= 4 ; 

	static final String[] TIMEOUT_TYPES = {"DELAY_BROADCAST", "REFRESH_INTEREST", "SEND_DATA", "INTEREST_CACHE_PURGE", "DATA_CACHE_PURGE"} ; 

	/** The type of the timer */	 
	int	EVT_Type ; 

	/** The object that the timer carries */
	Object	EVT_Obj ; 

	/** The handle that can be used for cancelling the event */
	drcl.comp.ACATimer handle ; 
 
	public String toString() 
	{ return TIMEOUT_TYPES[EVT_Type] + ", " + EVT_Obj; } 
 
	/** 
	 * Constructor 
	 * @param tp_: Timeout type
	 */ 
	public DiffTimer(int tp_) {
		EVT_Type = tp_;
	}
 
	/** 
	 * Constructor 
	 * @param tp_: Timeout type
	 * @param obj_: the associated object with the time out event 
	 */ 
	public DiffTimer(int tp_, Object obj_)  
	{ 
		EVT_Type = tp_; 
		EVT_Obj  = obj_; 
	}

	public DiffTimer(DiffTimer t)  
	{
		EVT_Type = t.getEVT_Type() ; 
		EVT_Obj  = t.getObject() instanceof drcl.ObjectCloneable ? ((drcl.ObjectCloneable)(t.getObject())).clone() : t.getObject() ;
		handle.duplicate(t.handle) ;
	}
		 
	/** 
	 * Functions to set or get information for a event 
	 *  
	 */ 

	/** Sets the type of the timer */
	public void setEVT_Type(int tp_) 
	{	EVT_Type = tp_; } 
 
	/** Gets the type of the timer */
	public int getEVT_Type()
	{ 	return EVT_Type; } 

	/** Sets the object that the timer carries */
	public void setObject(Object obj_) 
	{ 	EVT_Obj = obj_; } 
 
	/** Gets object that the timer carries */
	public Object getObject() 
	{ 	return EVT_Obj; } 
}
