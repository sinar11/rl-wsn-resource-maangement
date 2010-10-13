
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


/** This class provides the Intrusion detection application for DReL
*
* @author Kunal Shah
*/
public class DRLIntrusionDetectionApp extends DRLDiffApp {
 	/**
	 * 
	 */
	private static final long serialVersionUID = 4554418086492147581L;

	private static final double HEART_BEAT_INTERVAL = MicroLearner.TIMER_INTERVAL;

	private SensorType sensorType=SensorType.Motion;
	
	enum SensorType{
		Motion(MicroLearner.ENERGY_SAMPLE,0.25),
		Video(MicroLearner.ENERGY_SAMPLE*100,0.9);
		
		private SensorType(double energy, double confidence){
		   this.energy=energy;	
		   this.confidence=confidence;
		}
		double energy;	
		double confidence;
	}
	
	public DRLIntrusionDetectionApp (){
		super(); 
	}
	
	public void setSensorType(String type){
		this.sensorType= SensorType.valueOf(type);
	}
	
	public double getSamplingEnergy() {
		if(sensorType!=null) return sensorType.energy; 
		else return super.getSamplingEnergy();
	}
	
    protected void _start() {
    	super._start();
    	if(nid!=sink_nid)
    		setTimeout(new DiffTimer(DiffTimer.TIMEOUT_SEND_HEARTBEAT, null), HEART_BEAT_INTERVAL);
    }
			
