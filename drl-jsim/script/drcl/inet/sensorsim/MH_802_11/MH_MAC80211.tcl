# ############################################################
#
# Sample tcl file of a wireless sensor network simulation.
# Author: Nicholas Merizzi
# Date: 03/10/2005
#
# This simulation uses a multi-hop method to communicate
# information back to the base station. The MAC layer is based
# on the 802.11 protocol. 
#
#
# ###########################################################

source "../../../../test/include.tcl"

cd [mkdir -q drcl.comp.Component /multiHop80211]

# TOTAL number of nodes (sensor nodes + target nodes)
set node_num 26

# Number of TARGET nodes ONLY
set target_node_num 1
# Hence, number of SENSORS = node_num - target_node_num

set sink_id 0

#to keep track of all the sensors neighbors and their 
#location used in a subroutine further-on
set sensorList []
set delNodes []

#*************************
#NICHOLAS
#FOR FILE WRITTING
set count 0

# create the sensor channel
mkdir drcl.inet.sensorsim.SensorChannel chan 

# Capacity of the sensor channel is total number of nodes (sensors + targets)
# make simulation for $node_num nodes
! chan setCapacity $node_num

# create the propagation model
mkdir drcl.inet.sensorsim.SeismicProp seismic_Prop 
! seismic_Prop setD0 0.2

# create the sensor node position tracker
mkdir drcl.inet.sensorsim.SensorNodePositionTracker nodetracker
			# maxX  minX maxY minY
! nodetracker setGrid 30.0 0.0 100.0 0.0

# connect the sensor channel to the sensor node position tracker
connect chan/.tracker@ -and nodetracker/.channel@

# create the wireless channel
mkdir drcl.inet.mac.Channel channel

# Capacity of the wireless channel is number of sensors and sinks ONLY
# which is equal to $node_num - $target_node_num
! channel setCapacity [expr $node_num - $target_node_num]

# create the node position tracker
mkdir drcl.inet.mac.NodePositionTracker tracker

#the dx and dy below represent 'how far' my signal travels
#so in this case any node located in my 100x100m grid will hear
#what a sensor broadcasts
#                 maxX minX maxY minY  dX   dY
! tracker setGrid 30.0 0.0 100.0 0.0 30.0 100.0

connect channel/.tracker@ -and tracker/.channel@

#*****************
#NICHOLAS
# 	In order to graph total # of sensors still 
#	alive we created a new component that will 
#	have a connected port to a plotter
mkdir drcl.inet.sensorsim.AliveSensors	liveSensors
set numNodesPlot_ [mkdir drcl.comp.tool.Plotter .numNodesPlot]
connect -c liveSensors/.plotter@ -to $numNodesPlot_/0@0



