// @(#)DiffApp.java   10/2004
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

import drcl.inet.mac.EnergyContract;
import drcl.inet.sensorsim.* ;
import drcl.inet.sensorsim.SensorAppAgentContract.Message;
import drcl.inet.sensorsim.drl.CSVLogger;
import drcl.inet.sensorsim.drl.EnergyStats;
import drcl.inet.sensorsim.drl.SensorTask;
import drcl.inet.sensorsim.drl.algorithms.AbstractAlgorithm.Algorithm;
import drcl.inet.sensorsim.drl.diffext.DRLDiffApp.NodeState;
import drcl.comp.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import drcl.comp.Port;


/** This class implements directed diffusion (Mobicom 2000).
*
* @author Ahmed Sobeih
* @version 1.0, 10/19/2004
*/
public class DiffApp extends drcl.inet.sensorsim.SensorApp implements drcl.comp.ActiveComponent
{
	public static final String MOBILITY_PORT_ID    = ".mobility";

	/** The port used to query the current position of myself */
	protected Port mobilityPort    = addPort(MOBILITY_PORT_ID, false); 

	/** A node may suppress a received interest if it recently (i.e., within RESEND_INTEREST_WINDOW secs.) resent a matching interest. */	
	public static final double RESEND_INTEREST_WINDOW = 2.0 ;
	
	/** Period between two successive times to check if any entries in the interest cache need to be purged. */
	public static final double INTEREST_CACHE_PURGE_INTERVAL = 120.0 ; /* secs. */

	/** Period between two successive times to check if any entries in the data cache need to be purged */
	public static final double DATA_CACHE_PURGE_INTERVAL = 90.0 ; /* secs. */

	/** Size of the window of N events used for judging whether a neighbor needs to be negatively reinforced or not. Negatively reinforce that neighbor from which no new events have been received within a window of N events */
	public static final int N_WINDOW = 5 ;

	/** Name of the target that a sink node is interested in (and a sensor node capable of) detecting */
	public String TargetName ;

	public static final int INTEREST_PKT = 0 ;
	public static final int DATA_PKT = 1 ;
	public static final int POSITIVE_REINFORCEMENT_PKT = 2 ;
	public static final int NEGATIVE_REINFORCEMENT_PKT = 3 ;

	public static final double DELAY = 1.0; 			/* fixed delay to keep arp happy */


	public static java.util.Random rand = new java.util.Random(7777) ;

	public int numSubscriptions ;

	/** A Vector of previously seen interests. Each cache entry is an interest (i.e., a Vector of Attributes) and possibly other fields */
	public Vector interestCache = null ;

	/** A Vector of previously seen data packets (i.e., events). Each cache entry is an event (i.e., a Vector of Attributes) and possibly other fields */
	public Vector dataCache = null ;

	/** A Vector of active tasks initiated by a sink node */
	public Vector activeTasksList = null ;

	/** Timer used to periodically check if there are any entries in the interest cache that need to be purged. */
	public DiffTimer interestCache_purgeTimer ;

	/** Timer used to periodically check if there are any entries in the daat cache that need to be purged. */
	public DiffTimer dataCache_purgeTimer ;

	private long noOfTracks=0;
	private double lastTrackTime=0;
	private double averageDelay=0;
	
	private int noOfDataPkts;

	private int noOfReinforcements;

	private int noOfInterests;
	
	private Double lifetime;

	private int noOfNodes;

	private double initialEnergy;

	private double totalEnergyUsed;
	private double targetMoving;
	
	public long getNoOfTracks() {
		System.out.println("No. Of tracks="+noOfTracks);
		
		return noOfTracks;
	}

	public DiffApp ()
	{
		super();
		numSubscriptions = 0 ; 
		interestCache = new Vector () ;
		dataCache = new Vector () ;
		activeTasksList = new Vector () ;
		interestCache_purgeTimer = null ;
		dataCache_purgeTimer = null ;		
	}
	
	@Override
	 protected void _start() {
		if(nid==sink_nid)
			EnergyStats.init(noOfNodes);
		else
			initialEnergy=getEnergy();
		 setTimeout(new CPUCheck(), 5);	 
	 }

	/* protected void _stop()  {		 
	 }

	 protected void _resume() {		 
	 }*/
	  
	//work-around to shutdown java
    public void shutdown(){
    	System.out.println("SHUTTING DOWN..");
    	System.exit(0);
    }
    
	class CPUCheck{	
	}
	
	public void setNoOfNodes(int noOfNodes) {
		this.noOfNodes = noOfNodes;
		CSVLogger.noOfNodes=noOfNodes;
	}
	
	public double getEnergy(){
	// Contract type: ENERGY_QUERY =0
    double energy = ((EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(0, -1.0,-1))).getEnergyLevel();
    if (energy <= 0 && lifetime==null){
		lifetime=getTime();
		log(Level.WARNING,"Out Of Energy, lifetime="+lifetime);
	}
    return energy;
	}

	public String getName() { return "DiffApp"; }

	public void setTargetName(String name)
	{	TargetName = new String(name) ;	}

	/** Looks up an event in the data cache */
	public synchronized DataCacheEntry dataCache_lookup(AttributeVector event)
	{
		int no = dataCache.size() ;
		for (int i = 0; i < no; i++) 
		{
			DataCacheEntry entry = (DataCacheEntry)dataCache.elementAt(i);
			if ( 		( event.IsMatching(entry.getEvent()) == true )
				&&
					( entry.IsExpired(getTime()) == false )
			   )
				return entry ;
		}

		return null ;
	}

