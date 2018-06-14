package utils;

import java.net.URLEncoder;
import java.util.List;

import models.enums.InstanceType;
import play.Play;

public class InstanceConfig {

	private static InstanceConfig instance;
	private InstanceType instanceType;
	private String defaultHost;
	private String portalServerDomain;
	private String pluginServerDomain;
	private String adminEmail;
	
	private String defaultLanguage;
	private List<String> countries;
	
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
	public List<String> getCountries() {
		return countries;
	}

	public void init() {
		String instanceTypeStr = Play.application().configuration().getString("instanceType");
		
		if (instanceTypeStr.equals("local")) instanceType = InstanceType.LOCAL;
		else if (instanceTypeStr.equals("demo")) instanceType = InstanceType.DEMO;
		else if (instanceTypeStr.equals("test")) instanceType = InstanceType.TEST;
		else if (instanceTypeStr.equals("prod")) instanceType = InstanceType.PROD;
		else if (instanceTypeStr.equals("perftest")) instanceType = InstanceType.PERFTEST;
		
		if (instanceType.disableCORSProtection()) {
		  defaultHost = "*";
		} else {
		  defaultHost = Play.application().configuration().getString("portal.originUrl");
		}
		
		pluginServerDomain = Play.application().configuration().getString("visualizations.server");
		
		portalServerDomain = Play.application().configuration().getString("portal.server");
		
		adminEmail = Play.application().configuration().getString("emails.admin", "alexander.kreutz@midata.coop");// TODO change default email to something useful
		
		defaultLanguage = Play.application().configuration().getString("default.language", "en");
		
		countries = Play.application().configuration().getStringList("default.countries");
	}
}
