package controllers;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import models.Admin;
import models.Developer;
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
import models.enums.AccountActionFlags;
import models.enums.AccountNotifications;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.EMailStatus;
import models.enums.MessageReason;
import models.enums.ParticipationStatus;
import models.enums.PluginStatus;
import models.enums.SecondaryAuthType;
import models.enums.StudyAppLinkType;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.OAuthRefreshToken;
import utils.auth.PortalSessionToken;
import utils.auth.PreLoginSecured;
import utils.auth.OAuthCodeToken;
import utils.auth.ExtendedSessionToken;
import utils.auth.TokenCrypto;
import utils.auth.auth2factor.Authenticator;
import utils.auth.auth2factor.Authenticators;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.evolution.PostLoginActions;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;
import utils.messaging.SMSUtils;

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
	
	/*
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
		if (redirectUri==null || redirectUri.length()==0) throw new BadRequestException("error.internal", "Missing redirectUri in request.");
		if (app.redirectUri==null && !redirectUri.startsWith(InstanceConfig.getInstance().getPortalOriginUrl()+"/#/")) throw new InternalServerException("error.internal", "No redirect URI set for app.");		
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
				
				  				  
				  Set<Study> studies = Study.getAll(null, CMaps.map("_id", ids), Sets.create("_id", "name"));
				  ObjectNode obj = Json.newObject();								
				  obj.put("studies", JsonOutput.toJsonNode(studies, "Study", Sets.create("_id", "name")));					
			   
				  return ok(obj);
			  }
			}
		}
		
		
		if (sessionToken != null || !user.security.equals(AccountSecurityLevel.KEY_EXT_PASSWORD)) {
		  AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user, app._id);
		}
		
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
			} else appInstance = null;
			
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
			
			if (sessionToken == null && user.security.equals(AccountSecurityLevel.KEY_EXT_PASSWORD)) return Application.loginChallenge(user);
			boolean autoConfirm = KeyManager.instance.unlock(user._id, sessionToken, user.publicExtKey) == KeyManager.KEYPROTECTION_NONE;
			executor = autoConfirm ? user._id : null;
			
			if (appInstance != null && autoConfirm) {
			  MobileAPI.refreshApp(appInstance, executor, app._id, user, phrase);	
			} else {						
			  appInstance = MobileAPI.installApp(executor, app._id, user, phrase, autoConfirm, confirmStudy);
			}
			if (executor == null) executor = appInstance._id;
   		    meta = RecordManager.instance.getMeta(executor, appInstance._id, "_app").toMap();
	
									
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
	}*/

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
        	
        	OAuthRefreshToken refreshToken = OAuthRefreshToken.decrypt(refresh_token);
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
            
    		ExtendedSessionToken tk = ExtendedSessionToken.decrypt(code);
    		if (tk == null || tk.ownerId != null) throw new BadRequestException("error.internal", "invalid_grant");
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
    		phrase = tk.device;
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
        OAuthRefreshToken refresh = new OAuthRefreshToken(appInstance.applicationId, appInstance._id, appInstance.owner, aeskey, System.currentTimeMillis());
		
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
	
	/**
	 * Validate app used for login
	 * @param token
	 * @param json
	 * @return
	 * @throws AppException
	 */
	private static final Plugin validatePlugin(ExtendedSessionToken token, JsonNode json) throws AppException {
		
		
		// MIDATA PORTAL ??
				 
		Plugin app;
		if (token.appId == null) {
		   String name = JsonValidation.getString(json, "appname");
		   app = Plugin.getByFilename(name, Sets.create("type", "name", "redirectUri", "requirements", "termsOfUse", "unlockCode"));
		} else {			
		   app = Plugin.getById(token.appId, Sets.create("type", "name", "redirectUri", "requirements", "termsOfUse", "unlockCode"));			
		}
		
		// Check app
		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");		
		if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
		
		// Check redirect URI
		if (token.appId == null) {
			String redirectUri = JsonValidation.getString(json, "redirectUri");
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
		}
		
		// Check unlock code
		if (app.unlockCode != null) {				
			String code = JsonValidation.getStringOrNull(json, "unlockCode");
			if (code != null) {
				if (!app.unlockCode.toUpperCase().equals(code.toUpperCase())) throw new JsonValidationException("error.invalid.unlock_code", "unlockCode", "invalid", "Invalid unlock code");
				token.setAppUnlockedWithCode();
			}
		}	
						
		token.appId = app._id;
		return app;
	}
	
	/**
	 * Determine all requirements for login request
	 * @param token
	 * @param app
	 * @param role
	 * @param links
	 * @param confirmStudy
	 * @return
	 * @throws AppException
	 */
	private static final Set<UserFeature> determineRequirements(ExtendedSessionToken token, Plugin app, Set<StudyAppLink> links, Set<MidataId> confirmStudy) throws AppException {
		Set<UserFeature> requirements;
		if (token.userRole == null) throw new NullPointerException();
		
		if (app == null) {
			requirements = InstanceConfig.getInstance().getInstanceType().defaultRequirementsPortalLogin(token.userRole);
			return requirements;
		}
		
		requirements = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(token.userRole);
		if (app.requirements != null) requirements.addAll(app.requirements);
		
		
		for (StudyAppLink sal : links) {
			if (sal.isConfirmed() && sal.active && ((sal.type.contains(StudyAppLinkType.OFFER_P) && confirmStudy.contains(sal.studyId)) || sal.type.contains(StudyAppLinkType.REQUIRE_P))) {
				Study study = Study.getById(sal.studyId, Sets.create("requirements", "executionStatus"));				
				if (study.requirements != null) requirements.addAll(study.requirements);				
			}
		}
		
		if (requirements.contains(UserFeature.AUTH2FACTOR)) requirements.add(UserFeature.AUTH2FACTORSETUP);
		
		return requirements;
	}
	
	/**
	 * ready code challenge
	 * @param token
	 * @param json
	 */
	private static final void readyCodeChallenge(ExtendedSessionToken token, JsonNode json) {
		token.codeChallenge = JsonValidation.getStringOrNull(json, "code_challenge");
	    token.codeChallengeMethod = JsonValidation.getStringOrNull(json, "code_challenge_method");
	    
	}
	
	/**
	 * identify user to be logged in
	 * @param token
	 * @param json
	 * @return
	 * @throws AppException
	 */
	private static final User identifyUserForLogin(ExtendedSessionToken token, JsonNode json) throws AppException {
		if (token.userRole == null) throw new NullPointerException();
		
		User user = null;
		UserRole role = token.userRole;
		
		if (token.ownerId != null) {			
			switch (role) {
			  case MEMBER : user = Member.getById(token.ownerId, User.FOR_LOGIN);break;
			  case PROVIDER : user = HPUser.getById(token.ownerId, Sets.create(User.FOR_LOGIN, "provider"));break; 
			  case RESEARCH : user = ResearchUser.getById(token.ownerId, Sets.create(User.FOR_LOGIN, "organization"));break;
			  case DEVELOPER : user = Developer.getById(token.ownerId, User.FOR_LOGIN);break;
			  case ADMIN : user = Admin.getById(token.ownerId, User.FOR_LOGIN);break;
			}
			
		} else {
			String username = json.has("username") ? JsonValidation.getEMail(json, "username") : JsonValidation.getEMail(json, "email");
		
			switch (role) {
			  case MEMBER : user = Member.getByEmail(username, User.FOR_LOGIN);break;
			  case PROVIDER : user = HPUser.getByEmail(username, Sets.create(User.FOR_LOGIN, "provider"));break; 
			  case RESEARCH : user = ResearchUser.getByEmail(username, Sets.create(User.FOR_LOGIN, "organization"));break;
			  case DEVELOPER : user = Developer.getByEmail(username, User.FOR_LOGIN);				
				               if (user == null || user.role == UserRole.ADMIN) {
					              user = Admin.getByEmail(username, User.FOR_LOGIN);
					              if (user != null) token.userRole = UserRole.ADMIN;
				               }
				               break;
			}
		}
										    				
		if (user == null) throw new BadRequestException("error.invalid.credentials", "Unknown user or bad password");
		
		if (user.developer != null) token.developerId = user.developer;
		if (user instanceof HPUser) {
			token.orgId = ((HPUser) user).provider;
		}
		if (user instanceof ResearchUser) {
			token.orgId = ((ResearchUser) user).organization;
		}
						
		return user;
	}
	
	private static final Result checkPasswordAuthentication(ExtendedSessionToken token, JsonNode json, User user) throws AppException {
		if (token.ownerId != null) return null;
						
		String password = JsonValidation.getString(json, "password");
		String sessionToken = JsonValidation.getStringOrNull(json, "sessionToken");
				
		if (user.publicExtKey == null) {
			if (!json.has("nonHashed")) {
			  if (password.length() > 50) return ok("compatibility-mode");
			} else {
			   password = JsonValidation.getString(json, "nonHashed");
			}
		}
		
		try {
		  Result reccheck = PWRecovery.checkAuthentication(token, user, password, sessionToken);
		  if (reccheck != null) return reccheck;
		} catch (BadRequestException e) {
			AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user, token.appId);
			throw e;
		}
		
		token.ownerId = user._id;		
		return null;
		
	}
	
	/**
	 * non portal apps require study selection for researchers
	 * @param token
	 * @param json
	 * @return
	 * @throws AppException
	 */
	private static final Result checkStudySelectionRequired(ExtendedSessionToken token, JsonNode json) throws AppException {
		if (token.ownerId == null) throw new NullPointerException();
		
		if (token.userRole.equals(UserRole.RESEARCH) && token.appId != null && token.studyId == null) {
			MidataId studyContext = json.has("studyLink") ? JsonValidation.getMidataId(json, "studyLink") : null;
			
			if (studyContext == null) {
			  Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByMember(token.ownerId);
			  if (ugms.size() == 1) studyContext = ugms.iterator().next().userGroup;
			  else if (ugms.size() > 1) {
				  
				  Set<MidataId> ids = new HashSet<MidataId>();
				  for (UserGroupMember ugm : ugms) ids.add(ugm.userGroup);	
				  				  				  				  
				  Set<Study> studies = Study.getAll(null, CMaps.map("_id", ids), Sets.create("_id", "name"));
				  ObjectNode obj = Json.newObject();								
				  obj.set("studies", JsonOutput.toJsonNode(studies, "Study", Sets.create("_id", "name")));
				  obj.put("sessionToken", token.encrypt());
			   
				  return ok(obj);
			  }
			} else token.studyId = studyContext;
		}
	
		return null;
	}
	
	private static final void checkTwoFactorRequired(ExtendedSessionToken token, JsonNode json, User user, Set<UserFeature> notok) throws AppException {
				
		String securityToken = JsonValidation.getStringOrNull(json, "securityToken");
				
		if (notok!=null && token.securityToken != null) {
			notok.remove(UserFeature.AUTH2FACTOR);		
			return;
		}
		
		if (notok!=null && (notok.contains(UserFeature.EMAIL_VERIFIED) || notok.contains(UserFeature.ADMIN_VERIFIED) || notok.contains(UserFeature.ADDRESS_VERIFIED) || notok.contains(UserFeature.MIDATA_COOPERATIVE_MEMBER) || notok.contains(UserFeature.PHONE_ENTERED))) {
			notok.remove(UserFeature.AUTH2FACTOR);
			notok.remove(UserFeature.AUTH2FACTORSETUP);
			notok.remove(UserFeature.PHONE_VERIFIED);
			return;
		}
		
		if (notok!=null && notok.contains(UserFeature.AUTH2FACTORSETUP)) {
			notok.remove(UserFeature.AUTH2FACTOR);			
			notok.remove(UserFeature.PHONE_VERIFIED);
			return;
		}
		
		if (notok!=null && (notok.contains(UserFeature.AUTH2FACTOR))) {
			if (user.authType == SecondaryAuthType.SMS && user.mobileStatus != EMailStatus.VALIDATED) {
			  notok.remove(UserFeature.AUTH2FACTOR);
			  notok.add(UserFeature.PHONE_VERIFIED);
			}
		}
		
		if (notok!=null && notok.contains(UserFeature.PHONE_VERIFIED)) {
			if (securityToken == null) {
				Authenticators.getInstance(SecondaryAuthType.SMS).startAuthentication(user._id, "Code", user);
				notok.clear();
				notok.add(UserFeature.PHONE_VERIFIED);
			} else {
				if (securityToken.equals("_FAIL")) {
					user.mobile = null;
					user.mobileStatus = EMailStatus.UNVALIDATED;
					user.authType = null;					
					User.set(user._id, "mobile", user.mobile);
					User.set(user._id, "mobileStatus", user.mobileStatus);
					User.set(user._id, "authType", user.authType);
					Authenticators.getInstance(SecondaryAuthType.SMS).finishAuthentication(user._id, user);
					notok.remove(UserFeature.PHONE_VERIFIED);
					notok.remove(UserFeature.AUTH2FACTOR);
					notok.add(UserFeature.AUTH2FACTORSETUP);
				} else {
				
					Authenticators.getInstance(user.authType).checkAuthentication(user._id, user, securityToken);
					token.securityToken = securityToken;
					notok.remove(UserFeature.AUTH2FACTOR);
					notok.remove(UserFeature.PHONE_VERIFIED);
					user.mobileStatus = EMailStatus.VALIDATED;
					User.set(user._id, "mobileStatus", user.mobileStatus);
					if (notok.isEmpty()) {
						notok = null;
						Authenticators.getInstance(user.authType).finishAuthentication(user._id, user);
					}
				}
			}
		}
		
		if (notok!=null && notok.contains(UserFeature.AUTH2FACTOR)) {
			if (securityToken == null) {
				Authenticators.getInstance(user.authType).startAuthentication(user._id, "Token", user);
				notok.clear();
				notok.add(UserFeature.AUTH2FACTOR);
			} else {
				Authenticators.getInstance(user.authType).checkAuthentication(user._id, user, securityToken);
				token.securityToken = securityToken;
				notok.remove(UserFeature.AUTH2FACTOR);
				if (notok.isEmpty()) {
					notok = null;
					Authenticators.getInstance(user.authType).finishAuthentication(user._id, user);
				}
			}
		}
	}
	
	private static final MobileAppInstance checkExistingAppInstance(ExtendedSessionToken token, JsonNode json) throws AppException {
		// Portal
		if (token.appId == null) return null;
		
		if (token.device == null || token.appId == null || token.ownerId == null) throw new NullPointerException();
		
        MobileAppInstance appInstance = MobileAPI.getAppInstance(token.device, token.appId, token.ownerId, Sets.create("owner", "applicationId", "status", "passcode", "appVersion"));		
		
		//KeyManager.instance.login(60000l, false);		
		
		if (appInstance != null) {
			if (verifyAppInstance(appInstance, token.ownerId, token.appId)) {
				token.appInstanceId = appInstance._id;
				token.setAppUnlockedWithCode();
				token.setAppConfirmed();
			} else {
				appInstance = null;
				token.setAppUnlockedWithCode();
			}
			
		}
	
		return appInstance;
	}
	
	private static final Result checkAppConfirmationRequired(ExtendedSessionToken token, JsonNode node, Set<StudyAppLink> links) throws AppException {
		if (token.appId == null) return null;
		
		if (!token.getAppConfirmed()) {
			AuditManager.instance.fail(0, "Confirmation required", "error.missing.confirmation");
			boolean allRequired = true;
			for (StudyAppLink sal : links) {
				if (sal.isConfirmed() && sal.active && (sal.type.contains(StudyAppLinkType.REQUIRE_P) || sal.type.contains(StudyAppLinkType.OFFER_P))) {
					allRequired = allRequired && checkAlreadyParticipatesInStudy(sal.studyId, token.ownerId);
				}
			}
			return allRequired ? ok("CONFIRM-STUDYOK") : ok("CONFIRM");				
		}

		return null;
	}
			
	private static MobileAppInstance loginAppInstance(ExtendedSessionToken token, MobileAppInstance appInstance, User user, boolean autoConfirm) throws AppException {
		if (appInstance != null && autoConfirm) {
			  MobileAPI.refreshApp(appInstance, token.currentExecutor, token.appId, user, token.device);	
		} else {						
			  appInstance = MobileAPI.installApp(token.currentExecutor, token.appId, user, token.device, autoConfirm, token.confirmations);
		}
		if (token.currentExecutor == null) token.currentExecutor = appInstance._id;
		Map<String, Object> meta = RecordManager.instance.getMeta(token.currentExecutor, appInstance._id, "_app").toMap();
											
		if (!token.device.equals(meta.get("phrase"))) throw new InternalServerException("error.internal", "Internal error while validating consent");
        token.appInstanceId = appInstance._id;
		return appInstance;
	}
		
	
	private static void selectStudyContext(ExtendedSessionToken token) throws AppException {

		if (token.userRole.equals(UserRole.RESEARCH) && token.studyId != null) {
			if (token.appInstanceId == null) throw new NullPointerException();
			
			BasicBSONObject m = (BasicBSONObject) RecordManager.instance.getMeta(token.currentExecutor, token.appInstanceId, "_query");
			String old = m.getString("link-study");
			if (old != null && old.equals(token.studyId.toString())) { }
			else {
			  m.put("link-study", token.studyId.toString());
			  RecordManager.instance.setMeta(token.currentExecutor, token.appInstanceId, "_query", m.toMap());
			}
		}
	}
	
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result login() throws AppException {
			
        JsonNode json = request().body().asJson();		
        JsonValidation.validate(json, "appname", "username", "password", "device", "state", "redirectUri");

        ExtendedSessionToken token = new ExtendedSessionToken();
        token.created = System.currentTimeMillis();                               
        token.userRole = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;                
		token.state = JsonValidation.getString(json, "state");
		token.device = JsonValidation.getString(json, "device");
									    				
	    // Validate Mobile App	
		Plugin app = validatePlugin(token, json);		
		readyCodeChallenge(token, json);
		
		return loginHelper(token, json, app, null);
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(PreLoginSecured.class)
	public Result continuelogin() throws AppException {
		JsonNode json = request().body().asJson();		
       
		PortalSessionToken token1 = PortalSessionToken.session();
		ExtendedSessionToken token;
		if (token1 instanceof ExtendedSessionToken) {
           token = (ExtendedSessionToken) token1;
		} else {
			token = new ExtendedSessionToken();
			token.ownerId = token1.ownerId;
			token.orgId = token1.orgId;
			token.developerId = token1.developerId;			
			token.userRole = token1.userRole;
            token.handle = token1.handle;			
			token.created = token1.created;
            token.remoteAddress = token1.remoteAddress;
            token.setPortal();
		}
        
        Plugin app = token.getPortal() ? null : validatePlugin(token, json);
                       
        return loginHelper(token, json, app, null);
	}
	
	public static Result loginHelper() throws AppException {
		PortalSessionToken tk = PortalSessionToken.session();
		if (tk instanceof ExtendedSessionToken) {
			ExtendedSessionToken token = (ExtendedSessionToken) tk;
			token.currentExecutor = tk.ownerId;
			JsonNode json = Json.newObject();
			Plugin app = token.appId != null ? validatePlugin(token, json) : null;
			return loginHelper(token, json, app, token.currentExecutor);
		} else {
			ExtendedSessionToken token = new ExtendedSessionToken();
			token.ownerId = tk.ownerId;
			token.orgId = tk.orgId;
			token.developerId = tk.developerId;			
			token.userRole = tk.userRole;
            token.handle = tk.handle;			
			token.created = tk.created;
            token.remoteAddress = tk.remoteAddress;
            token.currentExecutor = tk.ownerId;
            token.setPortal();
            JsonNode json = Json.newObject();
            return loginHelper(token, json, null, token.currentExecutor);
		}
		//throw new AuthException("error.internal", "Wrong token type");
	}
	
	public static Result loginHelper(ExtendedSessionToken token, JsonNode json, Plugin app, MidataId currentExecutor) throws AppException {

		token.currentExecutor = currentExecutor;		
		
		if (app != null) {
			boolean confirmed = JsonValidation.getBoolean(json, "confirm");
			if (confirmed) token.setAppConfirmed();
			
			Set<MidataId> confirmStudy = json.has("confirmStudy") ? JsonExtraction.extractMidataIdSet(json.get("confirmStudy")) : null;
			if (confirmStudy != null) token.confirmations = confirmStudy;
		} else token.setPortal();						 
		
		Set<StudyAppLink> links = token.appId != null ? StudyAppLink.getByApp(token.appId) : null;		
		Set<UserFeature> requirements = determineRequirements(token, app, links, token.confirmations);
					
																		
		User user = identifyUserForLogin(token, json);		
		Result pw = checkPasswordAuthentication(token, json, user);
		if (pw != null) return pw;
						        				
		Result studySelectionRequired = checkStudySelectionRequired(token, json);
		if (studySelectionRequired != null) return studySelectionRequired;
					
		Set<UserFeature> notok = Application.loginHelperPreconditionsFailed(user, requirements);
		
		int keyType;
		if (token.handle != null) {			
			if (token.currentExecutor == null) {
				KeyManager.instance.continueSession(token.handle);
				token.currentExecutor = token.ownerId;
			}
			keyType = KeyManager.KEYPROTECTION_NONE;
		} else {
			String sessionToken = JsonValidation.getStringOrNull(json, "sessionToken");
			if (sessionToken == null && user.security.equals(AccountSecurityLevel.KEY_EXT_PASSWORD)) return Application.loginChallenge(token, user);
			token.handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true); // TODO Check lifetime
			keyType = KeyManager.instance.unlock(user._id, sessionToken, user.publicExtKey); 
		}		
		
		MobileAppInstance appInstance = checkExistingAppInstance(token, json);	
	
		Result recheck = checkAppConfirmationRequired(token, json, links);
		if (recheck != null) {
			if (token.handle != null) KeyManager.instance.persist(user._id);
			return recheck;
		}
		
		if (app != null && app.unlockCode != null && !token.getAppUnlockedWithCode()) {				
		   throw new JsonValidationException("error.invalid.unlock_code", "unlockCode", "invalid", "Invalid unlock code");
		}
		
		checkTwoFactorRequired(token, json, user, notok);
		
		if (notok != null && !notok.isEmpty()) {
		  if (token.handle != null) KeyManager.instance.persist(user._id);
		  if (notok.contains(UserFeature.PASSWORD_SET)) notok = Collections.singleton(UserFeature.PASSWORD_SET);		  	
		  if (notok.contains(UserFeature.EMAIL_VERIFIED) && !notok.contains(UserFeature.EMAIL_ENTERED)) notok = Collections.singleton(UserFeature.EMAIL_VERIFIED);
		  if (notok.contains(UserFeature.ADMIN_VERIFIED)) notok = Collections.singleton(UserFeature.ADMIN_VERIFIED);
		  
		  
		  return Application.loginHelperResult(token, user, notok);
		}
			
		
		
		if (app != null) {
			token.currentExecutor = keyType == KeyManager.KEYPROTECTION_NONE ? user._id : null;
				
			appInstance = loginAppInstance(token, appInstance, user, keyType == KeyManager.KEYPROTECTION_NONE);
			
			selectStudyContext(token);
							
			token.aeskey = KeyManager.instance.newAESKey(appInstance._id);		
										
			ObjectNode obj = Json.newObject();								
			obj.put("code", token.asCodeExchangeToken().encrypt());
			obj.put("istatus", appInstance.status.toString());
			
			AuditManager.instance.success();
					
			return ok(obj);
		} else {
			AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user);
			
			ObjectNode obj = Json.newObject();	
			if (keyType == 0 && token.handle != null) {
				  user = PostLoginActions.check(user);			  
				  KeyManager.instance.persist(user._id);
	        }	
			
			if (user.notifications != null && user.notifications == AccountNotifications.LOGIN) {
				Messager.sendMessage(app != null ? app._id : RuntimeConstants.instance.portalPlugin, MessageReason.LOGIN, null, Collections.singleton(user._id), InstanceConfig.getInstance().getDefaultLanguage(), new HashMap<String, String>());
			}
			 				
			obj.put("keyType", keyType);
			obj.put("role", user.role.toString().toLowerCase());
			obj.set("subroles", Json.toJson(user.subroles));
			obj.set("lastLogin", Json.toJson(user.login));
			token.setRemoteAddress(request());
			obj.put("sessionToken", token.asPortalSession().encrypt());
			  
			User.set(user._id, "login", new Date());			    
			AuditManager.instance.success();
			
			return ok(obj);
		}
	}
	
}
