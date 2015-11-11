package models;

import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

import models.enums.APSSecurityLevel;

public class ContentInfo extends Model {

	private @NotMaterialized static final String collection = "contentinfo";
	private @NotMaterialized static final Set<String> ALL = Sets.create("content","security","group");
	
	public String content;	
	public APSSecurityLevel security;
	public String group;
	
	public static String getWildcardName(String name) {
		int p = name.indexOf("/");
		if (p>=0) name = name.substring(0, p);
		return name;
	}
	
	public static ContentInfo getByName(String name) throws InternalServerException {
		int p = name.indexOf("/");
		if (p>=0) name = name.substring(0, p);
		ContentInfo r = Model.get(ContentInfo.class, collection, CMaps.map("content", name), ALL);
		if (r == null) {
			r = new ContentInfo();
			r.content = name;
			r.security = APSSecurityLevel.HIGH;			
			r.group = "other";
		}
		return r;
	}
	
	public static Set<ContentInfo> getByGroups(Set<String> group) throws InternalServerException {
		return Model.getAll(ContentInfo.class, collection, CMaps.map("group", group), ALL);
	}
}
