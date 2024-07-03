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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ContentCode;
import models.ContentInfo;
import models.FormatInfo;
import models.GroupContent;
import models.MidataId;
import models.Record;
import models.RecordGroup;
import models.enums.APSSecurityLevel;
import utils.access.Query;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.sync.Instances;

public class ContentTypeTools {

	public static void setRecordCodeAndContent(AccessContext context, Record record, Set<String> code, String content, String display) throws AppException {
		setRecordCodeAndContent(context, record, code, content ,display, null);
	}
	
	public static void setRecordCodeAndContent(AccessContext context, Record record, Set<String> code, String content, String display, String category) throws AppException {
		MidataId pluginId = context.getUsedPlugin();
		
		try {
		
		if (content != null && ContentInfo.isCoding(content)) {
			code = Collections.singleton(content);
			content = null;
		}
		if (code != null && !code.isEmpty()) {
			content = null;
			for (String c : code) {
			  String ncontent = ContentCode.getContentForSystemCode(c);
			  if (ncontent != null) {
				  if (content == null) content = ncontent;
				  else if (!content.equals(ncontent)) {
					  throw new PluginException(pluginId, "error.plugin", "A FHIR resource send by this plugin contains multiple codes. The resulting content type is ambiguous. Code: '"+code.toString()+"'");
				  }
			  }
			}
			
			if (content==null && display!=null) {
				AccessLog.log("Try to dynamically add display="+display+" code="+code);
				String splitted[] = code.iterator().next().split("\\s");
				if (splitted.length == 2 && splitted[0].startsWith("http")) {
				  String groupName = determineTargetGroup(context, record.format, category);
				  if (groupName != null) {
				    content = generateContent(context, splitted[0], splitted[1], display, record.format, groupName, category);
				  }
				}
			} 
			
			if (content == null) 			
				throw new PluginException(pluginId, "error.plugin", "A FHIR resource send by this plugin contains a code that has not been registered with the platform: '"+code.toString()+"'");	
		} else if (content != null ){
			ContentInfo ci = ContentInfo.getByName(content);
			content = ci.content;
			code = Collections.singleton(ci.defaultCode);			
		} else {
			throw new PluginException(pluginId, "error.plugin", "A FHIR resource send by this plugin does not contain any code to be used as content-type. Please recheck if FHIR resources are compatible to the FHIR specification. Record format is '"+record.format+"'");
		}
		record.code = code;
		record.content = content;
		} catch (BadRequestException e) {
			throw new PluginException(pluginId, "error.plugin", e.getMessage());
		}
	}
	
	public static String getContentForSystemCode(AccessContext context, String system, String code) throws PluginException {
		switch(system) {
		case "http://loinc.org" : return "loinc/"+code;
		case "http://snomed.info/sct" : return "snomed/"+code;
		}
		if (system.startsWith("http://")) system = system.substring("http://".length());
		else if (system.startsWith("https://")) system = system.substring("https://".length());
		else throw new PluginException(context.getUsedPlugin(), "error.plugin", "Code system ´"+system+"´ not supported for auto registration.");
		system = system.replace('.', '_').replace('/', '_');
		return system+"/"+code;
	}
	
