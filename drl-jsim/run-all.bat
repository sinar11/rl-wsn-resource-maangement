start /max java -classpath classes;jars\jython.jar;jars\tcl.zip -Dtarget.mobile=false -Dalgorithm=COIN -Dmobility.seed=%2 drcl.ruv.System %1 Lifetime -d -e &
start /wait java -classpath classes;jars\jython.jar;jars\tcl.zip -Dtarget.mobile=true -Dalgorithm=DIRL -Dmobility.seed=%2 drcl.ruv.System %1 NoOfHops -d -e &
exit