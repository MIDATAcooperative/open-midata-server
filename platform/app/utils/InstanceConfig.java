package utils;

import play.Play;
import models.enums.InstanceType;

public class InstanceConfig {

	private static InstanceConfig instance;
	private InstanceType instanceType;
	private String defaultHost;
	private String portalServerDomain;
	private String pluginServerDomain;
	
	public InstanceConfig() {
		init();
	}
	
	public static InstanceConfig getInstance() {
		if (instance == null) instance = new InstanceConfig();
		return instance;
	}
	
    /**
     * returns the type of instance of this MIDATA installation
     * @return 
     */
	public InstanceType getInstanceType() {
	  return instanceType;	
	}
	
	/**
	 * returns 
	 * @return 
	 */
	public String getDefaultHost() {
		return defaultHost;
	}
	
	/**
	 * returns domain of portal from config
	 * @return portal domain
	 */
	public String getPortalServerDomain() {
		return portalServerDomain;
	}
	
	/**
	 * returns domain of plugin server from config
	 * @return plugin server domain
	 */
	public String getPluginServerDomain() {
		return pluginServerDomain;
	}
		
	public void init() {
		String instanceTypeStr = Play.application().configuration().getString("instanceType");
		
		if (instanceTypeStr.equals("local")) instanceType = InstanceType.LOCAL;
		else if (instanceTypeStr.equals("demo")) instanceType = InstanceType.DEMO;
		else if (instanceTypeStr.equals("test")) instanceType = InstanceType.TEST;
		else if (instanceTypeStr.equals("prod")) instanceType = InstanceType.PROD;
		
		defaultHost = Play.application().configuration().getString("portal.originUrl");
		
		pluginServerDomain = Play.application().configuration().getString("visualizations.server");
		
		portalServerDomain = Play.application().configuration().getString("portal.server");
	}
}
