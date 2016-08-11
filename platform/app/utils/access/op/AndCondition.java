package utils.access.op;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scala.NotImplementedError;

/**
 * "And" operator for mongo expressions
 *
 */
public class AndCondition implements Condition {
		
    private List<Condition> checks;

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
		   		   
		   if (accessKey.equals("$gt") || accessKey.equals("!!!gt")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.GT));
		   } else if (accessKey.equals("$lt") || accessKey.equals("!!!lt")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.LT));
		   } else if (accessKey.equals("$le") || accessKey.equals("!!!le")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.LE));
		   } else if (accessKey.equals("$ge") || accessKey.equals("!!!ge")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.GE));
		   } else if (accessKey.equals("$in") || accessKey.equals("!!!in")) {			   
			  checks.add(new InCondition(makeSet(value)));
		   } else if (accessKey.equals("$or") || accessKey.equals("!!!or")) {			   
			  checks.add(new OrCondition(makeSet(value)));
		   } else if (accessKey.equals("$and") || accessKey.equals("!!!and")) {			   
			  for (Object obj : makeSet(value)) {
				  checks.add(parseRemaining(obj));
			  }
		   } else {		   
			   			   
			   Condition cond = FieldAccess.path(accessKey, parseRemaining(value));			   			   			   
		       checks.add(cond);
		   }
	       	       		     
	   }
	}
		
	
	@Override
	public Condition optimize() {
		if (checks.size() == 1) return checks.get(0).optimize();
		for (int i=0;i<checks.size();i++) {
			checks.set(i, checks.get(i).optimize());
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
	       return new EqualsSingleValueCondition(fragment);
	    } else if (fragment instanceof Map) {
	       return new AndCondition((Map<String,Object>) fragment); 
	    } else if (fragment instanceof Condition) {
	    	return (Condition) fragment;
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
	public Map<String, Condition> indexExpression() {
		// TODO Implement to support multi field indexes
		
		return null;
	}
		

}
