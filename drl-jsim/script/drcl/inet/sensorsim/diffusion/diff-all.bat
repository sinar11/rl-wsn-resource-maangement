cd C:\research\workspace\jsim
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor5_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor10_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor15_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor20_network_diffusion.tcl -d -e &
start /max java -Dtarget.mobile=false -classpath classes;jars\jython.jar;jars\tcl.zip drcl.ruv.System script/drcl/inet/sensorsim/diffusion/sensor25_network_diffusion.tcl -d -e &