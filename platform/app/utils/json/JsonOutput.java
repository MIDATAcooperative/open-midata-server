/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	    	throw new InternalServerException("error.internal", "Json error:"+e.getMessage());
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
	    	throw new InternalServerException("error.internal", "Json error:"+e.getMessage());
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