	 public static String findDynamicGroup(Map<String, Object> properties, String groupSystem, String format) throws BadRequestException, AppException {
	         	
	    	if (properties.containsKey("format") && Query.getRestriction(properties.get("format"), "format").contains(format)) {
	    	   
	    		String groupSystem1 = properties.containsKey("group-system") ? properties.get("group-system").toString() : "v1";
	    		boolean isDynamic = properties.containsKey("group-dynamic") ? "extend".equals(properties.get("group-dynamic").toString()) : false;
	    		Object group = properties.containsKey("group") ? properties.get("group") : null;
	    		if (group!=null && group instanceof Collection) group = ((Collection) group).iterator().next();
	            AccessLog.log("TEST dyn="+isDynamic+" gs="+groupSystem1+" group="+group);     		        		
	    		if (isDynamic && groupSystem1.equals(groupSystem) && group instanceof String) return group.toString();
	    	} 
	    	
	    	if (properties.containsKey("$or")) {
	    	   
	    		Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) properties.get("$or");
	    	
	    		for (Map<String, Object> part : parts) {
	    			String partResult = findDynamicGroup(part, groupSystem, format);
	    			if (partResult != null) return partResult;
	    		}
	    	
	    	}
	    	return null;
	}
	 
	public static boolean isSubGroupOf(String groupSystem, String sgroup, String pgroup) throws AppException {
		if (sgroup.equals(pgroup)) return true;
		RecordGroup gr = RecordGroup.getBySystemPlusName(groupSystem, sgroup);
		if (gr == null) return false;
		if (gr.parent != null && gr.parent.equals(pgroup)) return true;
		if (gr.parent != null) return isSubGroupOf(groupSystem, gr.parent, pgroup);
		return false;
	}
	
	public static String determineTargetGroup(AccessContext context, String format, String category) throws AppException {
		// from access filter
		String groupFromFilter = findDynamicGroup(context.getAccessRestrictions(), "v1", format);
		if (groupFromFilter == null) {
			AccessLog.log("no dynamic target group in filter for:", format);
			return null;
		}
		
		// from category
		String groupFromCategory = null;
		if (category != null) {
    		switch(category) {
    		case "laboratory" : groupFromCategory = "health/laboratory";break;
    		}
		}
		
		// from format
		FormatInfo finf = FormatInfo.getByName(format);
		String groupFromFormat = finf.defaultGroup;
		if (groupFromFormat == null) {
			AccessLog.log("no dynamic target group in format for:", format);
			return null;
		}
		
		// determine winner
		String winner = null;
		
		if (isSubGroupOf("v1", groupFromFilter, groupFromFormat)) winner = groupFromFilter;
		else if (isSubGroupOf("v1", groupFromFormat, groupFromFilter)) winner = groupFromFormat;
		else {
			AccessLog.log("dynamic groups dont fit. filter=", groupFromFilter," format=",groupFromFormat);
		    return null;
		}
		
		if (groupFromCategory==null || groupFromCategory.equals(winner)) return winner;
		
		if (isSubGroupOf("v1", winner, groupFromCategory)) return winner;
		else if (isSubGroupOf("v1", groupFromCategory, winner)) return groupFromCategory;
		else {
			AccessLog.log("dynamic groups dont fit. winner=", winner," category=",groupFromCategory);
		    return null;
		}
		
		
	}
	
	public static String generateContent(AccessContext context, String system, String code, String display, String format, String groupName, String category) throws AppException {
		ContentInfo ci = new ContentInfo();
		ci._id = new MidataId();
		ci.autoAddedAt = new Date();
		ci.autoAddedBy = context.getUsedPlugin();
		ci.content = getContentForSystemCode(context, system, code);
		ci.label = new HashMap<String, String>();
		ci.label.put("en", display);
		ci.defaultCode = system+" "+code;
		ci.lastUpdated = System.currentTimeMillis();
		ci.security = APSSecurityLevel.MEDIUM;
		ci.resourceType = format;
		ci.category = category;
			
		ContentCode co = new ContentCode();
		co._id = new MidataId();
		co.code = code;
		co.content = ci.content;
		co.lastUpdated = System.currentTimeMillis();
		co.system = system;
	    co.display = display;
	    
	    GroupContent gc = new GroupContent();
	    gc._id = new MidataId();
	    gc.content = ci.content;
	    gc.lastUpdated = System.currentTimeMillis();
	    gc.system = "v1";
	    gc.name = groupName;
	   
		
		ContentInfo.add(ci);
		ContentCode.add(co);
		GroupContent.add(gc);
		
		RecordGroup.reload(ci, gc);
		Instances.cacheClear("content", ci._id);
		
		return ci.content;
		
	}
}
