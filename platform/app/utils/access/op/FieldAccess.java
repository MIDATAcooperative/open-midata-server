package utils.access.op;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

/**
 * Field access operator for mongo conditions
 *
 */
public class FieldAccess implements Condition {

	private String field;
	private Condition cond;
	
	/**
	 * Constructor 
	 * @param field name of field that should be accessed on the data object
	 * @param cond sub condition that will be applied
	 */
	public FieldAccess(String field, Condition cond) {
		this.field = field;
		this.cond = cond;
	}
		
	@Override
	public boolean satisfiedBy(Object inputObj) {
	
		if (inputObj == null) return cond.satisfiedBy(null);
        Object res = access(inputObj, field);
        if (res == null) return cond.satisfiedBy(null);
           
        if (res instanceof BasicBSONList) {
           BasicBSONList lst = (BasicBSONList) res;
           
           for (Object object : lst) {
             if (cond.satisfiedBy(object)) return true;     
           }
           
           return false;
		}
		return cond.satisfiedBy(res);
	}
	
	/**
	 * access field of provided object
	 * @param obj object to access. May be null.
	 * @param path Name of field to access
	 * @return field of object or null if field does not exist on given object
	 */
	protected Object access(Object obj, String path) {
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

}