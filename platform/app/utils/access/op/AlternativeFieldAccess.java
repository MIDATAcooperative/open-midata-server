package utils.access.op;

import java.io.Serializable;

import org.bson.BSONObject;

public class AlternativeFieldAccess implements Condition, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6564693337792439980L;
	private String[][] field;
	private String fieldName;
	private Condition cond;
	
	/**
	 * Constructor 
	 * @param field name of field that should be accessed on the data object
	 * @param cond sub condition that will be applied
	 */
	public AlternativeFieldAccess(String field, Condition cond) {
		this.fieldName = field;
		String[] fields = field.split("\\|");		
		this.field = new String[fields.length][];
		for (int i=0;i<fields.length;i++) this.field[i] = fields[i].split("\\.");
		this.cond = cond;
	}
	
	
	
	
	
	public Condition getCondition() {
		return cond;
	}
	
	public String getField() {
		return fieldName;
	}
		
	@Override
	public boolean satisfiedBy(Object inputObj) {
	
		if (inputObj == null) return false;
		
		for (int i=0;i<field.length;i++) {
			Object test = inputObj;
			for (int j=0;j<field[i].length;j++) {
				test = access(test, field[i][j]);
				if (test == null) break;
			}
			if (test != null) {
				return cond.satisfiedBy(test);
			}
		}
		return cond.satisfiedBy(null);
	}
	
	/**
	 * access field of provided object
	 * @param obj object to access. May be null.
	 * @param path Name of field to access
	 * @return field of object or null if field does not exist on given object
	 */
	private Object access(Object obj, String path) {
    	if (obj == null) return null;
    	if (obj instanceof BSONObject) {
    		return ((BSONObject) obj).get(path);
    	}
    	return null;
    }

	@Override
	public Condition optimize() {
		cond = cond.optimize();
		return this;
	}

	@Override
	public Condition indexValueExpression() {		
		return null;
	}

	
	@Override
	public String toString() {		
		return fieldName + ":" + cond.toString();
	}

	@Override
	public boolean isInBounds(Object low, Object high) {		
		return false;
	}



	@Override
	public Condition indexExpression() {	
		Condition expr = cond.indexValueExpression();
		if (expr != null) return new AlternativeFieldAccess(fieldName, expr);
		return null;
	}



	@Override
	public Object asMongoQuery() {		
		return null;
	}

	
	
}