	/** Looks up an event (with a specified interval) in the data cache */
	public synchronized DataCacheEntry dataCache_lookup(AttributeVector event, float dataInterval)
	{
		int no = dataCache.size() ;
		for (int i = 0; i < no; i++) 
		{
			DataCacheEntry entry = (DataCacheEntry)dataCache.elementAt(i);
			if ( 		( event.IsMatching(entry.getEvent()) == true )
				&&
					( dataInterval == entry.getDataInterval() )
				&&
					( entry.IsExpired(getTime()) == false )
			   )
			{
				return entry ;
			}
		}

		return null ;
	}

	/** Returns all previously seen events matching a given event (with a specified interval) */
	public synchronized Vector dataCache_lookupAll(AttributeVector event, float dataInterval)
	{
		Vector returnVector = new Vector() ;
		int no = dataCache.size() ;
		for (int i = 0; i < no; i++) 
		{
			DataCacheEntry entry = (DataCacheEntry)dataCache.elementAt(i);
			if ( 		( event.IsMatching(entry.getEvent()) == true )
				&&
					( dataInterval == entry.getDataInterval() )
				&&
					( entry.IsExpired(getTime()) == false )
			   )
			{
				returnVector.addElement(new Long(entry.getSource())) ;
			}
		}

		return returnVector ;
	}

	/** Looks up an event (with a specified interval and from a given source) in the data cache */
        public synchronized DataCacheEntry dataCache_lookup(AttributeVector event, float dataInterval, long source)
        {
                int no = dataCache.size() ;
                for (int i = 0; i < no; i++)
                {
                        DataCacheEntry entry = (DataCacheEntry)dataCache.elementAt(i);
                        if (            ( event.IsMatching(entry.getEvent()) == true )
                                &&
                                        ( dataInterval == entry.getDataInterval() )
				&&
					( source == entry.getSource() )
                                &&
                                        ( entry.IsExpired(getTime()) == false )
                           )
                        {
                                return entry ;
                        }
                }

                return null ;
        }

	/** Inserts an event description in the data cache */
	public synchronized void dataCache_insert(DataCacheEntry e)
	{
		if ( dataCache_purgeTimer == null )
		{
			dataCache_purgeTimer = new DiffTimer(DiffTimer.TIMEOUT_DATA_CACHE_PURGE, null) ;
			if ( dataCache_purgeTimer != null )
			{
				dataCache_purgeTimer.handle = setTimeout(dataCache_purgeTimer, DATA_CACHE_PURGE_INTERVAL) ;
			}
		}

		dataCache.addElement(e) ;
	}

	/** Prints all entries in the data cache */	
	public synchronized void dataCache_print()
	{
		int no = dataCache.size() ;
		System.out.println("DiffApp " + nid + ": Printing the data cache: " + no + " entries.");
		for (int i = 0; i < no; i++) 
		{
			DataCacheEntry entry = (DataCacheEntry)dataCache.elementAt(i);
			entry.printDataEntry() ;
		}
	}

	/** Purges expired entries from the data cache */
	public synchronized void dataCache_purge()
	{
		double currentTime = getTime() ;
		for ( int i = 0 ; i < dataCache.size() ; )
		{
			DataCacheEntry entry = (DataCacheEntry)dataCache.elementAt(i);

			if ( entry.IsExpired(currentTime) == true )
			{
				dataCache.removeElementAt(i) ;
			}
			else
			{
				i++ ;
			}	
		}
	}

	/** Purges expired entries from the interest cache */
	public synchronized void interestCache_purge()
	{
		double currentTime = getTime() ;
		for ( int i = 0 ; i < interestCache.size() ; )
		{
			InterestCacheEntry entry = (InterestCacheEntry)interestCache.elementAt(i);
			entry.gradientList_purge(currentTime) ;

			if ( entry.IsGradientListEmpty() == true )
			{
				interestCache.removeElementAt(i) ;
			}
			else
			{
				i++ ;
			}	
		}
	}

	/** Looks up an interest in the interest cache */
	public synchronized InterestCacheEntry interestCache_lookup(AttributeVector interest)
	{
		int no = interestCache.size() ;
		for (int i = 0; i < no; i++) 
		{
			InterestCacheEntry entry = (InterestCacheEntry)interestCache.elementAt(i);
			if ( interest.IsMatching(entry.getInterest()) == true )
				return entry ;
		}
		return null ;
	}

	/** Inserts an interest cache entry in the interest cache */
	public synchronized void interestCache_insert(InterestCacheEntry e)
	{
		if ( interestCache_purgeTimer == null )
		{
			interestCache_purgeTimer = new DiffTimer(DiffTimer.TIMEOUT_INTEREST_CACHE_PURGE, null) ;
			if ( interestCache_purgeTimer != null )
			{
				interestCache_purgeTimer.handle = setTimeout(interestCache_purgeTimer, INTEREST_CACHE_PURGE_INTERVAL) ;
			}
		}

		interestCache.addElement(e) ;
	}

	/** Prints all interest cache entries in the interest cache */
	public synchronized void interestCache_print()
	{
		System.out.println("DiffApp " + nid + ": Printing the interest cache.");
		int no = interestCache.size() ;
		for (int i = 0; i < no; i++) 
		{
			InterestCacheEntry entry = (InterestCacheEntry)interestCache.elementAt(i);
			entry.printInterestEntry() ;
		}
	}

	/** Looks up an active task description in the active tasks list */
	public synchronized ActiveTasksEntry activeTasksList_lookup(AttributeVector interest)
	{
		int no = activeTasksList.size() ;
		for (int i = 0; i < no; i++) 
		{
			ActiveTasksEntry entry = (ActiveTasksEntry)activeTasksList.elementAt(i);
			if ( interest.IsMatching(entry.getInterest()) == true )
				return entry ;
		}
		return null ;
	}

