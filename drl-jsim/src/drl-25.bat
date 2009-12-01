cd C:\research\workspace\jsim
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=COIN drcl.ruv.System script/drl_sensor_network-25.tcl -d -e &
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=DIRL drcl.ruv.System script/drl_sensor_network-25.tcl -d -e &
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=TEAM drcl.ruv.System script/drl_sensor_network-25.tcl -d -e &
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=SORA drcl.ruv.System script/drl_sensor_network-25.tcl -d -e &
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=RANDOM drcl.ruv.System script/drl_sensor_network-25.tcl -d -e &
pause