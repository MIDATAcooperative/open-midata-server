package utils.json;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import utils.exceptions.InternalServerException;

/**
 * functions for serialization JSON so that only requested fields are returned
 *
 */
public class JsonOutput {
	
	public static String toJson(Object o, String filtered, Set<String> fields) throws InternalServerException {	   	    
		if (!fields.contains("_id")) {
			if (!(fields instanceof HashSet)) fields = new HashSet<String>(fields);
			fields.add("_id");
		}
		
	    SimpleBeanPropertyFilter filter =
	            new SimpleBeanPropertyFilter.FilterExceptFilter(fields);
	    SimpleFilterProvider fProvider = new SimpleFilterProvider();
	    fProvider.addFilter(filtered, filter);

	    ObjectMapper mapper = CustomObjectMapper.me;	  

	    try {
	      String json = mapper.writer(fProvider).writeValueAsString(o);
	      return json;
	    } catch (JsonProcessingException e) {
	    	throw new InternalServerException("error.internal", e);
	    }
	}
	
	public static String toJson(Object o, Map<String, Set<String>> fieldMap) throws InternalServerException {
		
		SimpleFilterProvider fProvider = new SimpleFilterProvider();
		
		for (Map.Entry<String, Set<String>> entry : fieldMap.entrySet()) {
			Set<String> fields = entry.getValue();
			if (!fields.contains("_id")) fields.add("_id");
			SimpleBeanPropertyFilter filter =
		            new SimpleBeanPropertyFilter.FilterExceptFilter(fields);
			fProvider.addFilter(entry.getKey(), filter);
		}
			   
	    ObjectMapper mapper = CustomObjectMapper.me;	  

	    try {
	      String json = mapper.writer(fProvider).writeValueAsString(o);
	      return json;
	    } catch (JsonProcessingException e) {
	    	throw new InternalServerException("error.internal", e);
	    }
	}
	
	public static JsonNode toJsonNode(Object o, String filtered, Set<String> fields) throws InternalServerException {	   	    
		if (!fields.contains("_id")) fields.add("_id");
		
	    SimpleBeanPropertyFilter filter =
	            new SimpleBeanPropertyFilter.FilterExceptFilter(fields);
	    SimpleFilterProvider fProvider = new SimpleFilterProvider();
	    fProvider.addFilter(filtered, filter);

	    ObjectMapper mapper = CustomObjectMapper.me.copy();
	    mapper.setFilters(fProvider);
	    
	    return mapper.valueToTree(o);	    	      	   
	}

}
