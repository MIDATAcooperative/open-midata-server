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
	
	public static ContentInfo getByName(String name) throws AppException {		
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
	
	public static void setRecordCodeAndContent(Record record, Set<String> code, String content) throws JsonValidationException, AppException {
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
				  else if (!content.equals(ncontent)) throw new JsonValidationException("error.field", "Record codes do not provide same content '"+code.toString()+"'!");
			  }
			}
			if (content == null) throw new JsonValidationException("error.field", "Unknown record codes '"+code.toString()+"'!");
		} else if (content != null ){
			ContentInfo ci = ContentInfo.getByName(content);
			content = ci.content;
			code = Collections.singleton(ci.defaultCode);			
		} else {
			throw new BadRequestException("error.field", "Neither code nor content-type available for record.");
		}
		record.code = code;
		record.content = content;
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
