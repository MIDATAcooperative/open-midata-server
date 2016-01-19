package utils.access.op;

/**
 * Condition in a mongo query
 *
 */
public interface Condition {
	/**
	 * returns true if the provided object satisfies this condition
	 * @param obj part of record to be checked
	 * @return true if the condition is satisfied
	 */
	public boolean satisfiedBy(Object obj);
}
