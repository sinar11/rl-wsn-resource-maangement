cd C:\research\workspace\jsim
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor20_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=DIRL drcl.ruv.System script/drcl/inet/sensorsim/diffusion/drl20_sensor_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=true -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor20_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=true -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=DIRL drcl.ruv.System script/drcl/inet/sensorsim/diffusion/drl20_sensor_network_diffusion.tcl -d -e &