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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElemMatchCondition extends AndCondition {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5762971270313448425L;

	public ElemMatchCondition(Map<String, Object> restrictions) {
		super(restrictions);
	}
	
	public ElemMatchCondition(List<Condition> parts) {
		super(parts);
	}
	
	@Override
	public Map<String, Object> asMongoQuery() {
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Object> parts = new HashMap<String, Object>();
		for (Condition check : checks) {
			parts.putAll(check.asMongoQuery());
		}
		result.put("$elemMatch", parts);
		return result;
	}
	
	public static Condition and(Condition cond1, Condition cond2) {
    	if (cond1 == null) return cond2;
    	if (cond2 == null) return cond1;
    	if (cond1 instanceof ElemMatchCondition) {
    		((ElemMatchCondition) cond1).checks.add(cond2);
    		return (ElemMatchCondition) cond1;
    	} else if (cond2 instanceof ElemMatchCondition) {
    		((ElemMatchCondition) cond2).checks.add(cond1);
    		return (ElemMatchCondition) cond2;
    	} else {
    		ElemMatchCondition result = new ElemMatchCondition(Collections.EMPTY_MAP);
    		result.checks.add(cond1);
    		result.checks.add(cond2);
    		return result;
    	}
    }

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("{ $elemMatch : { ");
		boolean first = true;
		for (Condition check : checks) {
			if (first) first=false; else result.append(", ");
			result.append(check.toString());
		}
		result.append("} }");
		return result.toString();
	}
	
	@Override
	public AndCondition createAndOfThisType(List<Condition> parts) {
		return new ElemMatchCondition(parts);
	}
	
	
}
