package utils.access.op;

import java.util.Date;
import java.util.Map;

import org.joda.time.format.ISODateTimeFormat;

/**
 * A comparison operator for mongo expressions
 *
 */
public class CompareCondition implements Condition {

    private Comparable<Object> val;
    private CompareOperator op;
	private boolean isDate;
    
    public enum CompareOperator {
    	GT, LT, LE, GE
    }


	/**
	 * Constructor.
	 * @param val a value to that the record fields should be compared with 
	 * @param op comparison operator. 
	 */
	public CompareCondition(Comparable<Object> val, CompareOperator op) {
		if (val == null) throw new NullPointerException("Null argument for comparator");
		this.val = val;
		this.op = op;
		this.isDate = ((Object) val) instanceof Date;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		if (obj == null) return false;
		try {
		  if (isDate) obj = ISODateTimeFormat.dateTimeParser().parseDateTime(obj.toString()).toDate();
		} catch (IllegalArgumentException e) { return false; }
		//AccessLog.debug(obj.toString()+" "+op.toString()+val.toString());
		//AccessLog.debug(obj.getClass().getName()+" "+op.toString()+val.getClass().getName());
		switch (op) {
		case GT:return val.compareTo(obj) < 0;
		case GE:return val.compareTo(obj) <= 0;
		case LE:return val.compareTo(obj) >= 0;
		case LT:return val.compareTo(obj) > 0;
		}
		return false;
	}

	@Override
	public Condition optimize() {
		return this;
	}

	@Override
	public Condition indexValueExpression() {
		return this;
	}

	@Override
	public Map<String, Condition> indexExpression() {		
		return null;
	}

	@Override
	public String toString() {
		return "$"+op+" : "+val.toString(); 
	}

	@Override
	public boolean isInBounds(Object low, Object high) {
		switch (op) {
		case GT:return high == null || val.compareTo(high) < 0;
		case GE:return high == null || val.compareTo(high) <= 0;
		case LE:return low == null || val.compareTo(low) >= 0;
		case LT:return low == null || val.compareTo(low) > 0;
		}
		return false;
	}
	
	
	

}
