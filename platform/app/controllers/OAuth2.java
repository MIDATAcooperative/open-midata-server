package controllers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import actions.MobileCall;
import models.HPUser;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.ResearchUser;
import models.Study;
import models.StudyAppLink;
import models.StudyParticipation;
import models.User;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ParticipationStatus;
import models.enums.StudyAppLinkType;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.MobileAppToken;
import utils.auth.OAuthCodeToken;
import utils.auth.TokenCrypto;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.evolution.PostLoginActions;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class OAuth2 extends Controller {

	/**
	 * handle OPTIONS requests. 
	 * @return status ok
	 */
	@MobileCall
	public Result checkPreflight() {		
		return ok();
	}
 	
	public static long OAUTH_CODE_LIFETIME = 1000l * 60l * 5l;
			
	
	private static boolean verifyAppInstance(MobileAppInstance appInstance, MidataId ownerId, MidataId applicationId) throws AppException {
		if (appInstance == null) return false;
        if (!appInstance.owner.equals(ownerId)) throw new InternalServerException("error.invalid.token", "Wrong app instance owner!");
        if (!appInstance.applicationId.equals(applicationId)) throw new InternalServerException("error.invalid.token", "Wrong app for app instance!");
        
        if (appInstance.status.equals(ConsentStatus.EXPIRED) || appInstance.status.equals(ConsentStatus.REJECTED)) 
        	throw new BadRequestException("error.blocked.consent", "Consent expired or blocked.");
        
        Plugin app = Plugin.getById(appInstance.applicationId);
        
        AccessLog.log("app-instance:"+appInstance.appVersion+" vs plugin:"+app.pluginVersion);
        if (appInstance.appVersion != app.pluginVersion) {
        	MobileAPI.removeAppInstance(appInstance);
        	return false;
        }
        
        Set<StudyAppLink> links = StudyAppLink.getByApp(app._id);
        for (StudyAppLink sal : links) {
        	if (sal.isConfirmed() && sal.active && sal.type.contains(StudyAppLinkType.REQUIRE_P)) {
        		
        		
        		   StudyParticipation sp = StudyParticipation.getByStudyAndMember(sal.studyId, appInstance.owner, Sets.create("status", "pstatus"));
        		   
        		   if (sp == null) {
	               		MobileAPI.removeAppInstance(appInstance);
	                   	return false;
	               	}
	               	if ( 
	               		sp.pstatus.equals(ParticipationStatus.MEMBER_RETREATED) || 
	               		sp.pstatus.equals(ParticipationStatus.MEMBER_REJECTED) || 
	               		sp.pstatus.equals(ParticipationStatus.RESEARCH_REJECTED)) {
	               		throw new BadRequestException("error.blocked.consent", "Research consent expired or blocked.");
	               	}
        		   
        		
        	}
        }        
        return true;
	}
	
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result login() throws AppException {
				
        JsonNode json = request().body().asJson();		
        JsonValidation.validate(json, "appname", "username", "password", "device", "state", "redirectUri");

        UserRole role = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;
        
        String name = JsonValidation.getString(json, "appname");
		String state = JsonValidation.getString(json, "state");
		String redirectUri = JsonValidation.getString(json, "redirectUri"); 
		String device = JsonValidation.getString(json, "device");
			
		String code_challenge = JsonValidation.getStringOrNull(json, "code_challenge");
	    String code_challenge_method = JsonValidation.getStringOrNull(json, "code_challenge_method");
	    boolean confirmed = JsonValidation.getBoolean(json, "confirm");
	    Set<MidataId> confirmStudy = JsonExtraction.extractMidataIdSet(json.get("confirmStudy"));
	   					
	    // Validate Mobile App	
		Plugin app = Plugin.getByFilename(name, Sets.create("type", "name", "redirectUri", "requirements", "termsOfUse", "unlockCode"));
		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");		
		if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
		if (app.redirectUri==null) throw new InternalServerException("error.internal", "No redirect URI set for app.");
		if (redirectUri==null || redirectUri.length()==0) throw new BadRequestException("error.internal", "Missing redirectUri in request.");
		if (!redirectUri.equals(app.redirectUri) && !redirectUri.startsWith(InstanceConfig.getInstance().getPortalOriginUrl()+"/#/")) {
			String[] multiple = app.redirectUri.split(" ");
			boolean found = false;
			// if length is 1 the URL has already been tested
			if (multiple.length > 1) {
				for (String rUri : multiple) {
					if (rUri.equals(redirectUri)) {
						found = true;					
					}
				}
			}
			if (!found) throw new InternalServerException("error.internal", "Wrong redirect uri");
		}
		
		
		
		Set<UserFeature> requirements = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(role);
		if (app.requirements != null) requirements.addAll(app.requirements);
		
		Set<StudyAppLink> links = StudyAppLink.getByApp(app._id);
		for (StudyAppLink sal : links) {
			if (sal.isConfirmed() && sal.active && ((sal.type.contains(StudyAppLinkType.OFFER_P) && confirmStudy.contains(sal.studyId)) || sal.type.contains(StudyAppLinkType.REQUIRE_P))) {
				Study study = Study.getById(sal.studyId, Sets.create("requirements", "executionStatus"));				
				if (study.requirements != null) requirements.addAll(study.requirements);				
			}
		}
		
				
		MobileAppInstance appInstance = null;		
		Map<String, Object> meta = null;
		
		
		String username = JsonValidation.getEMail(json, "username");
		String password = JsonValidation.getString(json, "password");
		String sessionToken = JsonValidation.getStringOrNull(json, "sessionToken");
		
		String phrase = device;
					
		User user = null;
		switch (role) {
		case MEMBER : user = Member.getByEmail(username, User.ALL_USER_INTERNAL);break;
		case PROVIDER : user = HPUser.getByEmail(username, User.ALL_USER_INTERNAL);break; 
		case RESEARCH : user = ResearchUser.getByEmail(username, User.ALL_USER_INTERNAL);break; 
		}
		if (user == null) throw new BadRequestException("error.invalid.credentials", "Unknown user or bad password");
		
		if (user.publicExtKey == null) {
			if (!json.has("nonHashed")) return ok("compatibility-mode");
			password = JsonValidation.getString(json, "nonHashed");
		}
		
		Result reccheck = PWRecovery.checkAuthentication(user, password, sessionToken);
		if (reccheck != null) return reccheck;
		
        boolean authenticationValid =  true;   
		MidataId studyContext = null;
		if (role.equals(UserRole.RESEARCH) && authenticationValid) {
			studyContext = json.has("studyLink") ? JsonValidation.getMidataId(json, "studyLink") : null;
			
			if (studyContext == null) {
			  Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByMember(user._id);
			  if (ugms.size() == 1) studyContext = ugms.iterator().next().userGroup;
			  else if (ugms.size() > 1) {
				  
				  Set<MidataId> ids = new HashSet<MidataId>();
				  for (UserGroupMember ugm : ugms) ids.add(ugm.userGroup);	
				  
				  /*Set<MidataId> ids2 = new HashSet<MidataId>();
				  for (StudyAppLink sal : links) {
					  if (sal.isConfirmed() && sal.type.contains(StudyAppLinkType.DATALINK)) {
						  ids2.add(sal.studyId);
					  }
				  }
				  
				  if (!ids2.isEmpty()) ids.retainAll(ids2);*/
				  				  
				  Set<Study> studies = Study.getAll(null, CMaps.map("_id", ids), Sets.create("_id", "name"));
				  ObjectNode obj = Json.newObject();								
				  obj.put("studies", JsonOutput.toJsonNode(studies, "Study", Sets.create("_id", "name")));					
			   
				  return ok(obj);
			  }
			}
		}
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user, app._id);
		if (!authenticationValid) {
			throw new BadRequestException("error.invalid.credentials",  "Unknown user or bad password");
		}
		Set<UserFeature> notok = Application.loginHelperPreconditionsFailed(user, requirements);
		
		
		appInstance = MobileAPI.getAppInstance(phrase, app._id, user._id, Sets.create("owner", "applicationId", "status", "passcode", "appVersion"));		
		
		KeyManager.instance.login(60000l, false);
		MidataId executor = null;
		boolean alreadyUnlocked = false;
		if (appInstance != null) {
			if (verifyAppInstance(appInstance, user._id, app._id)) {
				confirmed = true;
				alreadyUnlocked = true;
			}
			
		}
		
		//if (appInstance == null) {		
			if (!confirmed) {
				AuditManager.instance.fail(0, "Confirmation required", "error.missing.confirmation");
				boolean allRequired = true;
				for (StudyAppLink sal : links) {
					if (sal.isConfirmed() && sal.active && (sal.type.contains(StudyAppLinkType.REQUIRE_P) || sal.type.contains(StudyAppLinkType.OFFER_P))) {
						allRequired = allRequired && checkAlreadyParticipatesInStudy(sal.studyId, user._id);
					}
				}
				return allRequired ? ok("CONFIRM-STUDYOK") : ok("CONFIRM");				
			}
			
			if (app.unlockCode != null && !alreadyUnlocked) {				
				String code = JsonValidation.getStringOrNull(json, "unlockCode");
				if (code == null || !app.unlockCode.toUpperCase().equals(code.toUpperCase())) throw new JsonValidationException("error.invalid.unlock_code", "unlockCode", "invalid", "Invalid unlock code");
			}			
			
			if (notok != null) {
			  return Application.loginHelperResult(user, notok);
			}
			
			if (sessionToken == null) return Application.loginChallenge(user);
			boolean autoConfirm = KeyManager.instance.unlock(user._id, sessionToken, user.publicExtKey) == KeyManager.KEYPROTECTION_NONE;
			executor = autoConfirm ? user._id : null;
			
			if (alreadyUnlocked) MobileAPI.removeAppInstance(appInstance);
			
			appInstance = MobileAPI.installApp(executor, app._id, user, phrase, autoConfirm, confirmStudy);				
			if (executor == null) executor = appInstance._id;
   		    meta = RecordManager.instance.getMeta(executor, appInstance._id, "_app").toMap();
		/*} else {	
						
			if (!verifyAppInstance(appInstance, user._id, app._id)) {
				boolean allRequired = true;
				for (StudyAppLink sal : links) {
					if (sal.isConfirmed() && (sal.type.contains(StudyAppLinkType.REQUIRE_P) || sal.type.contains(StudyAppLinkType.OFFER_P))) {
						allRequired = allRequired && checkAlreadyParticipatesInStudy(sal.studyId, user._id);
					}
				}
				return allRequired ? ok("CONFIRM-STUDYOK") : ok("CONFIRM");
			}
			
			if (sessionToken == null) return Application.loginChallenge(user);
			
			KeyManager.instance.unlock(user._id, sessionToken, user.publicExtKey);
			meta = RecordManager.instance.getMeta(user._id, appInstance._id, "_app").toMap();
			executor = user._id;
			
			if (notok != null) {
				  return Application.loginHelperResult(user, notok);
			}
		}*/
									
		if (!phrase.equals(meta.get("phrase"))) throw new InternalServerException("error.internal", "Internal error while validating consent");
		
		if (role.equals(UserRole.RESEARCH) && studyContext != null) {
			BasicBSONObject m = (BasicBSONObject) RecordManager.instance.getMeta(executor, appInstance._id, "_query");
			String old = m.getString("link-study");
			if (old != null && old.equals(studyContext.toString())) { }
			else {
			  m.put("link-study", studyContext.toString());
			  RecordManager.instance.setMeta(executor, appInstance._id, "_query", m.toMap());
			}
		}
		
		String aeskey = KeyManager.instance.newAESKey(appInstance._id);
		OAuthCodeToken tk = new OAuthCodeToken(appInstance._id, phrase, aeskey, System.currentTimeMillis(), state, code_challenge, code_challenge_method);
									
		ObjectNode obj = Json.newObject();								
		obj.put("code", tk.encrypt());
		obj.put("istatus", appInstance.status.toString());
		
		AuditManager.instance.success();
				
		return ok(obj);
	}

	@BodyParser.Of(BodyParser.FormUrlEncoded.class)
	@MobileCall
	public Result authenticate() throws AppException {
				
        Map<String, String[]> data = request().body().asFormUrlEncoded();
        MidataId appInstanceId = null;
        MobileAppInstance appInstance = null;
        Map<String, Object> meta = null;
        
        String aeskey = null;
        ObjectNode obj = Json.newObject();	      
        
        KeyManager.instance.login(60000l, false);
        
        if (data==null) throw new BadRequestException("error.internal", "Missing request body of type form/urlencoded.");
        if (!data.containsKey("grant_type")) throw new BadRequestException("error.internal", "Missing grant_type");
        User user = null;
        String grant_type = data.get("grant_type")[0];
        if (grant_type.equals("refresh_token")) {
        	if (!data.containsKey("refresh_token")) throw new BadRequestException("error.internal", "Missing refresh_token");
        	String refresh_token = data.get("refresh_token")[0];
        	
        	MobileAppToken refreshToken = MobileAppToken.decrypt(refresh_token);
        	if (refreshToken == null) throw new BadRequestException("error.internal", "Bad refresh_token.");
        	if (refreshToken.created + MobileAPI.DEFAULT_REFRESHTOKEN_EXPIRATION_TIME < System.currentTimeMillis()) return MobileAPI.invalidToken();
			appInstanceId = refreshToken.appInstanceId;
			
			appInstance = MobileAppInstance.getById(appInstanceId, Sets.create("owner", "appVersion", "applicationId", "status"));
			if (!verifyAppInstance(appInstance, refreshToken.ownerId, refreshToken.appId)) throw new BadRequestException("error.internal", "Bad refresh token.");
			
			Plugin app = Plugin.getById(appInstance.applicationId);
			user = User.getById(appInstance.owner, User.ALL_USER_INTERNAL);
			Set<UserFeature> req = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(user.role);
			if (app.requirements != null) req.addAll(app.requirements);
			Set<UserFeature> notok = Application.loginHelperPreconditionsFailed(user, req);
			if (notok != null) {
				return status(UNAUTHORIZED);
			}                       
			
            
            if (KeyManager.instance.unlock(appInstance._id, refreshToken.phrase) == KeyManager.KEYPROTECTION_FAIL) return status(UNAUTHORIZED); 
            meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
                        
            if (refreshToken.created != ((Long) meta.get("created")).longValue()) {
            	return status(UNAUTHORIZED);
            }
            
            aeskey = KeyManager.instance.newAESKey(appInstance._id);
    		
           
        } else if (grant_type.equals("authorization_code")) {
        	if (!data.containsKey("redirect_uri")) throw new BadRequestException("error.internal", "Missing redirect_uri");
            
            if (!data.containsKey("code")) throw new BadRequestException("error.internal", "Missing code");
            
            String client_id = null;
            String phrase = null;
                            
            String code = data.get("code")[0];
            String redirect_uri = data.get("redirect_uri")[0];
            if (data.containsKey("client_id")) {
              client_id = data.get("client_id")[0];
            } else {
            	Optional<String> authh = request().header("Authorization");
            	if (authh.isPresent() && authh.get().startsWith("Basic")) {
            		String authstr = authh.get().substring("Basic ".length());
            		int p = authstr.indexOf(':');
            		if (p > 0) client_id = authstr.substring(0, p);
            	}
            }
    		
            if (client_id == null) throw new BadRequestException("error.internal", "Missing client_id");
            
    		OAuthCodeToken tk = OAuthCodeToken.decrypt(code);
    		if (tk == null) throw new BadRequestException("error.internal", "invalid_grant");
    		if (tk.created + OAUTH_CODE_LIFETIME < System.currentTimeMillis()) throw new BadRequestException("error.internal", "invalid_grant");
    		//AccessLog.log("cs:"+tk.codeChallenge);
    		//AccessLog.log("csm:"+tk.codeChallengeMethod);
    		
    		if (tk.codeChallenge != null) {
    			String csa[] = data.get("code_verifier");
    			String csm = csa!=null && csa.length>0 ? csa[0] : null;
    			if (csm == null) throw new BadRequestException("error.internal", "invalid_grant");
    			
    			if (tk.codeChallengeMethod == null || tk.codeChallengeMethod.equals("plain")) {
    			  if (!csm.equals(tk.codeChallenge)) throw new BadRequestException("error.internal", "invalid_grant");
    			} else if (tk.codeChallengeMethod.equals("S256")) {
    			   if (!TokenCrypto.sha256ThenBase64(csm).equals(tk.codeChallenge)) throw new BadRequestException("error.internal", "invalid_grant");    			  
    			} else throw new BadRequestException("error.internal", "invalid_grant");
    		}
    				
    	    // Validate Mobile App	
    		Plugin app = Plugin.getByFilename(client_id, Sets.create("type", "name", "secret"));
    		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");		
    		if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
    		
    		appInstance = MobileAppInstance.getById(tk.appInstanceId, Sets.create("owner", "applicationId", "status", "passcode"));
    		if (appInstance == null) throw new BadRequestException("error.internal", "invalid_grant");
    		phrase = tk.passphrase;
    		aeskey = tk.aeskey;
    		obj.put("state", tk.state);
    		
    		user = User.getById(appInstance.owner, Sets.create("role", "status"));
    		if (user == null || user.status.equals(UserStatus.DELETED) || user.status.equals(UserStatus.BLOCKED)) throw new BadRequestException("error.internal", "invalid_grant");
    		
    		if (appInstance == null) throw new NullPointerException();												
    		if (appInstance.passcode != null && !User.phraseValid(phrase, appInstance.passcode)) throw new BadRequestException("error.invalid.credentials", "Wrong password.");			
    		if (KeyManager.instance.unlock(appInstance._id, aeskey != null ? aeskey : phrase) == KeyManager.KEYPROTECTION_FAIL) throw new BadRequestException("error.internal", "invalid_grant");
    	
    		meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();							
    		if (!phrase.equals(meta.get("phrase"))) throw new InternalServerException("error.internal", "Internal error while validating consent");
    		
        } else throw new BadRequestException("error.internal", "Unknown grant_type");
               											
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, aeskey, System.currentTimeMillis() + MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME, user.role); 
        MobileAppToken refresh = new MobileAppToken(appInstance.applicationId, appInstance._id, appInstance.owner, aeskey, System.currentTimeMillis());
		
        meta.put("created", refresh.created);
        RecordManager.instance.setMeta(appInstance._id, appInstance._id, "_app", meta);
        
        BSONObject q = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_query");
        if (q.containsField("link-study")) {
        	MidataId studyId = MidataId.from(q.get("link-study"));
        	MobileAPI.prepareMobileExecutor(appInstance, session);
        	controllers.research.Studies.autoApproveCheck(appInstance.applicationId, studyId, appInstance.owner);
        }
            	
        if (meta.containsKey("aliaskey") && meta.containsKey("alias")) {
			MidataId alias = new MidataId(meta.get("alias").toString());
			byte[] key = (byte[]) meta.get("aliaskey");
			KeyManager.instance.unlock(appInstance.owner, alias, key);			
			RecordManager.instance.clearCache();
			PostLoginActions.check(user);						
        }
        
        
		// create encrypted authToken		
											
		obj.put("access_token", session.encrypt());
		obj.put("token_type", "Bearer");
		obj.put("scope", "user/*.*");
		
		obj.put("expires_in", MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME / 1000l);
		obj.put("patient", appInstance.owner.toString());
		obj.put("refresh_token", refresh.encrypt());
				
		response().setHeader("Cache-Control", "no-store");
		response().setHeader("Pragma", "no-cache"); 
		
		return ok(obj);
	}
	
	private static boolean checkAlreadyParticipatesInStudy(MidataId linkedStudy, MidataId owner) throws InternalServerException {
		if (linkedStudy == null) return true;
        StudyParticipation sp = StudyParticipation.getByStudyAndMember(linkedStudy, owner, Sets.create("status", "pstatus"));
        if (sp == null) return false;        		
        if ( 
        	sp.pstatus.equals(ParticipationStatus.MEMBER_RETREATED) || 
        	sp.pstatus.equals(ParticipationStatus.MEMBER_REJECTED) || 
        	sp.pstatus.equals(ParticipationStatus.RESEARCH_REJECTED)) {
        	return false;
        }        	
        return true;
	}
}
