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

import java.util.Date;
import java.util.Iterator;
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
		for (Iterator<SoftwareChangeLog> it=result.iterator();it.hasNext();) {
			SoftwareChangeLog entry = it.next();
			if (entry.type==null) it.remove();
			entry.published = entry._id.getCreationDate();
			if (entry.type != null && entry.type.equals("S")) {
				entry.title = "Fixed issue '"+entry.changeId+"'";
				entry.description = "";
			}
		}
		return result;	
	}
	
}
