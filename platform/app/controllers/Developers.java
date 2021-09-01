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

package controllers;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Admin;
import models.Developer;
import models.MidataId;
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
import play.mvc.Http.Request;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.CodeGenerator;
import utils.auth.DeveloperSecured;
import utils.auth.ExtendedSessionToken;
import utils.auth.KeyManager;
import utils.auth.PasswordResetToken;
import utils.auth.PortalSessionToken;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;

/**
 * login and registration for developers
 *
 */
public class Developers extends APIController {

	/**
	 * register a new developer
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result register(Request request) throws AppException {
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language", "reason", "priv_pw", "pub", "recovery");
							
		String email = JsonValidation.getEMail(json, "email");
		if (Developer.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
				
		
		Developer user = new Developer(email);
		user._id = new MidataId();
		user.role = UserRole.DEVELOPER;
		user.subroles = EnumSet.noneOf(SubUserRole.class);
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
		
		user.reason = JsonValidation.getString(json, "reason");
		user.coach = JsonValidation.getString(json, "coach");
		
		user.password = Developer.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.REQUESTED;	
		user.agbStatus = ContractStatus.REQUESTED;
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.mobileStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.apps = new HashSet<MidataId>();	
		user.visualizations = new HashSet<MidataId>();
		Terms.addAgreedToDefaultTerms(user);
		//user.authType = SecondaryAuthType.SMS;
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
				
		String pub = JsonValidation.getString(json, "pub");
		String pk = JsonValidation.getString(json, "priv_pw");
		Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));
			  		        	      		  		
		user.publicExtKey = KeyManager.instance.readExternalPublicKey(pub);		  
		KeyManager.instance.saveExternalPrivateKey(user._id, pk);		  
		String handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true);
			  
		user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(user._id, null);								
		Developer.add(user);
			  
	    KeyManager.instance.newFutureLogin(user);	
		PWRecovery.storeRecoveryData(user._id, recover);
				
		RecordManager.instance.createPrivateAPS(null, user._id, user._id);				
		
		Application.sendWelcomeMail(user, null);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
		
		Market.correctOwners();
		
		
		return OAuth2.loginHelper(request, new ExtendedSessionToken().forUser(user).withSession(handle), json, null, RecordManager.instance.createContextFromAccount(user._id));
			
	}
	
	/**
	 * login a developer or admin
	 * @return status ok / returns "admin" if person logged in is an admin
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result login(Request request) throws AppException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "email", "password");
		
        ExtendedSessionToken token = new ExtendedSessionToken();
		
		token.created = System.currentTimeMillis();                               
	    token.userRole = UserRole.DEVELOPER;                
										    				
		return OAuth2.loginHelper(request, token, json, null, null);
				
	}
	
	/**
	 * Creates a change password link for a test account
	 * @return
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(DeveloperSecured.class)
	public Result resetTestAccountPassword(Request request) throws AppException {
		JsonNode json = request.body().asJson();
			
		MidataId developerId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId targetUserId = JsonValidation.getMidataId(json, "user");
		User target = User.getById(targetUserId, Sets.create("developer", "role", "password","security"));		
		if (target == null || !target.developer.equals(developerId)) throw new BadRequestException("error.unknown.user", "No test user");
		
		PasswordResetToken token = new PasswordResetToken(target._id, target.role.toString().toLowerCase());
		target.set("resettoken", token.token);
		target.set("resettokenTs", System.currentTimeMillis());
		String encrypted = token.encrypt();
					   	
		String url = "/#/portal/setpw?token=" + encrypted;
		
		if (target.security != AccountSecurityLevel.KEY_EXT_PASSWORD) url+="&ns=1";
						
		return ok(url);		
	}
}