	/** This function is passed the data or exploratory data event. For each neighbor that has a gradient entry, the function creates a timer that generates the data at the "datarate" requested by the specified neighbor. */
	public synchronized void createDataTimers(InterestCacheEntry intrstEntry, AttributeVector event)
	{
		int no = intrstEntry.gradientList.size() ;
		for (int i = 0 ; i < no ; i++)
		{
			GradientEntry entry = (GradientEntry)intrstEntry.gradientList.elementAt(i);
			/* create a timer to periodically generate the data at the "datarate" 
			   requested by the specified neighbor. */
			if ( entry.dataTimer == null )
			{
				/* One may also consider adding sendPacket(, rand) here to send the data to the sink promptly instead of waiting until the gradient timer expires */
				sendPacket(new DataPacket(nid, entry.getPreviousHop(), event, entry.getDataRate(), getTime()), 0.0);
				/*entry.dataTimer = new DiffTimer(DiffTimer.TIMEOUT_SEND_DATA, new DataPacket(nid, entry.getPreviousHop(), event, entry.getDataRate(), getTime())) ;
				entry.dataTimer.handle = setTimeout(entry.dataTimer, (double)(entry.getDataRate())) ;*/
			}
			else
			{
			
			}
		}
	}

	/** Constructs a sensing event */
	public synchronized AttributeVector ConstructSensingEvent(SensorAppAgentContract.Message msg)
	{
		double sensorLocX, sensorLocY, snr;
		long targetNid;
		if (msg == null) {
			/*
			 * sensorLocX and sensorLocY are the X and Y coordinates of the
			 * sensor node and must be obtained from the mobility model.
			 */
			SensorPositionReportContract.Message positionMsg = new SensorPositionReportContract.Message();
			positionMsg = (SensorPositionReportContract.Message) mobilityPort
					.sendReceive(positionMsg);
			sensorLocX = positionMsg.getX();
			sensorLocY = positionMsg.getY();
			targetNid=this.nid;
			snr=500;
		}else{
			sensorLocX = msg.getTargetX() ;
			sensorLocY = msg.getTargetY() ;
		    targetNid=msg.getTargetNid();
		    snr=msg.getSNR();
		}
		
		AttributeVector event = new AttributeVector() ;
		event.addElement(new Attribute(Attribute.CLASS_KEY, Attribute.INT32_TYPE, Attribute.IS, -1, new Integer(Attribute.DATA_CLASS) ) ) ;
		event.addElement(new Attribute(Attribute.SCOPE_KEY, Attribute.INT32_TYPE, Attribute.IS, -1, new Integer(Attribute.NODE_LOCAL_SCOPE) ) ) ;
		event.addElement(new Attribute(Attribute.LONGITUDE_KEY, Attribute.FLOAT32_TYPE, Attribute.GE, -1, new Float(sensorLocX) ) ) ;
		event.addElement(new Attribute(Attribute.LONGITUDE_KEY, Attribute.FLOAT32_TYPE, Attribute.LE, -1, new Float(sensorLocX) ) ) ;
		event.addElement(new Attribute(Attribute.LATITUDE_KEY, Attribute.FLOAT32_TYPE, Attribute.GE, -1, new Float(sensorLocY) ) ) ;
		event.addElement(new Attribute(Attribute.LATITUDE_KEY, Attribute.FLOAT32_TYPE, Attribute.LE, -1, new Float(sensorLocY) ) ) ;
		event.addElement(new Attribute(Attribute.TARGET_KEY, Attribute.STRING_TYPE, Attribute.IS, TargetName.length(), TargetName) ) ;
		return event ;
	}

	/** Handles information received over the sensor channel  */
	public synchronized void recvSensorEvent(Object data_)
	{
		/* A sensor node that detects a target searches its interest cache for a matching interest entry. A matching entry is one whose rect encompasses the sensor location and the type of the entry matches the detected target type. */

		/* construct the event as a list of attribute-value pairs. */
		AttributeVector event = ConstructSensingEvent((Message) data_) ;

		InterestCacheEntry intrstEntry = interestCache_lookup(event) ;
		if ( intrstEntry != null )	/* if there is a matching interest. */
		{
			if ( numSubscriptions > 0 ) /* if there are nodes that subscribed to this interest. */
			{
				createDataTimers(intrstEntry, event) ;
			}
		}
	}

	/** Determines whether a node can satisfy an interest. Specifically, a sensor node can satisfy an interest if (a) the node is within the specified rect in the interest and (b) the type of the detected events (e.g., wheeled vehicle) is the same type as that in the interest */
	public synchronized boolean CanSatisfyInterest(AttributeVector interest)
	{
		/* construct the event as a list of attribute-value pairs. */
		AttributeVector event = ConstructSensingEvent(null) ;

		if ( interest.IsMatching(event) == true )
		{
			numSubscriptions++ ;
			return true ;		
		}
		else
		{
			return false ;
		}
	}

	/* Implements the data-driven local rule used to determine which neighbor needs to be reinforced. If the node must also reinforce at least one neighbor, it uses its data cache for this purpose. The same local rule choices apply. For example, this node might choose that neighbor from whom it first received the latest event matching the interest. */
	public synchronized long CheckToForwardPositiveReinforcement(AttributeVector interest, float newInterval)
	{		
		DataCacheEntry dataEntry = dataCache_lookup(interest) ;
		if ( dataEntry != null )	/* if there is a matching data packet, return the index of the node from whom I first received the latest event matching the interest. */
		{
                        long source = dataEntry.getSource() ;                        

			if ( dataCache_lookup(interest, newInterval, source) == null ) /* Obviously, we do not need to reinforce neighbors that are already sending traffic at the higher data rate. */
			{
				return source ;
			}
			else
			{
				return -1 ;
			}
		}

		return -1 ;
	}

