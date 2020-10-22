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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.format.ISODateTimeFormat;

/**
 * A comparison operator for mongo expressions
 *
 */
public class CompareCondition implements Condition, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 6959525790908419853L;
	private Comparable<Object> val;
    private CompareOperator op;
    private boolean nullTrue;
	private boolean isDate;
	private boolean isNumber;
    
    public enum CompareOperator {
    	GT, LT, LE, GE, EQ, NE
    }


	/**
	 * Constructor.
	 * @param val a value to that the record fields should be compared with 
	 * @param op comparison operator. 
	 */
	public CompareCondition(Comparable<Object> val, CompareCondition.CompareOperator op, boolean nullTrue) {
		if (val == null) throw new NullPointerException("Null argument for comparator");
		this.val = val;
		this.op = op;
		this.nullTrue = nullTrue;
		this.isDate = ((Object) val) instanceof Date;
		this.isNumber = ((Object) val) instanceof Double;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		if (obj == null) return nullTrue;
		try {
		  if (isDate) obj = ISODateTimeFormat.dateTimeParser().parseDateTime(obj.toString()).toDate();
		  if (isNumber && !(obj instanceof Double)) obj = new Double(obj.toString());
		} catch (IllegalArgumentException e) { return false; }
		//AccessLog.debug(obj.toString()+" "+op.toString()+val.toString());
		//AccessLog.debug(obj.getClass().getName()+" "+op.toString()+val.getClass().getName());
		switch (op) {
		case EQ:return val.compareTo(obj) == 0;
		case NE:return val.compareTo(obj) != 0;
		case GT:return val.compareTo(obj) < 0;
		case GE:return val.compareTo(obj) <= 0;
		case LE:return val.compareTo(obj) >= 0;
		case LT:return val.compareTo(obj) > 0;
		}
		return false;
	}

	@Override
	public Condition optimize() {
		return this;
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
	public String toString() {
		return "$"+op+(nullTrue?"n":"")+" : "+val.toString(); 
	}

	@Override
	public boolean isInBounds(Object low, Object high) {		
		try {
		  if (isDate) {
			  if (low!=null) low = ISODateTimeFormat.dateTimeParser().parseDateTime(low.toString()).toDate();
			  if (high!=null) high = ISODateTimeFormat.dateTimeParser().parseDateTime(high.toString()).toDate();
		  }
		  if (isNumber && low!=null && !(low instanceof Double)) low = new Double(low.toString());
		  if (isNumber && high!=null && !(high instanceof Double)) high = new Double(high.toString());
		} catch (IllegalArgumentException e) { return false; }
		
		
		switch (op) {
		case GT:return high == null || val.compareTo(high) <= 0;
		case GE:return high == null || val.compareTo(high) <= 0;
		case LE:return low == null || val.compareTo(low) >= 0;
		case LT:return low == null || val.compareTo(low) >= 0;
		}
		return false;
	}

	@Override
	public Object asMongoQuery() {
		Map<String, Object> result = new HashMap<String, Object>();
		switch (op) {
		case EQ:return val;
		case NE:result.put("$ne", val);break;
		case GT:result.put("$gt", val);break;
		case GE:result.put("$gte", val);break;
		case LE:result.put("$lte", val);break;
		case LT:result.put("$lt", val);break;
		}
		return result;
	}
	
	
	

}
