package utils.access.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scala.NotImplementedError;

public class OrCondition implements Condition {
		
    private List<Condition> checks;

    public static Condition or(Condition cond1, Condition cond2) {
    	if (cond1 == null) return cond2;
    	if (cond2 == null) return cond1;
    	if (cond1 instanceof OrCondition) {
    		((OrCondition) cond1).checks.add(cond2);
    		return (OrCondition) cond1;
    	} else if (cond2 instanceof AndCondition) {
    		((OrCondition) cond2).checks.add(cond1);
    		return (OrCondition) cond2;
    	} else {
    		OrCondition result = new OrCondition(Collections.EMPTY_LIST);
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
	public OrCondition(Collection<Object> restrictions) {
	   checks = new ArrayList<Condition>();
	   for (Object value : restrictions) {		  		  
		   checks.add(AndCondition.parseRemaining(value));		   		  
	   }	       	       		     	   
	}
		
	
	@Override
	public Condition optimize() {
		if (checks.size() == 1) return checks.get(0).optimize();
		
		boolean allFieldAccess = true;
		boolean allSingleEq = true;
		String commonField = null;
		
		for (int i=0;i<checks.size();i++) {
			Condition c = checks.get(i).optimize();
			if (c instanceof FieldAccess) {
				String field = ((FieldAccess) c).getField();
				if (commonField == null) commonField = field;
				else if (!commonField.equals(field)) allFieldAccess = false;
				allSingleEq = false;
			} else if (c instanceof EqualsSingleValueCondition) {
				allFieldAccess = false;			
			} else {
				allFieldAccess = false;
				allSingleEq = false;
			}
			checks.set(i, c);
		}
		
		if (allFieldAccess && commonField!=null) {
			for (int i=0;i<checks.size();i++) {
				checks.set(i, ((FieldAccess) checks.get(i)).getCondition());
			}		    
			return (new FieldAccess(commonField, this)).optimize();		    
		}
		
		if (allSingleEq) {
			Set<Object> set = new HashSet<Object>();
			for (Condition c : checks) set.add(((EqualsSingleValueCondition) c).getValue());
			return new InCondition(set);
		}
		return this;
	}
	
			
	@Override
	public boolean satisfiedBy(Object inputObj) {
		for (Condition check : checks) {
			if (check.satisfiedBy(inputObj)) return true;           
		}
		return false;
	}

	@Override
	public Condition indexValueExpression() {
		Condition result = null;
		for (Condition c : checks) {
			Condition ive = c.indexValueExpression();
			if (ive == null) return null;
			result = OrCondition.or(result, ive);
		}
		return result;
	}

	@Override
	public Map<String, Condition> indexExpression() {		
		return null;
	}
		

}