package utils.json;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonExtraction {

	/**
	 * Extracts a set with elements guaranteed to be strings.
	 */
	public static Set<String> extractStringSet(JsonNode json) {
		Set<String> set = new HashSet<String>();
		for (JsonNode jsonNode : json) {
			set.add(jsonNode.asText());
		}
		return set;
	}

	/**
	 * Extracts a general map.
	 */
	public static Map<String, Object> extractMap(JsonNode json) {
		Map<String, Object> map = new HashMap<String, Object>();
		Iterator<Entry<String, JsonNode>> iterator = json.fields();
		while (iterator.hasNext()) {
			Entry<String, JsonNode> cur = iterator.next();
			map.put(cur.getKey(), extract(cur.getValue()));
		}
		return map;
	}

	/**
	 * Extracts a general set.
	 */
	public static Set<Object> extractSet(JsonNode json) {
		Set<Object> set = new HashSet<Object>();
		for (JsonNode element : json) {
			set.add(extract(element));
		}
		return set;
	}

	/**
	 * Extracts any data type.
	 */
	private static Object extract(JsonNode json) {
		if (json.isObject() && json.has("$oid")) {
			return new ObjectId(json.get("$oid").asText());
		} else if (json.isObject()) {
			return extractMap(json);
		} else if (json.isArray()) {
			return extractSet(json);
		} else if (json.isBoolean()) {
			return json.asBoolean();
		} else {
			return json.asText();
		}
	}

}
