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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.Admin;
import models.KeyInfoExtern;
import models.KeyRecoveryData;
import models.KeyRecoveryProcess;
import models.Member;
import models.MidataId;
import models.User;
import models.enums.AccountActionFlags;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.MessageReason;
import play.libs.Json;
import play.mvc.BodyParser;
import utils.InstanceConfig;
import utils.PasswordHash;
import utils.RuntimeConstants;
import utils.access.EncryptionUtils;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.FutureLogin;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;
import utils.messaging.ServiceHandler;
import play.mvc.Result;
import play.mvc.Security;

// read -p "Enter Share:" x;printf $x| base64 -d |openssl rsautl -decrypt -inkey key.pem;echo

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
		  	
		finishRecovery(user, proc, sessionToken);
		
		Map<String,String> replacements = new HashMap<String, String>();
		String site = "https://" + InstanceConfig.getInstance().getPortalServerDomain();		  			  		  		  
		replacements.put("site", site);
		Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.USER_PRIVATE_KEY_RECOVERED, null, Collections.singleton(user._id), null, replacements);
		
		return ok();
	}
	
	public static void finishRecovery(User user, KeyRecoveryProcess proc, String sessionToken) throws AppException {
		FutureLogin fl = FutureLogin.getById(user._id);
		fl.intPart = proc.intPart;
		fl.set();
		
		if (sessionToken != null) KeyManager.instance.unlock(user._id, sessionToken, proc.nextPublicExtKey);		
		
		if (user.email.equals(RuntimeConstants.BACKEND_SERVICE)) {
			
			provideServiceKey(user);
			proc.nextPassword = null;
		}
		
		
		user.publicExtKey = proc.nextPublicExtKey;
		user.password = proc.nextPassword;
		user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;		  
		KeyManager.instance.saveExternalPrivateKey(user._id, proc.nextPk);
		user.updatePassword();  		
		KeyManager.instance.newFutureLogin(user);	
		PWRecovery.storeRecoveryData(user._id, proc.nextShares);
		user.removeFlag(AccountActionFlags.KEY_RECOVERY);
		KeyRecoveryProcess.delete(user._id);
	}
	
	private static void provideServiceKey(User user) throws AppException {
		BSONObject obj = RecordManager.instance.getMeta(user._id, user._id, "_aeskey");
		if (obj == null) {
			Map<String, Object> map = new HashMap<String, Object>();
			byte[] key = EncryptionUtils.randomize(EncryptionUtils.generateKey());
			map.put("key", key);
			RecordManager.instance.setMeta(user._id, user._id, "_aeskey", map);
			ServiceHandler.setKey(key);
			ServiceHandler.shareKey();
		} else {
			byte[] aeskey = (byte[]) obj.get("key");
			ServiceHandler.setKey(aeskey);
			ServiceHandler.shareKey();
		}
	}
	
	public static void finishRecovery(User user) throws AppException {
		KeyRecoveryProcess proc = KeyRecoveryProcess.getById(user._id);
		finishRecovery(user, proc, null);
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
		
		changePassword(user, json);
								       			
		// response
		AuditManager.instance.success();
		return ok();		
	}
	
	public static void changePassword(User user, JsonNode json) throws AppException {
		
		String password = JsonValidation.getPassword(json, "password");
		String pub = JsonValidation.getStringOrNull(json, "pub");
		String pk = JsonValidation.getStringOrNull(json, "priv_pw");		
		user.password = Member.encrypt(password);
		
		if (pub != null) {
			Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));	
			user.publicExtKey = KeyManager.instance.readExternalPublicKey(pub);		
			
			if (user.recoverKey == null) user.recoverKey = JsonValidation.getStringOrNull(json, "recoverKey");
			
			KeyManager.instance.saveExternalPrivateKey(user._id, pk);		  		  		 
			user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;				  								
			user.updatePassword();		  
			KeyManager.instance.newFutureLogin(user);	
			PWRecovery.storeRecoveryData(user._id, recover);
		} else {
			user.publicExtKey = null;
			user.recoverKey = null;
			user.security = AccountSecurityLevel.KEY;
			KeyManager.instance.removeExternalPrivateKey(user._id);
			user.updatePassword();
		}
		
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
		if (krd != null) {
		    process.encShares = krd.shares;
		} else {
			process.encShares = new HashMap<String, String>();
		}
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
    
    public static Result checkAuthentication(PortalSessionToken token, User user, String password, String sessionToken) throws AppException {
    	try {
	    	if (user.flags != null && user.flags.contains(AccountActionFlags.KEY_RECOVERY)) {
	    		if (PasswordHash.validatePassword(password, user.password)) {
	    			KeyRecoveryProcess.delete(user._id);
	    			user.removeFlag(AccountActionFlags.KEY_RECOVERY);
	    			return null;
	    		} else {
	    			KeyRecoveryProcess rec = KeyRecoveryProcess.getById(user._id);	    			
		    		if (PasswordHash.validatePassword(password, rec.nextPassword)) {
		    			if (sessionToken == null) {		    			
			    			ObjectNode obj = Json.newObject();
			    			
			    			obj.put("challenge", rec.challenge);
			    			//obj.put("keyEncrypted", key.privateKey);
			    			//obj.put("pub", user.publicExtKey);
			    			obj.put("recoverKey", user.recoverKey);
			    			obj.put("userid", user._id.toString());
			    			obj.put("tryrecover", true);
			    			obj.put("sessionToken", token.encrypt());
			    			return ok(obj);
		    			} else {
		    				KeyManager.instance.login(60000, false);
		    				finishRecovery(user, rec, sessionToken);
		    				return Application.loginChallenge(token, user);
		    			}
		    				
		    		} else {
		    			user.authenticationValid(password);
		    			throw new BadRequestException("error.invalid.credentials",  "Invalid user or password.");  	    			
		    		}
	    		}
	    			    		
	    	} else {
	    		if (user.authenticationValid(password)) {
	    			return null;
	    		} else throw new BadRequestException("error.invalid.credentials",  "Invalid user or password.");
	    	}
    	} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (InvalidKeySpecException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		}
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    @APICall
	@Security.Authenticated(AdminSecured.class)
    public Result requestServiceKeyRecovery() throws AppException {
    	JsonNode json = request().body().asJson();	
    	KeyManager.instance.login(60000, false);
    	User autorun = Admin.getByEmail(RuntimeConstants.BACKEND_SERVICE, User.FOR_LOGIN);
  	    int keytype = KeyManager.instance.unlock(autorun._id, null);
  	    if (keytype == KeyManager.KEYPROTECTION_FAIL) {
      	  PWRecovery.startRecovery(autorun, json);
      	  autorun.addFlag(AccountActionFlags.KEY_RECOVERY);
  	    } else {
  	      if (!InstanceConfig.getInstance().getInstanceType().simpleServiceKeyProtection()) {
  	        PWRecovery.changePassword(autorun, json);
  	      }
  	      autorun.password = null;
  	      User.set(autorun._id, "password", null);
		  provideServiceKey(autorun);
  	    }
      	
      	return ok();
    }
	
}
