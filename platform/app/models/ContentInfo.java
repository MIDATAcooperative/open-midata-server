package models;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import models.enums.APSSecurityLevel;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model for data content types.
 *
 */
public class ContentInfo extends Model {

	private @NotMaterialized static final String collection = "contentinfo";
	private @NotMaterialized static final Set<String> ALL = Sets.create("content","security","group");
	
	/**
	 * the name of the content type this class describes
	 */
	public String content;	
	
	/**
	 * the level of security this consent type should be handeled with
	 */
	public APSSecurityLevel security;
	
	/**
	 * the name of the group where records with this content types should be placed in in the record tree
	 */
	public String group;
	
	public static String getWildcardName(String name) {
		int p = name.indexOf("/");
		if (p>=0) name = name.substring(0, p);
		return name;
	}
	
	private static Map<String, ContentInfo> byName = new ConcurrentHashMap<String, ContentInfo>();
	public static ContentInfo getByName(String name) throws InternalServerException {
		int p = name.indexOf("/");
		if (p>=0) name = name.substring(0, p);
		
		ContentInfo r = byName.get(name);
		if (r != null) return r;
		
		r = Model.get(ContentInfo.class, collection, CMaps.map("content", name), ALL);
		if (r == null) {
			r = new ContentInfo();
			r.content = name;
			r.security = APSSecurityLevel.HIGH;			
			r.group = "other";
		}
		byName.put(name, r);
		
		return r;
	}
	
	public static Set<ContentInfo> getByGroups(Set<String> group) throws InternalServerException {
		return Model.getAll(ContentInfo.class, collection, CMaps.map("group", group), ALL);
	}
	
	public static Set<ContentInfo> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(ContentInfo.class, collection, properties, fields);
	}
}
