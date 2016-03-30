package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model class for the different record formats.
 *
 */
public class FormatInfo extends Model {

	private @NotMaterialized static final String collection = "formatinfo";
	private @NotMaterialized static final Set<String> ALL = Sets.create("format","visualization");
	
	/**
	 * the name of the format this class describes
	 */
	public String format;
	
	/**
	 * the default plugin to use when displaying records with this format
	 */
	public ObjectId visualization;
	
	/**
	 * Used for comments
	 */
	public String comment;
	
	/**
	 * Name of default visualization
	 */
	public String visName;
	
	public static FormatInfo getByName(String name) throws InternalServerException {
	
		FormatInfo r = Model.get(FormatInfo.class, collection, CMaps.map("format", name), ALL);
		if (r == null) {
			r = new FormatInfo();
			r.format = name;			
			r.visualization = null;			
		}
		return r;
	}
	
	public static Set<FormatInfo> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(FormatInfo.class, collection, properties, fields);
	}
		
}
