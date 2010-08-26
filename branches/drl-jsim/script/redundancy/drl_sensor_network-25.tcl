# Sample tcl file of a wireless sensor network simulation.
# Author: Ahmed Sobeih
# Date: 12/23/2003

source "script/test/include.tcl"

cd [mkdir -q drcl.comp.Component /aodvtest]

# TOTAL number of nodes (sensor nodes + target nodes)
set node_num 25

# Number of TARGET nodes ONLY
set target_node_num 1
# Hence, number of SENSORS = node_num - target_node_num
set sink_id 0

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
! nodetracker setGrid 450.0 100.0 400.0 100.0

# connect the sensor channel to the sensor node position tracker
connect chan/.tracker@ -and nodetracker/.channel@

# create the wireless channel
mkdir drcl.inet.mac.Channel channel

# Capacity of the wireless channel is number of sensors and sinks ONLY
# which is equal to $node_num - $target_node_num
! channel setCapacity [expr $node_num - $target_node_num]

# create the node position tracker
mkdir drcl.inet.mac.NodePositionTracker tracker
#                 maxX  minX maxY minY  dX   dY
! tracker setGrid 450.0 100.0 400.0 100.0 60.0 60.0

connect channel/.tracker@ -and tracker/.channel@

# FOR THE SINKs ONLY, do the following
# SINKs have only a network protocol stack
for {set i 0} {$i < [expr $sink_id + 1]} {incr i} {
	puts "create sink $i"
	set node$i [mkdir drcl.comp.Component n$i]
	
	cd n$i

	mkdir drcl.inet.sensorsim.drl.DRLSensorApp app
	! app setNid $i
	! app setSinkNid $sink_id
	! app setCoherentThreshold 1000.0

	# create wireless agent layers
	mkdir drcl.inet.sensorsim.WirelessAgent wireless_agent

	# connect the sensor application to the wireless agent
	# so that sinks can send through the wireless network protocol stack
	connect app/down@ -to wireless_agent/up@	

	# connect the wireless agent to the sensor application
	# so that sinks can receive thru the wireless network protocol stack
	connect wireless_agent/.toSensorApp@ -to app/.fromWirelessAgent@

	mkdir drcl.inet.mac.LL ll

	mkdir drcl.inet.mac.ARP arp

	mkdir drcl.inet.core.queue.FIFO queue

	mkdir drcl.inet.mac.Mac_802_11 mac

	mkdir drcl.inet.mac.WirelessPhy wphy
	! wphy setRxThresh 0.0
	! wphy setCSThresh 0.0	

	mkdir drcl.inet.mac.FreeSpaceModel propagation 

	mkdir drcl.inet.mac.MobilityModel mobility
	     
        set PD [mkdir drcl.inet.core.PktDispatcher      pktdispatcher]
        set RT [mkdir drcl.inet.core.RT                 rt]
        set ID [mkdir drcl.inet.core.Identity           id]
 
        $PD bind $RT
        $PD bind $ID	

	mkdir drcl.inet.protocol.aodv.AODV  aodv
	connect -c aodv/down@ -and pktdispatcher/103@up
	connect aodv/.service_rt@ -and rt/.service_rt@
	connect aodv/.service_id@ -and id/.service_id@
	connect aodv/.ucastquery@ -and pktdispatcher/.ucastquery@
	connect mac/.linkbroken@ -and aodv/.linkbroken@

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
	! wphy setNid        $nid
	! mobility setNid   $nid
	! id setDefaultID   $nid

	! queue setMode      "packet"
	! queue setCapacity  40

	# disable ARP 
	! arp setBypassARP  [ expr 2>1]
	
	! mac setRTSThreshold 0
	
	connect mobility/.report@ -and /aodvtest/tracker/.node@

	connect wphy/down@ -to /aodvtest/channel/.node@

	! /aodvtest/channel attachPort $i [! wphy getPort .channel]
	
#                                maxX maxY maxZ minX minY minZ dX dY dZ
    	! mobility setTopologyParameters 600.0 500.0 0.0 100.0 100.0 0.0 60.0 60.0 0.0

	! mac  disable_MAC_TRACE_ALL

	connect -c  wireless_agent/down@ -and pktdispatcher/1111@up
	
	cd ..
}