#****************************************************
# FOR THE SINKs ONLY, do the following
# SINKs have only a network protocol stack
for {set i 0} {$i < [expr $sink_id + 1]} {incr i} {
	puts "create sink $i"
	set node($i) [mkdir drcl.comp.Component n$i]
	
	cd n$i
	mkdir drcl.inet.sensorsim.MultiHop.SinkAppMH app
	! app setNid $i
	! app setSinkNid $sink_id
	
	mkdir drcl.inet.sensorsim.WirelessAgent wireless_agent
		
	
	# connect the sensor application to the wireless agent
	# so that sensors can send through the wireless network protocol stack
	connect app/down@ -to wireless_agent/up@	
	
	# connect the wireless agent to the sensor application
	# so that sensors can receive thru the wireless network protocol stack
	connect wireless_agent/.toSensorApp@ -to app/.fromWirelessAgent@

	mkdir drcl.inet.mac.LL ll
	mkdir drcl.inet.mac.ARP arp
	mkdir drcl.inet.core.queue.FIFO queue
	mkdir drcl.inet.mac.Mac_802_11 mac

	# added 09-04-04
	! mac disable_PSM

	mkdir drcl.inet.mac.WirelessPhy wphy

#************
#NICHOLAS
	! wphy setMultiHopMode 1	;#turn on MH mode settings
#************

	mkdir drcl.inet.mac.FreeSpaceModel propagation 
	mkdir drcl.inet.mac.MobilityModel mobility
	     
	set PD [mkdir drcl.inet.core.PktDispatcher      pktdispatcher]
    	set RT [mkdir drcl.inet.core.RT                 rt]
   	set ID [mkdir drcl.inet.core.Identity           id]
 
	! pktdispatcher setRouteBackEnabled 1

    	$PD bind $RT
    	$PD bind $ID	

#**************	
#Nicholas -->Above we inserted a permanent entry in the 
#	     table going to the sink. Here this contract is
#	     needed so we can send directly to neighbors.

	connect app/.setRoute@ -to rt/.service_rt@
#**************************

	# present if using 802.11 power-saving mode
	connect mac/.energy@ -and wphy/.energy@ 

	connect wphy/.mobility@    -and mobility/.query@
	connect wphy/.propagation@ -and propagation/.query@

	
	connect mac/down@ -and wphy/up@
	connect mac/up@   -and queue/output@

	connect ll/.mac@ -and mac/.linklayer@
	connect ll/down@ -and queue/up@ 
	connect ll/.arp@ -and arp/.arp@
	
	connect -c pktdispatcher/0@down -and ll/up@   
	 
	set nid $i
	
	! arp setAddresses  $nid $nid
	! ll  setAddresses  $nid $nid
	! mac setMacAddress $nid
	! wphy setNid       $nid
	! mobility setNid   $nid
	! id setDefaultID   $nid

	! queue setMode      "packet"
	! queue setCapacity  40
	
	# disable ARP 
	! arp setBypassARP  [ expr 2>1]
	
	! mac setRTSThreshold 0
	
	connect mobility/.report@ -and /multiHop80211/tracker/.node@

	connect wphy/down@ -to /multiHop80211/channel/.node@

	! /multiHop80211/channel attachPort $i [! wphy getPort .channel]
	
	#                              	maxX maxY  maxZ minX  minY minZ  dX  dY   dZ
    	! mobility setTopologyParameters 30.0 100.0 0.0 30.0 100.0 0.0 30.0 100.0 0.0

	! mac  disable_MAC_TRACE_ALL

	connect -c  wireless_agent/down@ -and pktdispatcher/1111@up
	
	cd ..
}

#*********************************************************************
# FOR THE SENSORS ONLY , do the following

