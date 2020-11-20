/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	public Condition indexExpression();
	
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
