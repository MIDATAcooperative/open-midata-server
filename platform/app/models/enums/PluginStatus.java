package models.enums;

/**
 * Status of MIDATA plugin
 *
 */
public enum PluginStatus {
	
	/**
	 * Plugin is still in development. Should not be shown in market
	 */
    DEVELOPMENT,
    
    /**
     * Plugin should be shown on beta (demo) servers but not in production
     */
    BETA,
    
    /**
     * Plugin should be shown in market
     */
    ACTIVE,
    
    /**
     * Plugin is deprecated and should no longer be used
     */
    DEPRECATED
}
