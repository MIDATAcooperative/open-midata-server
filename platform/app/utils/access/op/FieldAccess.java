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
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import utils.AccessLog;

/**
 * Field access operator for mongo conditions
 *
 */
public class FieldAccess implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2755088373173853881L;
	private String field;
	private Condition cond;
	
	/**
	 * Constructor 
	 * @param field name of field that should be accessed on the data object
	 * @param cond sub condition that will be applied
	 */
	public FieldAccess(String field, Condition cond) {
		this.field = field;
		this.cond = cond;
	}
	
	public static Condition path(String accessKey, Condition cond) {
		if (accessKey.contains("|")) return new AlternativeFieldAccess(accessKey, cond);
		
		String[] paths = accessKey.split("\\.");
		   		   		   
		for (int i = paths.length-1;i>=0;i--) cond = new FieldAccess(paths[i], cond);
		
		return cond;
	}
	
	public String getField() {
		return field;
	}
	
	public Condition getCondition() {
		return cond;
	}
		
	@Override
	public boolean satisfiedBy(Object inputObj) {
	
		if (inputObj == null) return cond.satisfiedBy(null);
        Object res = access(inputObj, field);
        if (res == null) return cond.satisfiedBy(null);
           
        if (res instanceof BasicBSONList) {
           BasicBSONList lst = (BasicBSONList) res;
           
           for (Object object : lst) {
             if (cond.satisfiedBy(object)) return true;     
           }
           
           return false;
		}
		return cond.satisfiedBy(res);
	}
	
	/**
	 * access field of provided object
	 * @param obj object to access. May be null.
	 * @param path Name of field to access
	 * @return field of object or null if field does not exist on given object
	 */
	protected Object access(Object obj, String path) {
    	if (obj == null) return null;
    	if (obj instanceof BSONObject) {
    		return ((BSONObject) obj).get(path);
    	}
    	return null;
    }

	@Override
	public Condition optimize() {
		cond = cond.optimize();
		if (field.contains(".")) return FieldAccess.path(field, cond);
		return this;
	}

	@Override
	public Condition indexValueExpression() {		
		return null;
	}

	@Override
	public Condition indexExpression() {
		String path = field;
		Condition c = cond;
				
		while (c instanceof FieldAccess) {
			path += "."+((FieldAccess) c).field;
			c = ((FieldAccess) c).cond;
		}
		Condition c2 = c.indexValueExpression();
		if (c2 == null) {
			if (c instanceof AndCondition) {
				Condition r = null;
				for (Condition part : ((AndCondition) c).getParts()) {
					r = AndCondition.and(r, new FieldAccess(path, part));
				}
				return r == null ? null : r.indexExpression();
			} else if (c instanceof OrCondition) {
				Condition r = null;
				for (Condition part : ((OrCondition) c).getParts()) {
					r = OrCondition.or(r, new FieldAccess(path, part));
				}
				return r == null ? null : r.indexExpression();
			}			
			return null;
		}		
		return new FieldAccess(path, c2); 
	}

	@Override
	public String toString() {		
		return field + ":" + cond.toString();
	}

	@Override
	public boolean isInBounds(Object low, Object high) {		
		return false;
	}

	@Override
	public Map<String, Object> asMongoQuery() {
		Map<String, Object> result = new HashMap<String, Object>();
		
		/*String fieldName = field;
		Condition c = cond;
		
		while (c instanceof FieldAccess) {
			fieldName += "."+((FieldAccess) c).field;
			c = ((FieldAccess) c).cond;
		}*/
		
		result.put(field, cond.asMongoValue());
		return result;
	}

	@Override
	public Condition mongoCompatible() {
		Condition c = cond.mongoCompatible();
		if (c instanceof ComplexMongoCondition) {
			return ((ComplexMongoCondition) c).prependFieldName(field);
		}
		if (c instanceof FieldAccess) {
			FieldAccess sub = (FieldAccess) c;
			return new FieldAccess(field+"."+sub.getField(), sub.getCondition());
		}
		return new FieldAccess(getField(), c);
	}
	
	

	
}
