package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Member;
import models.MobileAppInstance;
import models.Plugin;
import models.Study;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationInterest;
import models.enums.UserRole;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import play.mvc.BodyParser;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.access.RecordManager;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.json.JsonValidation;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.members.HealthProvider;

public class QuickRegistration extends APIController {

	/**
	 * register a new MIDATA member and prepare the account for a study
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "study", "app", "phrase");
		
		String studyCode = JsonValidation.getString(json, "study");
		String appName = JsonValidation.getString(json, "app");
		String phrase = JsonValidation.getString(json, "phrase");
		
		Study study = Study.getByCodeFromMember(studyCode, Study.ALL);
		if (study == null) throw new BadRequestException("error.badcode", "Unknown code for study.");
		
		if (!study.participantSearchStatus.equals(ParticipantSearchStatus.SEARCHING)) throw new BadRequestException("error.badcode", "Study not searching for members.");
		
		Plugin app = Plugin.getByFilename(appName, Plugin.ALL_PUBLIC);
		if (app == null) throw new BadRequestException("error.badcode", "Unknown code for app.");
		
		String email = JsonValidation.getEMail(json, "email");
		String firstName = JsonValidation.getString(json, "firstname");
		String lastName = JsonValidation.getString(json, "lastname");
		String password = JsonValidation.getPassword(json, "password");

		// check status
		if (Member.existsByEMail(email)) {
		  return badRequest("A user with this email address already exists.");
		}
		
		// create the user
		Member user = new Member();
		user._id = new ObjectId();
		user.email = email;
		user.name = firstName + " " + lastName;
		
		user.password = Member.encrypt(password);
		do {
		  user.midataID = CodeGenerator.nextUniqueCode();
		} while (Member.existsByMidataID(user.midataID));
		user.role = UserRole.MEMBER;
		
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip  = JsonValidation.getString(json, "zip");
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.birthday = JsonValidation.getDate(json, "birthday");
		user.ssn = JsonValidation.getString(json, "ssn");
						
		user.registeredAt = new Date();		
		
		user.status = UserStatus.ACTIVE;		
		user.contractStatus = ContractStatus.NEW;	
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		user.partInterest = ParticipationInterest.UNSET;
							
		user.apps = new HashSet<ObjectId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		user.messages = new HashMap<String, Set<ObjectId>>();
		user.messages.put("inbox", new HashSet<ObjectId>());
		user.messages.put("archive", new HashSet<ObjectId>());
		user.messages.put("trash", new HashSet<ObjectId>());
		user.login = DateTimeUtils.now();
		user.news = new HashSet<ObjectId>();
		//user.pushed = new HashSet<ObjectId>();
		//user.shared = new HashSet<ObjectId>();
		
		user.security = AccountSecurityLevel.KEY;
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
								
		Member.add(user);		
		KeyManager.instance.unlock(user._id, null);
		
		user.myaps = RecordManager.instance.createPrivateAPS(user._id, user._id);
		Member.set(user._id, "myaps", user.myaps);
		
		controllers.members.Studies.requestParticipation(user._id, study._id);
		MobileAppInstance appInstance = MobileAPI.installApp(user._id, app, user, phrase);
		HealthProvider.confirmConsent(user._id, appInstance._id);
										
		return Application.loginHelper(user);		
	}
}
