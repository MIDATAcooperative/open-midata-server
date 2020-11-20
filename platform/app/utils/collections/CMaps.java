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

package utils.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for creating String to Object Maps. 
 *
 */
public class CMaps {

	/**
	 * Create new Map<String,Object> with initialized with given mapping
	 * @param key initial key
	 * @param value initial value
	 * @return new Map
	 */
	public static NChainedMap<String, Object> map(String key, Object value) {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		result.put(key, value);
		return result;
	}
	
	/**
	 * Create new Map<String,Object> by copying an existing map.
	 * @param properties existing map to be copied
	 * @return new Map
	 */
	public static NChainedMap<String, Object> map(Map<String, Object> properties) {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		result.putAll(properties);
		return result;
	}
			
	/**
	 * Create new empty Map<String,Object>
	 * @return new Map
	 */
	public static NChainedMap<String, Object> map() {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();		
		return result;
	}
	
	/**
	 * Create new Map<String,Object> by combining multiple existing Maps using mongoDB like "or" snytax 
	 * @param properties maps to be combined using "or" operator
	 * @return new Map
	 */
	public static NChainedMap<String, Object> or(Map<String, Object>... properties) {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		List itms = new ArrayList();
		for (Map<String,Object> part : properties) itms.add(part);
		result.put("$or", itms);
		return result;
	}
	
	/**
	 * Create new Map<String,Object> by combining multiple existing Maps using mongoDB like "and" syntax
	 * @param properties maps to be combined using "and" operator
	 * @return
	 */
	public static NChainedMap<String, Object> and(Map<String, Object>... properties) {
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		List itms = new ArrayList();
		for (Map<String,Object> part : properties) itms.add(part);
		result.put("$and", itms);
		return result;
	}
	
	/**
	 * Create new Map<String,Object> and add key, value entry if value is not null 
	 * @param key initial key
	 * @param value initial value
	 * @return new Map
	 */
	public static NChainedMap<String, Object> mapNotEmpty(String key, Object value) {		
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		if (value != null && ! "".equals(value.toString())) result.put(key, value);
		return result;
	}
	
	public static NChainedMap<String, Object> mapPositive(String key, long value) {		
		NChainedMap<String,Object> result = new NChainedMap<String,Object>();
		if (value > 0) result.put(key, value);
		return result;
	}
}
