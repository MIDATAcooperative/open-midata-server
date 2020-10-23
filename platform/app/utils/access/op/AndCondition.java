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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import scala.NotImplementedError;

/**
 * "And" operator for mongo expressions
 *
 */
public class AndCondition implements Condition, Serializable {
		
    /**
	 * 
	 */
	private static final long serialVersionUID = 7999329976231518887L;
	protected List<Condition> checks;

    public static Condition and(Condition cond1, Condition cond2) {
    	if (cond1 == null) return cond2;
    	if (cond2 == null) return cond1;
    	if (cond1 instanceof AndCondition) {
    		((AndCondition) cond1).checks.add(cond2);
    		return (AndCondition) cond1;
    	} else if (cond2 instanceof AndCondition) {
    		((AndCondition) cond2).checks.add(cond1);
    		return (AndCondition) cond2;
    	} else {
    		AndCondition result = new AndCondition(Collections.EMPTY_MAP);
    		result.checks.add(cond1);
    		result.checks.add(cond2);
    		return result;
    	}
    }
    /**
     * Constructor
     * @param restrictions map with remainder of "and" expression
     */
	@SuppressWarnings("unchecked")
	public AndCondition(Map<String, Object> restrictions) {
	   checks = new ArrayList<Condition>();
	   for (String accessKey : restrictions.keySet()) {		  		  
		   Object value = restrictions.get(accessKey);
		   		   
		   if (accessKey.equals("$gt")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.GT, false));
		   } else if (accessKey.equals("$lt")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.LT, false));
		   } else if (accessKey.equals("$le")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.LE, false));
		   } else if (accessKey.equals("$ge")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.GE, false));
		   } else if (accessKey.equals("$gtn")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.GT, true));
		   } else if (accessKey.equals("$ltn")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.LT, true));
		   } else if (accessKey.equals("$len")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.LE, true));
		   } else if (accessKey.equals("$gen")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.GE, true));
		   } else if (accessKey.equals("$in")) {			   
			  checks.add(new InCondition(makeSet(value)));
		   } else if (accessKey.equals("$or")) {			   
			  checks.add(new OrCondition(makeSet(value)));		   
		   } else if (accessKey.equals("$and")) {			   
			  for (Object obj : makeSet(value)) {
				  checks.add(parseRemaining(obj));
			  }
		   } else if (accessKey.equals("$exists")) {
			   checks.add(new ExistsCondition(value.toString().toLowerCase().equals("true")));
		   } else {		   			  			   
			     Condition cond = FieldAccess.path(accessKey, parseRemaining(value));			   			   			   
		         checks.add(cond);			   
		   }
	       	       		     
	   }
	}
		
	
	@Override
	public Condition optimize() {
		if (checks.size() == 1) return checks.get(0).optimize();
		
		boolean allFieldAccess = true;	
		String commonField = null;
		
		for (int i=0;i<checks.size();i++) {
			Condition c = checks.get(i).optimize();
			if (c instanceof FieldAccess) {
				Condition c2 = ((FieldAccess) c).getCondition();
				if (c2 instanceof ElemMatchCondition || c2 instanceof CompareCaseInsensitive || c2 instanceof EqualsSingleValueCondition) {
					allFieldAccess = false;
				} else {
				   String field = ((FieldAccess) c).getField();
				   if (commonField == null) commonField = field;
				   else if (!commonField.equals(field)) allFieldAccess = false;
				}
			} else  {
				allFieldAccess = false;			
			} 
			checks.set(i, c);
		}
		
		if (allFieldAccess && commonField!=null) {
			for (int i=0;i<checks.size();i++) {
				checks.set(i, ((FieldAccess) checks.get(i)).getCondition());
			}		    
			return (new FieldAccess(commonField, this)).optimize();		    
		}
						
		return this;
	}
	
	/**
	 * parses a (part of a) json mongo expression. 
	 * @param fragment may be a map(json object) or a constant
	 * @return expression parsed into a condition object
	 */
	@SuppressWarnings("unchecked")
	public static Condition parseRemaining(Object fragment) {
		if (fragment instanceof String) {	    	   
	       return new EqualsSingleValueCondition((Comparable) fragment);
	    } else if (fragment instanceof Map) {
	       return new AndCondition((Map<String,Object>) fragment); 
	    } else if (fragment instanceof Condition) {
	    	return (Condition) fragment;
	    } else if (fragment instanceof MidataId) {
	    	return new EqualsSingleValueCondition((Comparable) fragment);
	    } else {
	       throw new NotImplementedError();
	    }
	}
	
	/**
	 * creates a set from a part of a mongo expression
	 * @param fragment
	 * @return
	 */
	public static Set<Object> makeSet(Object fragment) {
		if (fragment instanceof Set) return (Set) fragment;
		if (fragment instanceof Collection) return new HashSet<Object>((Collection) fragment);
		if (fragment instanceof String) return Collections.singleton(fragment);
		throw new NotImplementedError();
	}
			
	@Override
	public boolean satisfiedBy(Object inputObj) {
		for (Condition check : checks) {
			if (!check.satisfiedBy(inputObj)) return false;           
		}
		return true;
	}
	
	@Override
	public Condition indexValueExpression() {
		Condition result = null;
		for (Condition c : checks) {
			result = AndCondition.and(result, c.indexValueExpression());
		}
		return result;
	}
	
	@Override
	public Condition indexExpression() {
		Condition result = null;
		if (checks.size() > 0) {
			for (Condition cond : checks) {
				Condition indexAccess = cond.indexExpression();
				result = AndCondition.and(result, indexAccess);				
			}
		}
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("{ ");		
		for (Condition check : checks) {
			if (result.length() > 2) result.append(", ");
			result.append(check.toString());
		}
		result.append("}");
		return result.toString();
	}
	
	@Override
	public boolean isInBounds(Object low, Object high) {
		for (Condition check : checks) {
			if (!check.isInBounds(low, high)) return false;           
		}
		return true;
	}
	
	@Override
	public Map<String, Object> asMongoQuery() {
		Map<String, Object> result = new HashMap<String, Object>();
		boolean complex = false;
		List<Object> parts = new ArrayList<Object>();
		for (Condition check : checks) {
			Map<String, Object> part = (Map<String, Object>) check.asMongoQuery(); 
			parts.add(part); 
			for (Map.Entry<String, Object> entry : part.entrySet()) {
				if (result.containsKey(entry.getKey())) {
					complex = true;
				} else result.put(entry.getKey(), entry.getValue());
			}			
		}
		if (complex) {
			result.clear();
			result.put("$and", parts);
		} 		
		return result;
	}
	
	public List<Condition> getParts() {
		return checks;
	}
	

}