for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {

	puts "create sensor $i"
	set node($i) [mkdir drcl.comp.Component n$i]
	
	cd n$i

	#*****************
	#Nicholas
	mkdir drcl.inet.sensorsim.MultiHop.MultiHopApp app
	! app setNid $i
	! app setSinkNid $sink_id
	! app setCoherentThreshold 1000.0
	! app setNn_ [expr $node_num - 1]

	connect app/.getNeighbor@ -and /multiHop80211/nodetracker/.multiHop@
	#*****************

	# create nodes
	mkdir drcl.inet.sensorsim.SensorAgent agent

	! agent setDebugEnabled 0

	# create sensor physical layers
	mkdir drcl.inet.sensorsim.SensorPhy phy 
	! phy setRxThresh 0.0
	! phy setDebugEnabled 0

	# create mobility models
	mkdir drcl.inet.sensorsim.SensorMobilityModel mobility

	! phy setNid $i 
	! phy setRadius 100.0

	# connect phyiscal layers to sensor agents so that nodes can receive
	connect phy/.toAgent@ -to agent/.fromPhy@
	
	# connect sensor agent and sensor application
	connect agent/.toSensorApp@ -to app/.fromSensorAgent@

	# connect the sensor channel to the nodes so that they can receive
	! /multiHop80211/chan attachPort $i [! phy getPort .channel]

	# connect the nodes to the propagation model
	connect phy/.propagation@ -and /multiHop80211/seismic_Prop/.query@

	! mobility setNid $i

	# create wireless agent layers
	mkdir drcl.inet.sensorsim.WirelessAgent wireless_agent

	# connect the sensor application to the wireless agent
	# so that sensors can send through the wireless network protocol stack
	connect app/down@ -to wireless_agent/up@	

	# connect the wireless agent to the sensor application
	# so that sensors can receive thru the wireless network protocol stack
	connect wireless_agent/.toSensorApp@ -to app/.fromWirelessAgent@

	mkdir drcl.inet.mac.LL ll
 	mkdir drcl.inet.mac.ARP arp
	mkdir drcl.inet.core.queue.FIFO queue
	mkdir drcl.inet.mac.Mac_802_11 mac

	# added 09-04-04
	! mac disable_PSM

	#*************
	#NICHOLAS
	#added to let the MAC802.11 layer
	#it is in charge of maintaining the
	#radio modes.
	! mac setIs_uAMPS 0
	#*************

	mkdir drcl.inet.mac.WirelessPhy wphy

	#*************
	#NICHOLAS
	! wphy setMultiHopMode 1	;#turn on MH mode settings
	#************

	mkdir drcl.inet.mac.FreeSpaceModel propagation 

   	set PD [mkdir drcl.inet.core.PktDispatcher      pktdispatcher]
    	set RT [mkdir drcl.inet.core.RT                 rt]
    	set ID [mkdir drcl.inet.core.Identity           id]

	! pktdispatcher setRouteBackEnabled 1
 
    	$PD bind $RT
    	$PD bind $ID	

	#**************
	#NICHOLAS
	# create route configuration request for testing
	#this is to define the interfaces. So in this case each sensor
	#only has 1 interface (hence array size 1) and its eth0.
	#another example is (which has 3 interfaces 0, 2, and 4: 
	#set ifs [java::new drcl.data.BitSet [java::new {int[]} 3 {0 2 4}]]
	 
	set ifs [java::new drcl.data.BitSet [java::new {int[]} 1 {0}]]
	set base_entry [java::new drcl.inet.data.RTEntry $ifs]

	set key [java::new drcl.inet.data.RTKey $i 0 -1]
   	set entry_ [!!! [$base_entry clone]]

	! rt add $key $entry_ 
	
	#**************	
	#Nicholas -->Above we inserted a permanent entry in the 
	#	     table going to the sink. Here this contract is
	#	     needed so we can send directly to neighbors.

	connect app/.setRoute@ -to rt/.service_rt@
	#**************************

	# present if using 802.11 power-saving mode
	connect mac/.energy@ -and wphy/.energy@ 

	#*****************************
	#NICHOLAS
	connect app/.energy@ -and wphy/.appEnergy@
	mkdir drcl.inet.sensorsim.CPUAvr cpu

	connect app/.cpu@ -and cpu/.reportCPUMode@
	connect cpu/.battery@ -and wphy/.cpuEnergyPort@
	#******************************

	connect wphy/.mobility@    -and mobility/.query@
	connect wphy/.propagation@ -and propagation/.query@
	
	connect mac/down@ -and wphy/up@
	connect mac/up@   -and queue/output@
	
	connect ll/.mac@ -and mac/.linklayer@
	connect ll/down@ -and queue/up@ 
	connect ll/.arp@ -and arp/.arp@
	
	connect -c pktdispatcher/0@down -and ll/up@   
	 
	set nid $i
	
	! arp setAddresses  $nid $nid
	! ll  setAddresses  $nid $nid
	! mac setMacAddress $nid  ;#set MAC

	! wphy setNid       $nid
	! id setDefaultID   $nid

	! queue setMode      "packet"
	! queue setCapacity  40

	# disable ARP 
	! arp setBypassARP  [ expr 2>1]
	
	! mac setRTSThreshold 0
	
	connect mobility/.report@ -and /multiHop80211/tracker/.node@
	connect wphy/down@ -to /multiHop80211/channel/.node@

	! /multiHop80211/channel attachPort $i [! wphy getPort .channel]
	
	#                                maxX maxY maxZ minX minY minZ dX dY dZ
    	! mobility setTopologyParameters 30.0 100.0 0.0 30.0 100.0 0.0 30.0 100.0 0.0

	! mac  disable_MAC_TRACE_ALL

	connect -c  wireless_agent/down@ -and pktdispatcher/1111@up
	
	cd ..
}


