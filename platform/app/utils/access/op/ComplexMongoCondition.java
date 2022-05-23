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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexMongoCondition extends AndCondition {

	private String mode;	
	
	public final static String MODE_AND = "$and";
	public final static String MODE_OR = "$or";
	
	public ComplexMongoCondition(String mode, List<Condition> checks) {
		super(checks);
		this.mode = mode;	
	}
		
	@Override
	public Map<String, Object> asMongoQuery() {
		Map<String, Object> result = new HashMap<String, Object>();		
		List<Object> parts = new ArrayList<Object>();
		for (Condition check : checks) {
			Map<String, Object> part = check.asMongoQuery(); 
			parts.add(part); 					
		}
		result.put(mode, parts);		 		
		return result;		
	}

	@Override
	public Condition mongoCompatible() {
		List<Condition> out = new ArrayList<Condition>(checks.size());
		for (Condition c : checks) {
			Condition s = c.mongoCompatible();
			
			if (s instanceof ComplexMongoCondition) {
				ComplexMongoCondition child = (ComplexMongoCondition) s;
				if (child.mode.equals(mode)) {
					out.addAll(child.checks);
					continue;
				}
			}
			
			out.add(s);
		}
		return new ComplexMongoCondition(mode, out);
	}

	public Condition prependFieldName(String field) {
		List<Condition> out = new ArrayList<Condition>(checks.size());
		for (Condition c : checks) {
			out.add(new FieldAccess(field, c).mongoCompatible());
		}
		return new ComplexMongoCondition(mode, out);
	}

}
