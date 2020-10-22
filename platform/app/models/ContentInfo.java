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

package models;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import models.enums.APSSecurityLevel;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.json.JsonValidation.JsonValidationException;

/**
 * data model for data content types.
 *
 */
public class ContentInfo extends Model {

	private @NotMaterialized static final String collection = "contentinfo";
	private @NotMaterialized static final Set<String> ALL = Sets.create("content", "defaultCode", "security","label", "comment", "source");
	
	/**
	 * the name of the content type this class describes
	 */
	public String content;	
	
	/**
	 * the default system + code used for this content type
	 */
	public String defaultCode;
	
	/**
	 * the level of security this consent type should be handeled with
	 */
	public APSSecurityLevel security;
	
	/**
	 * a map from language to label containing a label for each supported language
	 */
	public Map<String, String> label;
	
	/**
	 * default FHIR resource type if storable in FHIR
	 */
	public String resourceType;
	
	/**
	 * default subtype value
	 */
	public String subType;
	
	/**
	 * (optional) default unit 
	 */
	public String defaultUnit;
	
	/**
	 * category for FHIR types
	 */
	public String category;
	
	/**
	 * (optional) source
	 */
	public String source;
				
	/**
	 * a comment
	 */
	public String comment;
	
	
	private @NotMaterialized static Map<String, ContentInfo> byName = new ConcurrentHashMap<String, ContentInfo>();
	
	public static ContentInfo getByName(String name) throws BadRequestException, InternalServerException {		
			ContentInfo r = byName.get(name);		
			if (r != null) return r;
			r = Model.get(ContentInfo.class, collection, CMaps.map("content", name), ALL);
			if (r == null) {
				throw new BadRequestException("error.unknown.content", "Content '"+name+"' is not registered with the platform.");
			}
			byName.put(name, r);			
			return r;					
	}
	
	public static void clear() {
		byName.clear();
	}
	
	public static void setRecordCodeAndContent(MidataId pluginId, Record record, Set<String> code, String content) throws PluginException, InternalServerException {
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
	
	public static boolean isCoding(String name) {
		return name.indexOf(' ')>0;
	}
		
	
	public static Set<ContentInfo> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(ContentInfo.class, collection, properties, fields);
	}
	
	public static void add(ContentInfo ci) throws InternalServerException {
		Model.insert(collection, ci);				
	}
	
	public static void upsert(ContentInfo cc) throws InternalServerException {
	    Model.upsert(collection, cc);
	}
	  
	public static void delete(MidataId ccId) throws InternalServerException {			
	    Model.delete(ContentInfo.class, collection, CMaps.map("_id", ccId));
	}
}