#***********************************************************
# FOR THE TARGET NODES ONLY , do the following
if { $target_node_num == 0 } {
	puts "No target agents .... "
} else {
	for {set i [expr $node_num - $target_node_num]} {$i < $node_num} {incr i} {
		puts "create target $i"

		set node$i [mkdir drcl.comp.Component n$i]
	
		cd n$i

		# create target agents
		mkdir drcl.inet.sensorsim.TargetAgent agent
		! agent setBcastRate 1.0
		! agent setSampleRate 1.0
	
		# create sensor physical layers
		mkdir drcl.inet.sensorsim.SensorPhy phy 
		! phy setRxThresh 0.0
		! phy setNid $i
		! phy setRadius 100.0

		! phy setDebugEnabled 0

		# create mobility models
		mkdir drcl.inet.sensorsim.SensorMobilityModel mobility

		# connect target agents to phy layers so that nodes can send
		connect agent/down@ -to phy/up@	
	
		# connect phy layers to sensor channel so that nodes can send
		connect phy/down@ -to /multiHop80211/chan/.node@

		# connect the nodes to the propagation model
		connect phy/.propagation@ -and /multiHop80211/seismic_Prop/.query@

		! mobility setNid $i

		# set the topology parameters
		! mobility setTopologyParameters 30.0 100.0 0.0 30.0 100.0 0.0

		cd ..
	}
}

#********************************************************************
# for SENSORs and TARGETs only. Not SINKs
for {set i [expr $sink_id + 1]} {$i < $node_num} {incr i} {
	# connect the mobility model of each node to the node position tracker
	connect n$i/mobility/.report_sensor@ -and /multiHop80211/nodetracker/.node@
	connect n$i/phy/.mobility@ -and n$i/mobility/.query@
}


#***********************************************
#Positioning
#

# set the position of sink nodes args=> (speed(m/sec), xCoord,yCoord,zCoord
! $node(0)/mobility setPosition 0.0 0.0 0.0 0.0
 
# for the sensors They will be randomly placed on the grid (2D only)
# set the position of sensor nodes args=> (speed(m/sec), xCoord,yCoord,zCoord
for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {
   ! n$i/mobility setPosition 0.0 [expr rand()*30] [expr rand() * 100] 0.0
}

# for the target we can include random mobility They will be randomly 
# placed on the grid (2D only) 
#set the position of target nodes args=> (speed(m/sec), xCoord,yCoord,zCoord
for {set i [expr $node_num - $target_node_num]} {$i < $node_num} {incr i} {
   ! n$i/mobility setPosition 0.0 [expr rand()*30] [expr rand() * 100] 0.0
}


#***********************************************
#routeInfo() to execute do:
#	script "routeInfo" -at 0.35 -period 4.0 -on $sim
proc routeInfo { } {
   global sim n1 
   puts "Current Route Table\n [! n1/rt info]"
}

#***********************************************
#Output remaining energy levels of the sensors to a plotter
set plot_ [mkdir drcl.comp.tool.Plotter .plot]
for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {
   connect -c n$i/app/.plotter@ -to $plot_/$i@0
}

#***********************************************
#Plotters for the SINK node

#Graph # 1
#To plot the total number of received packets at the sink
set sinkPlot1_ [mkdir drcl.comp.tool.Plotter .sinkPlot1]
connect -c n0/app/.PacketsReceivedPlot@ -to $sinkPlot1_/0@0

