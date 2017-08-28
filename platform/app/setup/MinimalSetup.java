package setup;


import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;

import models.Admin;
import models.Developer;
import models.History;
import models.MidataId;
import models.Plugin;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.PluginStatus;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
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
		
		if (Admin.getByEmail("admin@midata.coop", Sets.create("_id")) == null) {
			Admin admin = new Admin();
			admin._id = new MidataId("5608f881e4b0f992a4e197b3");
			admin.email = "admin@midata.coop";
			admin.emailLC = admin.email;
			admin.password = "1000:baef51f211e1d5c0df67ca748933a76ce9e6bb4f1d51813f:85a273f66396793a5bcc09fe1e8d8062c25b118f65651c7e";
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
			admin.history = new ArrayList<History>();
			admin.emailStatus = EMailStatus.VALIDATED;
			admin.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(admin._id);
			Admin.add(admin);
			
			//KeyManager.instance.unlock(admin._id, null);
			RecordManager.instance.createPrivateAPS(admin._id, admin._id);
		}
		
		if (Developer.getByEmail("developers@midata.coop", Sets.create("_id")) == null) {
			Developer developer = new Developer();
			developer._id = new MidataId("55eff624e4b0b767e88f92b9");
			developer.email = "developers@midata.coop";
			developer.emailLC = developer.email.toLowerCase();
			developer.password = "1000:25156cb392d80c023e57290637e96b4bb6674fa50f329f6e:c27fd59cb2b9ce964cfa194655cb83930bc6a762ace15290";
			developer.role = UserRole.DEVELOPER;
			developer.registeredAt = new Date();
			developer.resettokenTs = 0;
			developer.status = UserStatus.ACTIVE;
			developer.contractStatus = ContractStatus.SIGNED;
			developer.agbStatus = ContractStatus.SIGNED;
			developer.confirmationCode = "Q8IV-EQBJ";
			developer.firstname = "MIDATA";
			developer.lastname = "Developer";
			developer.gender = Gender.OTHER;
			developer.security = AccountSecurityLevel.KEY;
			developer.history = new ArrayList<History>();			
			developer.emailStatus = EMailStatus.VALIDATED;			
			developer.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(developer._id);
			
			Developer.add(developer);
			
			//KeyManager.instance.unlock(developer._id, null);
			RecordManager.instance.createPrivateAPS(developer._id, developer._id);
		}	
		
		if (Admin.getByEmail("autorun-service", Sets.create("_id")) == null) {
			Admin admin = new Admin();
			admin._id = new MidataId();
			admin.email = "autorun-service";
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
			admin.history = new ArrayList<History>();
			admin.emailStatus = EMailStatus.VALIDATED;
			admin.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(admin._id);
			Admin.add(admin);
			
			//KeyManager.instance.unlock(admin._id, null);
			RecordManager.instance.createPrivateAPS(admin._id, admin._id);
		}
		
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
	}

}
