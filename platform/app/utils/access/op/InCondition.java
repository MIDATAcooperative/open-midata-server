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
import java.util.Set;

import utils.collections.CMaps;

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