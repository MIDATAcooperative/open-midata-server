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

package utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.BSONObject;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import controllers.Application;
import controllers.Circles;
import models.Actor;
import models.Circle;
import models.Consent;
import models.ConsentEntity;
import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.MemberKey;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Study;
import models.StudyAppLink;
import models.StudyParticipation;
import models.User;
import models.UserGroup;
import models.enums.AccountActionFlags;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EMailStatus;
import models.enums.EntityType;
import models.enums.JoinMethod;
import models.enums.LinkTargetType;
import models.enums.MessageReason;
import models.enums.StudyAppLinkType;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.UserStatus;
import models.enums.WritePermissionType;
import utils.access.Feature_FormatGroups;
import utils.access.Feature_QueryRedirect;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.AccountCreationAccessContext;
import utils.context.AppAccessContext;
import utils.context.UserGroupAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.messaging.Messager;

public class AccountManagementTools {

	public static void validateUserAccountFilledOut(Member user) {
		if (user.firstname == null || user.firstname.trim().length()==0)
			throw new UnprocessableEntityException("Patient 'given' name not given.");
		if (user.lastname == null || user.lastname.trim().length()==0)
			throw new UnprocessableEntityException("Patient family name not given.");	
		if (user.country == null || user.country.trim().length()==0)
			throw new UnprocessableEntityException("Patient country not given.");
		if (user.gender == null)
			throw new UnprocessableEntityException("Patient gender not given.");
		if (user.birthday == null)
			throw new UnprocessableEntityException("Patient birth date not given.");

	}
	
	public static Member identifyExistingAccount(AccessContext context, Member user) throws AppException {
		 if (user.email != null) {
			 Member result = Member.getByEmail(user.email, Member.FOR_LOGIN);
		
			 if (result != null) {
				 // compare name
				 				 
				 if (!isSimilarFirstname(user.firstname, result.firstname, false)) {
					 throw new UnprocessableEntityException("Possible candidate has no match in person given name.");
				 }
				 boolean nameOk = isSimilar(user.lastname, result.lastname, false, false);					 
				 boolean birthdateOk = compareBirthdate(user.birthday, result.birthday, true);				
				 boolean addressOk = compareAddress(user, result);
					 
				 if (!nameOk && birthdateOk) {
					 // Log, notify
					 logAndNotify(context, user ,result, true);
				 } else if (!nameOk && !birthdateOk) {
					 logAndNotify(context, user ,result, false);
					 AuditManager.instance.fail(400, "Same email used", "error.exists.email");
					 throw new UnprocessableEntityException("Different account with same email already exists.");
				 } else if (nameOk && !birthdateOk && !addressOk) {
					 logAndNotify(context, user ,result, false);
					 AuditManager.instance.fail(400, "Same email used", "error.nomatch.birthdate");
					 throw new UnprocessableEntityException("Possible candidate has no matching birth date.");
				 } else if (nameOk && !birthdateOk && addressOk) {
					 // Ok, use
				 }
				 
				 // compare address
				 AccessLog.log("found existing patient with matching email");
				 return result;
			 } 
		 }
				 
		 String firstName = user.firstname.trim();
		 
		 Map<String, Object> properties = CMaps
				 .map("keywordsLC", user.lastname.toLowerCase())
				 .map("lastname", Pattern.compile("^"+user.lastname+"$", Pattern.CASE_INSENSITIVE))
				 .map("firstname", Pattern.compile("^"+firstName.split("\\s+")[0]+"($|\\s)", Pattern.CASE_INSENSITIVE))
				 .map("status", User.NON_DELETED)
				 .map("role", UserRole.MEMBER);
							 
		 Set<Member> candidates = Member.getAll(properties, Member.FOR_LOGIN);				 				 				
		 AccessLog.log("checking "+candidates.size()+" existing patients with matching name");
		 boolean mailGiven = nonEmpty(user.email);
		 for (Member candidate : candidates) {
			 boolean birthdateOk = compareBirthdate(user.birthday, candidate.birthday, false);
			 if (!birthdateOk) continue;
			 
			 boolean hasMail = nonEmpty(candidate.email);
			 boolean hasAddress = nonEmpty(candidate.address1) && nonEmpty(candidate.city);
			 boolean addressOk = compareAddress(user, candidate);
			 
			 if ((mailGiven && hasMail && !user.emailLC.equals(candidate.emailLC))
				|| (!mailGiven && hasMail)) {
				 if (hasAddress && addressOk) {
					 logAndNotify(context, user ,candidate, false);
					 AuditManager.instance.fail(400, "Same email used", "error.nomatch.email");
					 throw new UnprocessableEntityException("Account exists. Please provide same email as used for account."); 
				 }
			 } else if (!hasMail) {
				 						 					 						 					 
				 if (hasAddress && addressOk) {
					 AccessLog.log("found martching patient without email");
					 return candidate; 
				 }
			 }
			 
		 }				 
		 AccessLog.log("found no existing patient");
		 return null;
	}
	
