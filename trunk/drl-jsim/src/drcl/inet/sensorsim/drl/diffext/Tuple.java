package drcl.inet.sensorsim.drl.diffext;

import java.io.Serializable;

public class Tuple implements Serializable{

	private static final long serialVersionUID = 6998968783919086426L;
	
	public static enum Operator {
		IS {
			public boolean isFormal() {
				return false;
			}
		},
		LE, GE, LT, GT, EQ, NE, EQ_ANY;
		public boolean isFormal() {
			return true;
		}
	};
	
	public static enum Type{
		INT32_TYPE, 		// 32-bit signed integer
		FLOAT32_TYPE, 		// 32-bit
		FLOAT64_TYPE, 		// 64-bit
		STRING_TYPE, 		// UTF-8 format, max length 1024 chars
		BLOB_TYPE,  		// uninterpreted binary data
        BOOL_TYPE			// boolean type
	}
	
	public static final String LATITUDE_KEY = "long" ;		// FLOAT_TYPE
	public static final String LONGITUDE_KEY = "lat" ;		// FLOAT_TYPE

	public static final String TASK_FREQUENCY_KEY = "freq" ; 	// FLOAT_TYPE, in sec.
	public static final String TARGET_KEY = "target" ;		// STRING_TYPE
	public static final String TARGET_NID = "targetNid" ;		// INT_TYPE
	public static final String TARGET_RANGE_KEY = "range" ; 	// FLOAT_TYPE, in sec.
	public static final String SNR = "snr";
	public static final String CONFIDENCE="confidence";
	public static final String GROUP_ID="groupId";
	
	final private String key;
	final private Type type;
	
	final private Operator operator;
	private Object value;
	
	public Tuple(final String key,final Type type,final Operator operator,final Object value){
		if(key==null || type==null) throw new NullPointerException("key and type cannot be null");
		this.key=key;
		this.type=type;
		this.operator=operator;
		this.value=value;
	}
	
	@Override
	public String toString(){
		return "key="+key+",type="+type+",operator="+operator+",value="+value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result
				+ ((operator == null) ? 0 : operator.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple other = (Tuple) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (operator == null) {
			if (other.operator != null)
				return false;
		} else if (!operator.equals(other.operator))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	public String getKey() {
		return key;
	}

	public Type getType() {
		return type;
	}

	public Operator getOperator() {
		return operator;
	}

	public Object getValue() {
		return value;
	}
	
	public void setValue(Object o){
		this.value=o;
	}
	
	public boolean matches(Tuple interestTuple) {
		if(interestTuple==null) return false;
		if(!interestTuple.key.equals(this.key)) return false;
		if(!interestTuple.type.equals(this.type)) return false;
		switch(interestTuple.operator){
		case EQ_ANY:
			return true;
		case EQ:
			switch(interestTuple.type){
			case INT32_TYPE:
			case FLOAT32_TYPE:
			case FLOAT64_TYPE:
			case BOOL_TYPE:
				return (this.value==interestTuple.value);
			case STRING_TYPE:
			case BLOB_TYPE:
				 if(this.value==null) return (interestTuple.value==null);
				 return (this.value.equals(interestTuple.value));
			}
		case NE:
			switch(interestTuple.type){
			case INT32_TYPE:
			case FLOAT32_TYPE:
			case FLOAT64_TYPE:
			case BOOL_TYPE:
				return (this.value!=interestTuple.value);
			case STRING_TYPE:
			case BLOB_TYPE:
				 if(this.value==null) return (interestTuple.value!=null);
				 return (!this.value.equals(interestTuple.value));
			}
		case LE:
			switch(interestTuple.type){
			case INT32_TYPE:
			case FLOAT32_TYPE:
			case FLOAT64_TYPE:
				return (((Double)this.value)<=(Double)interestTuple.value);			
			}
		case GE:
			switch(interestTuple.type){
			case INT32_TYPE:
			case FLOAT32_TYPE:
			case FLOAT64_TYPE:
				return (((Double)this.value)>=(Double)interestTuple.value);			
			}
		case LT:
			switch(interestTuple.type){
			case INT32_TYPE:
			case FLOAT32_TYPE:
			case FLOAT64_TYPE:
				return (((Double)this.value)<(Double)interestTuple.value);			
			}
		case GT:
			switch(interestTuple.type){
			case INT32_TYPE:
			case FLOAT32_TYPE:
			case FLOAT64_TYPE:
				return (((Double)this.value)>(Double)interestTuple.value);			
			}
		}
		System.out.println("Invalid match:data="+toString()+", interest="+interestTuple);
		return false;
	}

}
