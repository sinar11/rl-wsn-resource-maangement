start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=COIN drcl.ruv.System %1 -d -e &
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=DIRL drcl.ruv.System %1 -d -e &
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=DIRLWoLF drcl.ruv.System %1 -d -e &
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=TEAM drcl.ruv.System %1 -d -e &
start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=SORA drcl.ruv.System %1 -d -e &
start /wait /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dalgorithm=RANDOM drcl.ruv.System %1 -d -e &
exit