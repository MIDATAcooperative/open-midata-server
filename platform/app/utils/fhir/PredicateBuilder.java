package utils.fhir;

import utils.access.op.AndCondition;
import utils.access.op.CompareCaseInsensitive;
import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;
import utils.access.op.CompareCondition;
import utils.access.op.CompareCondition.CompareOperator;
import utils.access.op.Condition;
import utils.access.op.ElemMatchCondition;
import utils.access.op.EqualsSingleValueCondition;
import utils.access.op.ExistsCondition;
import utils.access.op.FieldAccess;
import utils.access.op.OrCondition;

public class PredicateBuilder {
  
	private Condition result = null;
	private Condition block = null;
	private Condition current = null;
	
	private boolean indexable = true;
	
	public void addComp(String path, CompareOperator op, Object value, boolean nullTrue) {		
		Condition cond = new CompareCondition((Comparable<Object>) value, op, nullTrue);
		add(FieldAccess.path(path, cond));
	}
	
	public void addComp(String path, CompareOperator op, Object value, boolean nullTrue, CompareOperator op2, Object value2, boolean nullTrue2) {		
		Condition cond1 = new CompareCondition((Comparable<Object>) value, op, nullTrue);
		Condition cond2 = new CompareCondition((Comparable<Object>) value2, op2, nullTrue2);
		add(FieldAccess.path(path, AndCondition.and(cond1, cond2)));
	}
	
	public void addCompOr(String path, CompareOperator op, Object value, boolean nullTrue) {		
		Condition cond = new CompareCondition((Comparable<Object>) value, op, nullTrue);
		cond = FieldAccess.path(path, cond);
		
		if (current == null) current = cond; 
		else current = OrCondition.or(current, cond);
	}
	
	public void addExists(String path, boolean value) {
	  add(FieldAccess.path(path, new ExistsCondition(value)));
	}
		
	
	public void addEq(String path, Object value) {
	  add(FieldAccess.path(path, new EqualsSingleValueCondition((Comparable) value)));
	}
	
	public void addEq(String path, Object value, CompareCaseInsensitiveOperator op) {
		  add(FieldAccess.path(path, new CompareCaseInsensitive(value, op)));
	}
	
	public void addEq(String path, String sysPath, Object system, String valPath, Object value, CompareCaseInsensitiveOperator op) {
		  add(FieldAccess.path(path, 
				  ElemMatchCondition.and(
						  FieldAccess.path(valPath, new CompareCaseInsensitive(value, op)), 
						  FieldAccess.path(sysPath, new EqualsSingleValueCondition((Comparable) system)))));
	}
	
	public void and() {
		result = AndCondition.and(result, block);
		block = null;
	}
	
	public void or() {
		block = OrCondition.or(block, current);
		current = null;
	}
	
	public void add(Condition cond) {
		if (current == null) current = cond; 
		else current = AndCondition.and(current, cond);
	}
	
	public Condition get() {
		or();and();
		return result;
	}
	
		
}
