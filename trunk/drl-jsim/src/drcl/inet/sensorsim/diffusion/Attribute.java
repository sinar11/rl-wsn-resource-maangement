// @(#)Attribute.java   10/2004
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

/** This class implements the attribute used in naming interests AND data in directed diffusion.
*
* @author Ahmed Sobeih
* @version 1.0, 10/04/2004
*/
public class Attribute extends drcl.DrclObj {

	/* Keys */
	public static final int SCOPE_KEY = 1001 ;		// INT32_TYPE
	public static final int CLASS_KEY = 1002 ;		// INT32_TYPE
	public static final int LATITUDE_KEY = 1501 ;		// FLOAT_TYPE
	public static final int LONGITUDE_KEY = 1502 ;		// FLOAT_TYPE

	public static final int TASK_FREQUENCY_KEY = 2002 ; 	// FLOAT_TYPE, in sec.
	public static final int TARGET_KEY = 2005 ;		// STRING_TYPE
	public static final int TARGET_RANGE_KEY = 2006 ; 	// FLOAT_TYPE, in sec.

	/* Classes */
	public static final int INTEREST_CLASS = 10010 ;
	public static final int DISINTEREST_CLASS = INTEREST_CLASS + 1 ;
	public static final int DATA_CLASS = INTEREST_CLASS + 2 ;

	/* Scopes */
	public static final int NODE_LOCAL_SCOPE = 11010 ; 
	public static final int GLOBAL_SCOPE = NODE_LOCAL_SCOPE + 1 ;
	
	/* Types */
	public static final int INT32_TYPE = 0 ; 		// 32-bit signed integer
	public static final int FLOAT32_TYPE = 1 ; 		// 32-bit
	public static final int FLOAT64_TYPE = 2 ; 		// 64-bit
	public static final int STRING_TYPE = 3 ; 		// UTF-8 format, max length 1024 chars
	public static final int BLOB_TYPE = 4 ; 		// uninterpreted binary data

	/* Match Operators */
	public static final int IS = 0 ; 
	public static final int LE = 1 ; 
	public static final int GE = 2 ;
	public static final int LT = 3 ; 
	public static final int GT = 4 ;
	public static final int EQ = 5 ;
	public static final int NE = 6 ;
	public static final int EQ_ANY = 7 ;			// with EQ_ANY, the value is ignored.

	/** The key of the attribute */
	public int key ;

	/** The type of the attribute */
	public int type ;

	/** The operator used for comparison */
	public int op ;

	/** The length of the attribute */
	public int len ;

	/** The value of the attribute */
	public Object value ;

	/** Gets the key of the attribute */
	public synchronized int getKey()		{	return key ; }

	/** Gets the type of the attribute */
	public synchronized int getType() 		{	return type ; }

	/** Gets the operator used for comparison */
	public synchronized int getOp()		{	return op ; }

	/** Gets the length of the attribute */
	public synchronized int getLen()		{	return len ; }

	/** Gets the value of the attribute */
	public synchronized Object getValue()	{  	return value ; }

	public Attribute (int key_, int type_, int op_, int len_, Object value_)
	{
		super();
		key = key_ ;
		type = type_ ;
		op = op_ ;
		len = len_ ;
		value = value_ ;
	}

	public String getName() { return "Attribute"; }

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	        Attribute that_ = (Attribute) source_;
		key = that_.key ;
		type = that_.type ;
		op = that_.op ;
		len = that_.len ;
		value = that_.value ;
	}

	/** Prints the attribute */
	public void printAttribute()
	{
		System.out.println("key = " + key + " op = " + op + " value = " + value + " type = " + type) ;
	}
}
