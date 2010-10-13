
package drcl.inet.sensorsim.drl.diffext ;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import drcl.inet.sensorsim.CurrentTargetPositionTracker;
import drcl.inet.sensorsim.SensorAppAgentContract;
import drcl.inet.sensorsim.SensorPositionReportContract;
import drcl.inet.sensorsim.drl.CSVLogger;
import drcl.inet.sensorsim.drl.EnergyStats;
import drcl.inet.sensorsim.drl.diffext.InterestPacket.CostParam;
import drcl.inet.sensorsim.drl.diffext.Tuple.Operator;
import drcl.inet.sensorsim.drl.diffext.Tuple.Type;


/** This class implements the extension of directed diffusion using distributed RL
*
* @author Kunal Shah
*/
public class DRLTrackingApp extends DRLDiffApp
{
	private static final long serialVersionUID = -4429342392878956966L;

	public DRLTrackingApp(){
		super(); 
	}
	
	/** Constructs a sensing event */
	public List<Tuple> ConstructSensingEvent(SensorAppAgentContract.Message msg)
	{
		double locX, locY, snr;
		long targetNid;
		if (msg == null) {
			/*
			 * sensorLocX and sensorLocY are the X and Y coordinates of the
			 * sensor node and must be obtained from the mobility model.
			 */
			SensorPositionReportContract.Message positionMsg = new SensorPositionReportContract.Message();
			positionMsg = (SensorPositionReportContract.Message) mobilityPort
					.sendReceive(positionMsg);
			locX = positionMsg.getX();
			locY = positionMsg.getY();
			targetNid=this.nid;
			snr=500;
		}else{
			locX = msg.getTargetX() ;
		    locY = msg.getTargetY() ;
		    targetNid=msg.getTargetNid();
		    snr=msg.getSNR();
		}
		List<Tuple> event = new ArrayList<Tuple>() ;
		event.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.IS, locX)) ;
		event.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.IS, locY)) ;
		event.add(new Tuple(Tuple.TARGET_KEY, Type.STRING_TYPE, Operator.IS, TargetName)) ;
		event.add(new Tuple(Tuple.TARGET_NID, Type.INT32_TYPE, Operator.IS, targetNid)) ;
		event.add(new Tuple(Tuple.SNR, Type.FLOAT32_TYPE, Operator.IS, snr)) ;
		return event ;
	}
	
	/** Initiates a sensing task by sending an INTEREST packet */
	public void subscribe(int taskId, double longMin, double longMax, double latMin, double latMax, double duration, double interval, double dataInterval, double refreshPeriod, double payment, String costParam)
	{
		/* constructs an interest */
		List<Tuple> interest = new ArrayList<Tuple>() ;
		interest.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.GE, new Double(longMin) ) ) ;
		interest.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.LE,new Double(longMax) ) ) ;
		interest.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.GE, new Double(latMin) ) ) ;
		interest.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.LE, new Double(latMax) ) ) ;
		interest.add(new Tuple(Tuple.TARGET_KEY, Type.STRING_TYPE, Operator.IS, TargetName) ) ;
		interest.add(new Tuple(Tuple.TARGET_NID, Type.INT32_TYPE, Operator.EQ_ANY, TargetName) ) ;
		List<CostParam> costParams= new ArrayList<CostParam>();
		if(costParam!=null){
			this.costParam=costParam;
			costParams.add(new CostParam(CostParam.Type.valueOf(costParam),1));
		}else{
		    //costParams.add(new CostParam(CostParam.Type.Energy,0.5));
		     costParams.add(new CostParam(CostParam.Type.Lifetime,1.0));
		}
		List<Tuple> qosConstraints= new ArrayList<Tuple>();
		qosConstraints.add(new Tuple(Tuple.SNR,Type.FLOAT32_TYPE,Operator.GE, new Double(50)));
		super.subscribe(taskId, interest, costParams, qosConstraints, duration, interval, dataInterval, refreshPeriod, payment);		
	}
	
	protected void handleSinkData(DataPacket dataPkt) {
		List<Tuple> attributes= dataPkt.getAttributes();
		TaskEntry taskEntry= activeTasksList.get(dataPkt.getTaskId());
		if ((getTime() - lastTrackTime+0.1) >= taskEntry.getInterest().getDataInterval()) {
			lastTrackTime = getTime();
			averageDelay=(averageDelay*totalTrackingPkts+(lastTrackTime-dataPkt.getTimestamp()))/(totalTrackingPkts+1);
			totalTrackingPkts++;
			EnergyStats.markAsReporting();
			totalEnergyUsed=EnergyStats.getTotalEnergy();
			CSVLogger.log("Delay", ""+averageDelay ,false,microLearner.algorithm.getAlgorithm());
			//CSVLogger.log("Delay", ""+averageDelay ,false,algorithm.getAlgorithm());
			log(Level.INFO, "Tracking event with pkt:" + dataPkt);
			double[] target_location = new double[2];
			target_location[0] = (Double) TupleUtils.getAttributeValue(
					attributes, Tuple.LONGITUDE_KEY);
			target_location[1] = (Double) TupleUtils.getAttributeValue(
					attributes, Tuple.LATITUDE_KEY);
			long targetNid = (Long) TupleUtils.getAttributeValue(attributes,
					Tuple.TARGET_NID);
			double[] curr = CurrentTargetPositionTracker.getInstance()
					.getTargetPosition(targetNid);
			log(Level.INFO, "Tracked position:"
					+ doubleArrToString(target_location) + "  Actual position:"
					+ doubleArrToString(curr));
			double dist = Math.sqrt(Math.pow(Math.abs(target_location[0]- curr[0]), 2)
	                  + Math.pow(Math.abs(target_location[1] - curr[1]), 2));
			CSVLogger.log("target"+targetNid,target_location[0] + "," + target_location[1]
						+ "," + curr[0] + "," + curr[1] + "," + dist,false,microLearner.algorithm.getAlgorithm());	
		}		
	}
	
	
}
