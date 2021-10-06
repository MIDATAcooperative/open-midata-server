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

package setup;


import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;

import models.AccessPermissionSet;
import models.Admin;
import models.Developer;
import models.Member;
import models.MidataId;
import models.Plugin;
import models.UserGroup;
import models.enums.AccountActionFlags;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.PluginStatus;
import models.enums.SecondaryAuthType;
import models.enums.SubUserRole;
import models.enums.UserGroupType;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.RuntimeConstants;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.collections.Sets;
import utils.exceptions.AppException;

/**
 * Minimal setup that is necessary to start a fresh MIDATA platform.
 */
public class MinimalSetup {

	public static void dosetup() throws AppException {
		System.out.println("Starting to create minimal setup for MIDATA platform.");
		
		if (Admin.getById(MidataId.from("5608f881e4b0f992a4e197b3"), Sets.create("_id")) == null && Admin.getByEmail("admin@example.com", Sets.create("_id")) == null) {
			Admin admin = new Admin();
			admin._id = new MidataId("5608f881e4b0f992a4e197b3");
			admin.email = "admin@example.com";
			admin.emailLC = admin.email;
			
			// initial password for first login after a fresh installation is string "secret". 
			// Password change is enforced on first login 
			admin.password = "1000:baef51f211e1d5c0df67ca748933a76ce9e6bb4f1d51813f:85a273f66396793a5bcc09fe1e8d8062c25b118f65651c7e";
			admin.flags = EnumSet.of(AccountActionFlags.CHANGE_PASSWORD);
			
			admin.role = UserRole.ADMIN;
			admin.subroles = EnumSet.of(SubUserRole.SUPERADMIN, SubUserRole.CONTENTADMIN, SubUserRole.NEWSWRITER, SubUserRole.PLUGINADMIN, SubUserRole.STUDYADMIN, SubUserRole.USERADMIN);
			admin.status = UserStatus.ACTIVE;
			admin.contractStatus = ContractStatus.SIGNED;	
			admin.agbStatus = ContractStatus.SIGNED;
			admin.registeredAt = new Date();
			admin.resettokenTs = 0;				
			admin.confirmationCode = "B7M0-K7CR";
			admin.firstname = "System";
			admin.lastname = "Administrator";
			admin.gender = Gender.OTHER;
			admin.security = AccountSecurityLevel.KEY;
			
			admin.emailStatus = EMailStatus.VALIDATED;
			admin.authType = SecondaryAuthType.NONE;
			admin.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(admin._id);
			Admin.add(admin);
			
			//KeyManager.instance.unlock(admin._id, null);
			RecordManager.instance.createPrivateAPS(null, admin._id, admin._id);
		}
		
		if (Developer.getByEmail("developers@midata.coop", Sets.create("_id")) == null) {
			Developer developer = new Developer();
			developer._id = new MidataId("55eff624e4b0b767e88f92b9");
			developer.email = "developers@midata.coop";
			developer.emailLC = developer.email.toLowerCase();
			developer.password = null;
			developer.role = UserRole.DEVELOPER;
			developer.registeredAt = new Date();
			developer.resettokenTs = 0;
			developer.status = UserStatus.BLOCKED;
			developer.contractStatus = ContractStatus.SIGNED;
			developer.agbStatus = ContractStatus.SIGNED;
			developer.confirmationCode = "Q8IV-EQBJ";
			developer.firstname = "MIDATA";
			developer.lastname = "Developer";
			developer.gender = Gender.OTHER;
			developer.security = AccountSecurityLevel.KEY;
					
			developer.emailStatus = EMailStatus.VALIDATED;
			developer.authType = SecondaryAuthType.NONE;
			developer.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(developer._id);
			
			Developer.add(developer);
			
			//KeyManager.instance.unlock(developer._id, null);
			RecordManager.instance.createPrivateAPS(null, developer._id, developer._id);
		}	
		
		if (Admin.getByEmail(RuntimeConstants.AUTORUN_USERNAME, Sets.create("_id")) == null) {
			Admin admin = new Admin();
			admin._id = new MidataId();
			admin.email = RuntimeConstants.AUTORUN_USERNAME;
			admin.emailLC = admin.email.toLowerCase();
			admin.password = null;
			admin.role = UserRole.ADMIN;
			admin.subroles = EnumSet.noneOf(SubUserRole.class);
			admin.status = UserStatus.ACTIVE;
			admin.contractStatus = ContractStatus.SIGNED;
			admin.agbStatus = ContractStatus.SIGNED;	
			admin.registeredAt = new Date();
			admin.resettokenTs = 0;				
			admin.confirmationCode = "";
			admin.firstname = "Midata Import";
			admin.lastname = "Service";
			admin.gender = Gender.OTHER;
			admin.security = AccountSecurityLevel.KEY;
			
			admin.emailStatus = EMailStatus.VALIDATED;
			admin.authType = SecondaryAuthType.NONE;
			admin.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(admin._id);
			Admin.add(admin);
			
			//KeyManager.instance.unlock(admin._id, null);
			RecordManager.instance.createPrivateAPS(null, admin._id, admin._id);
		}
		
		if (Admin.getByEmail(RuntimeConstants.BACKEND_SERVICE, Sets.create("_id")) == null) {
			Admin admin = new Admin();
			admin._id = new MidataId();
			admin.email = RuntimeConstants.BACKEND_SERVICE;
			admin.emailLC = admin.email.toLowerCase();
			admin.password = null;
			admin.role = UserRole.ADMIN;
			admin.subroles = EnumSet.noneOf(SubUserRole.class);
			admin.status = UserStatus.ACTIVE;
			admin.contractStatus = ContractStatus.SIGNED;
			admin.agbStatus = ContractStatus.SIGNED;	
			admin.registeredAt = new Date();
			admin.resettokenTs = 0;				
			admin.confirmationCode = "";
			admin.firstname = "Midata Backend";
			admin.lastname = "Service";
			admin.gender = Gender.OTHER;
			admin.security = AccountSecurityLevel.KEY;
			
			admin.emailStatus = EMailStatus.VALIDATED;
			admin.authType = SecondaryAuthType.NONE;
			admin.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(admin._id);
			Admin.add(admin);
			
			//KeyManager.instance.unlock(admin._id, null);
			RecordManager.instance.createPrivateAPS(null, admin._id, admin._id);
		}
		
		if (Member.getByEmail(RuntimeConstants.PUBLIC_USER, Sets.create("_id")) == null) {
			Member publicUser = new Member();
			publicUser._id = new MidataId("5ccab0dcaed6452048f2b010");
			publicUser.email = RuntimeConstants.PUBLIC_USER;
			publicUser.emailLC = publicUser.email.toLowerCase();
			publicUser.password = null;
			publicUser.role = UserRole.MEMBER;
			publicUser.subroles = EnumSet.noneOf(SubUserRole.class);
			publicUser.status = UserStatus.ACTIVE;
			publicUser.contractStatus = ContractStatus.SIGNED;
			publicUser.agbStatus = ContractStatus.SIGNED;	
			publicUser.registeredAt = new Date();
			publicUser.resettokenTs = 0;				
			publicUser.confirmationCode = "";
			publicUser.firstname = "Account";
			publicUser.lastname = "Public";
			publicUser.gender = Gender.OTHER;
			publicUser.security = AccountSecurityLevel.KEY;
			
			publicUser.emailStatus = EMailStatus.VALIDATED;
			publicUser.authType = SecondaryAuthType.NONE;
			publicUser.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(publicUser._id);
			Member.add(publicUser);			
			//KeyManager.instance.unlock(admin._id, null);
						
			UserGroup ug = new UserGroup();
			ug._id = RuntimeConstants.publicGroup;
			ug.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(ug._id);
			ug.registeredAt = new Date();
			ug.name = RuntimeConstants.PUBLIC_GROUP;
			ug.creator = publicUser._id;
			ug.status = UserStatus.ACTIVE;
			ug.add();
			
			RecordManager.instance.createAnonymizedAPS(publicUser._id, ug._id, publicUser._id, false);
			RecordManager.instance.createPrivateAPS(null, ug._id, ug._id);
		}
		
		// Bugfix for test instance - remove after next update
		if (AccessPermissionSet.getById(RuntimeConstants.publicGroup)==null) {
			KeyManager.instance.login(5000, false);
			KeyManager.instance.unlock(RuntimeConstants.publicGroup, null);
			RecordManager.instance.createPrivateAPS(null, RuntimeConstants.publicGroup, RuntimeConstants.publicGroup);
		}
		// End bugfix
		
		if (Plugin.getByFilename("portal", Sets.create("_id")) == null ) {
			Plugin portal = new Plugin();
			portal._id = MidataId.from("588b53a7aed64509f5095def");
			portal.filename = "portal";
			portal.name = "Midata Portal";
			portal.targetUserRole = UserRole.ANY;
			portal.status = PluginStatus.ACTIVE;
			portal.type = "visualization";
			portal.description = "The MIDATA portal";
			portal.creator = Developer.getByEmail("developers@midata.coop", Sets.create("_id"))._id;
			portal.creatorLogin = "developers@midata.coop";
			portal.defaultQuery=new HashMap<String, Object>();
			Plugin.add(portal);
		} 
		
		if (Plugin.getById(MidataId.from("588b53a7aed64509f5095def")) == null) {
			Plugin portal = new Plugin();
			portal._id = MidataId.from("588b53a7aed64509f5095def");
			portal.filename = "portal-old";
			portal.name = "Midata Portal";
			portal.targetUserRole = UserRole.ANY;
			portal.status = PluginStatus.DEPRECATED;
			portal.type = "visualization";
			portal.description = "The MIDATA portal";
			portal.creator = Developer.getByEmail("developers@midata.coop", Sets.create("_id"))._id;
			portal.creatorLogin = "developers@midata.coop";
			portal.defaultQuery=new HashMap<String, Object>();
			Plugin.add(portal);
		}
		
		if (Plugin.getByFilename("common", Sets.create("_id")) == null ) {
			Plugin common = new Plugin();
			common._id = new MidataId();
			common.filename = "common";
			common.name = "Midata Common Messages";
			common.targetUserRole = UserRole.ADMIN;
			common.status = PluginStatus.ACTIVE;
			common.type = "visualization";
			common.description = "Midata Common Messages";
			common.creator = Developer.getByEmail("developers@midata.coop", Sets.create("_id"))._id;
			common.creatorLogin = "developers@midata.coop";
			common.defaultQuery=new HashMap<String, Object>();
			Plugin.add(common);
		}
		
		if (Plugin.getByFilename("midata_autoapprover", Sets.create("_id")) == null ) {
			Plugin common = new Plugin();
			common._id = new MidataId();
			common.filename = "midata_autoapprover";
			common.name = "Midata Project Auto-Approver";
			common.targetUserRole = UserRole.RESEARCH;
			common.status = PluginStatus.ACTIVE;
			common.type = "analyzer";
			common.description = "Internal 'analyzer' that automatically assigns project participants to groups and approves the participation.";
			common.creator = Developer.getByEmail("developers@midata.coop", Sets.create("_id"))._id;
			common.creatorLogin = "developers@midata.coop";
			common.defaultQuery=new HashMap<String, Object>();
			common.defaultQuery.put("content", Collections.emptySet());
			Plugin.add(common);
		}
	}

}
