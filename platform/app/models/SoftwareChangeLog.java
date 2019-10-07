package models;

import java.util.Date;
import java.util.List;
import java.util.Set;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;

/**
 * Changelog for Midata Platform
 *
 */
public class SoftwareChangeLog extends Model {
 
	@NotMaterialized
	private static final String collection = "changelog";
	
	public @NotMaterialized final static Set<String> ALL  = 
			 Sets.create("_id", "changeId", "type", "title", "description", "published", "developed");

	/**
	 * Unique id of change
	 */
	public String changeId;
	
	/**
	 * Type of change
	 */
	public String type;
	
	/**
	 * Title of change
	 */
	public String title;
	
	/**
	 * Description
	 */
	public String description;
	
	/**
	 * Publish date on this instance
	 */
	public Date published;
	
	/**
	 * Development date/period
	 */
	public String developed;
	
	/**
	 * Get all recent changes
	 * @return
	 * @throws AppException
	 */
	public static List<SoftwareChangeLog> getAll() throws AppException {
		List<SoftwareChangeLog> result = Model.getAllList(SoftwareChangeLog.class, collection, CMaps.map(), ALL, 100, "_id", -1);
		for (SoftwareChangeLog entry : result) entry.published = entry._id.getCreationDate();
		return result;	
	}
	
}
