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

import java.util.Collections;
import java.util.Set;

import models.enums.IconUse;
import models.enums.PluginStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Icons for Apps and Plugins
 *
 */
public class PluginIcon extends Model {

	@NotMaterialized
	private static final String collection = "pluginicons";
	
	@NotMaterialized
	public static final Set<String> FIELDS = Collections.unmodifiableSet(Sets.create("_id", "plugin", "status", "use", "contentType", "data"));
	
	/**
	 * Internal name of plugin
	 */
	public String plugin;
	
	/**
	 * Status of plugin image
	 */
	public PluginStatus status;
	
	/**
	 * Where is the icon used
	 */
	public IconUse use;
	
	/**
	 * ContentType of icon
	 */
	public String contentType;
	
	/**
	 * Image data
	 */
	public byte[] data;
	
	public static PluginIcon getByPluginAndUse(String name, IconUse use) throws InternalServerException {
		return Model.get(PluginIcon.class, collection, CMaps.map("plugin", name).map("use", use), FIELDS);
	}
	
	public static Set<PluginIcon> getByPlugin(String name) throws InternalServerException {
		return Model.getAll(PluginIcon.class, collection, CMaps.map("plugin", name), FIELDS);
	}
	
	public static void add(PluginIcon pluginIcon) throws InternalServerException {
		Model.insert(collection, pluginIcon);	
	}
	
	public static void delete(String name, IconUse use) throws InternalServerException {				
		Model.delete(PluginIcon.class, collection, CMaps.map("plugin", name).map("use", use));		
	}
	
	public static void delete(String name) throws InternalServerException {				
		Model.delete(PluginIcon.class, collection, CMaps.map("plugin", name));		
	}
	
	public static void updateStatus(String name, PluginStatus status) throws InternalServerException {
		Model.setAll(PluginIcon.class, collection, CMaps.map("plugin", name), "status", status);
	}
}
