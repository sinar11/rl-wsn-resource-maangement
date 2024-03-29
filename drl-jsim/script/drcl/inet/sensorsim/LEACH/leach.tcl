# *********************************************************
# Sample tcl file of a wireless sensor network simulation.

# Author: Nicholas Merizzi
# Date: 12/23/2003
#
# This script will simulate a sensor network which utilizes
# the hierarchichal routing protocol called LEACH. J-Sim was
# modified so that clustering can occur, as well as complete
# power control over the radio components. 
#
#
#**********************************************************

source "../../../../test/include.tcl"

cd [mkdir -q drcl.comp.Component /LEACH]

# TOTAL number of nodes (sensor nodes + target nodes)
set node_num 26

# Number of TARGET nodes ONLY
set target_node_num 2
# Hence, number of SENSORS = node_num - target_node_num

set sink_id 0

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
! tracker setGrid 100.0 0.0 100.0 0.0 100.0 100.0

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
	mkdir drcl.inet.sensorsim.LEACH.SinkAppLEACH app
	! app setNid $i
	! app setSinkNid $sink_id
	! app setCoherentThreshold 1000.0

	# connect the sensor application to the wireless agent
	# so that sinks can send through the wireless network protocol stack
	# create wireless agent layers
	mkdir drcl.inet.sensorsim.LEACH.WirelessLEACHAgent wireless_agent	
	
	# connect the sensor application to the wireless agent
	# so that sensors can send through the wireless network protocol stack
	connect app/down@ -to wireless_agent/up@	
	
	# connect the wireless agent to the sensor application
	# so that sensors can receive thru the wireless network protocol stack
	connect wireless_agent/.toSensorApp@ -to app/.fromWirelessAgent@

	mkdir drcl.inet.mac.LL ll
	mkdir drcl.inet.mac.ARP arp
	mkdir drcl.inet.core.queue.FIFO queue
	mkdir drcl.inet.mac.CSMA.Mac_CSMA mac
	mkdir drcl.inet.mac.WirelessPhy wphy
	
	#*************
	#NICHOLAS
	! wphy setLEACHMode 1
	connect wphy/.channelCheck@ -and mac/.wphyRadioMode@
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
	#Nicholas --> Here this contract is
	#     needed so we can send directly to neighbors.

	connect app/.setRoute@ -to rt/.service_rt@
	#**************************

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
	! mac setNode_num_  $nid       ;#set the MAc address
	
	#let it know we are running LEACH for SS 
	! mac setLEACHmode 1
	! mac setMacAddress $nid  ;#set MAC
	! wphy setNid       $nid
	! mobility setNid   $nid
	! id setDefaultID   $nid

	! queue setMode      "packet"
	! queue setCapacity  40

	# disable ARP 
	! arp setBypassARP  [ expr 2>1]
	
	connect mobility/.report@ -and /LEACH/tracker/.node@
	connect wphy/down@ -to /LEACH/channel/.node@

	! /LEACH/channel attachPort $i [! wphy getPort .channel]
	
	#                              	maxX maxY  maxZ minX  minY minZ  dX  dY   dZ
    	! mobility setTopologyParameters 100.0 100.0 0.0 100.0 100.0 0.0 100.0 100.0 0.0

	connect -c  wireless_agent/down@ -and pktdispatcher/1111@up
	
	cd ..
}

#*********************************************************************
# FOR THE SENSORS ONLY , do the following