#Graph # 2
#Calculate the avg latency when the sink finally receives it
set sinkPlot2_ [mkdir drcl.comp.tool.Plotter .sinkPlot2]
connect -c n0/app/.latencyPlot@ -to $sinkPlot2_/0@0


#Graph # 3
#plot the actual phenomena being sensed.
! n$sink_id/app createSnrPorts $node_num $target_node_num
set sinkPlot3_ [mkdir drcl.comp.tool.Plotter .sinkPlot3]

for {set i 0} {$i < $target_node_num} {incr i} {
   
   connect -c n$sink_id/app/.snr$i@ -to $sinkPlot3_/$i@$i
   if { $testflag } {
       attach -c $testfile/in@ -to n$sink_id/app/.snr$i@
   }
}


#*************************************************
#wsnLoop()
#
#	This method is called periodically to check 
#	if the simulation should continue or not. If  
# 	all nodes are dead then stop the simulator and
#	display the cummulative statistics... o.w keep
#	running
proc wsnLoop { } { 
	global sim node_num node sink_id target_node_num

	#reset variables
	set live_sensors 0
	set dead_sensors 0
	set total_packets 0
	set dropped_packets 0

	#check how many are still alive
	for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {
		if { [! $node($i)/app isSensorDead] == 0 } {
			incr live_sensors
		} else {
			incr dead_sensors
		}

	}
	script [! liveSensors setLiveNodes $live_sensors]
	script [! liveSensors updateGraph]

	#display statistics if they are all dead.
	if { $dead_sensors == [expr $node_num - $target_node_num - 1] } {
	   puts "All nodes dead at [! $sim getTime]"
	   $sim stop
	   puts "----------------------------------------------"
	   puts "Simulation Terminated\n"
	   puts "Results:"
	   puts "Base Station Received [! n0/app getTotalINPackets]"
	   puts "Collisions at Base Station: [! n0/mac getCollision]"
	   
           for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {
	      set curr_packets [! n$i/app geteID]
	      puts "Sensor$i Sent $curr_packets Packets to BS"   
	      set total_packets [expr $total_packets + $curr_packets] 
	   }
	   
	   set app_dropped [! n1/app getDropped_packets]
	   set wphy_dropped [! n1/wphy getDropped_packets] 
	   #set mac_dropped [! n1/mac getDropped_packets]

	   puts "Total packets dropped at Application layer: $app_dropped"
	   puts "Total packets dropped at physical layer: $wphy_dropped"	
	   #puts "Drops due to collisions (discovered at MAC layer: $mac_dropped"	
	   #set dropped_packets [expr $app_dropped + $wphy_dropped + $mac_dropped] 
	   set dropped_packets [expr $app_dropped + $wphy_dropped] 

	   puts "Total Packets sent from all nodes: $total_packets"
	   puts "Number of Dropped Packets: $dropped_packets"
	   puts "Success Rate: [expr ([! n0/app getTotalINPackets].0 / $total_packets.0) * 100]"
	}
}



#***********************************************
#sensorLocPrintOut()
#	Goes throught all sensors and prints their
#	(X,Y,Z) Coordinates
proc sensorLocPrintOut { } {
	global sink_id node_num	target_node_num
	for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {
		script [! n$i/app printNodeLoc]	
	}
}

#***********************************************
#Output sensor location and status 
#to a file for GUI to read from
proc file_output { } {
   global sink_id node_num count target_node_num

   #open a file for writting
   set filename "sensorInfo$count.log"
   set out [open $filename w]

   for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {
     #get its x and y location
     set Xloc [! n$i/app getX]
     set Yloc [! n$i/app getY]

     #get its energy
     set status [expr [! n$i/app isSensorDead] - 0]

     #write it to the file
     puts $out "$Xloc $Yloc $status"
   }
   incr count

   close $out
}


