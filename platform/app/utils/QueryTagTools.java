/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
