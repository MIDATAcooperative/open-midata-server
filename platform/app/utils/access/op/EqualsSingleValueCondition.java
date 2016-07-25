package utils.access.op;

import java.util.Map;

/**
 * check if object is equal to a fixed value
 *
 */
public class EqualsSingleValueCondition implements Condition {

	private Object val;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public EqualsSingleValueCondition(Object val) {
		this.val = val;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		return val.equals(obj);
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
