package utils.json;

import java.util.Set;

import utils.exceptions.InternalServerException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * functions for serialization JSON so that only requested fields are returned
 *
 */
public class JsonOutput {
	
	public static String toJson(Object o, String filtered, Set<String> fields) throws InternalServerException {	   	    
		fields.add("_id");
		
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
	
	public static JsonNode toJsonNode(Object o, String filtered, Set<String> fields) throws InternalServerException {	   	    
		fields.add("_id");
		
	    SimpleBeanPropertyFilter filter =
	            new SimpleBeanPropertyFilter.FilterExceptFilter(fields);
	    SimpleFilterProvider fProvider = new SimpleFilterProvider();
	    fProvider.addFilter(filtered, filter);

	    ObjectMapper mapper = CustomObjectMapper.me.copy();
	    mapper.setFilters(fProvider);
	    
	    return mapper.valueToTree(o);	    	      	   
	}

}
