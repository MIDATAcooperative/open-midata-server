package controllers;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.KeyRecoveryData;
import models.KeyRecoveryProcess;
import models.Member;
import models.MidataId;
import models.User;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import play.mvc.BodyParser;
import utils.audit.AuditManager;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.FutureLogin;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import play.mvc.Result;
import play.mvc.Security;

// read -p "Enter encrypted:" x;printf $x| base64 -d |openssl rsautl -decrypt -inkey key.pem;echo

public class PWRecovery extends APIController {

	@BodyParser.Of(BodyParser.Json.class) 
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result storeRecoveryData() throws AppException {
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "user", "entries");	
		
		MidataId user =  new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId usercheck = JsonValidation.getMidataId(json, "user");
		
		if (!user.equals(usercheck)) throw new InternalServerException("error.internal", "User mismatch");
		
		Map<String, String> entries = JsonExtraction.extractStringMap(json.get("entries"));
		
		storeRecoveryData(user, entries);
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class) 
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result storeRecoveryShare() throws AppException {
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "_id", "shares");	
				
		MidataId user = JsonValidation.getMidataId(json, "_id");
										
		KeyRecoveryProcess proc = KeyRecoveryProcess.getById(user);
		
		if (proc == null) throw new InternalServerException("error.internal", "Unknown recovery process");		
		proc.shares = JsonExtraction.extractStringMap(json.get("shares"));		
		KeyRecoveryProcess.update(proc);
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class) 
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result finishRecovery() throws AppException {
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "_id", "session");
		
		MidataId userid = JsonValidation.getMidataId(json, "_id");
		String sessionToken = JsonValidation.getString(json, "session");

        KeyRecoveryProcess proc = KeyRecoveryProcess.getById(userid);		
		if (proc == null) throw new InternalServerException("error.internal", "Unknown recovery process");
		
		User user = User.getById(userid, User.ALL_USER_INTERNAL);
		  	
		FutureLogin fl = FutureLogin.getById(userid);
		fl.intPart = proc.intPart;
		fl.set();
		
		KeyManager.instance.unlock(user._id, sessionToken, proc.nextPublicExtKey);		
		
		user.publicExtKey = proc.nextPublicExtKey;
		user.password = proc.nextPassword;
		user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;		  
		KeyManager.instance.saveExternalPrivateKey(user._id, proc.nextPk);
		user.updatePassword();  
		KeyManager.instance.newFutureLogin(user);	
		PWRecovery.storeRecoveryData(user._id, proc.nextShares);		
		KeyRecoveryProcess.delete(user._id);
		
		return ok();
	}
	
	
	public static void storeRecoveryData(MidataId user, Map<String, String> entries) throws AppException {
		KeyRecoveryData data = new KeyRecoveryData();
		data._id = user;
		data.shares = entries;
		KeyRecoveryData.update(data);
	}
	
	/**
	 * set a new password for a user account by providing the old password
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result changePassword() throws JsonValidationException, AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "oldPassword", "oldPasswordHash", "password");
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		String oldPassword = JsonValidation.getString(json, "oldPassword");
		String oldPasswordHash = JsonValidation.getString(json, "oldPasswordHash");
		
		
		
		User user = User.getById(userId, User.ALL_USER_INTERNAL);
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_PASSWORD_CHANGE, user);
		if (!user.authenticationValid(oldPasswordHash) && !user.authenticationValid(oldPassword)) throw new BadRequestException("error.invalid.password_old","Bad password.");
		
		String password = JsonValidation.getPassword(json, "password");
		String pub = JsonValidation.getString(json, "pub");
		String pk = JsonValidation.getString(json, "priv_pw");
		Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));
		
		changePassword(user, json);
								       			
		// response
		AuditManager.instance.success();
		return ok();		
	}
	
	public static void changePassword(User user, JsonNode json) throws AppException {
		
		String password = JsonValidation.getPassword(json, "password");
		String pub = JsonValidation.getString(json, "pub");
		String pk = JsonValidation.getString(json, "priv_pw");
		Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));		
		user.password = Member.encrypt(password);
		user.publicExtKey = KeyManager.instance.readExternalPublicKey(pub);		  
		KeyManager.instance.saveExternalPrivateKey(user._id, pk);		  		  		 
		user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;				  								
		user.updatePassword();		  
		KeyManager.instance.newFutureLogin(user);	
		PWRecovery.storeRecoveryData(user._id, recover);
		
	}
	
    public static void startRecovery(User user, JsonNode json) throws AppException {
		
		String password = JsonValidation.getPassword(json, "password");
		String pub = JsonValidation.getString(json, "pub");
		String pk = JsonValidation.getString(json, "priv_pw");
		Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));
		
		KeyRecoveryData krd = KeyRecoveryData.getById(user._id);
		
		FutureLogin fl = FutureLogin.getById(user._id);
		
		KeyRecoveryProcess process = new KeyRecoveryProcess();
		process._id = user._id;
		process.name = user.email;
		process.started = new Date();
		process.nextPublicExtKey = KeyManager.instance.readExternalPublicKey(pub);
		process.nextPk = pk;
		process.nextPassword = Member.encrypt(password);
		process.shares = new HashMap<String, String>();
		//process.shares.put("encrypted", krd.shares.get("encrypted"));
		//process.shares.put("iv", krd.shares.get("iv"));
		process.encShares = krd.shares;
		process.nextShares = recover;
		process.intPart = fl.intPart;
		process.challenge = Base64.getEncoder().encodeToString(Arrays.copyOfRange(fl.extPartEnc, 4, fl.extPartEnc.length));
		KeyRecoveryProcess.update(process);
						
	}
    
    @APICall
	@Security.Authenticated(AdminSecured.class)
    public Result getUnfinished() throws AppException {
    	Set<KeyRecoveryProcess> open = KeyRecoveryProcess.getUnfinished();
    	
    	return ok(JsonOutput.toJson(open, "KeyRecoveryProcess", KeyRecoveryProcess.ALL)).as("application/json");
    }
	
}
