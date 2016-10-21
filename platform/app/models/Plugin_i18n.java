package models;

/**
 * Used inside plugin class to store internationalization information
 *
 */
public class Plugin_i18n implements JsonSerializable {

	/**
	 * public name of this plugin
	 */
	public String name;
	
	/**
	 * description text of this plugin
	 */
	public String description;
	
	/**
	 * The default title for the tile where this plugin is displayed.
	 * null for mobile apps
	 */
	public String defaultSpaceName;
}