#***********************************************
#Output sensor location and status 
#to a file for GUI to read from
proc energy_dist { } {
   global sink_id node_num count target_node_num
     
   #open a file for writting
   set filename "energyDistrNode.log"
   set out [open $filename w]
							   
   puts $out "TxCost     RxCost    IdleCost    SleepCost    CPUcost"
   
   for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {	
      #collect the data 
      set txCost [! n$i/wphy getRadioTotalTX]
      set rxCost [! n$i/wphy getRadioTotalRX]
      set idleCost [! n$i/wphy getRadioTotalidle]
      set sleepCost [! n$i/wphy getRadioTotalsleep]
      set cpuCost [! n$i/wphy getTotalCPU]

      #write it to the file
      puts $out "$txCost $rxCost $idleCost $sleepCost $cpuCost"
   }

   close $out
}


#***********************************************
#Prints out each sensors neighbor at the present time
#
proc printNeighborList { } {
   global sink_id node_num count target_node_num
     
   for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {	
	script [! n$i/app printNeighborID]
   }
   puts "\n"	
	
}

#***********************************************
#To call this do: 
# script "getQueueSize" -at 20.0 -period 1.0 -on $sim

proc getQueueSize { } {
   global sink_id node_num count target_node_num
     
   puts "\n-------\nQUEUE SIZES\n-------"
   for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {	
   	puts "Queue size of sensor$i is:  [! n$i/queue getSize]"	
   }
	
}

#***********************************************
#Find next hop neighbor-- node to whom always send data.
#Choose closest node that is in the direction of the base station.
#NOTE!  This algorithm assumes nodes know the location of all nodes
#near them.  In practice, this would require an initial set-up
#phase where this information is disseminated throughout the network
#and that each node has a GPS receiver or other location-tracking
#algorithms to determine node locations.
proc setNeighbor { } {
   global sink_id node_num count sensorList target_node_num
   
   for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {	
   	script [! n$i/app setNeighbor]	
   
	#we also need to store it in the script
	#to maintain their next neighbor when a 
	#node dies.

        set selfID [! n$i/app getNid]
	set tempID [! n$i/app getNeighbor_id]
	set tempX  [! n$i/app getNeighborX]
	set tempY  [! n$i/app getNeighborY]
	set tempZ  [! n$i/app getNeighborZ]
	set tempDist [! n$i/app getNeighbor_dist]

	#inner list: [nid, neighborID, neigh_X, neigh_Y, neighZ, tempDist]
	set iThSensor [list $selfID $tempID $tempX $tempY $tempZ $tempDist]

	#append it to the main list.
	set sensorList [lappend SensorNeighbors $iThSensor]
   }
   #puts "Size of List is: [llength $sensorList]"  
}

#***********************************************
proc neighborUpdate { } {
   global sink_id node_num count sensorList delNodes
  
   for {set i 0 } {$i < [llength $sensorList]} {incr i} {
     #extract the current element
      set current_node [lindex $sensorList $i]

      set newSID [lindex $current_node 0]
      set newNID [lindex $current_node 1]
      set newX  [lindex $current_node 2]
      set newY  [lindex $current_node 3]
      set newZ  [lindex $current_node 4] 
      set newDist [lindex $current_node 5]

      #collect the data 
      set remEnergy [! n$newSID/wphy getRemEnergy]
  
      if { $remEnergy <= 0 } {
              	
	for {set j 0 } {$j < [llength $sensorList]} {incr j} {

           set currentID [lindex [lindex $sensorList $j] 0]
	   set neighborID [lindex [lindex $sensorList $j] 1]
	      	   
	#   puts "Checking Sensor$currentID Neighbor Settings"
           if { $currentID != $newSID } {	      
	      if { $neighborID == $newSID} {
                 #update the nodes neighbor in Java
	         script [! n$currentID/app setNewNeighborID $newNID]
		 script [! n$currentID/app setNewNeighborX $newX]
		 script [! n$currentID/app setNewNeighborY $newY]
		 script [! n$currentID/app setNewNeighborY $newZ]
		 script [! n$currentID/app setNewNeighborDist $newDist]

                 #update the nodes neighbor w/in script
	 	 set iThSensor [list $currentID $newNID $newX $newY $newZ $newDist]
		 #append it to the main list.
	         set sensorList [lreplace $sensorList $j $j $iThSensor]
	      } 
	   }
	};#end inner for
   #     puts " inserting number $newSID into delNodes"
        set delNodes [lappend delNodes $newSID]

      };#end if energy = 0   
    
   } ;#end outer for 
   #Now remove the elements from sensorList
   for {set k 0 } {$k < [llength $delNodes]} {incr k} {

      set delID [lindex $delNodes $k] ;#which node is it
  #    puts "getting index of $delID"
      for {set z 0 } {$z < [llength $sensorList]} {incr z} {
         set currentID [lindex [lindex $sensorList $z] 0]
	 if { $currentID == $delID } {  
            set index  $z
	 }
      }
      set sensorList [lreplace $sensorList $index $index] 
   }
   set delNodes [] ;#clear that temp list.
} 



