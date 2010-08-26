// @(#)AttributeVector.java   10/2004
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


/** This class implements a list of attribute-value pairs that describe an interest/event.
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class AttributeVector extends Vector {

	public AttributeVector ()
	{
		super();
	}

	public AttributeVector (AttributeVector v)
	{
		super();
		for ( int i = 0 ; i < v.size() ; i++ )
		{
			Attribute attr = (Attribute)v.elementAt(i);
			addElement(new Attribute(attr.getKey(), attr.getType(), attr.getOp(), attr.getLen(), attr.getValue() instanceof drcl.ObjectCloneable ? 
			((drcl.ObjectCloneable)attr.getValue()).clone() : attr.getValue() )) ;
		}
	}

	public String getName() { return "AttributeVector"; }

	/** Gets the type of the interest/event */
	public synchronized String getType()
	{
		int no = size();
		for (int i = 0; i < no; i++) 
		{
			Attribute attr = (Attribute)elementAt(i);
			if ( attr.getKey() == Attribute.TARGET_KEY )
			{
				return (String)(attr.getValue()) ;
			}
		}

		return null ;
	}

	/** Gets the frequency attribute, which is also called the interval (or datarate) attribute*/
	public synchronized float getFrequency()
	{
		int no = size();
		for (int i = 0; i < no ; i++) 
		{
			Attribute attr = (Attribute)elementAt(i);
			if ( attr.getKey() == Attribute.TASK_FREQUENCY_KEY )
			{
				Float f = (Float)(attr.getValue()) ;
				return (f.floatValue());
			}
		}

		return -1 ;
	}

	/** Gets the range attribute, which is also called the duration attribute */
	public synchronized float getRange()
	{
		int no = size();
		for (int i = 0; i < no ; i++) 
		{
			Attribute attr = (Attribute)elementAt(i);
			if ( attr.getKey() == Attribute.TARGET_RANGE_KEY )
			{
				Float f = (Float)(attr.getValue()) ;
				return (f.floatValue());
			}
		}

		return -1 ;
	}

	/** Returns the value of a coordinate whose key is Coordinate_Key and op is Coordinate_Op */
	public synchronized float getCoordinate(int Coordinate_Key, int Coordinate_Op)	
	{
		int no = size();
		for (int i = 0; i < no ; i++) 
		{
			Attribute attr = (Attribute)elementAt(i);
			if ( (attr.getKey() == Coordinate_Key) && (attr.getOp() == Coordinate_Op) )
			{
				Float f = (Float)(attr.getValue()) ;
				return (f.floatValue());
			}
		}

		return -1 ;
	}

	/** Deterimnes if two interests/events are matching */
	public synchronized boolean IsMatching(AttributeVector interest)
	{
		float xmin_1 = getCoordinate(Attribute.LONGITUDE_KEY, Attribute.GE) ;
		float xmax_1 = getCoordinate(Attribute.LONGITUDE_KEY, Attribute.LE) ;
		float ymin_1 = getCoordinate(Attribute.LATITUDE_KEY, Attribute.GE) ;
		float ymax_1 = getCoordinate(Attribute.LATITUDE_KEY, Attribute.LE) ;

		float xmin_2 = interest.getCoordinate(Attribute.LONGITUDE_KEY, Attribute.GE) ;
		float xmax_2 = interest.getCoordinate(Attribute.LONGITUDE_KEY, Attribute.LE) ;
		float ymin_2 = interest.getCoordinate(Attribute.LATITUDE_KEY, Attribute.GE) ;
		float ymax_2 = interest.getCoordinate(Attribute.LATITUDE_KEY, Attribute.LE) ;

		if ( 		( getType().equals(interest.getType()) == true ) 
			&&	( ((xmin_1 <= xmin_2) && (xmin_2 <= xmax_1) && (xmin_1 <= xmax_2) && (xmax_2 <= xmax_1)) || ((xmin_2 <= xmin_1) && (xmin_1 <= xmax_2) && (xmin_2 <= xmax_1) && (xmax_1 <= xmax_2)) )
			&&	( ((ymin_1 <= ymin_2) && (ymin_2 <= ymax_1) && (ymin_1 <= ymax_2) && (ymax_2 <= ymax_1)) || ((ymin_2 <= ymin_1) && (ymin_1 <= ymax_2) && (ymin_2 <= ymax_1) && (ymax_1 <= ymax_2)) )
		   )
		{
			return true ;
		}
		else 	/* different types or the two rect attributes are (possibly partially) disjoint */
		{
			return false ;
		}
	}

	/** Prints the interest/event */
	public void printAttributeVector()
	{
		int no = size();
		for (int i = 0; i < no ; i++) 
		{
			Attribute attr = (Attribute)elementAt(i);
			attr.printAttribute() ;
		}
	}
}
