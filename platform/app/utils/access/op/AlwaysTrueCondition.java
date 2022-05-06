package utils.access.op;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;

import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;

public class AlwaysTrueCondition implements Condition, Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2687477531757558051L;

	
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public AlwaysTrueCondition() {
		
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		return true;
	}

	@Override
	public Condition optimize() {
		return this;
	}
	
	public Object getValue() {
		return null;
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
	public Object asMongoValue() {	
		return true;
	}
	
	@Override
	public String toString() {				
		return "true";
	}

	@Override
	public boolean isInBounds(Object low, Object high) {
		return true;
	}

	@Override
	public Map<String, Object> asMongoQuery() {
		throw new NullPointerException();
	}

	@Override
	public Condition mongoCompatible() {
		return this;
	}
	
	
	
	
}
