package utils.access.op;

import java.util.regex.Pattern;

import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;

public class IndexCompare implements Condition {

	private String val;
			
	private CompareCaseInsensitiveOperator op;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public IndexCompare(String val, CompareCaseInsensitiveOperator op) {
		this.val = val.toUpperCase();
		this.op = op;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		if (obj == null) return false;
		switch (op) {
		case EQUALS: return val.equals(obj.toString().toUpperCase());
		case STARTSWITH: return obj.toString().toUpperCase().startsWith(val);		
		}
		return false;
	}

	@Override
	public Condition optimize() {
		return this;
	}
	
	public Object getValue() {
		return val;
	}

	@Override
	public Condition indexValueExpression() {
		return this;
	}

	@Override
	public Condition indexExpression() {		
		return null;
	}
	
	private static String first(Object o, int l) {
		String o2 = o.toString();
		return o2.substring(0, Math.min(o2.length(), l));
	}

	@Override
	public boolean isInBounds(Object low, Object high) {		
		if (op == CompareCaseInsensitiveOperator.STARTSWITH) {
			return (low == null || val.compareTo(IndexCompare.first(low, val.length())) >= 0) && (high == null || val.compareTo(IndexCompare.first(high, val.length())) < 0);			
		} else
		return (low == null || val.compareTo(low.toString()) >= 0) && (high == null || val.compareTo(high.toString()) < 0);
	}

	@Override
	public Object asMongoQuery() {
		//Map<String, Object> result = new HashMap();
		switch (op) {
		case EQUALS: return Pattern.compile("^"+val+"$", Pattern.CASE_INSENSITIVE);
		case STARTSWITH: return Pattern.compile("^"+val, Pattern.CASE_INSENSITIVE);		
		}
		return null;
	}
	
	@Override
	public String toString() {		
		switch (op) {
		case EQUALS: return "/^"+val+"$/";
		case STARTSWITH: return "/^"+val+"/";		
		}
		return null;
	}
	
	
}