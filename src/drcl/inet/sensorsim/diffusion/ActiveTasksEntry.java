// @(#)ActiveTasksEntry.java   10/2004
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


/** This class implements an active task stored at the sink node.
*
* @author Ahmed Sobeih
* @version 1.0, 10/13/2004
*/

public class ActiveTasksEntry
{
	/** Description of the active task */
	public AttributeVector interest = null ;

	/** Data interval */
	public float dataInterval ;

	/** Period between two successive INTEREST packets */
	public double refreshPeriod ;

	/** Start time of the task */
	public double startTime ;

	/** List of neighbors that have already been positively reinforced. If a neighbor has already been positively reinforced but no new events have been received from it within a window of N events, the sink negatively reinforces that neighbor. */
	public Vector reinforcedNeighbors = null ;	

	/** A window of N entries that contains the list of the IDs of the neighbors that sent the most recently N new events */
	public Vector newEventsWindow = null ;	

	/** Size of the window represented by newEventsWindow */
	public int newEventsWindowMaxSize ; 

	public ActiveTasksEntry(AttributeVector interest_, float dataInterval_, double refreshPeriod_, double startTime_, int newEventsWindowMaxSize_)
	{
		super() ;
		interest = interest_ ;
		dataInterval = dataInterval_ ;
		refreshPeriod = refreshPeriod_ ;
		startTime = startTime_ ;
		newEventsWindowMaxSize = newEventsWindowMaxSize_ ;

		reinforcedNeighbors = new Vector () ;
		newEventsWindow = new Vector () ;
	}

	public ActiveTasksEntry(ActiveTasksEntry actvTasksEntry)
	{
		super() ;
		interest = new AttributeVector(actvTasksEntry.getInterest()) ;
		dataInterval = actvTasksEntry.getDataInterval() ;
		refreshPeriod = actvTasksEntry.getRefreshPeriod() ;
		startTime = actvTasksEntry.getStartTime() ;
		newEventsWindowMaxSize = actvTasksEntry.getNewEventsWindowMaxSize() ;

		int i ;
		Long entry ;

		reinforcedNeighbors = new Vector () ; 
		for ( i = 0 ; i < actvTasksEntry.getReinforcedNeighbors().size() ; i++) 
		{
			entry = (Long)(actvTasksEntry.getReinforcedNeighbors().elementAt(i)) ;
			reinforcedNeighbors.addElement(new Long(entry.longValue())) ;
		}

		newEventsWindow = new Vector () ; 
		for ( i = 0 ; i < actvTasksEntry.getNewEventsWindow().size() ; i++) 
		{
			entry = (Long)(actvTasksEntry.getNewEventsWindow().elementAt(i)) ;
			newEventsWindow.addElement(new Long(entry.longValue())) ;
		}
	}

	/** Gets the description of the active task */
	public synchronized AttributeVector getInterest()	{ return interest ; }

	/** Gets the data interval */
	public synchronized float getDataInterval()		{ return dataInterval ; }

	/** Gets the period between two successive INTEREST packets */
	public synchronized double getRefreshPeriod()		{ return refreshPeriod ; }

	/** Gets the start time of the task */
	public synchronized double getStartTime()		{ return startTime ; }

	/** Gets the maximum size of the window containing the list of the IDs of the neighbors that sent the most recently N new events */
	public synchronized int getNewEventsWindowMaxSize()	{ return newEventsWindowMaxSize ; }

	/** Gets the list of neighbors that have already been positively reinforced */
	public synchronized Vector getReinforcedNeighbors()	{ return reinforcedNeighbors ; }

	/** Gets the window of N entries that contains the list of the IDs of the neighbors that sent the most recently N new events */
	public synchronized Vector getNewEventsWindow()		{ return newEventsWindow ; }

	/** Looks up a node in the list of neighbors that have already been positively reinforced */
	public synchronized boolean reinforcedNeighbors_lookup(long nodeID)
	{
		int no = reinforcedNeighbors.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			Long entry = (Long)(reinforcedNeighbors.elementAt(i)) ;
			if ( entry.longValue() == nodeID )
				return true ;
		}

		return false ;
	}

	/** Inserts a node in the list of neighbors that have already been positively reinforced */
	public synchronized void reinforcedNeighbors_insert(long nodeID)
	{	reinforcedNeighbors.addElement(new Long(nodeID)) ;	}

	/** Looks up a node in the window of N entries that contains the list of the IDs of the neighbors that sent the most recently N new events */
	public synchronized boolean newEventsWindow_lookup(long nodeID)
	{
		int no = newEventsWindow.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			Long entry = (Long)(newEventsWindow.elementAt(i)) ;
			if ( entry.longValue() == nodeID )
				return true ;
		}

		return false ;
	}

	/** Inserts a node in the window of N entries that contains the list of the IDs of the neighbors that sent the most recently N new events */
	public synchronized void newEventsWindow_insert(long nodeID)
	{	
		if ( newEventsWindow.size() >= newEventsWindowMaxSize )
			newEventsWindow.removeElementAt(0) ;

		newEventsWindow.addElement(new Long(nodeID)) ;
	}

	/** Prints the window of N entries that contains the list of the IDs of the neighbors that sent the most recently N new events */
	public synchronized void newEventsWindow_print()
	{	
		int no = reinforcedNeighbors.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			Long entry = (Long)(reinforcedNeighbors.elementAt(i)) ;
			System.out.println("	Reinforced Neighbor(" + i + ") = " + entry.longValue()) ;
		}

		no = newEventsWindow.size() ;
		for (int i = 0 ; i < no ; i++) 
		{
			Long entry = (Long)(newEventsWindow.elementAt(i)) ;
			System.out.println("	newEventsWindow(" + i + ") = " + entry.longValue()) ;
		}
	}

	/** Determines the list of neighbors that should be negatively reinforced */
	public synchronized Vector getListOfNeighborsToNegativelyReinforce()
	{
		Vector returnVector = new Vector() ;

		for (int i = 0 ; i < reinforcedNeighbors.size() ; ) 
		{
			Long entry = (Long)(reinforcedNeighbors.elementAt(i)) ;
			if ( newEventsWindow_lookup(entry.longValue()) == false )
			{
				returnVector.addElement(new Long(entry.longValue())) ;

				reinforcedNeighbors.removeElementAt(i) ;
			}
			else
			{
				i++ ;
			}
		}

		return returnVector ;
	}

	/** Prints the active task */
	public synchronized void printActiveTasksEntry()
	{
		interest.printAttributeVector() ;
		newEventsWindow_print() ;
	}
}
