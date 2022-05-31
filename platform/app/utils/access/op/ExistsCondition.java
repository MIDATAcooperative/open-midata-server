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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import utils.collections.CMaps;

public class ExistsCondition implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4953224071488125722L;
	private boolean shouldExist;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public ExistsCondition(boolean shouldExist) {
		this.shouldExist = shouldExist;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		return shouldExist ? (obj != null) : (obj == null);
	}

	@Override
	public Condition optimize() {
		return this;
	}
		
	@Override
	public Condition indexValueExpression() {
		return null;
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
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("$exists", shouldExist);
		return result;
	}
			
	@Override
	public Condition mongoCompatible() {
		return this;
	}

	@Override
	public String toString() {		
		return "{ $exists : "+shouldExist+" }";
	}
	
	
}