	/** Handles an incoming positive reinforcement packet */
	public synchronized void HandleIncomingPositiveReinforcement(PositiveReinforcementPacket PstvReinforcementPkt)
	{
		double currentTime = getTime() ;

		/* Process the packet only if the node is the destination */
		if ( PstvReinforcementPkt.getDestination() == nid )
		{
			AttributeVector interest = PstvReinforcementPkt.getInterest() ;
			InterestCacheEntry intrstEntry = interestCache_lookup(interest) ;
			if ( intrstEntry != null )	/* if there is a matching interest. */
			{
				long source = PstvReinforcementPkt.getSource() ;

				/* Check to see if a gradient already exists in the gradientList of the interest cache entry */
				GradientEntry grdntEntry = intrstEntry.gradientList_lookup(source, currentTime) ;
				if ( grdntEntry != null ) /* if it does exist in the gradientList of the interest cache entry */
				{
					float newInterval = PstvReinforcementPkt.getPositiveReinforcementInterval() ;

					if ( intrstEntry.IsToForwardPositiveReinforcement(newInterval) )
					{
						/* the node must also reinforce at least one neighbor. The node should use its data cache for this purpose. However, we obviously do not need to reinforce neighbors that are already sending traffic at the higher data rate. */

						long destination = CheckToForwardPositiveReinforcement(interest, newInterval) ;
						if ( (destination != nid) && (destination != -1) )
						{
							sendPacket(new PositiveReinforcementPacket(nid, destination, interest, newInterval), DELAY * rand.nextDouble()) ;
						}
						else
						{
						}
					}

					grdntEntry.setDataRate(newInterval) ;
			
					/* cancel the current data timer if any. */
					if ( grdntEntry.dataTimer != null )
					{
						cancelTimeout(grdntEntry.dataTimer.handle) ;
						grdntEntry.dataTimer.handle = null ;
						grdntEntry.dataTimer.setObject(null) ;
						grdntEntry.dataTimer = null ;

						/* set a new data timer using the new data rate. */
						grdntEntry.dataTimer = new DiffTimer(DiffTimer.TIMEOUT_SEND_DATA, new DataPacket(nid, grdntEntry.getPreviousHop(), ConstructSensingEvent(null), grdntEntry.getDataRate(), getTime())) ;

						grdntEntry.dataTimer.handle = setTimeout(grdntEntry.dataTimer, (double)(grdntEntry.getDataRate())) ;
					}
				} // end if
			} // end if
		} // end if
	}

	/** Implements the data-driven local rule used to reinforce one particular neighbor in order to draw down real data. One example of such a rule is to reinforce any neighbor from which a node receives a previously unseen event (i.e., an event that does not exist in the data cache. */
	public synchronized boolean CheckToSendPositiveReinforcement(ActiveTasksEntry taskEntry, DataPacket dataPkt)
	{		
		AttributeVector event = dataPkt.getEvent() ;
		DataCacheEntry dataEntry = dataCache_lookup(event) ;
		if ( dataEntry == null )	/* if there is NOT a matching data packet (i.e., this data was not seen). */
		{
			/* add the received message to the data cache */
			dataCache_insert(new DataCacheEntry(dataPkt.getEvent(), dataPkt.getDataInterval(), dataPkt.getSource(), getTime())) ;

			taskEntry.newEventsWindow_insert(dataPkt.getSource()) ;

			Vector NeighborsToNegativelyReinforce = taskEntry.getListOfNeighborsToNegativelyReinforce() ;
			for ( int i = 0 ; i < NeighborsToNegativelyReinforce.size() ; i++ )
			{
				Long neighborEntry = (Long)(NeighborsToNegativelyReinforce.elementAt(i)) ;
				sendPacket(new NegativeReinforcementPacket(nid, neighborEntry.longValue(), taskEntry.getInterest(), taskEntry.getInterest().getFrequency()), DELAY * rand.nextDouble()) ;
			}

			if ( dataPkt.getDataInterval() != taskEntry.getDataInterval() )		/* Obviously, we do not need to reinforce neighbors that are already sending traffic at the higher data rate. */
			{
				return true ;
			}
			else
			{
				return false ;
			}
		}

		return false ;
	}

	/** Implements the data driven local rule used to determine which neighbors need to be negatively reinforced. The node uses its data cache for this purpose. The node negatively reinforces those neighbors that have been sending data to it. */
	public synchronized Vector CheckToForwardNegativeReinforcement(AttributeVector interest, float dataInterval)
	{		
		return dataCache_lookupAll(interest, dataInterval) ;
	}

