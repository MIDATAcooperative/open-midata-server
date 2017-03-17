package utils.access.op;

import java.util.HashMap;
import java.util.Map;

public class ExistsCondition implements Condition {

	private boolean shouldExist;
	
	/**
	 * Constructor
	 * @param val value to compare target object with
	 */
	public ExistsCondition(boolean shouldExist) {
		this.shouldExist = shouldExist;
	}
	
	@Override
	public boolean satisfiedBy(Object obj) {
		return shouldExist ? (obj != null) : (obj == null);
	}

	@Override
	public Condition optimize() {
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
	public boolean isInBounds(Object low, Object high) {
		return true;
	}

	@Override
	public Object asMongoQuery() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("$exists", shouldExist);
		return result;
	}
	
	@Override
	public String toString() {		
		return "{ $exists : "+shouldExist+" }";
	}
	
	
}
