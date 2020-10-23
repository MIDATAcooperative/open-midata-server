/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access.op;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


public class InIndexCondition implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4857879713781565949L;
	private Set<String> val;
					
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public InIndexCondition(Set<Object> val) {
		this.val = new HashSet<String>();
		for (Object v : val) {
			if (v != null) this.val.add(v.toString().toUpperCase());
		}		
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		if (obj == null) return false;						
		return val.contains(obj.toString().toUpperCase());		
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
		return this;
	}

	@Override
	public Condition indexExpression() {		
		return null;
	}
	
	
	@Override
	public boolean isInBounds(Object low, Object high) {		
		for (String v : val) {
		  boolean match = (low == null || v.compareTo(low.toString()) >= 0) && (high == null || v.compareTo(high.toString()) <= 0);
		  if (match) return true;
		}
		return false;
	}

	@Override
	public Object asMongoQuery() {
		return val;		
	}
	
	@Override
	public String toString() {		
		return "in "+val.toString();		
	}
	
	
}
