package utils.access.op;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import scala.NotImplementedError;

/**
 * "And" operator for mongo expressions
 *
 */
public class AndCondition implements Condition {
		
    private List<Condition> checks;

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
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.GT));
		   } else if (accessKey.equals("$lt")) {
			  checks.add(new CompareCondition((Comparable<Object>) value, CompareCondition.CompareOperator.LT));
		   } else {		   
			   String[] paths = accessKey.split("\\.");
			   
			   Condition cond = parseRemaining(value);
			   
			   for (int i = paths.length-1;i>=0;i--) cond = new FieldAccess(paths[i], cond);
			   
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
	    } else {
	       throw new NotImplementedError();
	    }
	}
			
	@Override
	public boolean satisfiedBy(Object inputObj) {
		for (Condition check : checks) {
			if (!check.satisfiedBy(inputObj)) return false;           
		}
		return true;
	}
		

}