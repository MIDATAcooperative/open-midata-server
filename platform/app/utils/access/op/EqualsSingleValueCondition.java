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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

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
		return (low == null || val.compareTo(low) >= 0) && (high == null || val.compareTo(high) <= 0);
	}

	@Override
    public Map<String, Object> asMongoQuery() {
		return null;
	}
	
	@Override
	public Object asMongoValue() {
		return val;
	}
	
	public Condition mongoCompatible() {
		return this;
	}
	
	@Override
	public String toString() {		
		return val == null ? "null" : val.toString();
	}
	
	
}
