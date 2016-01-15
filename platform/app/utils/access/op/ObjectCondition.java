package utils.access.op;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import scala.NotImplementedError;

public class ObjectCondition implements Condition {

	private List<AbstractMap.SimpleEntry<String[], Condition>> checks;
	
	public ObjectCondition(Map<String, Object> restrictions) {
	   checks = new ArrayList<AbstractMap.SimpleEntry<String[],Condition>>();
	   for (String accessKey : restrictions.keySet()) {
		   Object value = restrictions.get(accessKey);
		   
		   String[] path = accessKey.split("\\.");
	       Condition condition;
	       if (value instanceof String) {
	    	  condition = new EqualsSingleValueCondition(value);
	       } else if (value instanceof Map) {
	    	  condition = new ObjectCondition((Map<String,Object>) value); 
	       } else {
	    		throw new NotImplementedError();
	       }
	       
	       checks.add(new AbstractMap.SimpleEntry<String[], Condition>(path, condition));		      
	   }
	}
			
	@Override
	public boolean satisfiedBy(Object inputObj) {
		for (AbstractMap.SimpleEntry<String[], Condition> check : checks) {
			Object obj = inputObj;
			String path[] = check.getKey();
			Condition condition = check.getValue();
            for (int i=0;i<path.length;i++) {
            	obj = access(obj, path[i]);
            }
            if (obj != null && obj instanceof BasicBSONList) {
              BasicBSONList lst = (BasicBSONList) obj;
              boolean anyMatch = false;
              for (Object object : lst) {
            	  if (anyMatch || condition.satisfiedBy(object)) anyMatch = true;     
              }
              if (!anyMatch) return false;
            } else {
               if (!condition.satisfiedBy(obj)) return false;
            }
		}
		return true;
	}
	
	protected Object access(Object obj, String path) {
    	if (obj == null) return null;
    	if (obj instanceof BSONObject) {
    		return ((BSONObject) obj).get(path);
    	}
    	return null;
    }

}