	/** Constructs a sensing event */
	public List<Tuple> ConstructSensingEvent(SensorAppAgentContract.Message msg)
	{
		double locX, locY;
		long targetNid;
		String groupId=null;
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
			targetNid=-1;			
			log(Level.FINE,"Sending heart-beat");
		}else{
			locX = msg.getTargetX() ;
		    locY = msg.getTargetY() ;
		    targetNid=msg.getTargetNid();
		    groupId=Long.toString(targetNid);
		    log(Level.FINE,"Sending target position, SensorType:"+this.sensorType);
		}
		List<Tuple> event = new ArrayList<Tuple>() ;
		event.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.IS, locX)) ;
		event.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.IS, locY)) ;
		event.add(new Tuple(Tuple.TARGET_KEY, Type.STRING_TYPE, Operator.IS, TargetName)) ;
		event.add(new Tuple(Tuple.TARGET_NID, Type.INT32_TYPE, Operator.IS, targetNid)) ;
		event.add(new Tuple(Tuple.CONFIDENCE, Type.FLOAT32_TYPE, Operator.IS, this.sensorType.confidence)) ;
		if(groupId==null){
			groupId=NodePositionGroupClassifier.getInstance().getNodeGroupIdentifier(nid, new double[]{locX,locY});
		}
		log(Level.FINE,"My groupId:"+groupId);
		event.add(new Tuple(Tuple.GROUP_ID, Type.STRING_TYPE,Operator.IS,groupId));
		return event ;
	}
	
	/** Initiates a sensing task by sending an INTEREST packet */
	public void subscribe(int taskId, double longMin, double longMax, double latMin, double latMax, double duration, double confidence, double dataInterval, double refreshPeriod, double payment, String costParam)
	{
		/* constructs an interest */
		List<Tuple> interest = new ArrayList<Tuple>() ;
		interest.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.GE, new Double(longMin) ) ) ;
		interest.add(new Tuple(Tuple.LONGITUDE_KEY, Type.FLOAT32_TYPE, Operator.LE,new Double(longMax) ) ) ;
		interest.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.GE, new Double(latMin) ) ) ;
		interest.add(new Tuple(Tuple.LATITUDE_KEY, Type.FLOAT32_TYPE, Operator.LE, new Double(latMax) ) ) ;
		interest.add(new Tuple(Tuple.TARGET_KEY, Type.STRING_TYPE, Operator.IS, TargetName) ) ;
		interest.add(new Tuple(Tuple.TARGET_NID, Type.INT32_TYPE, Operator.EQ_ANY, TargetName) ) ;
		interest.add(new Tuple(Tuple.CONFIDENCE, Type.FLOAT32_TYPE, Operator.GE, new Double(confidence)));
		
		List<CostParam> costParams= new ArrayList<CostParam>();
		if(costParam!=null){
			this.costParam=costParam;
			costParams.add(new CostParam(CostParam.Type.valueOf(costParam),1));
		}else{
		    //costParams.add(new CostParam(CostParam.Type.Energy,0.5));
		     costParams.add(new CostParam(CostParam.Type.Lifetime,1.0));
		}
		List<Tuple> qosConstraints= new ArrayList<Tuple>();
		qosConstraints.add(new Tuple(Tuple.CONFIDENCE, Type.FLOAT32_TYPE, Operator.GE, new Double(confidence)));
		super.subscribe(taskId, interest, costParams, qosConstraints, duration, dataInterval, dataInterval, refreshPeriod, payment);		
	}

	/** Handles a timeout event */
	protected void timeout(Object data_){
		if ( data_ instanceof DiffTimer )
		{
			DiffTimer d = (DiffTimer)data_ ;
			int type = d.EVT_Type ;
			switch ( type )
			{
				case DiffTimer.TIMEOUT_SEND_HEARTBEAT :
					if(getTime()-microLearner.lastSourceParticipation>HEART_BEAT_INTERVAL)
						microLearner.handleSensorEvent(ConstructSensingEvent(null));
					setTimeout(d, HEART_BEAT_INTERVAL);
					break ;
			}
		}
		super.timeout(data_);
	}	
	
	protected void handleSinkData(DataPacket dataPkt) {
		List<Tuple> attributes = dataPkt.getAttributes();
		TaskEntry taskEntry = activeTasksList.get(dataPkt.getTaskId());
		long targetNid = (Long) TupleUtils.getAttributeValue(attributes,
				Tuple.TARGET_NID);
		/*
		 * double confidence=(Double) TupleUtils.getAttributeValue( attributes,
		 * Tuple.CONFIDENCE);
		 */
		double[] target_location = new double[2];
		
		if (targetNid >= 0) { // if target is set, see if we just changed state
								// and should send out new interest with higher
								// confidence requirement and higher payment
			double confidence = (Double) TupleUtils.getAttributeValue(taskEntry
					.getInterest().getAttributes(), Tuple.CONFIDENCE);
			target_location[0] = (Double) TupleUtils.getAttributeValue(
					attributes, Tuple.LONGITUDE_KEY);
			target_location[1] = (Double) TupleUtils.getAttributeValue(
					attributes, Tuple.LATITUDE_KEY);
			if (confidence < 0.75) {
				TupleUtils.updateAttribute(taskEntry.getInterest()
						.getAttributes(), Tuple.LONGITUDE_KEY, Operator.GE, target_location[0]-25);
				TupleUtils.updateAttribute(taskEntry.getInterest()
						.getAttributes(), Tuple.LONGITUDE_KEY, Operator.LE, target_location[0]+25);
				TupleUtils.updateAttribute(taskEntry.getInterest()
						.getAttributes(), Tuple.LATITUDE_KEY, Operator.GE, target_location[1]-25);
				TupleUtils.updateAttribute(taskEntry.getInterest()
						.getAttributes(), Tuple.LATITUDE_KEY, Operator.LE, target_location[1]+25);
				TupleUtils.updateAttribute(taskEntry.getInterest()
						.getAttributes(), Tuple.CONFIDENCE, Operator.GE, 0.8);
				
				double newPayment=taskEntry.getPayment()*2;
				taskEntry.setPayment(newPayment);
				taskEntry.interest.setPayment(newPayment);
				collectStats();
				log(Level.WARNING,
						"State changed.. sending new confidence value..");
				refreshInterest(taskEntry);
			}
		}
		EnergyStats.markAsReporting();
		totalEnergyUsed = EnergyStats.getTotalEnergy();
		CSVLogger.log("Delay", "" + averageDelay, false, microLearner.algorithm
				.getAlgorithm());
		// CSVLogger.log("Delay", ""+averageDelay
		// ,false,algorithm.getAlgorithm());
		if (targetNid < 0){
			log(Level.INFO, "Heart-beat with pkt:" + dataPkt);
			totalHeartBeatPkts++;
		}else {
			log(Level.INFO, "Tracking event with pkt:" + dataPkt);
			lastTrackTime = getTime();
			averageDelay = (averageDelay * totalTrackingPkts + (lastTrackTime - dataPkt
					.getTimestamp()))
					/ (totalTrackingPkts + 1);
			totalTrackingPkts++;
			
			double[] curr = CurrentTargetPositionTracker.getInstance()
					.getTargetPosition(targetNid);
			log(Level.INFO, "Tracked position:"
					+ doubleArrToString(target_location) + "  Actual position:"
					+ doubleArrToString(curr));
			double dist = Math.sqrt(Math.pow(Math.abs(target_location[0]
					- curr[0]), 2)
					+ Math.pow(Math.abs(target_location[1] - curr[1]), 2));
			CSVLogger.log("target" + targetNid, target_location[0] + ","
					+ target_location[1] + "," + curr[0] + "," + curr[1] + ","
					+ dist, false, microLearner.algorithm.getAlgorithm());
		}
	}

}
