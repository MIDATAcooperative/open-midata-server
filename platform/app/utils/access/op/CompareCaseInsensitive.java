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
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

public class CompareCaseInsensitive implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2543649644913299628L;
	private String val;
	
	public enum CompareCaseInsensitiveOperator {
    	EQUALS, STARTSWITH, CONTAINS, ENDSWITH
    }
	
	private CompareCaseInsensitiveOperator op;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public CompareCaseInsensitive(Object val, CompareCaseInsensitive.CompareCaseInsensitiveOperator op) {
		this.val = val.toString().toLowerCase();
		this.op = op;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		if (obj == null) return false;
		switch (op) {
		case EQUALS: return val.equals(obj.toString().toLowerCase());
		case STARTSWITH: return obj.toString().toLowerCase().startsWith(val);
		case ENDSWITH: return obj.toString().toLowerCase().endsWith(val);
		case CONTAINS: return obj.toString().toLowerCase().indexOf(val) >= 0;
		}
		return false;
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
		if (val instanceof String) {
			if (op == CompareCaseInsensitiveOperator.EQUALS) return new IndexCompare(val, CompareCaseInsensitiveOperator.EQUALS);
			else if (op == CompareCaseInsensitiveOperator.STARTSWITH) return new IndexCompare(val, CompareCaseInsensitiveOperator.STARTSWITH);
		}
		return this;
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
    public Map<String, Object> asMongoQuery() {
		return null;
	}
	
	@Override
	public Object asMongoValue() {
		//Map<String, Object> result = new HashMap();
		switch (op) {
		case EQUALS: return Pattern.compile("^"+val+"$", Pattern.CASE_INSENSITIVE);
		case STARTSWITH: return Pattern.compile("^"+val, Pattern.CASE_INSENSITIVE);
		case ENDSWITH: return Pattern.compile(val+"$", Pattern.CASE_INSENSITIVE);
		case CONTAINS: return Pattern.compile(val, Pattern.CASE_INSENSITIVE);
		}
		return null;
	}
			
	@Override
	public Condition mongoCompatible() {
		return this;
	}

	@Override
	public String toString() {		
		switch (op) {
		case EQUALS: return "/^"+val+"$/";
		case STARTSWITH: return "/^"+val+"/";
		case ENDSWITH: return "/"+val+"$/";
		case CONTAINS: return "/"+val+"/";
		}
		return "unknown op";
	}
	
	
}
