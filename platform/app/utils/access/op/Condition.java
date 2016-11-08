package utils.access.op;

import java.util.Map;

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
	
	/**
	 * returns an optimized version of the condition
	 * @return optimized version of condition (may be same object)
	 */
	public Condition optimize();
	
	/**
	 * Convert this expression into a value to be applied to an index.
	 * Returns null if conversion is not possible.
	 * @return
	 */
	public Condition indexValueExpression();
	
	/**
	 * Convert this expression into an index operation.
	 * Returns null if conversion is not possible.
	 * @return
	 */
	public Map<String, Condition> indexExpression();
	
	/**
	 * Converts the condition to a mongo query that works on a standard collection
	 * @return object to be passed as properties to the mongo query
	 */
	public Object asMongoQuery();
	
	/**
	 * Returns if this condition may be true when applied to values in the given range. This is used by the index engine.
	 * @param low low value to test
	 * @param high high value to test
	 * @return true if condition may be satisfied by values inside the given bounds.
	 */
	public boolean isInBounds(Object low, Object high);
}
