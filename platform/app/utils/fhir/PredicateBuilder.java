package utils.fhir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.access.op.AndCondition;
import utils.access.op.CompareCaseInsensitive;
import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;
import utils.access.op.CompareCondition;
import utils.access.op.Condition;
import utils.access.op.EqualsSingleValueCondition;
import utils.access.op.FieldAccess;
import utils.access.op.OrCondition;
import utils.access.op.CompareCondition.CompareOperator;

public class PredicateBuilder {
  
	private Condition result = null;
	private Condition block = null;
	private Condition current = null;
	
	private boolean indexable = true;
	
	public void addComp(String path, String pred, Object value) {
		CompareOperator op = CompareOperator.LE;
		Condition cond = new CompareCondition((Comparable<Object>) value, op);
		add(FieldAccess.path(path, cond));
	}
	
	
	
	public void addEq(String path, Object value) {
	  add(FieldAccess.path(path, new EqualsSingleValueCondition(value)));
	}
	
	public void addEq(String path, Object value, CompareCaseInsensitiveOperator op) {
		  add(FieldAccess.path(path, new CompareCaseInsensitive(value, op)));
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
