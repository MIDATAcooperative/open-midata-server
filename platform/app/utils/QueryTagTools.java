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

package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import models.Record;
import models.enums.UserRole;
import utils.access.DBRecord;
import utils.access.RecordConversion;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

public class QueryTagTools {

	public static String SECURITY_READONLY = "security:read-only";
	public static String SECURITY_RELIABLE = "security:reliable";
	public static String SECURITY_NODELETE = "security:no-delete";
	public static String SECURITY_HIDDEN = "security:hidden";
	public static String SECURITY_PUBLIC = "security:public";
	public static String SECURITY_GENERATED = "security:generated";
	public static String SECURITY_PLATFORM_MAPPED = "security:platform-mapped";
	public static String SECURITY_LOCALCOPY = "security:local-copy";
	public static String SECURITY_NOT_PSEUDONYMISABLE = "security:not-pseudonymisable";
	
	public static List<SecurityTag> tags;
	
	public static Map<String, Object> handleSecurityTags(UserRole role, Map<String, Object> properties, Set<String> fields) {
		
		if (role.equals(UserRole.MEMBER)) {
			if (properties.containsKey("data") || properties.containsKey("filter") || fields.contains("data")) {
				if (properties.containsKey("_id")) {
				  AccessLog.log("handleSecurityTags role=",role.toString()," clear-hidden");
				  properties.put("clear-hidden", true);
				} else {
				  AccessLog.log("handleSecurityTags role=",role.toString()," remove-hidden");					
				  properties.put("remove-hidden", true);
				}
			} 
		}
		return properties;
	}

	public static void checkTagsForUpdate(AccessContext context, Record record, DBRecord rec) throws AppException {
		Set<String> oldTags = RecordConversion.instance.getTags(rec);
		Set<String> newTags = record.tags;
		if (newTags==null) newTags = Collections.emptySet();
		if (oldTags==null) oldTags = Collections.emptySet();
		
		if (oldTags.contains(SECURITY_READONLY)) throw new BadRequestException("error.plugin", "Tried to write to read-only resource.");
		if (oldTags.contains(SECURITY_RELIABLE) && !newTags.contains(SECURITY_RELIABLE)) throw new BadRequestException("error.plugin", "Cannot reduce reliability.");
		if (oldTags.contains(SECURITY_PUBLIC) != newTags.contains(SECURITY_PUBLIC)) throw new BadRequestException("error.plugin", "Cannot change public security tag on existing resource.");
		if (oldTags.contains(SECURITY_PLATFORM_MAPPED) && !newTags.contains(SECURITY_PLATFORM_MAPPED)) throw new BadRequestException("error.plugin", "Cannot change platform-mapped security tag on existing resource.");
		
		rec.meta.put("tags", record.tags);
	}
	
	public static String getTagForCoding(String system, String code) {
		for (SecurityTag tag : tags) {
			if (tag.system.equals(system) && tag.code.equals(code)) return tag.internal;
		}
		return null;
	}
	
	public static Pair<String, String> getSystemCodeForTag(String internal) {
		for (SecurityTag tag : tags) {
			if (tag.internal.equals(internal)) return Pair.of(tag.system, tag.code);
		}
		return null;
	}
	
	static {
		tags = new ArrayList<SecurityTag>();
		tags.add(new SecurityTag(SECURITY_HIDDEN, "http://terminology.hl7.org/CodeSystem/v3-ActCode", "PHY", null));
		tags.add(new SecurityTag(SECURITY_PUBLIC, "http://terminology.hl7.org/CodeSystem/v3-Confidentiality", "U", null));
		tags.add(new SecurityTag(SECURITY_PUBLIC, "http://midata.coop/codesystems/security", "public", null));
		tags.add(new SecurityTag(SECURITY_GENERATED, "http://midata.coop/codesystems/security", "generated", null));
		tags.add(new SecurityTag(SECURITY_LOCALCOPY, "http://midata.coop/codesystems/security", "local-copy", null));
		tags.add(new SecurityTag(SECURITY_READONLY, "http://terminology.hl7.org/CodeSystem/v3-ActCode", "INFOREADONLY", null));
		tags.add(new SecurityTag(SECURITY_NODELETE, "http://midata.coop/codesystems/security", "no-delete", null));
		tags.add(new SecurityTag(SECURITY_RELIABLE, "http://terminology.hl7.org/CodeSystem/v3-ObservationValue", "reliable", null));
		tags.add(new SecurityTag(SECURITY_NOT_PSEUDONYMISABLE, "http://midata.coop/codesystems/security", "not-pseudonymisable", null));
		tags.add(new SecurityTag(SECURITY_PLATFORM_MAPPED, "http://midata.coop/codesystems/security", "platform-mapped", null));
	}
		
}

class SecurityTag {
  
  final String system;
  final String code;
  final String display;
  final String internal;
  
  SecurityTag(String internal, String system, String code, String display) {
	  this.system = system;
	  this.code = code;
	  this.display = display;
	  this.internal = internal;
  }
}