#***********************************************
#Strictly used for validation purposes. This method
#can be called to get the energy at any random
#point of a particular node.
proc getCurrEnergy { } {
  
   global sink_id node_num count target_node_num

   puts "\n\n"

   for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {	
   	puts "------------------------------------------------"	
       
	puts "Sensor$i radio in Idle mode used: [! n$i/wphy getRadioTotalidle] joules"
	puts "Sensor$i radio in Rx mode used: [! n$i/wphy getRadioTotalRX] joules"
	puts "Sensor$i radio in Tx mode used: [! n$i/wphy getRadioTotalTX] joules\n"	

	puts "Sensor$i Radio was idle for: [! n$i/wphy getTotalRadioIdleTime] sec"
	puts "Sensor$i Radio was Tx for: [! n$i/wphy getTotalRadioActiveTime] sec"
	puts "Sensor$i Radio was Rx for: [! n$i/wphy getTotalRadioRxTime] sec\n"
		
	puts "Sensor$i CPU active used: [! n$i/wphy getTotalCPUactive] joules"
	puts "Sensor$i CPU sleep used: [! n$i/wphy getTotalCPUsleep] joules\n"

	puts "Sensor$i CPU was active for: [! n$i/wphy getTotalCPUactiveTime] sec"
	puts "Sensor$i CPU was sleep for: [! n$i/wphy getTotalCPUsleepTime] sec\n"

	
	puts "Sensor$i CPU Total used: [! n$i/wphy getTotalCPU] joules \n"

	puts "Sensor$i has [! n$i/wphy getRemEnergy] joules remaining"

   }
}
					     
#-------------------------


#***********************************************
puts "simulation begins...\n"
set sim [attach_simulator .]
#$sim setDebugEnabled 1
$sim stop

#******************start the sink************************
script {run n0} -at 0.001 -on $sim

#***************start the sensors************************
for {set i [expr $sink_id + 1]} {$i < $node_num} {incr i} {
	script puts "run n$i" -at 0.01 -on $sim
}

#*********print out all the node locations**************
script "sensorLocPrintOut" -at 0.03 -on $sim

#********determine who their neighbors are**************
script "setNeighbor" -at 0.1 -on $sim

#*******print off each of their respective neighbors****
script "printNeighborList" -at 0.15 -on $sim

#*******Check if Sensor Status**************************
script "wsnLoop" -at 1.0 -period 3.0 -on $sim

#*******To maintain neighbor settings*******************
script "neighborUpdate" -at 15.0 -period 0.5 -on $sim

#************For Matlab plotting************************
#script "file_output" -at 1.0 -period 100.0 -on $sim

#the following line is only used in validation!
#script "getCurrEnergy" -at 10.0 -period 10.0 -on $sim

$sim resumeTo 10000.1