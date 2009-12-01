package drcl.inet.sensorsim.drl.diffext;

import java.util.List;

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
}
