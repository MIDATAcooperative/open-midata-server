package utils.access.op;

import java.io.Serializable;
import java.util.regex.Pattern;

public class CompareCaseInsensitive implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2543649644913299628L;
	private String val;
	
	public enum CompareCaseInsensitiveOperator {
    	EQUALS, STARTSWITH, CONTAINS, ENDSWITH
    }
	
	private CompareCaseInsensitiveOperator op;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public CompareCaseInsensitive(Object val, CompareCaseInsensitive.CompareCaseInsensitiveOperator op) {
		this.val = val.toString().toLowerCase();
		this.op = op;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		if (obj == null) return false;
		switch (op) {
		case EQUALS: return val.equals(obj.toString().toLowerCase());
		case STARTSWITH: return obj.toString().toLowerCase().startsWith(val);
		case ENDSWITH: return obj.toString().toLowerCase().endsWith(val);
		case CONTAINS: return obj.toString().toLowerCase().indexOf(val) >= 0;
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
		if (val instanceof String) {
			if (op == CompareCaseInsensitiveOperator.EQUALS) return new IndexCompare(val, CompareCaseInsensitiveOperator.EQUALS);
			else if (op == CompareCaseInsensitiveOperator.STARTSWITH) return new IndexCompare(val, CompareCaseInsensitiveOperator.STARTSWITH);
		}
		return this;
	}

	@Override
	public Condition indexExpression() {		
		return null;
	}

	@Override
	public boolean isInBounds(Object low, Object high) {
		return true;
	}

	@Override
	public Object asMongoQuery() {
		//Map<String, Object> result = new HashMap();
		switch (op) {
		case EQUALS: return Pattern.compile("^"+val+"$", Pattern.CASE_INSENSITIVE);
		case STARTSWITH: return Pattern.compile("^"+val, Pattern.CASE_INSENSITIVE);
		case ENDSWITH: return Pattern.compile(val+"$", Pattern.CASE_INSENSITIVE);
		case CONTAINS: return Pattern.compile(val, Pattern.CASE_INSENSITIVE);
		}
		return null;
	}
	
	@Override
	public String toString() {		
		switch (op) {
		case EQUALS: return "/^"+val+"$/";
		case STARTSWITH: return "/^"+val+"/";
		case ENDSWITH: return "/"+val+"$/";
		case CONTAINS: return "/"+val+"/";
		}
		return null;
	}
	
	
}
