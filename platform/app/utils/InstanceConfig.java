package utils;

import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import models.enums.InstanceType;

public class InstanceConfig {

	private static InstanceConfig instance;
	
	private InstanceType instanceType;
	private String defaultHost;
	private String portalServerDomain;
	private String pluginServerDomain;
	private String platformServer;
	private String adminEmail;
	private String portalOriginUrl;
	
	private String defaultLanguage;
	private List<String> countries;
	
	
	private Config config;
	
	public InstanceConfig(Config config) {
		this.config = config;		
		init();		
	}
	
	public static InstanceConfig getInstance() {		
		return instance;
	}
	
	public static void setInstance(InstanceConfig instance1) {		
		instance = instance1;
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
	
	public String getServiceURL() {
		return "https://"+getPortalServerDomain()+"/#/portal/service";
	}
	
	/**
	 * returns domain of plugin server from config
	 * @return plugin server domain
	 */
	public String getPluginServerDomain() {
		return pluginServerDomain;
	}
	
	public String getPortalOriginUrl() {
		return portalOriginUrl;
	}
	
	/**
	 * returns domain of backend
	 * @return
	 */
	public String getPlatformServer() {
		return platformServer;
	}
	
	/**
	 * returns the email adress for admin messages
	 * @return email of admin
	 */
	public String getAdminEmail() {
		return adminEmail;
	}
	
	/**
	 * returns the default language code for this platform
	 * @return
	 */		
	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	/**
	 * returns the list of supported countries for this platform
	 * @return
	 */
	/*public List<String> getCountries() {
		return countries;
	}*/
	
	public Config getConfig() {
		return config;
	}

	public void init() {
		try {

		String instanceTypeStr = config.getString("instanceType");	
		if (instanceTypeStr.equals("local")) instanceType = InstanceType.LOCAL;
		else if (instanceTypeStr.equals("demo")) instanceType = InstanceType.DEMO;
		else if (instanceTypeStr.equals("test")) instanceType = InstanceType.TEST;
		else if (instanceTypeStr.equals("prod")) instanceType = InstanceType.PROD;
		else if (instanceTypeStr.equals("perftest")) instanceType = InstanceType.PERFTEST;
		
		if (instanceType.disableCORSProtection()) {
		  defaultHost = "*";
		} else {
		  defaultHost = config.getString("portal.originUrl");
		}
		
		portalOriginUrl = config.getString("portal.originUrl");
		
		pluginServerDomain = config.getString("visualizations.server");
		if (pluginServerDomain.endsWith("/plugin")) pluginServerDomain = pluginServerDomain.substring(0, pluginServerDomain.length()-"/plugin".length());
		
		portalServerDomain = config.getString("portal.server");
		
		platformServer = config.getString("platform.server");
		
		adminEmail = config.getString("emails.admin");
		
		defaultLanguage = config.getString("default.language");
		
		//countries = config.getStringList("default.countries");
		} catch (ConfigException e) {
			System.out.println(e.toString());
			throw e;
		}
	}
}
