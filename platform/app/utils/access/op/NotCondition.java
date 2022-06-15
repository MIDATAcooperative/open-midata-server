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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import utils.collections.CMaps;

public class NotCondition implements Condition, Serializable {

	private Condition cond;
	
	
	public NotCondition(Condition cond) {
		this.cond = cond;
	}
	
	public Condition getCondition() {
		return cond;
	}
		
	@Override
	public boolean satisfiedBy(Object inputObj) {	
		return !cond.satisfiedBy(inputObj);
	}
		
	@Override
	public Condition optimize() {
		cond = cond.optimize();	
		if (cond instanceof FieldAccess) {
			FieldAccess fa = (FieldAccess) cond;
			return new FieldAccess(fa.getField(), new NotCondition(fa.getCondition()).optimize());
		} else if (cond instanceof EqualsSingleValueCondition) {
			EqualsSingleValueCondition eq = (EqualsSingleValueCondition) cond;
			return new NotInCondition(Collections.singleton(eq.getValue()));
		} else if (cond instanceof InCondition) {
			InCondition ic = (InCondition) cond;
			return new NotInCondition(ic.getValues());
		}
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
	public String toString() {		
		return "{ $not :" + cond.toString()+" }";
	}

	@Override
	public boolean isInBounds(Object low, Object high) {		
		return true;
	}

	@Override
	public Map<String, Object> asMongoQuery() {
		Map<String, Object> result = new HashMap<String, Object>();		
		return CMaps.map("$not", cond.asMongoValue());
	}

	@Override
	public Condition mongoCompatible() {
		Condition c = cond.mongoCompatible();
		return new NotCondition(c);
	}
		
	
}