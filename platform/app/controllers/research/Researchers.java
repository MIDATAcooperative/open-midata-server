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

package controllers.research;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import controllers.APIController;
import controllers.Application;
import controllers.OAuth2;
import controllers.PWRecovery;
import controllers.Terms;
import models.Member;
import models.MidataId;
import models.Research;
import models.ResearchUser;
import models.User;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.SecondaryAuthType;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.ExtendedSessionToken;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.auth.ResearchSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.OrganizationResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * login and registration functions for researchers
 *
 */
public class Researchers extends APIController {

	/**
	 * register a new researcher
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result register() throws AppException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "email", "description", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language");
					
		String name = JsonValidation.getString(json, "name");				
		String email = JsonValidation.getEMail(json, "email");
		
		
		Research research = new Research();
		
		research._id = new MidataId();
		research.name = name;
		research.description = JsonValidation.getString(json, "description");
		
		ResearchUser user = new ResearchUser(email);
		
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip  = JsonValidation.getString(json, "zip");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.language = JsonValidation.getString(json, "language");
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		
		user.password = ResearchUser.encrypt(JsonValidation.getPassword(json, "password"));		
				
		user._id = new MidataId();
		//user.authType = SecondaryAuthType.SMS;
		Application.developerRegisteredAccountCheck(user, json);		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		
		if (research != null && Research.existsByName(research.name)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A research organization with this name already exists.");			
		if (ResearchUser.existsByEMail(user.email)) throw new JsonValidationException("error.exists.user", "email", "exists", "A user with this email address already exists.");
						
		user.role = UserRole.RESEARCH;
		user.subroles = EnumSet.of(SubUserRole.MASTER);
		user.registeredAt = new Date();				
		if (user.status == null) user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.REQUESTED;
		user.agbStatus = ContractStatus.REQUESTED;
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
				
		user.apps = new HashSet<MidataId>();	
		user.visualizations = new HashSet<MidataId>();
		Terms.addAgreedToDefaultTerms(user);
				
		String pub = JsonValidation.getString(json, "pub");
		String pk = JsonValidation.getString(json, "priv_pw");
		Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));
		  		        	      		  		
		user.publicExtKey = KeyManager.instance.readExternalPublicKey(pub);		  
		KeyManager.instance.saveExternalPrivateKey(user._id, pk);		  
		String handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true);
		  
		user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(user._id, null);								
		if (research != null) {
			  Research.add(research);
			  user.organization = research._id;
		}
		ResearchUser.add(user);
		  
		KeyManager.instance.newFutureLogin(user);	
		PWRecovery.storeRecoveryData(user._id, recover);
			
		RecordManager.instance.createPrivateAPS(null, user._id, user._id);
						
		Application.sendWelcomeMail(user, null);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
			
		return OAuth2.loginHelper(new ExtendedSessionToken().forUser(user).withSession(handle), json, null, RecordManager.instance.createContextFromAccount(user._id));
			
	}
	
	/**
	 * register a new research
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public Result registerOther() throws AppException {
		requireSubUserRole(SubUserRole.MASTER);
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "country", "language");
							
		String email = JsonValidation.getEMail(json, "email");
			
		MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		User executingUser = User.getById(executorId, User.ALL_USER);
		
	    ResearchUser user = new ResearchUser(email);
		
	    user._id = new MidataId();
		user.address1 = JsonValidation.getStringOrNull(json, "address1");
		user.address2 = JsonValidation.getStringOrNull(json, "address2");
		user.city = JsonValidation.getStringOrNull(json, "city");
		user.zip  = JsonValidation.getStringOrNull(json, "zip");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.language = JsonValidation.getString(json, "language");
		user.phone = JsonValidation.getStringOrNull(json, "phone");
		user.mobile = JsonValidation.getStringOrNull(json, "mobile");
		user.organization = PortalSessionToken.session().orgId;
		if (user.organization == null) throw new InternalServerException("error.internal", "No organization in session for register researcher!");
		user.status = UserStatus.ACTIVE;
		//user.authType = SecondaryAuthType.SMS;
						
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, null, new MidataId(request().attrs().get(play.mvc.Security.USERNAME)), user);
		register(user ,null, executingUser);
			
		AuditManager.instance.success();
		return ok();		
	}
	
	
	public static void register(ResearchUser user, Research research, User executingUser) throws AppException {
					
		if (research != null && Research.existsByName(research.name)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A research organization with this name already exists.");			
		if (ResearchUser.existsByEMail(user.email)) throw new JsonValidationException("error.exists.user", "email", "exists", "A user with this email address already exists.");
				
		if (user._id == null) user._id = new MidataId();
		user.role = UserRole.RESEARCH;
		user.subroles = EnumSet.noneOf(SubUserRole.class);
		user.registeredAt = new Date();				
		if (user.status == null) user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.REQUESTED;
		user.agbStatus = ContractStatus.REQUESTED;
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
		user.apps = new HashSet<MidataId>();	
		user.visualizations = new HashSet<MidataId>();
				
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		
		if (research != null) {
		  Research.add(research);
		  user.organization = research._id;
		  OrganizationResourceProvider.updateFromResearch(executingUser._id, research);
		}
		ResearchUser.add(user);
					
		RecordManager.instance.createPrivateAPS(null, user._id, user._id);		
		
		Application.sendWelcomeMail(user, executingUser);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
						
	}
	
	/**
	 * login a researcher
	 * @return status ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result login() throws AppException {
		
	    JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "password");
		
        ExtendedSessionToken token = new ExtendedSessionToken();
		
		token.created = System.currentTimeMillis();                               
	    token.userRole = UserRole.RESEARCH;                
										    				
		return OAuth2.loginHelper(token, json, null, null);
							
	}
	

	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getOrganization(String id) throws AppException {
			
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId researchid = MidataId.from(id);
						
		Research research = Research.getById(researchid, Research.ALL);
		if (research == null) return notFound();						
		return ok(JsonOutput.toJson(research, "Research", Research.ALL));		
	}

	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public Result updateOrganization(String id) throws AppException {
		requireSubUserRole(SubUserRole.MASTER);
		MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "_id", "name", "description");
					
		String name = JsonValidation.getString(json, "name");				
		String description = JsonValidation.getString(json, "description");
				
		MidataId researchid = PortalSessionToken.session().getOrgId();
		
		if (!researchid.equals(JsonValidation.getMidataId(json, "_id"))) throw new InternalServerException("error.internal", "Tried to change other research organization!");
		
		if (Research.existsByName(name, researchid)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A research organization with this name already exists.");			
		
		Research research = Research.getById(researchid, Research.ALL);

		if (research == null) throw new InternalServerException("error.internal", "Research organization not found.");
		
		research.name = name;
		research.description = description;
		research.set("name", research.name);
		research.set("description", research.description);						
		
		OrganizationResourceProvider.updateFromResearch(executorId, research);
		return ok();		
	}
		
}
