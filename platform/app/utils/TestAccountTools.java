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

import models.Consent;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.ConsentType;
import models.enums.TestAccountsAcceptance;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;

public class TestAccountTools {

    public static String testCustomerFromName(String name) {
        String parts[] = name.split("\\-");
        if (parts.length == 3) {
            if (!parts[2].toLowerCase().equals("test")) return null;
            return parts[0];
        }
        return null;
    }
    
    public static MidataId testUserAppOrNull(AccessContext context, MidataId user) throws InternalServerException {    
    	User theUser = (context == null || context.getRequestCache() == null) ? User.getById(user, User.PUBLIC) : context.getRequestCache().getUserById(user, true);
    	if (theUser != null) return theUser.testUserApp;
    	return null;
    }
      
    public static boolean allowInstallation(AccessContext context, MidataId userId, MidataId targetPlugin) throws InternalServerException {
    	MidataId testUserPlugin = testUserAppOrNull(context, userId);
    	if (testUserPlugin==null) return true;
    	return doesAcceptTestUsers(testUserPlugin, targetPlugin);
    }
    
    public static boolean doesAcceptTestUsers(AccessContext context, MidataId plugin) throws InternalServerException  {
    	if (plugin==null) return false;
    	return doesAcceptTestUsers(context.getUsedPlugin(), Plugin.getById(plugin));
    }
    
    public static boolean doesAcceptTestUserData(AccessContext context, MidataId plugin) throws InternalServerException  {
    	//if (plugin==null) return false;
    	return doesAcceptTestUsers(plugin, Plugin.getById(context.getUsedPlugin()));
    }
    
    public static boolean doesAcceptTestUsers(MidataId testUserPlugin, MidataId targetPlugin) throws InternalServerException  {
    	return doesAcceptTestUsers(testUserPlugin, Plugin.getById(targetPlugin));
    }
    
    public static boolean doesAcceptTestUsers(MidataId testUserPlugin, Plugin plugin) {
    	if (testUserPlugin == null) return true; // It is a normal user and no test user
    	if (plugin.acceptTestAccounts == TestAccountsAcceptance.NONE) return false;
    	if (plugin.acceptTestAccounts == TestAccountsAcceptance.ALL) return true;
    	if (plugin.acceptTestAccountsFromApp != null && plugin.acceptTestAccountsFromApp.contains(testUserPlugin)) return true;
    	return false;
    }
    
    public static void prepareNewUser(AccessContext context, User user, String testUserExtension) throws AppException {
        String customer = testCustomerFromName(user.lastname);
        if (customer == null) customer = testUserExtension;
        
        if (customer != null) {
            user.testUserApp = context.getUsedPlugin();
            user.testUserCustomer = customer;
            
            if (!doesAcceptTestUsers(context, context.getUsedPlugin())) throw new PluginException(context.getUsedPlugin(), "error.blocked.testuser", "Application does not accept test users.");
        }
    }
    
    public static void createNewUser(AccessContext context, User user) throws AppException {
        if (user.testUserApp != null) {
        	Plugin pl = Plugin.getById(user.testUserApp, Plugin.FOR_TEST_ACCOUNTS);
        	if (pl == null || !pl.status.isUsable()) throw new InternalServerException("error.internal", "Test user creating plugin not found or usable");
        	
        	if (pl.testAccountsCurrent < pl.testAccountsMax) {
        		pl.testAccountsCurrent++;
        		Plugin.set(pl._id, "testAccountsCurrent", pl.testAccountsCurrent);
        	} else {
        		throw new PluginException(context.getUsedPlugin(), "error.toomany.testuser", "No more test user available");
        	}
        }
    }
    
    public static void prepareConsent(AccessContext context, Consent consent) throws InternalServerException, PluginException {
    	if (consent.owner != null && consent.testUserApp == null) {
    		User user = context.getRequestCache().getUserById(consent.owner, true);
    		if (user != null && user.testUserApp != null) {
    		  consent.testUserApp = user.testUserApp;
    		  
    		  if (consent.type == ConsentType.EXTERNALSERVICE || consent.type == ConsentType.API) {
    			MobileAppInstance app = (MobileAppInstance) consent;
    			if (!doesAcceptTestUsers(consent.testUserApp, app.applicationId)) throw new PluginException(consent.testUserApp, "error.blocked.testapp", "Target application does not accept test user.");  	  
    		  } else if (consent.type == ConsentType.STUDYPARTICIPATION) {
    			 StudyParticipation part = (StudyParticipation) consent;
    			 Study study = Study.getById(part.study, Sets.create("_id", "autoJoinTestGroup"));
    			 if (study.autoJoinTestGroup != null) part.group = study.autoJoinTestGroup;
    		  }
    		  
    		}
    	}
    }
}
