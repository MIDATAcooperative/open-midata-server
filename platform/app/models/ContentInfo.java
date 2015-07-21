package models;

import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;

import models.enums.APSSecurityLevel;

public class ContentInfo extends Model {

	private static final String collection = "contentinfo";
	private static final Set<String> ALL = Sets.create("content","security","group");
	
	public String content;	
	public APSSecurityLevel security;
	public String group;
	
	public static ContentInfo getByName(String name) throws ModelException {
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
	
	public static Set<ContentInfo> getByGroups(Set<String> group) throws ModelException {
		return Model.getAll(ContentInfo.class, collection, CMaps.map("group", group), ALL);
	}
}