	/** Handles an incoming negative reinforcement packet */
	public synchronized void HandleIncomingNegativeReinforcement(NegativeReinforcementPacket NgtvReinforcementPkt)
	{
		double currentTime = getTime() ;

		/* Process the packet only if the node is the destination */
		if ( NgtvReinforcementPkt.getDestination() == nid )
		{
			AttributeVector interest = NgtvReinforcementPkt.getInterest() ;

			InterestCacheEntry intrstEntry = interestCache_lookup(interest) ;
			if ( intrstEntry != null )	/* if there is a matching interest. */
			{
				long source = NgtvReinforcementPkt.getSource() ;

				/* Check to see if a gradient already exists in the gradientList of the interest cache entry */
				GradientEntry grdntEntry = intrstEntry.gradientList_lookup(source, currentTime) ;
				if ( grdntEntry != null ) /* if it does exist in the gradientList of the interest cache entry */
				{
					float newInterval = NgtvReinforcementPkt.getNegativeReinforcementInterval() ;
					float oldInterval = grdntEntry.getDataRate() ;

					grdntEntry.setDataRate(newInterval) ;

					if ( intrstEntry.IsToForwardNegativeReinforcement(newInterval) )
					{
						Vector neighbors = CheckToForwardNegativeReinforcement(interest, oldInterval) ;
						for ( int i = 0 ; i < neighbors.size() ; i++ )
						{
							Long nID = (Long)(neighbors.elementAt(i)) ;			
							sendPacket(new NegativeReinforcementPacket(nid, nID.longValue(), interest, newInterval), DELAY * rand.nextDouble()) ;
						}

						if ( neighbors.size() == 0 )
						{
						}
					}
					else
					{
					}
			
					/* cancel the current data timer if any. */
					if ( grdntEntry.dataTimer != null )
					{
						cancelTimeout(grdntEntry.dataTimer.handle) ;
						grdntEntry.dataTimer.handle = null ;
						grdntEntry.dataTimer.setObject(null) ;
						grdntEntry.dataTimer = null ;

						/* set a new data timer using the new data rate. */
						grdntEntry.dataTimer = new DiffTimer(DiffTimer.TIMEOUT_SEND_DATA, new DataPacket(nid, grdntEntry.getPreviousHop(), ConstructSensingEvent(null), grdntEntry.getDataRate(),getTime())) ;

						grdntEntry.dataTimer.handle = setTimeout(grdntEntry.dataTimer, (double)(grdntEntry.getDataRate())) ;
					}
				} // end if
			} // end if
		} // end if
	}

	/** Handles an incoming DATA packet */
	public synchronized void HandleIncomingData(DataPacket dataPkt)
	{
		double currentTime = getTime() ;

		/* Process the packet only if the node is the destination */
		if ( dataPkt.getDestination() == nid )
		{
			AttributeVector event = dataPkt.getEvent() ;
			InterestCacheEntry intrstEntry = interestCache_lookup(event) ;
			if ( intrstEntry != null )	/* if there is a matching interest. */
			{
				DataCacheEntry dataEntry = dataCache_lookup(event, dataPkt.getDataInterval()) ;
				if ( dataEntry == null )	/* if there is NOT a matching data packet (i.e., this data was not seen or transmitted before). */
				{
					/* add the received message to the data cache */
					dataCache_insert(new DataCacheEntry(event, dataPkt.getDataInterval(), dataPkt.getSource(), getTime())) ;

					/* the data message is resent to the node's neighbors. */
					int no = intrstEntry.gradientList.size() ;
					for (int i = 0 ; i < no ; i++)
					{
						GradientEntry entry = (GradientEntry)intrstEntry.gradientList.elementAt(i);
						sendPacket(new DataPacket(nid, entry.getPreviousHop(), event, entry.getDataRate(),getTime()), 0.0) ;
					} // end for
				} // end if
				else
				{
				} // end else
			} // end if
			else
			{
				ActiveTasksEntry taskEntry = activeTasksList_lookup(event) ;
				if ( taskEntry != null ) /* if there is a matching task */
				{
					if ((getTime() - lastTrackTime+0.1) >= taskEntry.getDataInterval()) {
						lastTrackTime = getTime();
						averageDelay=(averageDelay*noOfTracks+(lastTrackTime-dataPkt.timestamp))/(noOfTracks+1);
						noOfTracks++;						
						EnergyStats.markAsReporting();
						totalEnergyUsed=EnergyStats.getTotalEnergy();
					}
					
					/* Check if the neighbor sending the event needs to be positively reinforced. */
					if ( CheckToSendPositiveReinforcement(taskEntry, dataPkt) == true )
					{
						if ( isDebugEnabled() )
							System.out.println("DiffApp " + nid + ": Receiving information about an active task from " + dataPkt.getSource() + " at time " + currentTime + "! Positively reinforcing that neighbor") ;
						sendPacket(new PositiveReinforcementPacket(nid, dataPkt.getSource(), taskEntry.getInterest(), taskEntry.getDataInterval()), DELAY * rand.nextDouble()) ;

						if ( taskEntry.reinforcedNeighbors_lookup(dataPkt.getSource()) == false )
		 				{
							taskEntry.reinforcedNeighbors_insert(dataPkt.getSource()) ;
						}
					}
					else
					{
						//if ( isDebugEnabled() )
							//System.out.println("DiffApp " + nid + ": Receiving information about an active task from " + dataPkt.getSource() + " at time " + currentTime + "! But this information was already seen before OR " + dataPkt.getSource() + " is already sending data at the higher data rate.") ;
					}
				}
			} // end else
		} // end if
	}

