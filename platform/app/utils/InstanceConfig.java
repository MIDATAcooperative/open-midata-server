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

package utils;

import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import models.enums.InstanceType;
import models.enums.UserRole;

public class InstanceConfig {

	private static InstanceConfig instance;
	
	private InstanceType instanceType;
	private String defaultHost;
	private String portalServerDomain;
	private String pluginServerDomain;
	private String platformServer;
	private String adminEmail;
	private String portalOriginUrl;
	private String internalBuilderUrl;
	
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
		return "https://"+getPortalServerDomain()+"/#/public/service";
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
	
	public String getInternalBuilderUrl() {
		return internalBuilderUrl;
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
		
		if (config.hasPath("platform.builder")) {
		  internalBuilderUrl = config.getString("platform.builder");
		} else {
		  internalBuilderUrl = null;
		}
		
		//countries = config.getStringList("default.countries");
		} catch (ConfigException e) {
			System.out.println(e.toString());
			throw e;
		}
	}
	
	/**
	 * Get current version of terms of use
	 * @return
	 */
	public String getTermsOfUse(UserRole role) {	
	  if (role == UserRole.PROVIDER) return "midata-terms-of-use-hp--" + (config.hasPath("versions.midata-terms-of-use-hp") ? config.getString("versions.midata-terms-of-use-hp") : "1.0");
	  return "midata-terms-of-use--" + (config.hasPath("versions.midata-terms-of-use") ? config.getString("versions.midata-terms-of-use") : "1.0");
	}
	
	/**
	 * Get current version of privacy policy
	 * @return
	 */
	public String getPrivacyPolicy(UserRole role) {
	   if (role == UserRole.PROVIDER) return "midata-privacy-policy-hp--" + (config.hasPath("versions.midata-privacy-policy-hp") ? config.getString("versions.midata-privacy-policy-hp") : "1.0");
	   return "midata-privacy-policy--" + (config.hasPath("versions.midata-privacy-policy") ? config.getString("versions.midata-privacy-policy") : "1.0");		
	}
}
