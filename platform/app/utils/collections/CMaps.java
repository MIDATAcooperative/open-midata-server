package utils.collections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CMaps {

	public static NChainedMap<String, Object> map(String key, Object value) {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		result.put(key, value);
		return result;
	}
	
	public static NChainedMap<String, Object> map(Map<String, Object> properties) {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		result.putAll(properties);
		return result;
	}
			
	public static NChainedMap<String, Object> map() {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();		
		return result;
	}
	
	public static NChainedMap<String, Object> or(Map<String, Object>... properties) {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();		
		result.put("$or", properties);
		return result;
	}
	
	public static NChainedMap<String, Object> and(Map<String, Object>... properties) {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();		
		result.put("$and", properties);
		return result;
	}
	
	public static NChainedMap<String, Object> mapNotEmpty(String key, Object value) {		
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		if (value != null && ! "".equals(value.toString())) result.put(key, value);
		return result;
	}
}
