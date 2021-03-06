package setup;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import models.Admin;
import models.Developer;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.db.DBLayer;
import utils.search.Search;

/**
 * Minimal setup that is necessary to start a fresh MIDATA platform.
 */
public class MinimalSetup {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting to create minimal setup for MIDATA platform.");

		// connecting
		System.out.print("Connecting to MongoDB...");
		start(fakeApplication(fakeGlobal()));
		DBLayer.connect();
		System.out.println("done.");
		System.out.print("Connecting to ElasticSearch...");
		Search.connect();
		System.out.println("done.");

		// initializing
		System.out.print("Setting up MongoDB...");
		DBLayer.initialize();
		
		if (Admin.getByEmail("admin@midata.coop", Sets.create("_id")) == null) {
			Admin admin = new Admin();
			admin._id = new ObjectId("5608f881e4b0f992a4e197b3");
			admin.email = "admin@midata.coop";
			admin.password = "1000:baef51f211e1d5c0df67ca748933a76ce9e6bb4f1d51813f:85a273f66396793a5bcc09fe1e8d8062c25b118f65651c7e";
			admin.role = UserRole.ADMIN;
			admin.status = UserStatus.ACTIVE;
			admin.contractStatus = ContractStatus.SIGNED;		
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
			
			KeyManager.instance.unlock(admin._id, null);
			RecordManager.instance.createPrivateAPS(admin._id, admin._id);
		}
		
		if (Developer.getByEmail("development@midata.coop", Sets.create("_id")) == null) {
			Developer developer = new Developer();
			developer._id = new ObjectId("55eff624e4b0b767e88f92b9");
			developer.email = "development@midata.coop";
			developer.password = "1000:25156cb392d80c023e57290637e96b4bb6674fa50f329f6e:c27fd59cb2b9ce964cfa194655cb83930bc6a762ace15290";
			developer.role = UserRole.DEVELOPER;
			developer.registeredAt = new Date();
			developer.resettokenTs = 0;
			developer.status = UserStatus.ACTIVE;
			developer.contractStatus = ContractStatus.SIGNED;
			developer.confirmationCode = "Q8IV-EQBJ";
			developer.firstname = "MIDATA";
			developer.lastname = "Developer";
			developer.gender = Gender.OTHER;
			developer.security = AccountSecurityLevel.KEY;
			developer.history = new ArrayList<History>();			
			developer.emailStatus = EMailStatus.VALIDATED;			
			developer.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(developer._id);
			
			Developer.add(developer);
			
			KeyManager.instance.unlock(developer._id, null);
			RecordManager.instance.createPrivateAPS(developer._id, developer._id);
		}
						
		System.out.println("done.");
		System.out.print("Setting up ElasticSearch...");
		Search.initialize();
		System.out.println("done.");

		// terminating
		System.out.println("Shutting down...");
		DBLayer.close();
		Search.close();
		System.out.println("Minimal setup complete.");
	}

}
