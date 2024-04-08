/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package models;

import java.util.Map;
import java.util.Set;

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
	private @NotMaterialized static final Set<String> ALL = Sets.create("format","visualization","defaultGroup");
	
	/**
	 * the name of the format this class describes
	 */
	public String format;
	
	/**
	 * the default plugin to use when displaying records with this format
	 */
	public String visualization;
	
	/**
	 * Used for comments
	 */
	public String comment;
	
	/**
	 * Name of default visualization
	 */
	public String visName;
	
	/**
	 * Default group in record tree for format
	 */
	public String defaultGroup;
	
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
