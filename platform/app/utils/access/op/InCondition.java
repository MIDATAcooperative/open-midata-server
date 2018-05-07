package utils.access.op;

import java.io.Serializable;
import java.util.Set;

/**
 * check if object is in a list of objects
 *
 */
public class InCondition implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3810696744848273080L;
	private Set<Object> val;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public InCondition(Set<Object> val) {
		this.val = val;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		return val.contains(obj);
	}

	@Override
	public Condition optimize() {
		return this;
	}

	@Override
	public Condition indexValueExpression() {
		return new InIndexCondition(val);
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
		return val;
	}
	
	
}