# FOR THE SENSORS ONLY , do the following
# create sensor application, battery, CPU, Radio models, etc.
for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {
	puts "create sensor $i"
	set node$i [mkdir drcl.comp.Component n$i]
	
	cd n$i

	mkdir drcl.inet.sensorsim.drl.DRLSensorApp app
	! app setNid $i
	! app setSinkNid $sink_id
	! app setCoherentThreshold 1000.0

	# create nodes
	mkdir drcl.inet.sensorsim.SensorAgent agent

	# create sensor physical layers
	mkdir drcl.inet.sensorsim.SensorPhy phy 
	! phy setRxThresh 0.0

	# create mobility models
	mkdir drcl.inet.sensorsim.SensorMobilityModel mobility

	! phy setNid $i 
	! phy setRadius 500.0

	# connect phyiscal layers to sensor agents so that nodes can receive
	connect phy/.toAgent@ -to agent/.fromPhy@
	
	# connect sensor agent and sensor application
	connect agent/.toSensorApp@ -to app/.fromSensorAgent@

	# connect the sensor channel to the nodes so that they can receive
	! /aodvtest/chan attachPort $i [! phy getPort .channel]

	# connect the nodes to the propagation model
	connect phy/.propagation@ -and /aodvtest/seismic_Prop/.query@

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

	mkdir drcl.inet.mac.WirelessPhy wphy
	! wphy setRxThresh 0.0
	! wphy setCSThresh 0.0	

	mkdir drcl.inet.mac.FreeSpaceModel propagation 

        set PD [mkdir drcl.inet.core.PktDispatcher      pktdispatcher]
        set RT [mkdir drcl.inet.core.RT                 rt]
        set ID [mkdir drcl.inet.core.Identity           id]
 
        $PD bind $RT
        $PD bind $ID	

	mkdir drcl.inet.protocol.aodv.AODV  aodv
	connect -c aodv/down@ -and pktdispatcher/103@up
	connect aodv/.service_rt@ -and rt/.service_rt@
	connect aodv/.service_id@ -and id/.service_id@
	connect aodv/.ucastquery@ -and pktdispatcher/.ucastquery@
	connect mac/.linkbroken@ -and aodv/.linkbroken@

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
	
	#*************
	#NICHOLAS
	#added to let the MAC802.11 layer
	#it is in charge of maintaining the
	#radio modes.
	! mac setIs_uAMPS 0
	! wphy setMIT_uAMPS 0
	#*************

	#*************
	#Nicholas: 
	#The energy module is contained within the 
	#wirelessPhy.java component. The following connects
	#the CPU model, application layer, and wirelessPhy
	#components together.
	connect app/.energy@ -and wphy/.appEnergy@
	mkdir drcl.inet.sensorsim.CPUAvr cpu

	connect app/.cpu@ -and cpu/.reportCPUMode@
	connect cpu/.battery@ -and wphy/.cpuEnergyPort@

	# End Energy Model
	#**************
		 
	set nid $i
	
	! arp setAddresses  $nid $nid
	! ll  setAddresses  $nid $nid
	! mac setMacAddress $nid
	! wphy setNid        $nid
	! id setDefaultID   $nid

	! queue setMode      "packet"
	! queue setCapacity  40

	# disable ARP 
	! arp setBypassARP  [ expr 2>1]
	
	! mac setRTSThreshold 0
	
	connect mobility/.report@ -and /aodvtest/tracker/.node@

	connect wphy/down@ -to /aodvtest/channel/.node@

	! /aodvtest/channel attachPort $i [! wphy getPort .channel]
	
#                                    maxX maxY maxZ minX minY minZ dX dY dZ
    ! mobility setTopologyParameters 600.0 500.0 0.0 100.0 100.0 0.0 60.0 60.0 0.0

	! mac  disable_MAC_TRACE_ALL

	connect -c  wireless_agent/down@ -and pktdispatcher/1111@up
	
	cd ..
}

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
		! agent setBcastRate 20.0
		! agent setSampleRate 1.0
	
		# create sensor physical layers
		mkdir drcl.inet.sensorsim.SensorPhy phy 
		! phy setRxThresh 0.0
		! phy setNid $i 
		! phy setRadius 80.0

		# create mobility models
		mkdir drcl.inet.sensorsim.SensorMobilityModel mobility

		# connect target agents to phy layers so that nodes can send
		connect agent/down@ -to phy/up@	
	
		# connect phy layers to sensor channel so that nodes can send
		connect phy/down@ -to /aodvtest/chan/.node@

		# connect the nodes to the propagation model
		connect phy/.propagation@ -and /aodvtest/seismic_Prop/.query@

		! mobility setNid $i

		# set the topology parameters
		! mobility setTopologyParameters 600.0 500.0 0.0 100.0 100.0 0.0

		cd ..
	}
}

