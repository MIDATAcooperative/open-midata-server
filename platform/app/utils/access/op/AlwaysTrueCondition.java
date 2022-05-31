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
import java.util.Map;
import java.util.regex.Pattern;

import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;

public class AlwaysTrueCondition implements Condition, Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2687477531757558051L;

	
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public AlwaysTrueCondition() {
		
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		return true;
	}

	@Override
	public Condition optimize() {
		return this;
	}
	
	public Object getValue() {
		return null;
	}

	@Override
	public Condition indexValueExpression() {
		return this;
	}

	@Override
	public Condition indexExpression() {		
		return null;
	}
	
	
	@Override
	public Object asMongoValue() {	
		return true;
	}
	
	@Override
	public String toString() {				
		return "true";
	}

	@Override
	public boolean isInBounds(Object low, Object high) {
		return true;
	}

	@Override
	public Map<String, Object> asMongoQuery() {
		throw new NullPointerException();
	}

	@Override
	public Condition mongoCompatible() {
		return this;
	}
	
	
	
	
}
