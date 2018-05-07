package utils.access.op;

import java.io.Serializable;
import java.util.Map;

import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;

/**
 * check if object is equal to a fixed value
 *
 */
public class EqualsSingleValueCondition implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2376421768573187153L;
	private Comparable<Object> val;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public EqualsSingleValueCondition(Comparable<Object> val) {
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
		if (((Object) val) instanceof String) return new IndexCompare(val.toString(), CompareCaseInsensitiveOperator.EQUALS);
		else return this;
	}

	@Override
	public Condition indexExpression() {		
		return null;
	}

	@Override
	public boolean isInBounds(Object low, Object high) {
		return (low == null || val.compareTo(low) >= 0) && (high == null || val.compareTo(high) < 0);
	}

	@Override
	public Object asMongoQuery() {
		return val;
	}
	
	@Override
	public String toString() {		
		return val == null ? "null" : val.toString();
	}
	
	
}