	/** Handles an incoming INTEREST packet */
	public synchronized void HandleIncomingInterest(InterestPacket interestPkt)
	{
		double currentTime = getTime() ;
		AttributeVector interest = interestPkt.getInterest() ;
		float datarate = interest.getFrequency() ;
		float duration = interest.getRange() ;
		long source = interestPkt.source ;

		if ( activeTasksList_lookup(interest) != null )
		{	/* if I have already originated a matching interest */
		}
		else
		{
			/* Check to see if the interest already exists in the interest cache */
			InterestCacheEntry intrstEntry = interestCache_lookup(interest) ;
			if ( intrstEntry == null ) /* if it does not exist */
			{
				/* create an interest entry whose parameters are instantiated from the received interest. */
				Vector gradientList = new Vector () ;
				gradientList.addElement(new GradientEntry(source, datarate, duration, currentTime)) ;

				/* Insert an entry in the interest cache */
				interestCache_insert(new InterestCacheEntry(interest, currentTime, gradientList)) ;

				/* Forward the interest to the neighbors. */
				sendPacket(interest, DELAY * rand.nextDouble()) ; /* note that the last time the interest was sent has already been updated. */

				if ( CanSatisfyInterest(interest) )
				{
				}
			}
			else	/* if an interest entry does exist in the interest cache */
			{
				//double lastTimeSent = intrstEntry.getLastTimeSent() ;

				/* Check to see if a gradient already exists in the gradientList of the interest cache entry */
				GradientEntry grdntEntry = intrstEntry.gradientList_lookup(source, currentTime) ;
				if ( grdntEntry == null ) /* if it does not exist */
				{
					intrstEntry.gradientList_insert(new GradientEntry(source, datarate, duration, currentTime)) ;

					if ( intrstEntry.IsToResendInterest(currentTime) )
					{
						/* Forward the interest to the neighbors. */
						sendPacket(interest, DELAY * rand.nextDouble()) ;
						intrstEntry.setLastTimeSent(currentTime) ;
					}
				}
				else /* if a gradient does exist in the gradientList of the interest cache entry */
				{
					grdntEntry.setTimeStamp(currentTime) ;
					grdntEntry.setDuration(duration) ;

					if ( intrstEntry.IsToResendInterest(currentTime) )
					{
						/* Forward the interest to the neighbors. */
						sendPacket(interest, DELAY * rand.nextDouble()) ;
						intrstEntry.setLastTimeSent(currentTime) ;
					}
				} // end else
			} // end else
		} // end else
	}

	/** Handles information received over the wireless channel  */
	protected synchronized void recvSensorPacket(Object data_)
	{
		if ( data_ instanceof SensorPacket )
		{
			SensorPacket spkt = (SensorPacket)data_ ;
			switch ( spkt.pktType )
			{
				case DiffApp.INTEREST_PKT :
					InterestPacket interestPkt = (InterestPacket)spkt.getBody() ;
					noOfInterests++;
					HandleIncomingInterest(interestPkt) ;
					break ;
				case DiffApp.DATA_PKT :
					DataPacket dataPkt = (DataPacket)spkt.getBody() ;
					noOfDataPkts++;
					HandleIncomingData(dataPkt) ;
					break ;
				case DiffApp.POSITIVE_REINFORCEMENT_PKT :
					PositiveReinforcementPacket pstvReinforcementPkt = (PositiveReinforcementPacket)spkt.getBody() ;
					noOfReinforcements++;
					HandleIncomingPositiveReinforcement(pstvReinforcementPkt) ;
					break ;
				case DiffApp.NEGATIVE_REINFORCEMENT_PKT :
					NegativeReinforcementPacket ngtvReinforcementPkt = (NegativeReinforcementPacket)spkt.getBody() ;
					noOfReinforcements++;
					HandleIncomingNegativeReinforcement(ngtvReinforcementPkt) ;
					break ;
				default :
					super.recvSensorPacket(data_) ;
			}
		}
		else
			super.recvSensorPacket(data_) ;
    	}

	/** Initiates a sensing task by sending an INTEREST packet */
	public synchronized void subscribe(float longMin, float longMax, float latMin, float latMax, float duration, float interval, float dataInterval, double refreshPeriod)
	{
		/* constructs an interest */
		AttributeVector interest = new AttributeVector() ;
		interest.addElement(new Attribute(Attribute.CLASS_KEY, Attribute.INT32_TYPE, Attribute.IS, -1, new Integer(Attribute.INTEREST_CLASS) ) ) ;
		interest.addElement(new Attribute(Attribute.SCOPE_KEY, Attribute.INT32_TYPE, Attribute.IS, -1, new Integer(Attribute.GLOBAL_SCOPE) ) ) ;
		interest.addElement(new Attribute(Attribute.LONGITUDE_KEY, Attribute.FLOAT32_TYPE, Attribute.GE, -1, new Float(longMin) ) ) ;
		interest.addElement(new Attribute(Attribute.LONGITUDE_KEY, Attribute.FLOAT32_TYPE, Attribute.LE, -1, new Float(longMax) ) ) ;
		interest.addElement(new Attribute(Attribute.LATITUDE_KEY, Attribute.FLOAT32_TYPE, Attribute.GE, -1, new Float(latMin) ) ) ;
		interest.addElement(new Attribute(Attribute.LATITUDE_KEY, Attribute.FLOAT32_TYPE, Attribute.LE, -1, new Float(latMax) ) ) ;
		interest.addElement(new Attribute(Attribute.TARGET_KEY, Attribute.STRING_TYPE, Attribute.IS, TargetName.length(), TargetName) ) ;
		interest.addElement(new Attribute(Attribute.TARGET_RANGE_KEY, Attribute.FLOAT32_TYPE, Attribute.IS, -1, new Float(duration) ) ) ;
		interest.addElement(new Attribute(Attribute.TASK_FREQUENCY_KEY, Attribute.FLOAT32_TYPE, Attribute.IS, -1, new Float(interval) ) ) ;

		ActiveTasksEntry taskEntry = activeTasksList_lookup(interest) ;
		if ( taskEntry == null ) /* if there is NOT a matching task entry */
		{
			ActiveTasksEntry activeTask = new ActiveTasksEntry(interest, dataInterval, refreshPeriod, getTime(), N_WINDOW) ;

			int size = activeTasksList.size() ;
			activeTasksList.addElement(activeTask) ;

			/* create a timer to periodically refresh the interest */
			DiffTimer refresh_EVT = new DiffTimer(DiffTimer.TIMEOUT_REFRESH_INTEREST, new Integer(size)); /* the object passed to DiffTimer is the index of the interest to be refreshed in the interest cache. */
			refresh_EVT.handle = setTimeout(refresh_EVT, refreshPeriod) ;

			if ( isDebugEnabled() )
				System.out.println("DiffApp " + nid + ": Sending INTEREST packet at time " + getTime()) ;

			/* sends the interest */
                	sendPacket( ((ActiveTasksEntry)(activeTasksList.get(size))).interest, 0.0) ;
		}
		else
		{
                        if ( isDebugEnabled() )
                                System.out.println("DiffApp " + nid + ": Sending INTEREST packet at time " + getTime()) ;

			/* sends the interest */
			sendPacket(taskEntry.interest, 0.0) ;
		}
	}

