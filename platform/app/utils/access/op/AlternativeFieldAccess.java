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

import org.bson.BSONObject;

public class AlternativeFieldAccess implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6564693337792439980L;
	private String[][] field;
	private String fieldName;
	private Condition cond;
	
	/**
	 * Constructor 
	 * @param field name of field that should be accessed on the data object
	 * @param cond sub condition that will be applied
	 */
	public AlternativeFieldAccess(String field, Condition cond) {
		this.fieldName = field;
		String[] fields = field.split("\\|");		
		this.field = new String[fields.length][];
		for (int i=0;i<fields.length;i++) this.field[i] = fields[i].split("\\.");
		this.cond = cond;
	}
	
	
	
	
	
	public Condition getCondition() {
		return cond;
	}
	
	public String getField() {
		return fieldName;
	}
		
	@Override
	public boolean satisfiedBy(Object inputObj) {
	
		if (inputObj == null) return false;
		
		for (int i=0;i<field.length;i++) {
			Object test = inputObj;
			for (int j=0;j<field[i].length;j++) {
				test = access(test, field[i][j]);
				if (test == null) break;
			}
			if (test != null) {
				return cond.satisfiedBy(test);
			}
		}
		return cond.satisfiedBy(null);
	}
	
	/**
	 * access field of provided object
	 * @param obj object to access. May be null.
	 * @param path Name of field to access
	 * @return field of object or null if field does not exist on given object
	 */
	private Object access(Object obj, String path) {
    	if (obj == null) return null;
    	if (obj instanceof BSONObject) {
    		return ((BSONObject) obj).get(path);
    	}
    	return null;
    }

	@Override
	public Condition optimize() {
		cond = cond.optimize();
		return this;
	}

	@Override
	public Condition indexValueExpression() {		
		return null;
	}

	
	@Override
	public String toString() {		
		return fieldName + ":" + cond.toString();
	}

	@Override
	public boolean isInBounds(Object low, Object high) {		
		return false;
	}



	@Override
	public Condition indexExpression() {	
		Condition expr = cond.indexValueExpression();
		if (expr != null) return new AlternativeFieldAccess(fieldName, expr);
		return null;
	}



	@Override
	public Object asMongoQuery() {		
		return null;
	}

	
	
}
