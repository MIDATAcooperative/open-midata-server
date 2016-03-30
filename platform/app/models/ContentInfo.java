package models;

import java.util.HashMap;
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

/**
 * data model for data content types.
 *
 */
public class ContentInfo extends Model {

	private @NotMaterialized static final String collection = "contentinfo";
	private @NotMaterialized static final Set<String> ALL = Sets.create("content","security","group","alias");
	
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
	
	/**
	 * a name of another content value with the same meaning;
	 */
	public String alias;
	
	/**
	 * a comment
	 */
	public String comment;
	
	
	/*public static String getWildcardName(String name) {
		int p = name.indexOf("/");
		if (p>=0) name = name.substring(0, p);
		return name;
	}*/
	
	private static Map<String, ContentInfo> byName = new ConcurrentHashMap<String, ContentInfo>();
	
	public static ContentInfo getByName(String name) throws AppException {
		if (name.startsWith("http://midata.coop ")) name = name.substring("http://midata.coop ".length());
		
		if (!name.startsWith("http:")) { 
		//name = name.toLowerCase();
		int p = name.indexOf("/");
		if (p>=0) name = name.substring(0, p);
		
		ContentInfo r = byName.get(name);
		if (r != null) return r;
		
		r = Model.get(ContentInfo.class, collection, CMaps.map("content", name), ALL);
		if (r == null) {
			r = new ContentInfo();
			r.content = name;
			r.security = APSSecurityLevel.HIGH;			
			r.group = "unknown";
		}
		byName.put(name, r);
        return r;		
		} else {
			ContentInfo r = byName.get(name);
			//if (r!=null && r.alias != null) return getByName(r.alias);
			if (r != null) return r;
			r = Model.get(ContentInfo.class, collection, CMaps.map("content", name), ALL);
			if (r == null) {
				throw new BadRequestException("error.content.unknown", "Content '"+name+"' is not registered with the platform.");
			}
			byName.put(name, r);
			//if (r.alias != null) return getByName(r.alias);
			return r;
		}
				
	}
	
	public static Set<ContentInfo> getByGroups(Set<String> group) throws InternalServerException {
		return Model.getAll(ContentInfo.class, collection, CMaps.map("group", group), ALL);
	}
	
	public static Set<ContentInfo> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(ContentInfo.class, collection, properties, fields);
	}
}
