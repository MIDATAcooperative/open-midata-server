package models;

import java.util.Set;

import models.enums.APSSecurityLevel;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class FormatInfo extends Model {

	private @NotMaterialized static final String collection = "formatinfo";
	private @NotMaterialized static final Set<String> ALL = Sets.create("format","visualization");
	
	public String format;
	public ObjectId visualization;
	
	public static FormatInfo getByName(String name) throws InternalServerException {
	
		FormatInfo r = Model.get(FormatInfo.class, collection, CMaps.map("format", name), ALL);
		if (r == null) {
			r = new FormatInfo();
			r.format = name;			
			r.visualization = null;			
		}
		return r;
	}
		
}
