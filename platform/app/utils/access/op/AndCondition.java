package utils.access.op;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import scala.NotImplementedError;

public class AndCondition implements Condition {
	
	/**
	 * Constructor. 	
	 */
    private List<Condition> checks;

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
	
	public Condition parseRemaining(Object fragment) {
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
