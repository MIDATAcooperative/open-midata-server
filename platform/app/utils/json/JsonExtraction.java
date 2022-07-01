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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import utils.json.JsonValidation.JsonValidationException;

public class JsonExtraction {

	/**
	 * Extracts a set with elements guaranteed to be strings.
	 */
	public static Set<String> extractStringSet(JsonNode json) throws JsonValidationException {
		if (json == null) return null;
		if (json.isTextual()) {
			String txt = json.asText();
			if (txt.length() > JsonValidation.MAX_STRING_LENGTH) throw new JsonValidationException("error.toolong.field","JSON too long");
			return Collections.singleton(txt);
		}
		Set<String> set = new HashSet<String>();
		for (JsonNode jsonNode : json) {
			String txt = jsonNode.asText();
			if (txt.length() > JsonValidation.MAX_STRING_LENGTH) throw new JsonValidationException("error.toolong.field","JSON too long");
			set.add(txt);
		}
		return set;
	}
	
	/**
	 * Extracts a set with elements guaranteed to be strings.
	 */
	public static <T extends Enum<T>> Set<T> extractEnumSet(JsonNode json, String field, Class<T> en) throws JsonValidationException {
		json = json.get(field);
		if (json == null) return null;		
		Set<T> set = new HashSet<T>();
		for (JsonNode jsonNode : json) {
			String val = jsonNode.asText().toUpperCase();
			try {
			   T result = (T) Enum.valueOf(en, val);
			   set.add(result);
			} catch (IllegalArgumentException e) {
			  throw new JsonValidationException("error.validation.enum", "Value of parameter '" + field + "' has none of the valid values.");		
			}			
		}
		return set;
	}
	
	/**
	 * Extracts a set with elements guaranteed to be strings.
	 */
	public static Set<MidataId> extractMidataIdSet(JsonNode json) {
		Set<MidataId> set = new HashSet<MidataId>();
		for (JsonNode jsonNode : json) {
			set.add(new MidataId(jsonNode.asText()));
		}
		return set;
	}

	/**
	 * Extracts a general map.
	 */
	public static Map<String, Object> extractMap(JsonNode json) throws JsonValidationException {
		Map<String, Object> map = new HashMap<String, Object>();
		Iterator<Entry<String, JsonNode>> iterator = json.fields();
		while (iterator.hasNext()) {
			Entry<String, JsonNode> cur = iterator.next();
			String key = cur.getKey();
			if (key.startsWith("!!!")) key = "$"+key.substring(3);
			String ck = key.trim().toLowerCase();	
			if (!ck.startsWith("$where")) { 
			  map.put(key, extract(cur.getValue()));
			}
		}
		return map;
	}
	
	/**
	 * Extracts a general map.
	 */
	public static Map<String, String> extractStringMap(JsonNode json) {
		Map<String, String> map = new HashMap<String, String>();
		Iterator<Entry<String, JsonNode>> iterator = json.fields();
		while (iterator.hasNext()) {
			Entry<String, JsonNode> cur = iterator.next();
			map.put(cur.getKey(), cur.getValue().asText());
		}
		return map;
	}


	/**
	 * Extracts a general set.
	 */
	public static Set<Object> extractSet(JsonNode json) throws JsonValidationException  {
		Set<Object> set = new HashSet<Object>();
		for (JsonNode element : json) {
			set.add(extract(element));
		}
		return set;
	}
	
	/**
	 * Extracts a general list.
	 */
	public static List<Object> extractList(JsonNode json) throws JsonValidationException  {
		List<Object> set = new ArrayList<Object>();
		for (JsonNode element : json) {
			set.add(extract(element));
		}
		return set;
	}
	
	/**
	 * Extracts a list with elements guaranteed to be strings.
	 */
	public static List<String> extractStringList(JsonNode json) throws JsonValidationException {
		if (json == null) return null;
		if (json.isTextual()) {
			String txt = json.asText();
			if (txt.length() > JsonValidation.MAX_STRING_LENGTH) throw new JsonValidationException("error.toolong.field","JSON too long");
			return Collections.singletonList(txt);
		}
		List<String> list = new ArrayList<String>();
		for (JsonNode jsonNode : json) {
			String txt = jsonNode.asText();
			if (txt.length() > JsonValidation.MAX_STRING_LENGTH) throw new JsonValidationException("error.toolong.field","JSON too long");
			list.add(txt);
		}
		return list;
	}

	/**
	 * Extracts any data type.
	 */
	private static Object extract(JsonNode json) throws JsonValidationException  {
		if (json.isObject() && json.has("$oid")) {
			return new MidataId(json.get("$oid").asText());
		} else if (json.isObject()) {
			return extractMap(json);
		} else if (json.isArray()) {
			return extractSet(json);
		} else if (json.isBoolean()) {
			return json.asBoolean();
		} else if (json.isLong()) {
			return json.asLong();
		} else if (json.isInt()) {
			return json.asInt();
		} else if (json.isNumber()) {
			return json.asDouble();
		} else {
			String txt = json.asText();
			if (txt != null && txt.length() > JsonValidation.MAX_STRING_LENGTH) throw new JsonValidationException("error.toolong.field","JSON too long");
			return txt;
		}
	}

}