# for SENSORs and TARGETs only. Not SINKs
for {set i [expr $sink_id + 1]} {$i < $node_num} {incr i} {
	# connect the mobility model of each node to the node position tracker
	connect n$i/mobility/.report_sensor@ -and /aodvtest/nodetracker/.node@

	connect n$i/phy/.mobility@ -and n$i/mobility/.query@
}

! n$sink_id/app createSnrPorts $node_num $target_node_num
set plot_ [mkdir drcl.comp.tool.Plotter .plot]
#for {set i 0} {$i < $target_node_num} {incr i} {
for {set i [expr $sink_id + 1]} {$i < [expr $node_num - $target_node_num]} {incr i} {
	foreach task "Route Sample Sleep" {
		#connect -c n$i/app/.Qval$task@ -to $plot_/$task@$i
		connect -c n$i/app/$task@ -to $plot_/$task@$i
	}
	#connect -c n$i/app/.energy@ -to $plot_/6@$i
	if { $testflag } {
		attach -c $testfile/in@ -to n$sink_id/app/.snr$i@
	}
}
set position_ [mkdir drcl.comp.tool.Plotter .position]
connect -c n$sink_id/app/.track@ -to $position_/track@
connect -c n$sink_id/app/.actual@ -to $position_/actual@
#for {set i 0} {$i < $target_node_num} {incr i} {
	#connect -c n$sink_id/app/.snr$i@ -to $position_/$i@$i
#	if { $testflag } {
#		attach -c $testfile/in@ -to n$sink_id/app/.snr$i@
#	}
#}
# set the position of sink nodes
! n0/mobility setPosition 0.0 250.0 100.0 0.0

# set the first argument of setPosition to 30.0 
# set the position of sensor nodes
# should be made to read from a scenario file
! n1/mobility setPosition 0.0 200.0 125.0 0.0
! n1/app setDestId 0
! n2/mobility setPosition 0.0 220.0 125.0 0.0
! n2/app setDestId 0
! n3/mobility setPosition 0.0 240.0 125.0 0.0
! n3/app setDestId 0
! n4/mobility setPosition 0.0 265.0 125.0 0.0
! n4/app setDestId 0
! n5/mobility setPosition 0.0 280.0 125.0 0.0
! n5/app setDestId 0
! n6/mobility setPosition 0.0 300.0 125.0 0.0
! n6/app setDestId 0
! n7/mobility setPosition 0.0 200.0 160.0 0.0
! n7/app setDestId 1
! n8/mobility setPosition 0.0 210.0 140.0 0.0
! n8/app setDestId 1
! n9/mobility setPosition 0.0 220.0 160.0 0.0
! n9/app setDestId 2
! n10/mobility setPosition 0.0 230.0 140.0 0.0
! n10/app setDestId 2
! n11/mobility setPosition 0.0 240.0 160.0 0.0
! n11/app setDestId 3
! n12/mobility setPosition 0.0 250.0 140.0 0.0
! n12/app setDestId 3
! n13/mobility setPosition 0.0 260.0 160.0 0.0
! n13/app setDestId 4
! n14/mobility setPosition 0.0 270.0 140.0 0.0
! n14/app setDestId 4
! n15/mobility setPosition 0.0 280.0 160.0 0.0
! n15/app setDestId 5
! n16/mobility setPosition 0.0 290.0 140.0 0.0
! n16/app setDestId 5
! n17/mobility setPosition 0.0 300.0 160.0 0.0
! n17/app setDestId 6
! n18/mobility setPosition 0.0 300.0 140.0 0.0
! n18/app setDestId 6
! n19/mobility setPosition 0.0 200.0 175.0 0.0
! n19/app setDestId 7
! n20/mobility setPosition 0.0 220.0 175.0 0.0
! n20/app setDestId 9
! n21/mobility setPosition 0.0 240.0 175.0 0.0
! n21/app setDestId 11
! n22/mobility setPosition 0.0 260.0 175.0 0.0
! n22/app setDestId 13
! n23/mobility setPosition 0.0 375.0 175.0 0.0
! n23/app setDestId 15