	public static void logAndNotify(AccessContext context, Member user, Member existing, boolean used) throws AppException {
		AuditManager.instance.addAuditEvent(
				AuditEventBuilder
				   .withType(used ? AuditEventType.NON_PERFECT_ACCOUNT_MATCH : AuditEventType.TRIED_USER_REREGISTRATION)
				   .withApp(context.getUsedPlugin())
				   .withModifiedActor(existing)
		);
		Set targets = new HashSet();
		if (existing.email != null) targets.add(existing._id);
		if (used && user.email != null && !user.email.equals(existing.email)) targets.add(user.email);		
		Messager.sendMessage(context, used ? MessageReason.NON_PERFECT_ACCOUNT_MATCH : MessageReason.TRIED_USER_REREGISTRATION, null, targets, InstanceConfig.getInstance().getDefaultLanguage(), new HashMap<String, String>());
		
	}
	
	public static Member checkNoExistingConsents(AccessContext context, Member user) throws AppException {
		 if (user == null) return null;
		 Set<Consent> exist = Circles.getAllWriteableByAuthorizedAndOwner(context, user._id);
			if (!exist.isEmpty())
				throw new UnprocessableEntityException("Already exists and consent is already given. Use search instead.");
		return user;
	}
	
	public static void precheckRegistration(AccessContext context) throws AppException {
		
		 Plugin plugin = Plugin.getById(context.getUsedPlugin());
	     if (plugin.type.equals("external")) {
	       // Nothing yet
	     } else if (plugin.type.equals("analyzer") || plugin.targetUserRole.equals(UserRole.RESEARCH)) {
	       // Nothing yet
	     } else if (plugin.type.equals("broker")) {	        	
	    	if (context.getAccessorEntityType() != EntityType.USERGROUP) throw new PluginException(context.getUsedPlugin(), "error.plugin", "Data broker cannot directly create patient resources.");   
	     } 							

	}
	
	public static AccessContext registerUserAccount(AccessContext context, Member user) throws AppException {
		Application.registerSetDefaultFields(user, false);
		
		Plugin plugin = Plugin.getById(context.getUsedPlugin());
		user.status = UserStatus.ACTIVE;
				
		user.emailStatus = (user.email != null && plugin.accountEmailsValidated) ? EMailStatus.EXTERN_VALIDATED : EMailStatus.UNVALIDATED;				
		user.flags = EnumSet.of(AccountActionFlags.CHANGE_PASSWORD);
				
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, context.getUsedPlugin(), context.getLegacyOwner(), user);

		user.security = AccountSecurityLevel.KEY;
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		Member.add(user);
		
		KeyManager.instance.unlock(user._id, null);
		AccessContext tempContext = new AccountCreationAccessContext(context, user._id);									
		user.myaps = RecordManager.instance.createPrivateAPS(tempContext.getCache(), user._id, user._id);
		Member.set(user._id, "myaps", user.myaps);
				
		if (user.status == UserStatus.ACTIVE) Application.sendWelcomeMail(context, context.getUsedPlugin(), user, Actor.getActor(context, context.getActor()));			
		
