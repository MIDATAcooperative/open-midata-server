package utils.access.op;

import java.util.Date;

import org.joda.time.format.ISODateTimeFormat;

import utils.access.AccessLog;

public class CompareCondition implements Condition {

    private Comparable<Object> val;
    private CompareOperator op;
	private boolean isDate;
    
    enum CompareOperator {
    	GT, LT, LE, GE
    }


	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public CompareCondition(Comparable<Object> val, CompareOperator op) {
		this.val = val;
		this.op = op;
		this.isDate = ((Object) val) instanceof Date;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		if (isDate) obj = ISODateTimeFormat.dateTimeParser().parseDateTime(obj.toString());
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
	

}