puts "simulation begins..."
set sim [attach_simulator .]
$sim stop

# need to start different nodes at different time
# in order to avoid route request collision
script {run n0} -at 0.3 -on $sim
script {run n1} -at 0.5 -on $sim
script {run n2} -at 0.5 -on $sim
script {run n3} -at 0.8 -on $sim
script {run n4} -at 0.8 -on $sim
script {run n5} -at 0.8 -on $sim
script {run n6} -at 1.0 -on $sim
script {run n7} -at 1.2 -on $sim
script {run n8} -at 1.5 -on $sim
script {run n9} -at 1.8 -on $sim
script {run n10} -at 2.1 -on $sim
script {run n11} -at 2.5 -on $sim
script {run n12} -at 2.8 -on $sim
script {run n13} -at 3.0 -on $sim
script {run n14} -at 3.2 -on $sim
script {run n15} -at 3.5 -on $sim
script {run n16} -at 3.8 -on $sim
script {run n17} -at 4.0 -on $sim
script {run n18} -at 4.2 -on $sim
script {run n19} -at 4.5 -on $sim
script {run n20} -at 5.0 -on $sim
script {run n21} -at 5.5 -on $sim
script {run n22} -at 6.0 -on $sim
script {run n23} -at 6.5 -on $sim
script {run n24} -at 8.0 -on $sim

# set the position of target nodes
# Max. speed is the first argument of setPosition.
# In order to make the target nodes mobile with max. speed (e.g., 30) m/sec., 
#! n24/mobility setPosition 0.0 250.0  250.0 0.0
set np 13; # number of points
set t [java::new {double[][]} $np]
$t set 0 [java::new {double[]} 4 "0 150.0 225.0 0"]
$t set 1 [java::new {double[]} 4 "1000 175.0 150.0 0"]
$t set 2 [java::new {double[]} 4 "2000 175.0 150.0 0"]
$t set 3 [java::new {double[]} 4 "3000 200.0 240.0 0"]
$t set 4 [java::new {double[]} 4 "4000 200.0 240.0 0"]
$t set 5 [java::new {double[]} 4 "5000 225.0 185.0 0"]
$t set 6 [java::new {double[]} 4 "6000 225.0 185.0 0"]
$t set 7 [java::new {double[]} 4 "7000 250.0 250.0 0"]
$t set 8 [java::new {double[]} 4 "8000 250.0 250.0 0"]
$t set 9 [java::new {double[]} 4 "9000 275.0 160.0 0"]
$t set 10 [java::new {double[]} 4 "10000 275.0 160.0 0"]
$t set 11 [java::new {double[]} 4 "11000 300.0 250.0 0"]
$t set 12 [java::new {double[]} 4 "12000 300.0 250.0 0"]
! n24/mobility installTrajectory $t

# collect statistics at the end
set end 6000.0
script {! n0/app collectStats} -at $end -on $sim
script {! n1/app collectStats} -at $end -on $sim
script {! n2/app collectStats} -at $end -on $sim
script {! n3/app collectStats} -at $end -on $sim
script {! n4/app collectStats} -at $end -on $sim
script {! n5/app collectStats} -at $end -on $sim
script {! n6/app collectStats} -at $end -on $sim
script {! n7/app collectStats} -at $end -on $sim
script {! n8/app collectStats} -at $end -on $sim
script {! n9/app collectStats} -at $end -on $sim
script {! n10/app collectStats} -at $end -on $sim
script {! n11/app collectStats} -at $end -on $sim
script {! n12/app collectStats} -at $end -on $sim
script {! n13/app collectStats} -at $end -on $sim
script {! n14/app collectStats} -at $end -on $sim
script {! n15/app collectStats} -at $end -on $sim
script {! n16/app collectStats} -at $end -on $sim
script {! n17/app collectStats} -at $end -on $sim
script {! n18/app collectStats} -at $end -on $sim
script {! n19/app collectStats} -at $end -on $sim
script {! n20/app collectStats} -at $end -on $sim
script {! n21/app collectStats} -at $end -on $sim
script {! n22/app collectStats} -at $end -on $sim
script {! n23/app collectStats} -at $end -on $sim


$sim resumeTo $end 
