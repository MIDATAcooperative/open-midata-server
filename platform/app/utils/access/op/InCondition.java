package utils.access.op;

import java.util.Set;

/**
 * check if object is in a list of objects
 *
 */
public class InCondition implements Condition {

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
	
	
}