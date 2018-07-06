package utils.access.op;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ElemMatchCondition extends AndCondition {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5762971270313448425L;

	public ElemMatchCondition(Map<String, Object> restrictions) {
		super(restrictions);
	}
	
	@Override
	public Map<String, Object> asMongoQuery() {
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Object> parts = new HashMap<String, Object>();
		for (Condition check : checks) {
			parts.putAll((Map<String, Object>) check.asMongoQuery());
		}
		result.put("$elemMatch", parts);
		return result;
	}
	
	public static Condition and(Condition cond1, Condition cond2) {
    	if (cond1 == null) return cond2;
    	if (cond2 == null) return cond1;
    	if (cond1 instanceof ElemMatchCondition) {
    		((ElemMatchCondition) cond1).checks.add(cond2);
    		return (ElemMatchCondition) cond1;
    	} else if (cond2 instanceof ElemMatchCondition) {
    		((ElemMatchCondition) cond2).checks.add(cond1);
    		return (ElemMatchCondition) cond2;
    	} else {
    		ElemMatchCondition result = new ElemMatchCondition(Collections.EMPTY_MAP);
    		result.checks.add(cond1);
    		result.checks.add(cond2);
    		return result;
    	}
    }
}