	/** Sends a packet */
	public synchronized void sendPacket(Object data_, double delay)
	{
		if ( data_ instanceof AttributeVector ) /* Interest Packet */
		{
			AttributeVector interest = (AttributeVector)data_ ;
			if (delay != 0.0)
			{
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, interest);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			} 
			else
			{
				InterestPacket pkt = new InterestPacket(nid, interest) ;
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, INTEREST_PKT, pkt)) ;
			}
		}
		else if ( data_ instanceof DataPacket ) /* Data packet */
		{
			DataPacket dataPkt = (DataPacket)data_ ;
			if (delay != 0.0)
			{
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, dataPkt);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			}
			else
			{
				//downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, DATA_PKT, dataPkt)) ;
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET,
						dataPkt.destination, nid, 100, DATA_PKT, dataPkt)) ;
			}
		}
		else if ( data_ instanceof PositiveReinforcementPacket ) /* PositiveReinforcement Packet */
		{
			PositiveReinforcementPacket pstvReinforcementPkt = (PositiveReinforcementPacket)data_ ;	
			if (delay != 0.0)
			{
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, pstvReinforcementPkt);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			}
			else
			{
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, POSITIVE_REINFORCEMENT_PKT, pstvReinforcementPkt)) ;
			}
		}
		else if ( data_ instanceof NegativeReinforcementPacket ) /* NegativeReinforcement Packet */
		{
			NegativeReinforcementPacket ngtvReinforcementPkt = (NegativeReinforcementPacket)data_ ;
			if (delay != 0.0)
			{
				DiffTimer bcast_EVT = new DiffTimer(DiffTimer.TIMEOUT_DELAY_BROADCAST, ngtvReinforcementPkt);
				bcast_EVT.handle = setTimeout(bcast_EVT, delay);
			}
			else
			{
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, NEGATIVE_REINFORCEMENT_PKT, ngtvReinforcementPkt)) ;
			}
		}
	}

	/** Handles a timeout event */
	protected synchronized void timeout(Object data_) 
	{
		if ( data_ instanceof DiffTimer )
		{
			DiffTimer d = (DiffTimer)data_ ;
			int type = d.EVT_Type ;
			
			switch ( type )
			{
				case DiffTimer.TIMEOUT_DATA_CACHE_PURGE :
					dataCache_purge() ;

					/* if the size of the dataCache has become 0, there is no need for the timer. The timer will be set next time dataCache_insert is called. */
					if ( (dataCache.size() == 0) && (dataCache_purgeTimer.handle != null) )
					{
						cancelTimeout(dataCache_purgeTimer.handle) ;
						dataCache_purgeTimer.setObject(null) ;
						dataCache_purgeTimer.handle = null ; 
						dataCache_purgeTimer = null ;
					}
					else
					{
						/* reset the timer. */
						dataCache_purgeTimer.handle = setTimeout(dataCache_purgeTimer, DATA_CACHE_PURGE_INTERVAL) ;
					}
					break ;
				case DiffTimer.TIMEOUT_INTEREST_CACHE_PURGE :
					interestCache_purge() ;

					/* if the size of the interestCache has become 0, there is no need for the timer. The timer will be set next time interestCache_insert is called. */
					if ( (interestCache.size() == 0) && (interestCache_purgeTimer.handle != null) )
					{
						cancelTimeout(interestCache_purgeTimer.handle) ;
						interestCache_purgeTimer.setObject(null) ;
						interestCache_purgeTimer.handle = null ; 
						interestCache_purgeTimer = null ;
					}
					else
					{
						/* reset the timer. */
						interestCache_purgeTimer.handle = setTimeout(interestCache_purgeTimer, INTEREST_CACHE_PURGE_INTERVAL) ;
					}
					break ;
				case DiffTimer.TIMEOUT_SEND_DATA :
					DataPacket dataPkt = (DataPacket)(d.getObject()) ;
					DataCacheEntry dataEntry = dataCache_lookup(dataPkt.getEvent(), dataPkt.getDataInterval()) ;
					if ( dataEntry == null )
					{
						dataCache_insert(new DataCacheEntry(dataPkt.getEvent(), dataPkt.getDataInterval(), dataPkt.getSource(), getTime())) ;
					}
					else
					{
						dataEntry.setTimeStamp(getTime()) ;
					}
					dataPkt.timestamp=getTime();
					sendPacket(dataPkt, 0.0) ;
					InterestCacheEntry intrstEntry = interestCache_lookup(dataPkt.getEvent()) ;
					if ( intrstEntry != null )
					{
						GradientEntry grdntEntry = intrstEntry.gradientList_lookup(dataPkt.getDestination(), getTime()) ;
						if ( grdntEntry != null )
						{
							grdntEntry.dataTimer = null ;
						}
					}

					cancelTimeout(d.handle) ;
					d.handle = null ;
					d.setObject(null) ;
					d = null ;
					break ;
				case DiffTimer.TIMEOUT_DELAY_BROADCAST :
					
					if ( d.getObject() instanceof AttributeVector )
					{
						sendPacket((AttributeVector)(d.getObject()), 0.0) ;
					}
					else if ( d.getObject() instanceof DataPacket )
					{
						dataPkt = (DataPacket)(d.getObject()) ;
						dataEntry = dataCache_lookup(dataPkt.getEvent(), dataPkt.getDataInterval()) ;
						if ( dataEntry == null )
						{
							dataCache_insert(new DataCacheEntry(dataPkt.getEvent(), dataPkt.getDataInterval(), dataPkt.getSource(), getTime())) ;
						}
						else
						{
							dataEntry.setTimeStamp(getTime()) ;
						}

						sendPacket(dataPkt, 0.0) ;
					}
					else if ( d.getObject() instanceof PositiveReinforcementPacket )
					{
						PositiveReinforcementPacket pstvReinforcementPkt = (PositiveReinforcementPacket)(d.getObject()) ;
						sendPacket(pstvReinforcementPkt, 0.0) ;
					}
					else if ( d.getObject() instanceof NegativeReinforcementPacket )
					{
						NegativeReinforcementPacket ngtvReinforcementPkt = (NegativeReinforcementPacket)(d.getObject()) ;
						sendPacket(ngtvReinforcementPkt, 0.0) ;
					}
					break ;
				case DiffTimer.TIMEOUT_REFRESH_INTEREST :
					
					Integer I = (Integer)(d.getObject()) ;
					int i = I.intValue() ;
					ActiveTasksEntry a = (ActiveTasksEntry)(activeTasksList.get(i)) ;
					AttributeVector interest = a.getInterest() ;

					if ( (getTime() - a.getStartTime()) <= interest.getRange() ) /* depends on getTime() - interest start time */
					{
						if ( isDebugEnabled() )
							System.out.println("DiffApp " + nid + ": Sending INTEREST packet at time " + getTime()) ;

						sendPacket(interest, 0.0 ) ;
						DiffTimer refresh_EVT = new DiffTimer(DiffTimer.TIMEOUT_REFRESH_INTEREST, new Integer(i)); 
						refresh_EVT.handle = setTimeout(refresh_EVT, a.refreshPeriod) ;
					}
					else if ( d.handle != null )
					{
						/* The task state has to be purged from the node after the time indicated by the duration attribute. */
						activeTasksList.remove(i) ;
					}

					cancelTimeout(d.handle) ;
					d.setObject(null) ;
					d.handle = null ;
					break ;
			}
		}else if(data_ instanceof CPUCheck){
			if(nid==sink_nid) return;
			double currEnergy= getEnergy();
			EnergyStats.update((int) nid, initialEnergy-currEnergy,currEnergy,currEnergy>0,getTime());			
			WakeUp();		
			setTimeout(data_, 5);
		}else
		{
			super.timeout(data_) ;
		}    
	}
	public void log(Level level, String string) {
		if(level.intValue()>=Level.INFO.intValue())
			Logger.global.log(Level.INFO,getTime()+"[Node:"+nid+"]["+"] "+string);        
    }
	
	public void collectStats(){
        log(Level.INFO,"*******************STATS**************");
        //nid,noOfEventsMissed,totalEvents,noOfPktsDropped,totalPkts,task1Id,task1,task2Id,task2,task3Id,task3,totalCost,totalReward,trPackets
        if(nid==sink_nid){
        	EnergyStats.NodeStat lowestLifeNode=EnergyStats.getNodeWithLowestLifetime();
        	String mobility=System.getProperties().getProperty("target.mobile", "true");
        	String stats=noOfNodes+","+nid+","+getNoOfTracks()+","+lastTrackTime+","+averageDelay+","+totalEnergyUsed+","
        		+lowestLifeNode.toString()+","+Boolean.parseBoolean(mobility);
        	CSVLogger.logGlobal("sinkStats",stats,Algorithm.DIFFUSION);        	
        }else{
        	String nodeStats=noOfNodes+","+nid+","+noOfInterests+","+noOfDataPkts+","+noOfReinforcements+","+getEnergy()+","+lifetime; //+","+totalPkts;
            CSVLogger.logGlobal("nodeStats", nodeStats, Algorithm.DIFFUSION);
        	interestCache_print();
        }
        
       /* if(!globalLogged){
        	
        for(int i=0; i< globalRewardManager.getGlobalRewards().size();i++){
            CSVLogger.log("reward",""+globalRewardManager.getGlobalRewards().get(i),false,algorithm.getAlgorithm());            
        }
        CSVLogger.logGlobal("global-stats",globalRewardManager.stats(),algorithm.getAlgorithm());
        globalLogged=true;
        }*/
    }
	/**
     * Set both Radio components in IDLE
     * so that they are ready for either receiving, sending, and/or
     * processing.
    */
    public void WakeUp()
    {
    	//set the CPU to ACTIVE
       // if (cpuMode != 2) {
        	setCPUMode(2);    //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        //}
        //set the radio to IDLE
        //Contract type: SET_RADIO_MODE = 1 & Radio Modes:RADIO_IDLE=0
        EnergyContract.Message temp = (EnergyContract.Message)wirelessPhyPort.sendReceive(new EnergyContract.Message(1, -1.0, 0));
        if (temp.getRadioMode() != 0) {
            System.out.println("Unable to turn radio back on to Idle mode. Its mode is: " + temp.getRadioMode());
        }
        return;
    }
}
