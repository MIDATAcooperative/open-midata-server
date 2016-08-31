package utils.access.op;

import java.util.Map;

public class CompareCaseInsensitive implements Condition {

	private String val;
	
	public enum CompareCaseInsensitiveOperator {
    	EQUALS, STARTSWITH, CONTAINS, ENDSWITH
    }
	
	private CompareCaseInsensitiveOperator op;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public CompareCaseInsensitive(Object val, CompareCaseInsensitiveOperator op) {
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
		return this;
	}

	@Override
	public Map<String, Condition> indexExpression() {		
		return null;
	}
	
	
}
