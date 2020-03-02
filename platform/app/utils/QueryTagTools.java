package utils;

import java.util.Map;
import java.util.Set;

import models.enums.UserRole;

public class QueryTagTools {

	public static Map<String, Object> handleSecurityTags(UserRole role, Map<String, Object> properties, Set<String> fields) {
		
		if (role.equals(UserRole.MEMBER)) {
			if (properties.containsKey("data") || properties.containsKey("filter") || fields.contains("data")) {
				if (properties.containsKey("_id")) {
				  AccessLog.log("handleSecurityTags role="+role+" clear-hidden");
				  properties.put("clear-hidden", true);
				} else {
				  AccessLog.log("handleSecurityTags role="+role+" remove-hidden");					
				  properties.put("remove-hidden", true);
				}
			} 
		}
		return properties;
	}
}
