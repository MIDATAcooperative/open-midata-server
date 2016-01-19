package utils.access.op;

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
	
	
}
