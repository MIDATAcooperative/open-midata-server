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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import actions.MobileCall;
import models.Admin;
import models.Consent;
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
import models.enums.EMailStatus;
import models.enums.JoinMethod;
import models.enums.LinkTargetType;
import models.enums.MessageReason;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationStatus;
import models.enums.RejoinPolicy;
import models.enums.SecondaryAuthType;
import models.enums.StudyAppLinkType;
import models.enums.UsageAction;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.InstanceConfig;
import utils.LinkTools;
import utils.PluginLoginCache;
import utils.RuntimeConstants;
import utils.TestAccountTools;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.ExecutionInfo;
import utils.auth.ExtendedSessionToken;
import utils.auth.KeyManager;
import utils.auth.LicenceChecker;
import utils.auth.MobileAppSessionToken;
import utils.auth.OAuthRefreshToken;
import utils.auth.OTPTools;
import utils.auth.PortalSessionToken;
import utils.auth.PreAuthSecured;
import utils.auth.PreLoginSecured;
import utils.auth.TokenCrypto;
import utils.auth.auth2factor.Authenticators;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.evolution.PostLoginActions;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.fhir.FHIRServlet;
import utils.fhir.ReadWriteResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;
import utils.stats.UsageStatsRecorder;

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
			
	
	public static boolean verifyAppInstance(AccessContext context, MobileAppInstance appInstance, MidataId ownerId, MidataId applicationId, Set<StudyAppLink> links) throws AppException {
		if (appInstance == null) return false;
        if (!appInstance.owner.equals(ownerId)) throw new InternalServerException("error.invalid.token", "Wrong app instance owner!");
        if (!appInstance.applicationId.equals(applicationId)) throw new InternalServerException("error.invalid.token", "Wrong app for app instance!");
        
        if (!appInstance.status.isSharingData()) {
        	AccessLog.log("appInstance not sharing data");
        	throw new BadRequestException("error.blocked.consent", "Consent expired or blocked.");
        }
        
        Plugin app = Plugin.getById(appInstance.applicationId);
        
        AccessLog.log("app-instance:"+appInstance.appVersion+" vs plugin:"+app.pluginVersion);
        if (appInstance.appVersion != app.pluginVersion) {
        	if (context!=null) ApplicationTools.removeAppInstance(context, appInstance.owner, appInstance);
        	return false;
        }
        
        if (!TestAccountTools.allowInstallation(context, ownerId, applicationId)) 
			return false;
	        
        if (links == null) links = StudyAppLink.getByApp(app._id);
        for (StudyAppLink sal : links) {
        	if (sal.isConfirmed() && sal.active && sal.type.contains(StudyAppLinkType.CHECK_P)) {
        		
        		if (sal.linkTargetType == LinkTargetType.ORGANIZATION || sal.linkTargetType == LinkTargetType.SERVICE) {
      			  Consent c = LinkTools.findConsentForAppLink(appInstance.owner, sal);
      			  if (c instanceof MobileAppInstance) {
      				MobileAppInstance mai = (MobileAppInstance) c;
      				Plugin checkedPlugin = Plugin.getById(mai.applicationId); 
      				if (checkedPlugin == null || mai.appVersion != checkedPlugin.pluginVersion) {
      					AccessLog.log("linked service outdated");
      					// Actively removing service will cause trouble with email notifications and subscriptions.
      					/*if (context != null) {	      					
	      					ApplicationTools.removeAppInstance(context, mai.owner, mai);
      					}*/
      					c = null;
      				}
      			  }
      			  if (c == null) {
      				  
	      				  AccessLog.log("remove instance due to missing linked service: "+sal.serviceAppId);
	      				if (context != null) ApplicationTools.removeAppInstance(context, appInstance.owner, appInstance);
      				  
		                  return false;
      			  }
      		   } else {
        		   StudyParticipation sp = StudyParticipation.getByStudyAndMember(sal.studyId, appInstance.owner, Sets.create("status", "pstatus"));
        		   
        		   if (sp == null) {
        			    AccessLog.log("remove instance due to missing project: "+sal.studyId);
        			    if (context != null) ApplicationTools.removeAppInstance(context, appInstance.owner, appInstance);
	                   	return false;
	               	}
				    if ( 
						sp.pstatus.equals(ParticipationStatus.MEMBER_RETREATED) || 
						sp.pstatus.equals(ParticipationStatus.MEMBER_REJECTED)) {
				    	    Study project = Study.getById(sp.study, Sets.create("rejoinPolicy"));
				    	    if (project != null && project.rejoinPolicy == RejoinPolicy.NO_REJOIN) {
							   throw new BadRequestException("error.blocked.projectconsent", "Research consent expired or blocked.");
				    	    } else {
				    	    	AccessLog.log("remove instance due to retreted from project: "+sal.studyId);
		        			    if (context != null) ApplicationTools.removeAppInstance(context, appInstance.owner, appInstance);
			                   	return false;
				    	    }
					}
				    if (sp.pstatus.equals(ParticipationStatus.RESEARCH_REJECTED)) {
							throw new BadRequestException("error.blocked.participation", "Research consent expired or blocked.");
					}
      		   }
        		
        	}
        }        
        return true;
	}
	
	/**
	 * Return OpenID Connect compatible user info
	 * @param request
	 * @return
	 * @throws AppException
	 */
	@MobileCall	
	public Result userinfo(Request request) throws AppException {
		Optional<String> param = request.header("Authorization");
		if (!param.isPresent() || !param.get().startsWith("Bearer ")) OAuth2.invalidToken();
		String token = param.get().substring("Bearer ".length());
	    AccessContext inf = ExecutionInfo.checkToken(request, token, false, false);
	    User user = User.getById(inf.getAccessor(), User.ALL_USER);
	    ObjectNode obj = Json.newObject();	 
	    
	    obj.put("sub", user._id.toString());
	    obj.put("name", user.firstname+" "+user.lastname);
	    obj.put("family_name", user.lastname);
	    obj.put("given_name", user.firstname);
	    obj.put("email", user.email);
	    obj.put("email_verified", (user.emailStatus == EMailStatus.VALIDATED || user.emailStatus == EMailStatus.EXTERN_VALIDATED));
        if (user.gender != null) obj.put("gender", user.gender.toString().toLowerCase());
        	    
	    return ok(obj).withHeader("Cache-Control", "no-store").withHeader("Pragma", "no-cache");
	}
	
	
	
	@BodyParser.Of(BodyParser.FormUrlEncoded.class)
	@MobileCall
	public Result authenticate(Request request) throws AppException {
				
        Map<String, String[]> data = request.body().asFormUrlEncoded();
       
        MobileAppInstance appInstance = null;
        AccessContext tempContext = null;
        Map<String, Object> meta = null;
        
        String aeskey = null;
        ObjectNode obj = Json.newObject();	      
        User user = null;
        KeyManager.instance.login(60000l, false);
        
        if (data==null) throw new BadRequestException("error.internal", "Missing request body of type form/urlencoded.");
        if (!data.containsKey("grant_type")) throw new BadRequestException("error.internal", "Missing grant_type");
        
        String grant_type = data.get("grant_type")[0];
        if (grant_type.equals("refresh_token")) {
        	if (!data.containsKey("refresh_token")) throw new BadRequestException("error.internal", "Missing refresh_token");
        	String refresh_token = data.get("refresh_token")[0];
			
			Pair<User, MobileAppInstance> pair = useRefreshToken(refresh_token);
			user = pair.getLeft();
			appInstance = pair.getRight();
            
			aeskey = KeyManager.instance.newAESKey(appInstance._id);
			tempContext = ContextManager.instance.createLoginOnlyContext(appInstance._id, user.role, appInstance);
			meta = RecordManager.instance.getMeta(tempContext, appInstance._id, "_app").toMap();
    		                        
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
            	Optional<String> authh = request.header("Authorization");
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
    		Plugin app = PluginLoginCache.getByFilename(client_id);
    		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");		
    		if (!app.type.equals("mobile")) throw new PluginException(app._id,"error.plugin", "Wrong application type. Only smartphone/web type applications may use the OAuth login.");
    		
    		appInstance = MobileAppInstance.getById(tk.appInstanceId, MobileAppInstance.APPINSTANCE_ALL);
    		if (appInstance == null) throw new BadRequestException("error.internal", "invalid_grant");
    		phrase = tk.device;
    		aeskey = tk.aeskey;
    		obj.put("state", tk.state);
    		
    		user = User.getById(appInstance.owner, User.ALL_USER_INTERNAL);
    		if (user == null || user.status.isDeleted() || user.status.equals(UserStatus.BLOCKED)) throw new BadRequestException("error.internal", "invalid_grant");
    		
    		//This check can be disabled: both phrase and appInstance._id come from authorization "code" which has been validated										
    		//if (appInstance.passcode != null && !User.phraseValid(phrase, appInstance.passcode)) throw new BadRequestException("error.invalid.credentials", "Wrong password.");
    		
    		if (KeyManager.instance.unlock(appInstance._id, aeskey != null ? aeskey : phrase) == KeyManager.KEYPROTECTION_FAIL) throw new BadRequestException("error.internal", "invalid_grant");
    	
    		tempContext = ContextManager.instance.createLoginOnlyContext(appInstance._id, appInstance.applicationId, user.role);
    		meta = RecordManager.instance.getMeta(tempContext, appInstance._id, "_app").toMap();							
    		if (!phrase.equals(meta.get("phrase"))) throw new InternalServerException("error.internal", "Internal error while validating consent");
    		
    		UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.LOGIN);
        } else throw new BadRequestException("error.internal", "Unknown grant_type");
               											
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, aeskey, System.currentTimeMillis() + MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME, user != null ? user.role : UserRole.ANY, null);
		
        OAuthRefreshToken refresh = createRefreshToken(tempContext, appInstance, aeskey);
        
        BSONObject q = RecordManager.instance.getMeta(tempContext, appInstance._id, "_query");
        
        tempContext = ContextManager.instance.upgradeSessionForApp(tempContext, appInstance);	
        
		if (user != null) {		
				PostLoginActions.check(tempContext, user);	
				
				if (q.containsField("link-study")) {
		        	MidataId studyId = MidataId.from(q.get("link-study"));
		        	//context = MobileAPI.prepareMobileExecutor(appInstance, session);
		        	controllers.research.Studies.autoApproveCheck(appInstance.applicationId, studyId, tempContext);
		        }
		}
               
		// create encrypted authToken		
											
		obj.put("access_token", session.encrypt());
		obj.put("token_type", "Bearer");
		obj.put("scope", "openid fhirUser offline_access user/*.crus");
		
		obj.put("expires_in", MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME / 1000l);
		if (user != null) {
			obj.put("role", user.role.toPublicString());
			obj.put("fhirUser", getFhirUser(user));
		} 		
		obj.put("patient", appInstance.owner.toString());
		
		obj.put("refresh_token", refresh.encrypt());	
						
		return ok(obj).withHeader("Cache-Control", "no-store").withHeader("Pragma", "no-cache");
	}
	
	public static String getFhirUser(User user) {
		return user.role.toPublicString()+"/"+user._id.toString();
	}

	public static Pair<User, MobileAppInstance> useRefreshToken(String refresh_token) throws AppException {
		OAuthRefreshToken refreshToken = OAuthRefreshToken.decrypt(refresh_token);
		if (refreshToken == null) {
			throw new BadRequestException("error.internal", "Bad refresh_token.");
		}
		
		MidataId appInstanceId = refreshToken.appInstanceId;
		
		MobileAppInstance appInstance = MobileAppInstance.getById(appInstanceId, MobileAppInstance.APPINSTANCE_ALL);
		
		if (refreshToken.created + MobileAPI.DEFAULT_REFRESHTOKEN_EXPIRATION_TIME < System.currentTimeMillis()) {
			AccessLog.log("Refresh token has expired at "+(refreshToken.created + MobileAPI.DEFAULT_REFRESHTOKEN_EXPIRATION_TIME)+" now="+System.currentTimeMillis());
			// Begin: Allow expired refresh tokens for key recovery
		    boolean isInvalid = true;
			if (appInstance != null && appInstance.owner != null) {
			  User checkuser = User.getById(appInstance.owner, User.ALL_USER_INTERNAL);
			  if (checkuser != null && checkuser.flags != null && checkuser.flags.contains(AccountActionFlags.KEY_RECOVERY)) {
				  AccessLog.log("Refresh token expired becuase key recovery required.");
				  isInvalid = false;
			  }
			}
			// End: Allow expired refresh tokens for key recovery
			if (isInvalid) OAuth2.invalidToken();
		}
		
		if (!verifyAppInstance(null, appInstance, refreshToken.ownerId, refreshToken.appId, null)) throw new BadRequestException("error.internal", "Bad refresh token.");
		
		Plugin app = Plugin.getById(appInstance.applicationId);
		if (app == null) {
			AccessLog.log("refresh token: unknown app");
			throw new BadRequestException("error.unknown.app", "Unknown app");			
		}
		if (!app.type.equals("mobile") && !app.type.equals("analyzer") && !app.type.equals("external")) {
			AccessLog.log("refresh token: wrong application type");
			throw new InternalServerException("error.internal", "Wrong app type");
		}
	
		User user = null;
		if (!app.type.equals("external") && !app.type.equals("analyzer")) {
			user = User.getById(appInstance.owner, User.ALL_USER_INTERNAL);
			if (user == null) {
				AccessLog.log("refresh token: user does not exist");
				invalidToken();
			}
			
			if (!LicenceChecker.checkAppInstance(user._id, app, appInstance)) {
				AccessLog.log("refresh token: licence not valid");
				invalidToken();
			}
								
			Set<UserFeature> req = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(user.role);
			if (app.requirements != null) req.addAll(app.requirements);
			Set<UserFeature> notok = Application.loginHelperPreconditionsFailed(user, req);
			if (notok != null) {
				AccessLog.log("refresh token: login preconditions failed: "+notok.toString());
				invalidToken();
			}                       
		}
		
		if (KeyManager.instance.unlock(appInstance._id, refreshToken.phrase) == KeyManager.KEYPROTECTION_FAIL) {
			AccessLog.log("refresh token: could not unlock account");
			invalidToken();
		}
		
		AccessContext temp = ContextManager.instance.createLoginOnlyContext(appInstance._id, user.role, appInstance);
		Map<String, Object> meta = RecordManager.instance.getMeta(temp, appInstance._id, "_app").toMap();
					
		if (refreshToken.created != ((Long) meta.get("created")).longValue()) {
			AccessLog.log("refresh token: created for a different application version");
			invalidToken();
		}

		UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.REFRESH);

		return Pair.of(user, appInstance);
	}

	public static OAuthRefreshToken createRefreshToken(AccessContext context, MobileAppInstance appInstance, String aeskey)
			throws AppException {
		//if (executorId == null) executorId = appInstance._id;
		OAuthRefreshToken refresh = new OAuthRefreshToken(appInstance.applicationId, appInstance._id, appInstance.owner, aeskey, System.currentTimeMillis());
		
		Map<String, Object> meta = RecordManager.instance.getMeta(context, appInstance._id, "_app").toMap();
        meta.put("created", refresh.created);
        RecordManager.instance.setMeta(context, appInstance._id, "_app", meta);
		return refresh;
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
	static final Plugin validatePlugin(ExtendedSessionToken token, JsonNode json) throws AppException {
		
		
		// MIDATA PORTAL ??
				 
		Plugin app;
		if (token.appId == null) {
		   String name = JsonValidation.getString(json, "appname");
		   app = PluginLoginCache.getByFilename(name);
		} else {			
		   app = PluginLoginCache.getById(token.appId);			
		}
		
		// Check app
		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");		
		if (!app.type.equals("mobile")) throw new PluginException(app._id,"error.plugin", "Wrong application type. Only smartphone/web type applications may use the OAuth login.");
		
		// Check redirect URI
		if (token.appId == null) {
			String redirectUri = JsonValidation.getString(json, "redirectUri");
			if (app.redirectUri==null) throw new PluginException(app._id,"error.plugin", "No redirect URI set for this application. Check settings in developer portal.");
			if (redirectUri==null || redirectUri.length()==0) throw new PluginException(app._id,"error.plugin", "Missing redirect URI in request to OAuth login page.");
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
				if (!found) throw new PluginException(app._id,"error.plugin", "Redirect URI provided to request does not match redirect URI(s) set in developer portal.");
			}
		}
		
		checkUnlockCode(token, app, json);
						
		token.appId = app._id;
		return app;
	}
	
	public static void checkUnlockCode(ExtendedSessionToken token, Plugin app, JsonNode json) throws JsonValidationException{
		// Check unlock code
		if (app.unlockCode != null) {				
			String code = JsonValidation.getStringOrNull(json, "unlockCode");
			if (code != null) {
				if (!app.unlockCode.toUpperCase().equals(code.toUpperCase())) throw new JsonValidationException("error.invalid.unlock_code", "unlockCode", "invalid", "Invalid unlock code");
				token.setAppUnlockedWithCode();
			}
		}
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
		if (confirmStudy==null) confirmStudy = Collections.emptySet();
		if (app == null) {
			AccessLog.log("no app set, using portal requirements");
			requirements = InstanceConfig.getInstance().getInstanceType().defaultRequirementsPortalLogin(token.userRole);
			return requirements;
		}
		
		requirements = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(token.userRole);
		if (app.requirements != null) requirements.addAll(app.requirements);
		
		
		for (StudyAppLink sal : links) {
			if (sal.isConfirmed() && sal.active && ((sal.type.contains(StudyAppLinkType.OFFER_P) && confirmStudy.contains(sal.studyId)) || sal.type.contains(StudyAppLinkType.AUTOADD_P))) {
				if (sal.linkTargetType == null || sal.linkTargetType == LinkTargetType.STUDY) {
					Study study = sal.getStudy();				
					if (study.requirements != null) requirements.addAll(study.requirements);		
				}
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
	private static final void readyCodeChallenge(ExtendedSessionToken token, JsonNode json, Plugin app) throws JsonValidationException, BadRequestException {
		token.codeChallenge = JsonValidation.getStringOrNull(json, "code_challenge");
	    token.codeChallengeMethod = JsonValidation.getStringOrNull(json, "code_challenge_method");
	    if (app.codeChallenge && token.codeChallenge==null) throw new BadRequestException("error.no_code_challenge", "Code challenge missing");
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
		if (role == UserRole.ANY) return null;
		
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
										    				
		if (user == null) {
			if (JsonValidation.getStringOrNull(json, "password")==null) return null;
			throw new BadRequestException("error.invalid.credentials", "Unknown user or bad password");
		}
		
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
		if (token.ownerId != null && token.getIsAuthenticated()) return null;			
		String password = JsonValidation.getStringOrNull(json, "password");
		String sessionToken = JsonValidation.getStringOrNull(json, "sessionToken");		
		if (password == null) {
		  if (user == null) return ok("ask-password");
		  if (user.password == null) {
			  String otp = JsonValidation.getStringOrNull(json, "otp");
			  if (otp == null) {				 
			    Application.sendOTP(token.currentContext, token.appId, user);			  
			    token.ownerId = user._id;
			    return Application.loginHelperResult(null, token, user, Collections.singleton(UserFeature.OTP_VERIFIED));
			  } else {				
				user.checkLoginAttempts();
				if (OTPTools.checkToken(user, otp)) {					
					token.setIsAuthenticated();
					user.recordLoginAttempt(true);
					
					if (OTPTools.tokenConfirmsEMail(user) && user.emailStatus == EMailStatus.UNVALIDATED) {
					   AuditManager.instance.addAuditEvent(AuditEventType.USER_EMAIL_CONFIRMED, user);
				       			    	   		         
			           user.emailStatus = EMailStatus.VALIDATED;
				       user.set("emailStatus", EMailStatus.VALIDATED);		
					}
					
					return null;
				} else {					
					user.recordLoginAttempt(false);
					throw new BadRequestException("error.invalid.otp",  "Invalid one time password.");
				}
			  }
		  }		
		  return ok("ask-password");
		}
		AccessLog.log("Passed password verification");
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
		token.setIsAuthenticated();
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
		if (user == null) throw new NullPointerException();
		
		String securityToken = JsonValidation.getStringOrNull(json, "securityToken");
				
		// If 2FA is already done we do not need to check again
		if (notok!=null && token.securityToken != null) {
			notok.remove(UserFeature.AUTH2FACTOR);
			notok.remove(UserFeature.PHONE_VERIFIED);		
			return;
		}
		
		// If 2FA is enabled (for other apps) and address or birthday must be changed do 2FA 
		if (notok!=null && (notok.contains(UserFeature.ADDRESS_ENTERED) || notok.contains(UserFeature.BIRTHDAY_SET) || notok.contains(UserFeature.GENDER_SET) || notok.contains(UserFeature.NEWEST_PRIVACY_POLICY_AGREED) || notok.contains(UserFeature.NEWEST_TERMS_AGREED))) {			
			if (user.authType != null && user.authType != SecondaryAuthType.NONE) {
				notok.add(UserFeature.AUTH2FACTOR);
			}
		}
		
		// Do not do 2FA if no phone number present, account email not confirmed (but required) or admin unlock required 
		if (notok!=null && (notok.contains(UserFeature.EMAIL_VERIFIED) || notok.contains(UserFeature.ADMIN_VERIFIED) || notok.contains(UserFeature.PHONE_ENTERED))) {
			notok.remove(UserFeature.AUTH2FACTOR);
			notok.remove(UserFeature.AUTH2FACTORSETUP);
			notok.remove(UserFeature.PHONE_VERIFIED);
			return;
		}
		
		// If 2FA is not setup yet do not ask for it
		if (notok!=null && notok.contains(UserFeature.AUTH2FACTORSETUP)) {
			notok.remove(UserFeature.AUTH2FACTOR);			
			notok.remove(UserFeature.PHONE_VERIFIED);
			return;
		}
					
		// If 2FA is required but phone not validated yet, validate phone instead
		if (notok!=null && (notok.contains(UserFeature.AUTH2FACTOR))) {
			if (user.authType == SecondaryAuthType.SMS && user.mobileStatus != EMailStatus.VALIDATED) {
			  notok.remove(UserFeature.AUTH2FACTOR);
			  notok.add(UserFeature.PHONE_VERIFIED);
			}
		}
		
		// Do phone verification
		if (notok!=null && notok.contains(UserFeature.PHONE_VERIFIED)) {
			if (securityToken == null) {
				if ((user.mobile==null || user.mobile.trim().length()==0)
						&& (user.phone==null || user.phone.trim().length()==0)) {
					notok.clear();
					notok.add(UserFeature.PHONE_ENTERED);
				} else {
				  Authenticators.getInstance(SecondaryAuthType.SMS).startAuthentication(user._id, "Code", user);
				  notok.clear();
				  notok.add(UserFeature.PHONE_VERIFIED);
				}
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
				
					try {
 					    Authenticators.getInstance(SecondaryAuthType.SMS).checkAuthentication(user._id, user, securityToken);
					} catch (AppException e) {						
						AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user, token.appId);
						throw e;
					}
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
		
		// Do 2FA
		if (notok!=null && notok.contains(UserFeature.AUTH2FACTOR)) {
			if (securityToken == null) {
				Authenticators.getInstance(user.authType).startAuthentication(user._id, "Code", user);
				notok.clear();
				notok.add(UserFeature.AUTH2FACTOR);
			} else {
				try {
					Authenticators.getInstance(user.authType).checkAuthentication(user._id, user, securityToken);
				} catch (AppException e) {						
					AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user, token.appId);
					throw e;
				}
				token.securityToken = securityToken;
				notok.remove(UserFeature.AUTH2FACTOR);
				if (notok.isEmpty()) {
					notok = null;
					Authenticators.getInstance(user.authType).finishAuthentication(user._id, user);
				}
			}
		}
	}
	
	private static final MobileAppInstance checkExistingAppInstance(ExtendedSessionToken token, AccessContext tempContext, JsonNode json, Set<StudyAppLink> links) throws AppException {		
		long tStart = System.currentTimeMillis();

		// Portal
		if (token.appId == null) return null;
		
		if (token.device == null || token.appId == null || token.ownerId == null) throw new NullPointerException();
		
        MobileAppInstance appInstance = MobileAPI.getAppInstance(tempContext, token.device, token.appId, token.ownerId, MobileAppInstance.APPINSTANCE_ALL);		
		
		//KeyManager.instance.login(60000l, false);		
		
		if (appInstance != null) {
			if (verifyAppInstance(tempContext, appInstance, token.ownerId, token.appId, links)) {
				token.appInstanceId = appInstance._id;
				token.setAppUnlockedWithCode();
				token.setAppConfirmed();
			} else {
				appInstance = null;
				token.setAppUnlockedWithCode();
			}
			
		}
	
		AccessLog.log("checkExistingAppInstance time="+(System.currentTimeMillis()-tStart));
		return appInstance;
	}
	
	private static final UserFeature checkAppConfirmationRequired(ExtendedSessionToken token, JsonNode node, Set<StudyAppLink> links) throws AppException {
		if (token.appId == null) return null;
		
		if (!token.getAppConfirmed()) {
			
			AuditManager.instance.fail(0, "Confirmation required", "error.missing.confirmation");
			boolean allRequired = true;
			for (StudyAppLink sal : links) {
				if (sal.isConfirmed() && sal.active && (sal.type.contains(StudyAppLinkType.CHECK_P) || sal.type.contains(StudyAppLinkType.OFFER_P))) {
					if (sal.linkTargetType == LinkTargetType.ORGANIZATION || sal.linkTargetType == LinkTargetType.SERVICE) {
					  Consent existingConsent = LinkTools.findConsentForAppLink(token.ownerId, sal);
					  allRequired = allRequired && (existingConsent != null);
					} else {
					  if (allRequired) {
					     allRequired = checkAlreadyParticipatesInStudy(sal.studyId, token.ownerId);
					     if (!allRequired && sal.type.contains(StudyAppLinkType.AUTOADD_P) && token.joinCode==null) {
					    	Study study = sal.getStudy();
					    	if (!study.joinMethods.contains(token.appId != null ? JoinMethod.APP : JoinMethod.PORTAL)) throw new JsonValidationException("error.blocked.joinmethod", "code", "joinmethod", "Study is not searching for participants using this channel.");
					     }
					  }
					}
				}
			}
			return allRequired ? UserFeature.APP_NO_PROJECT_CONFIRM : UserFeature.APP_CONFIRM;				
		}

		return null;
	}
	
	private static final void checkJoinWithCode(ExtendedSessionToken token, Set<StudyAppLink> links) throws AppException {
		if (token.joinCode == null) return;
		if (links == null || links.isEmpty()) return;
		
		for (StudyAppLink sal : links) {
			if (sal.isConfirmed() && sal.active && (sal.linkTargetType == null || sal.linkTargetType == LinkTargetType.STUDY)) {
							
				Study study = sal.getStudy();							
				if (study.joinMethods.contains(JoinMethod.APP_CODE)) {	
					ContextManager.instance.clearCache();
					AccessContext context = ContextManager.instance.createSession(token);
				    controllers.members.Studies.requestParticipation(context, token.ownerId, sal.studyId, token.appId, JoinMethod.APP_CODE, token.joinCode);
				    ContextManager.instance.clearCache();
				}
			}
		}
	}
			
	private static MobileAppInstance loginAppInstance(ExtendedSessionToken token, MobileAppInstance appInstance, User user, boolean autoConfirm, Set<StudyAppLink> links) throws AppException {
		if (appInstance != null && autoConfirm) {
			  ApplicationTools.refreshApp(appInstance, token.currentContext.getAccessor(), token.appId, user, token.device);	
		} else {				  
			  appInstance = ApplicationTools.installApp(token.currentContext, token.appId, user, token.device, autoConfirm, token.confirmations, links);
		}
		if (token.currentContext == null) token.currentContext = ContextManager.instance.createLoginOnlyContext(appInstance._id, user.role, appInstance);
		Map<String, Object> meta = RecordManager.instance.getMeta(token.currentContext, appInstance._id, "_app").toMap();
											
		if (!token.device.equals(meta.get("phrase"))) throw new InternalServerException("error.internal", "Internal error while validating consent");
        token.appInstanceId = appInstance._id;
		return appInstance;
	}
		
	
	private static void selectStudyContext(ExtendedSessionToken token) throws AppException {

		if (token.userRole.equals(UserRole.RESEARCH) && token.studyId != null) {
			if (token.appInstanceId == null) throw new NullPointerException();
			
			BasicBSONObject m = (BasicBSONObject) RecordManager.instance.getMeta(token.currentContext, token.appInstanceId, "_query");
			String old = m.getString("link-study");
			if (old != null && old.equals(token.studyId.toString())) { }
			else {
			  m.put("link-study", token.studyId.toString());
			  RecordManager.instance.setMeta(token.currentContext, token.appInstanceId, "_query", m.toMap());
			}
		}
	}
				
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result login(Request request) throws AppException {
			
        JsonNode json = request.body().asJson();		
        JsonValidation.validate(json, "appname", "username", "device", "state", "redirectUri");

        ExtendedSessionToken token = new ExtendedSessionToken();
        if (json.has("loginToken")) {
        	ExtendedSessionToken token1 = ExtendedSessionToken.decrypt(JsonValidation.getString(json, "loginToken"));
        	if (token1.getIsChallengeResponse()) {
        	  token = token1;
        	} else return unauthorized();
        } else {
	        token.created = System.currentTimeMillis();                               
	        token.userRole = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;                
			token.state = JsonValidation.getString(json, "state");
			token.device = JsonValidation.getString(json, "device");
			token.joinCode = JsonValidation.getStringOrNull(json, "joinCode");
        }
		if (token.device != null && token.device.length()<4) throw new BadRequestException("error.illegal.device", "Value for device is too short.");
	    // Validate Mobile App	
		Plugin app = validatePlugin(token, json);		
		readyCodeChallenge(token, json, app);
		
		return loginHelper(request, token, json, app, null);
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(PreAuthSecured.class)
	public Result continuelogin(Request request) throws AppException {
		JsonNode json = request.body().asJson();		
       
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
                       
        return loginHelper(request, token, json, app, null);
	}
	
	/**
	 * accessToken expired result
	 * @return
	 * @throws BadRequestException
	 */
	public static void invalidToken() throws BadRequestException {
		throw new BadRequestException("error.invalid.token", "Invalid or expired authToken.", Http.Status.UNAUTHORIZED);
	}

	public static Result loginHelper(Request request) throws AppException {
		PortalSessionToken tk = PortalSessionToken.session();
		if (tk instanceof ExtendedSessionToken) {
			AccessLog.log("reusing existing extended session token");
			ExtendedSessionToken token = (ExtendedSessionToken) tk;
			token.currentContext = ContextManager.instance.createLoginOnlyContext(tk.ownerId, null, tk.userRole);
			JsonNode json = Json.newObject();
			Plugin app = token.appId != null ? validatePlugin(token, json) : null;
			return loginHelper(request, token, json, app, token.currentContext);
		} else {
			AccessLog.log("creating new extended session token");
			ExtendedSessionToken token = new ExtendedSessionToken();
			token.ownerId = tk.ownerId;
			token.orgId = tk.orgId;
			token.developerId = tk.developerId;			
			token.userRole = tk.userRole;
            token.handle = tk.handle;			
			token.created = tk.created;
            token.remoteAddress = tk.remoteAddress;
            token.currentContext = ContextManager.instance.createLoginOnlyContext(tk.ownerId, null, tk.userRole);
            token.setPortal();
            JsonNode json = Json.newObject();
            return loginHelper(request, token, json, null, token.currentContext);
		}
		//throw new AuthException("error.internal", "Wrong token type");
	}
	
	public static Result loginHelper(Request request, ExtendedSessionToken token, JsonNode json, Plugin app, AccessContext currentContext) throws AppException {
        long ts1 = System.currentTimeMillis();
		AccessLog.log("[login] gather information");
		token.currentContext = currentContext;		
		
		if (app != null) {
			boolean confirmed = JsonValidation.getBoolean(json, "confirm");
			if (confirmed) token.setAppConfirmed();
			
			Set<MidataId> confirmStudy = json.has("confirmStudy") ? JsonExtraction.extractMidataIdSet(json.get("confirmStudy")) : null;
			if (confirmStudy != null) token.confirmations = confirmStudy;
		} else token.setPortal();						 

		if (json.has("project") && token.studyId == null) {
			Study study = Study.getByCodeFromMember(JsonValidation.getString(json, "project"), Study.ALL);
			if (study != null && study.participantSearchStatus.equals(ParticipantSearchStatus.SEARCHING)) {
			  token.studyId = study._id;
			}
		}
		Set<StudyAppLink> links = token.appId != null ? StudyAppLink.getByApp(token.appId) : null;
		
		if (token.studyId != null && token.getRole() == UserRole.MEMBER) {			
			StudyAppLink dynLink = new StudyAppLink(token.studyId, token.appId);			
			if (links == null) links = new HashSet<StudyAppLink>();
			links.add(dynLink);
		}
		
		Set<UserFeature> requirements = determineRequirements(token, app, links, token.confirmations);
		 		
																		
		User user = identifyUserForLogin(token, json);
		Set<UserFeature> notok;
		MobileAppInstance appInstance = null;
		
		if (token.getFake()) {		
			AccessLog.log("is fake login");			
		    return Application.loginHelperResult(request, token, user, Collections.singleton(UserFeature.EMAIL_VERIFIED));
		}
		long ts2 = System.currentTimeMillis();
		AccessLog.log("[login] check preconditions, time=",Long.toString(ts2-ts1));
		
		Result pw = checkPasswordAuthentication(token, json, user);
		if (pw != null) return pw;
						    		
		Result studySelectionRequired = checkStudySelectionRequired(token, json);
		if (studySelectionRequired != null) return studySelectionRequired;
		
		notok = Application.loginHelperPreconditionsFailed(user, requirements);
	    if (notok != null) AccessLog.log("not ok conditions: ",notok.toString());
		
		long ts3 = System.currentTimeMillis();
		AccessLog.log("[login] preparing session, time=", Long.toString(ts3-ts2));
	 
		int keyType;
		if (token.handle != null) {			 
			if (token.currentContext == null) {
				KeyManager.instance.continueSession(token.handle);
				token.currentContext = ContextManager.instance.createLoginOnlyContext(token.ownerId, token.appId, user.role );
			}
			keyType = KeyManager.KEYPROTECTION_NONE;
		} else {
			String sessionToken = JsonValidation.getStringOrNull(json, "sessionToken");
			if (sessionToken == null && user.security.equals(AccountSecurityLevel.KEY_EXT_PASSWORD)) {
				
				AccessLog.log("[login] returning login challenge");
				return Application.loginChallenge(token, user);
			}
			token.handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true); // TODO Check lifetime
			keyType = KeyManager.instance.unlock(user._id, sessionToken, user.publicExtKey); 
			token.currentContext = ContextManager.instance.createLoginOnlyContext(user._id, token.appId, user.role);
		}		
		AccessLog.log("Using context="+token.currentContext.toString());
		appInstance = checkExistingAppInstance(token, token.currentContext, json, links);
			
		if (app != null && app.unlockCode != null && !token.getAppUnlockedWithCode()) {
			  if (notok == null) notok = new HashSet<UserFeature>();
			  notok.add(UserFeature.APP_UNLOCK_CODE);
		}
	
		UserFeature recheck = checkAppConfirmationRequired(token, json, links);
		if (recheck != null) {
			if (notok == null) notok = new HashSet<UserFeature>();
			notok.add(recheck);			
		} 
		
		if (app != null && !LicenceChecker.checkAppInstance(user._id, app, appInstance)) {
			return Application.loginHelperResult(request, token, user, Collections.singleton(UserFeature.VALID_LICENCE));
		}
	
		checkTwoFactorRequired(token, json, user, notok);
		
		if (notok != null && !notok.isEmpty()) {
		  if (token.handle != null) KeyManager.instance.persist(user._id);
		  if (notok.contains(UserFeature.PASSWORD_SET)) notok = Collections.singleton(UserFeature.PASSWORD_SET);		  	
		  if (notok.contains(UserFeature.EMAIL_VERIFIED) && !notok.contains(UserFeature.EMAIL_ENTERED)) notok = Collections.singleton(UserFeature.EMAIL_VERIFIED);
		  if (notok.contains(UserFeature.APP_UNLOCK_CODE)) notok = Collections.singleton(UserFeature.APP_UNLOCK_CODE);
		  if (notok.contains(UserFeature.BIRTHDAY_SET)) notok = Collections.singleton(UserFeature.BIRTHDAY_SET);
		  if (notok.contains(UserFeature.GENDER_SET)) notok = Collections.singleton(UserFeature.GENDER_SET);
		  if (notok.contains(UserFeature.ADDRESS_ENTERED) || notok.contains(UserFeature.PHONE_ENTERED)) notok.retainAll(Sets.createEnum(UserFeature.ADDRESS_ENTERED, UserFeature.PHONE_ENTERED));
		  if (notok.contains(UserFeature.ADMIN_VERIFIED)) notok = Collections.singleton(UserFeature.ADMIN_VERIFIED);		  
		  if (notok.contains(UserFeature.ADDRESS_VERIFIED)) notok = Collections.singleton(UserFeature.ADDRESS_VERIFIED);
		  if (notok.contains(UserFeature.PHONE_VERIFIED)) notok = Collections.singleton(UserFeature.PHONE_VERIFIED);
		  if (notok.contains(UserFeature.AUTH2FACTORSETUP)) notok = Collections.singleton(UserFeature.AUTH2FACTORSETUP);
		  if (notok.contains(UserFeature.AUTH2FACTOR)) notok = Collections.singleton(UserFeature.AUTH2FACTOR);
		  
		  return Application.loginHelperResult(request, token, user, notok);
		}
		
		checkJoinWithCode(token, links);
				
		long ts4 = System.currentTimeMillis();
		AccessLog.log("[login] do, time=", Long.toString(ts4-ts3));
		
		if (app != null) {
			token.currentContext = keyType == KeyManager.KEYPROTECTION_NONE ? ContextManager.instance.createLoginOnlyContext(user._id, app._id, user.role) : null;
				
			appInstance = loginAppInstance(token, appInstance, user, keyType == KeyManager.KEYPROTECTION_NONE, links);
			
			selectStudyContext(token);
							
			token.aeskey = KeyManager.instance.newAESKey(appInstance._id);		
										
			ObjectNode obj = Json.newObject();								
			obj.put("code", token.asCodeExchangeToken().encrypt());
			obj.put("istatus", appInstance.status.toString());
			
			if (user.resettoken != null && user.resettokenType != null && user.resettokenType.isOtp()) OTPTools.clearToken(user);
			
			AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user, app._id);
			
			AuditManager.instance.success();
					
			long ts5 = System.currentTimeMillis();
            token.currentContext.clearCache();
			AccessLog.log("[login] done app, time=", Long.toString(ts5-ts4));
			return ok(obj);
		} else {
			AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user);
			
			ObjectNode obj = Json.newObject();	
			if (keyType == 0 && token.handle != null) {
				  user = PostLoginActions.check(token.currentContext, user);			  
				  KeyManager.instance.persist(user._id);
	        }	
			
			if (user.notifications != null && user.notifications == AccountNotifications.LOGIN) {
				Messager.sendMessage(app != null ? app._id : RuntimeConstants.instance.portalPlugin, MessageReason.LOGIN, null, Collections.singleton(user._id), InstanceConfig.getInstance().getDefaultLanguage(), new HashMap<String, String>());
			}
			
			UsageStatsRecorder.protokoll(RuntimeConstants.instance.portalPlugin, "portal", UsageAction.LOGIN);
			 				
			obj.put("keyType", keyType);
			obj.put("role", user.role.toString().toLowerCase());
			obj.set("subroles", Json.toJson(user.subroles));
			obj.set("lastLogin", Json.toJson(user.login));
			token.setRemoteAddress(request);
			obj.put("sessionToken", token.asPortalSession().encrypt());
			  
			User.set(user._id, "login", new Date());	
			
			if (user.resettoken != null) OTPTools.clearToken(user);
			
			AuditManager.instance.success();
			
			long ts5 = System.currentTimeMillis();
			token.currentContext.clearCache();
			AccessLog.log("[login] done, time=", Long.toString(ts5-ts4));
			return ok(obj).as("application/json");
		}
	}
	
	@BodyParser.Of(BodyParser.FormUrlEncoded.class)
	@MobileCall
	public Result tokenIntrospect(Request request) throws AppException {
					
	    Map<String, String[]> data = request.body().asFormUrlEncoded();
	    String[] tk = data.get("token");
	    if (tk==null) throw new BadRequestException("error.internal", "Missing token parameter");
	    if (tk.length != 1) throw new BadRequestException("error.internal", "Exactly one token parameter required");
	    String token = tk[0];
	    ObjectNode obj = Json.newObject();
	    
	    try {
	    		
			   AccessContext context = ExecutionInfo.checkToken(request, token, false, true);	
			   MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(token);
				
		       User user = User.getById(context.getOwner(), User.FOR_LOGIN);
		       Plugin plugin = Plugin.getById(context.getUsedPlugin());
		       obj.put("active", true);
			   obj.put("client_id", plugin.filename);	
			   obj.put("actor_id", context.getActor().toString());
			   obj.put("scope", getScope(plugin, context) /*"patient/*.read openid fhirUser" */);				
			   if (user!=null) obj.put("fhirUser", getFhirUser(user));
			   if (authToken != null) obj.put("exp", authToken.expiration / 1000l);
			   if (context.getUserGroupAccessor() != null) {
				   obj.put("group_id", context.getUserGroupAccessor().toString());
			   }
		  
	    } catch (AppException e) {
	       obj.put("active", false);
	    }					
						
		return ok(obj).as("application/json");
	}
	
	public String getScope(Plugin plugin, AccessContext context) throws AppException {
		StringBuilder result = new StringBuilder();
		result.append("offline_access openid fhirUser");
		String add = ".rs";
		switch (plugin.writes) {
		case CREATE_SHARED: add = ".crs";break;
		case UPDATE_AND_CREATE: add = ".crus";break;
		case UPDATE_EXISTING: add = ".rus";break;
		case WRITE_ANY: add = ".crus";break;
		}
		for (String type : FHIRServlet.myProviders.keySet()) {
			if (context.mayAccess(null, type)) {
				if (FHIRServlet.myProviders.get(type) instanceof ReadWriteResourceProvider) {
				  result.append(" "+"user/"+type+add);
				} else {
				  result.append(" "+"user/"+type+".rs");
				}
			}
		}
		
		return result.toString();
	}
	
}
