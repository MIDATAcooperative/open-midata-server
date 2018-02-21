package models;

import java.util.Collections;
import java.util.Set;

import models.enums.IconUse;
import models.enums.PluginStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

/**
 * Icons for Apps and Plugins
 *
 */
public class PluginIcon extends Model {

	private static final String collection = "pluginicons";
	private static final Set<String> FIELDS = Collections.unmodifiableSet(Sets.create("plugin", "status", "use", "contentType", "data"));
	
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