		return tempContext;
	}
	
	public static Consent createHPConsent(AccessContext info, Member user, HPUser hpuser, boolean active) throws AppException {
		String consentName = hpuser.firstname + " " + hpuser.lastname;
		/*if (hpuser.provider != null) {
			HealthcareProvider prov = HealthcareProvider.getById(hpuser.provider, HealthcareProvider.ALL);
			if (prov != null)
				consentName = prov.name;
		}*/
		
		Plugin plugin = Plugin.getById(info.getUsedPlugin());
	
		Consent consent = new MemberKey();
		consent.writes = WritePermissionType.WRITE_ANY;
		consent.owner = user._id;
		consent.name = consentName;
		consent.creatorApp = info.getUsedPlugin();
		consent.authorized = new HashSet<MidataId>();
		consent.status = active ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
		consent.authorized.add(info.getLegacyOwner());
		consent.sharingQuery = new HashMap<String, Object>();
		consent.sharingQuery.put("owner", "self");
		consent.sharingQuery.put("app", plugin.filename);
		
		List<Consent> existing = LinkTools.findConsentAlreadyExists(info, consent);
		
		if (existing.isEmpty()) {		
			Circles.addConsent(info, consent, false, null, true);
			return consent;
		} else {
			return null;
		}
	}
	
	public static Consent createDataBrokerConsent(AccessContext context, Member user, boolean active) throws AppException {
		
		if (context.getAccessorEntityType() != EntityType.USERGROUP) throw new PluginException(context.getUsedPlugin(), "error.plugin", "Data broker cannot directly create patient resources.");
		
		AccessContext ac = context;
		while (ac != null && !(ac instanceof UserGroupAccessContext)) ac = ac.getParent();
		
		UserGroup userGroup = context.getRequestCache().getUserGroupById(ac.getAccessor());
		if (userGroup == null) throw new InternalServerException("error.internal", "User group for data broker not found.");

		String consentName = userGroup.name;				
		Plugin app = Plugin.getById(context.getUsedPlugin());
	
		Consent consent = new MemberKey();
		consent.writes = app.writes;
		consent.owner = user._id;
		consent.name = consentName;
		consent.creatorApp = context.getUsedPlugin();
		consent.authorized = new HashSet<MidataId>();
		consent.status = active ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
		consent.authorized.add(ac.getAccessor());
		consent.entityType = context.getAccessorEntityType();
		
		if (app.resharesData) {
			consent.allowedReshares = new ArrayList<ConsentEntity>();
			ConsentEntity ce = new ConsentEntity();
			ce.type = EntityType.SERVICES;
			ce.id = app._id;
			ce.name = app.name;
			consent.allowedReshares.add(ce);
			
			Set<StudyAppLink> sals = StudyAppLink.getByApp(app._id);			
			for (StudyAppLink sal : sals) {
				if (sal.isConfirmed() && sal.active && (sal.linkTargetType==null || sal.linkTargetType == LinkTargetType.STUDY) && (sal.type.contains(StudyAppLinkType.REQUIRE_P) || sal.type.contains(StudyAppLinkType.OFFER_P))) {
					ce = new ConsentEntity();
					ce.type = EntityType.USERGROUP;
					ce.id = sal.studyId;
					Study study = Study.getById(sal.studyId, Sets.create("code", "name"));
					if (study != null) {
						ce.name = study.code+" ("+study.name+")";
						consent.allowedReshares.add(ce);
					}
				}
			}
				
		}
		//convertAppQueryToConsent
		Map<String, Object> query = Feature_FormatGroups.convertQueryToContents(app.defaultQuery, false);		    
		consent.sharingQuery = LinkTools.convertAppQueryToConsent(query);	
				
		List<Consent> existing = LinkTools.findConsentAlreadyExists(context, consent);
		
		if (existing.isEmpty()) {	
		    AccessLog.log("no existing data broker consent found, creating one");
		    Circles.addConsent(context, consent, true, null, true);
		    return consent;
		} else {
		    AccessLog.log("existing data broker consent found, skipping creation");
			return null;
		}
		
	}
	
	public static Consent createRepresentativeConsent(AccessContext context, User executorUser, MidataId targetUser, boolean active) throws AppException {
				
		Consent consent = new Circle();
		consent.type = ConsentType.REPRESENTATIVE;
		consent.owner = targetUser;
		consent.name = executorUser.getPublicIdentifier();
		consent.creatorApp = context.getUsedPlugin();
		consent.authorized = new HashSet<MidataId>();
		consent.status = active ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
		consent.authorized.add(context.getLegacyOwner());
		consent.writes=WritePermissionType.UPDATE_AND_CREATE;
		consent.sharingQuery = new HashMap<String, Object>();
		consent.sharingQuery.put("group", "all");
		consent.sharingQuery.put("group-system", "v1");
		Set<MidataId> observers = ApplicationTools.getObserversForApp(context.getUsedPlugin());
		consent.observers = observers;
	
		Circles.addConsent(context, consent, false, null, true);
		
		return consent;	
	}
	
	public static Consent createExternalServiceConsent(AccessContext info, Member user, boolean active) throws AppException {
	    
	    // Check for consent already existing
	    Set<MobileAppInstance> instances = MobileAppInstance.getActiveByApplicationAndOwner(info.getUsedPlugin(), user._id, MobileAppInstance.APPINSTANCE_ALL);
	    Plugin app = Plugin.getById(info.getUsedPlugin());
	    for (MobileAppInstance appInstance : instances) {
	        if (appInstance.appVersion == app.pluginVersion) return appInstance;
	    }
	    
		if (!active) throw new UnprocessableEntityException("Cannot create consent for existing account.");
		
		MobileAppInstance instance = ApplicationTools.installApp(info, info.getUsedPlugin(), user, null, true, null, null);		
		return instance;
	}
	
	public static MidataId getProjectForAnalyzerFromContext(AccessContext ac) throws AppException {
		while (ac != null && !(ac instanceof AppAccessContext)) ac = ac.getParent();
		if (ac == null) throw new InternalServerException("error.intenal", "Required access context not found.");
		Map<String, Object> query = ConsentQueryTools.getSharingQuery(((AppAccessContext) ac).getAppInstance(), false);
		if (query == null) throw new InternalServerException("error.intenal", "Analyzer access filter not found.");
		return MidataId.from(query.get("link-study"));		
	}
	
	public static Consent createAnalyzerConsent(AccessContext info, Member user, FHIRPatientHolder fhirPatient, boolean active) throws AppException {		
		MidataId linkedProject = getProjectForAnalyzerFromContext(info);	
		ApplicationTools.sendFirstUseMessage(user, Plugin.getById(info.getUsedPlugin()));
		return participateToProject(info, user, fhirPatient, linkedProject, active, JoinMethod.API);
	}
	
	public static Consent createConsentFromAPIContext(AccessContext context, Member user, FHIRPatientHolder fhirPatient, Member existing) throws AppException {
		AccessLog.logBegin("begin create consent from API context:", context.toString());
		try {
	        Plugin plugin = Plugin.getById(context.getUsedPlugin());
	        if (plugin.type.equals("external")) {
	        	return createExternalServiceConsent(context, user, existing == null || plugin.usePreconfirmed);
	        } else if (plugin.type.equals("analyzer") || plugin.targetUserRole.equals(UserRole.RESEARCH)) {
	        	return createAnalyzerConsent(context, user, fhirPatient, existing == null || plugin.usePreconfirmed);
	        } else if (plugin.type.equals("broker")) {	        	
	        	return createDataBrokerConsent(context, user, existing == null || plugin.usePreconfirmed);
	        } 
			HPUser hpuser = HPUser.getById(context.getLegacyOwner(), Sets.create("provider", "firstname", "lastname"));
			if (hpuser != null) {
				Consent consent = AccountManagementTools.createHPConsent(context, user, hpuser, existing == null);
				return consent;
								
			} else {
				User executorUser = context.getRequestCache().getUserById(context.getLegacyOwner());
				if (executorUser!=null && executorUser.getRole()==UserRole.MEMBER) {
					Consent consent = AccountManagementTools.createRepresentativeConsent(context, executorUser, user._id, existing == null);
					return consent;
				
				}
			}
			return null;
		} finally {
			AccessLog.logEnd("end create consent from API context");
		}
	}
	
	public static MidataId getLinkedProject(AccessContext context) throws AppException {
		Plugin plugin = Plugin.getById(context.getUsedPlugin());
		if (plugin.targetUserRole.equals(UserRole.RESEARCH)) {
			AccessLog.log("is researcher app");
			BSONObject query = RecordManager.instance.getMeta(context, context.getTargetAps(), "_query");
			AccessLog.log("q=" + query.toString());

			if (query != null && query.containsField("link-study")) {
				Map<String, Object> q = query.toMap();
				MidataId studyId = MidataId.from(q.get("link-study"));
				AccessLog.log("found linked study:" + studyId);
				return studyId;
			}
		}
		return null;
	}

	public static Set<MidataId> getProjectIdsFromPatient(FHIRPatientHolder thePatient) throws AppException {
		Set<MidataId> result = new HashSet<MidataId>();
		Set<String> codes = new HashSet<String>();
		codes.addAll(thePatient.getCodesFromExtension("http://midata.coop/extensions/join-project"));
		codes.addAll(thePatient.getCodesFromExtension("http://midata.coop/extensions/join-study"));
		for (String studyName : codes) {
			Study study = Study.getByCodeFromMember(studyName, Study.ALL);
			if (study == null)
				throw new BadRequestException("error.invalid.code", "Unknown code for study.");

			result.add(study._id);			
		}
		return result;
	}
	
	public static Set<MidataId> getProjectIdsFromUsedApp(AccessContext context) throws AppException {
		MidataId pluginId = context.getUsedPlugin();
		if (pluginId == null) return null;
		
		Plugin plugin = Plugin.getById(pluginId);	
		if (plugin != null && plugin.type.equals("broker")) {
			Set<StudyAppLink> sals = StudyAppLink.getByApp(pluginId);		
			if (sals.isEmpty()) return Collections.emptySet();
			Set<MidataId> studies = new HashSet<MidataId>();
			for (StudyAppLink sal : sals) {			   
				if (sal.isConfirmed() && sal.active && (sal.linkTargetType==null || sal.linkTargetType == LinkTargetType.STUDY) && sal.type.contains(StudyAppLinkType.REQUIRE_P)) {
					studies.add(sal.studyId);					
				}
			}
			return studies;
		}
		return Collections.emptySet();
	}
	
	public static List<Study> determineProjectsFromUsedApp(AccessContext context, boolean fromLinks) throws AppException {
		MidataId pluginId = context.getUsedPlugin();
		if (pluginId == null) return null;
		
		Plugin plugin = Plugin.getById(pluginId);
		if (plugin != null && plugin.type.equals("analyzer")) {
			MidataId studyId = getProjectForAnalyzerFromContext(context);
			return Collections.singletonList(Study.getById(studyId, Sets.create("_id","code","name")));
		}
		
		if (!fromLinks) return null;
		
		Set<StudyAppLink> sals = StudyAppLink.getByApp(pluginId);
		if (sals.isEmpty()) return null;
		List<Study> studies = new ArrayList<Study>();
		for (StudyAppLink sal : sals) {
			if (sal.isConfirmed() && sal.active && (sal.linkTargetType==null || sal.linkTargetType == LinkTargetType.STUDY) && (sal.type.contains(StudyAppLinkType.REQUIRE_P) || sal.type.contains(StudyAppLinkType.OFFER_P))) {
				studies.add(Study.getById(sal.studyId, Sets.create("_id","code","name")));
			}
		}
		return studies;
	}
	
	public static void participateToProjects(AccessContext context, Member user, FHIRPatientHolder fhirPatient, Set<MidataId> projectsToParticipate, boolean active) throws AppException {
	      AccessLog.log("request participation to "+projectsToParticipate.size()+" projects.");
		  for (MidataId projectId : projectsToParticipate) {
			     participateToProject(context, user, fhirPatient, projectId, active, context.getUsedPlugin().equals(RuntimeConstants.instance.portalPlugin) ? JoinMethod.RESEARCHER : JoinMethod.APP);				
		  }	  
	}
	
	public static StudyParticipation participateToProject(AccessContext context, Member user, FHIRPatientHolder fhirPatient, MidataId projectId, boolean active, JoinMethod method) throws AppException {
		StudyParticipation part = null;
		Set<UserFeature> studyReq = controllers.members.Studies.precheckRequestParticipation(null, projectId);
		AccessLog.log("request participation ",projectId.toString());
		if (active) {
			part = controllers.members.Studies.requestParticipation(context, user._id, projectId, context.getUsedPlugin(), method, null);
		} else {
			part = controllers.members.Studies.match(context, user._id, projectId, context.getUsedPlugin(), method);
		}
		
		if (part != null && context.getCache().getAPS(part._id).isAccessible()) {
			if (part.ownerName != null && !part.ownerName.equals("???")) {
				Study study = Study.getById(projectId, Sets.create("_id","code","name"));
				fhirPatient.populateIdentifier(context, study, part);							
			}
		}
		
		AccessLog.log("end request participation");
		return part;
	}
	
	public static boolean isSimilarFirstname(String str1, String str2, boolean matchIfEmpty) {
		if (str1 == null) str1 = "";
		if (str2 == null) str2 = "";
		str1 = str1.trim().split(" ")[0];
		str2 = str2.trim().split(" ")[0];
		return isSimilar(str1, str2, matchIfEmpty, false);
	}
	
	public static boolean isSimilar(String str1, String str2, boolean matchIfEmpty, boolean isStreet) {
		if (str1 == null) str1 = "";
		if (str2 == null) str2 = "";
		str1 = str1.trim().toLowerCase();
		str2 = str2.trim().toLowerCase();
		if (str2.length() == 0 && matchIfEmpty) return true;
		str1 = soundsSimilar(str1, isStreet);
		str2 = soundsSimilar(str2, isStreet);
		return str1.equals(str2);
	}
	
	public static String replStart(String str, String start, String repl) {
		if (str.startsWith(start)) str = str.replace(start, repl);
		return str;
	}
	
	private static String soundsSimilar(String str, boolean isStreet) {
		str = str.replace(".", " ").replace("/", " ").replace("-", " ").replace("  ", " ");
		if (isStreet) {
			str = str.replace(" ","");
			str = replStart(str, "avenue", "av");
			str = replStart(str, "boulevard", "bd");
			str = replStart(str, "chemin", "ch");
			str = replStart(str, "esplanade", "espl");
			str = replStart(str, "impasse", "imp");
			str = replStart(str, "passage", "pass");
			str = replStart(str, "promenade", "prom");
			str = replStart(str, "route", "rte");
			str = replStart(str, "ruelle", "rle");
			str = replStart(str, "sentier", "sent");
			str = str.replace("street","st").replace("straße","str").replace("strasse","str").replace("platz","pl");
			str = str.replace("avenue","ave").replace("boulevard","blvd").replace("lane","ln").replace("road","rd").replace("drive","dr");
			str = str.replace("crescent","cres").replace("court","ct").replace("place","pl").replace("square","sq");
			str = str.replace("sankt","st");						
		}
		str = str.replace("ß", "s").replace("ss","s");
		str = str.replace("ll", "l").replace("rr", "r");
		str = str.replace("d", "t").replace("tt", "t");
		str = str.replace("v", "f");
		str = str.replace("ñ", "n");
		str = str.replace("b", "p").replace("pp", "p").replace("pf", "f");
		str = str.replace("g", "k").replace("c", "k").replace("kk", "k");
		str = str.replace("ê", "e").replace("â","a").replace("ô", "o").replace("î","i");
		str = str.replace("é", "e").replace("á","a").replace("ó", "o").replace("í","i");
		str = str.replace("è", "e").replace("à","a").replace("ò", "o").replace("ì","i");
		str = str.replace("ă", "a");
		str = str.replace("ë", "e").replace("ï","i");
		str = str.replace("õ", "o");
		str = str.replace("æ", "ae").replace("œ", "oe").replace("ç", "c").replace("ę", "e").replace("ą", "a");
		str = str.replace("ee", "e").replace("aa", "a").replace("oo", "o").replace("ii", "i");
		str = str.replace("eh", "e").replace("ah", "a").replace("oh", "o").replace("ih", "i");				
		str = str.replace("ü", "ue").replace("ä", "ae").replace("ö", "oe");
		
		return str;
	}
	
	public static boolean nonEmpty(String str) {
		return str != null && str.trim().length()>0;
	}
	
	public static boolean compareBirthdate(Date date1, Date date2, boolean matchIfEmpty) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy");
		if (date1 == null || date2 == null) return matchIfEmpty;
		String dateStr1 = sdf.format(date1);
		String dateStr2 = sdf.format(date2);
		return dateStr1.equals(dateStr2);
	}
	
	public static boolean compareAddress(User user1, User user2) {
		if (!isSimilar(user1.country, user2.country, true, false)) return false;
		if (!isSimilar(user1.address1, user2.address1, true, true)) return false;
		if (!isSimilar(user1.city, user2.city, true, false) && !isSimilar(user1.zip, user2.zip, true, false)) return false;
		return true;		
	}
		
}
