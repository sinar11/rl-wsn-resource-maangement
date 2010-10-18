cd C:\research\workspace\jsim
start /max java -Dtarget.mobile=true -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor10_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=true -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=COIN drcl.ruv.System script/drcl/inet/sensorsim/diffusion/drl10_sensor_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor10_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=COIN drcl.ruv.System script/drcl/inet/sensorsim/diffusion/drl10_sensor_network_diffusion.tcl -d -e &