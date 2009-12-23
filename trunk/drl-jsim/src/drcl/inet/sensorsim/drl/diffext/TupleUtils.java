package drcl.inet.sensorsim.drl.diffext;

import java.util.List;

import drcl.inet.sensorsim.drl.diffext.Tuple.Type;

public class TupleUtils {

	public static boolean oneWayMatch(List<Tuple> interest, List<Tuple> data){
		for(Tuple interestTuple: interest){
			if(interestTuple.getOperator().isFormal()){
				boolean matchFound=false;
				for(Tuple tuple:data){
					if(tuple.getKey().equals(interestTuple.getKey()) && !tuple.getOperator().isFormal()){
						if(!tuple.matches(interestTuple)) return false;
						else matchFound=true;						
					}
				}
				if(!matchFound) return false;
			}
		}
		return true;
	}

	public static boolean isMatching(List<Tuple> interest, List<Tuple> data){
		boolean matched=false;
		matched= oneWayMatch(interest, data);
		if(!matched) return false;
		else
			return oneWayMatch(data, interest);
	}
	
	public static Object getAttributeValue(List<Tuple> data, String key){
		for(Tuple attribute: data){
			if(attribute.getKey().equals(key)){
				return attribute.getValue();
			}
		}
		return null;
	}

	public static double calcQuality(List<Tuple> qosConstraints,
			List<Tuple> attributes) {
		if(isMatching(qosConstraints, attributes))
			return 1.0;
		else
			return 0;/*
			
		double quality=0;
		for(Tuple constraint: qosConstraints){
			Object value= getAttributeValue(attributes, constraint.getKey());
			//TODO this is assuming that higher the value higher is Quality..
			if(constraint.getType().equals(Type.FLOAT32_TYPE)||constraint.getType().equals(Type.FLOAT64_TYPE)
					||constraint.getType().equals(Type.INT32_TYPE)){
				quality+=((Double)value-(Double)constraint.getValue())/((Double)value);
			}
		}
		if(quality>1) return 1;
		if(quality<0) return 0;
		return quality;*/
	}
}