for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {

	puts "create sensor $i"
	set node($i) [mkdir drcl.comp.Component n$i]
	
	cd n$i

	mkdir drcl.inet.sensorsim.LEACH.LEACHApp app
	! app setNid $i
	! app setSinkNid $sink_id
	! app setCoherentThreshold 1000.0

	#******
	#Nicholas
	! app setNn_ [expr $node_num - 1]
	! app setNum_clusters 3
	! app setTotal_rounds 5


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
	! /LEACH/chan attachPort $i [! phy getPort .channel]

	# connect the nodes to the propagation model
	connect phy/.propagation@ -and /LEACH/seismic_Prop/.query@

	! mobility setNid $i

	# create wireless agent layers
	mkdir drcl.inet.sensorsim.LEACH.WirelessLEACHAgent wireless_agent

	# connect the sensor application to the wireless agent
	# so that sensors can send through the wireless network protocol stack
	connect app/down@ -to wireless_agent/up@	

	# connect the wireless agent to the sensor application
	# so that sensors can receive thru the wireless network protocol stack
	connect wireless_agent/.toSensorApp@ -to app/.fromWirelessAgent@

	mkdir drcl.inet.mac.LL ll
	mkdir drcl.inet.mac.ARP arp
	mkdir drcl.inet.core.queue.FIFO queue
	mkdir drcl.inet.mac.CSMA.Mac_CSMA mac
	mkdir drcl.inet.mac.WirelessPhy wphy

	#***************
	#NICHOLAS
	! mac setLEACHmode 1	;#let it know we are running LEACH for SS 
	! wphy setLEACHMode 1	;#let it know we are running LEACH for SS 
	! wphy setMIT_uAMPS 1	;#turn on MH mode settings
	connect wphy/.channelCheck@ -and mac/.wphyRadioMode@

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

	#*****************************
	#NICHOLAS
	connect app/.energy@ -and wphy/.appEnergy@
	mkdir drcl.inet.sensorsim.CPUAvr cpu

	connect app/.cpu@ -and cpu/.reportCPUMode@
	connect cpu/.battery@ -and wphy/.cpuEnergyPort@
	#******************************

	#******************************
	#NICHOLAS
	connect mac/.sensorApp@ -and app/.macSensor@	
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
	! mac setNode_num_  $nid       	;#set the MAc address
	! mac setMacAddress $nid  	;#same as above
	! wphy setNid       $nid
	! id setDefaultID   $nid

	! queue setMode      "packet"
	! queue setCapacity  40

	# disable ARP 
	! arp setBypassARP  [ expr 2>1]

	
	connect mobility/.report@ -and /LEACH/tracker/.node@			   
	connect wphy/down@ -to /LEACH/channel/.node@

	! /LEACH/channel attachPort $i [! wphy getPort .channel]
	
	#                                maxX maxY maxZ minX minY minZ dX dY dZ
	! mobility setTopologyParameters 100.0 100.0 0.0 100.0 100.0 0.0 100.0 100.0 0.0


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
		connect phy/down@ -to /LEACH/chan/.node@

		# connect the nodes to the propagation model
		connect phy/.propagation@ -and /LEACH/seismic_Prop/.query@

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
	connect n$i/mobility/.report_sensor@ -and /LEACH/nodetracker/.node@
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
#red line-> actual packets received
#blue line-> what the sink actually would have received if it 
#	     wasn't for CH aggregation
set sinkPlot1_ [mkdir drcl.comp.tool.Plotter .sinkPlot1]
connect -c n0/app/.PacketsReceivedPlot@ -to $sinkPlot1_/0@0
connect -c n0/app/.theo_PacketsReceivedPlot@ -to $sinkPlot1_/1@0

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

#Graph # 4 --Instead this graph is combined into Graph#1 to 
#see exactly how fewer the number of incomming packets are
#at the base station.
#set sinkPlot4_ [mkdir drcl.comp.tool.Plotter .sinkPlot4]
#connect -c n0/app/.theo_PacketsReceivedPlot@ -to $sinkPlot4_/0@0

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
	   set mac_dropped [! n1/mac getDropped_packets]

	   puts "Total packets dropped at Application layer: $app_dropped"
	   puts "Total packets dropped at physical layer: $wphy_dropped"	
	   puts "Drops due to collisions (discovered at MAC layer: $mac_dropped"	
	   set dropped_packets [expr $app_dropped + $wphy_dropped + $mac_dropped] 

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
#Strictly used for validation purposes. This method
#can be called to get the energy at any random
#point of a particular node.
proc getCurrEnergy { } {
   
   puts "\n\n"
   global sink_id node_num count target_node_num

   puts "last update time was at: [! n1/wphy getLastUpdateTime] seconds"

   for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {	
   	puts "------------------------------------------------"	
 	puts "Sensor$i has [! n$i/wphy getRemEnergy] joules remaining\n"
        
	puts "Sensor$i idle radio used: [! n$i/wphy getRadioTotalidle] joules"
	puts "Sensor$i Radio was in idle mode for: [! n$i/wphy getTotalRadioIdleTime] sec\n"
		
	puts "Sensor$i CPU active used: [! n$i/wphy getTotalCPUactive] joules"
	puts "Sensor$i CPU was active for: [! n$i/wphy getTotalCPUactiveTime] sec\n"
		
	puts "Sensor$i CPU sleep used: [! n$i/wphy getTotalCPUsleep] joules"
	puts "Sensor$i CPU was sleep for: [! n$i/wphy getTotalCPUsleepTime] sec\n"

	puts "Sensor$i CPU Total used: [! n$i/wphy getTotalCPU] joules \n"

   }
}
					     

#***********************************************
puts "Simulation begins...\n"
set sim [attach_simulator .]
$sim stop


#******************start the sink************************
script {run n0} -at 0.001 -on $sim

#***************start the sensors************************
for {set i [expr $sink_id + 1]} {$i < $node_num} {incr i} {
	script puts "run n$i" -at 0.1 -on $sim
}

#*********print out all the node locations**************
script "sensorLocPrintOut" -at 0.002 -on $sim

#*******Check if Sensor Status**************************
script "wsnLoop" -at 1.0 -period 2.0 -on $sim


#************For Matlab plotting************************
#script "file_output" -at 200.0 -period 100.0 -on $sim

#************One-time Capture of where energy went******
#script "energy_dist" -at 500.0 -period 200.0 -on $sim

#the following line is only used in validation!
#script "getCurrEnergy" -at 4.99 -on $sim

$sim resumeTo 100000.0