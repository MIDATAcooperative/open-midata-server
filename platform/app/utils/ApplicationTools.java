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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import controllers.Circles;
import controllers.OAuth2;
import controllers.members.HealthProvider;
import controllers.members.Studies;
import models.Consent;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.ServiceInstance;
import models.Study;
import models.StudyAppLink;
import models.StudyParticipation;
import models.User;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EntityType;
import models.enums.JoinMethod;
import models.enums.LinkTargetType;
import models.enums.MessageReason;
import models.enums.ParticipationStatus;
import models.enums.PluginStatus;
import models.enums.StudyAppLinkType;
import models.enums.UsageAction;
import models.enums.UserStatus;
import models.enums.WritePermissionType;
import utils.access.AccessContext;
import utils.access.ConsentAccessContext;
import utils.access.Feature_FormatGroups;
import utils.access.Feature_QueryRedirect;
import utils.access.Feature_UserGroups;
import utils.access.RecordManager;
import utils.access.RepresentativeAccessContext;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;
import utils.messaging.SubscriptionManager;
import utils.stats.UsageStatsRecorder;

/**
 * Tools for app and plugin management
 */
public class ApplicationTools {

	public static MobileAppInstance installApp(AccessContext context, MidataId appId, User member, String phrase, boolean autoConfirm, Set<MidataId> studyConfirm, Set<StudyAppLink> links) throws AppException {
		AccessLog.logBegin("beginn install app id="+appId+" context="+(context != null ? context.toString() : "null"));
		Plugin app = Plugin.getById(appId, Sets.create("name", "type", "pluginVersion", "defaultQuery", "predefinedMessages", "termsOfUse", "writes", "defaultSubscriptions"));
		if (app == null) throw new InternalServerException("error.internal", "App not found");

		// Get project links
		if (links == null) links = StudyAppLink.getByApp(appId);
		if (studyConfirm==null) studyConfirm = Collections.emptySet();
		
		// check consents accepted
		checkProjectConsentsAccepted(member, studyConfirm, links);
							
		// Create app instance *
		MobileAppInstance appInstance = null;
		
		Set<MidataId> observers = getObserversForApp(links);		
		
		if (app.type.equals("external")) {
			Set<ServiceInstance> instances = ServiceInstance.getByApp(appId, ServiceInstance.ALL);
			if (instances.size() == 1) {
				if (context == null) throw new InternalServerException("error.internal", "Missing context");
				appInstance = createServiceUseInstance(context, context.getOwner(), app, instances.iterator().next());
			} else throw new InternalServerException("error.internal", "No service instance");
			
		} else appInstance = createAppInstance(member._id, phrase, app, null, observers);	

		// app first use audit entry
		AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.APP_FIRST_USE).withApp(app._id).withActorUser(member).withConsent(appInstance));		    	    	
		
		// Create consents for project (moved from before confirm)
		if (context != null) createConsentsForProjects(context, member, studyConfirm, app, links);

		// Create APS for AppInstance *
		if (!app.type.equals("external")) createAppInstanceAPS(context, member._id, phrase, autoConfirm, app, appInstance);
		
		// Add app to set of used apps
		if (!member.apps.contains(app._id)) {
			member.apps.add(app._id);
			User.set(member._id, "apps", member.apps);
		}
		
		// Agree to terms and co
		if (app.termsOfUse != null) member.agreedToTerms(app.termsOfUse, app._id);					
		
		Circles.consentStatusChange(context, appInstance, null, false);
		
		// Send email of first use
		sendFirstUseMessage(member, app);

		// protokoll app installation
		UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.INSTALL);
		
		// confirm audit entries
		AuditManager.instance.success();

		AccessLog.logEnd("end install app");
		return appInstance;
	}
	
	/**
	 * install an internal or external service (for an account holder; not the external service owner)
	 * @param executor
	 * @param serviceAppId
	 * @param member
	 * @param studyConfirm
	 * @return
	 * @throws AppException
	 */
	public static MobileAppInstance refreshOrInstallService(AccessContext context, MidataId serviceAppId, User member,  Set<MidataId> studyConfirm) throws AppException {
		AccessLog.logBegin("begin refresh or install service:"+serviceAppId);
		Set<MobileAppInstance> insts = MobileAppInstance.getActiveByApplicationAndOwner(serviceAppId, member._id, MobileAppInstance.APPINSTANCE_ALL);
		MobileAppInstance result = null;
		boolean foundValid = false;
		for (MobileAppInstance inst : insts) {
			if (foundValid) {
				removeAppInstance(context, context.getAccessor(), inst);
			} else if (OAuth2.verifyAppInstance(context, inst, member._id, serviceAppId, null)) {
				foundValid = true;
				result = inst;
			}
		}
		if (!foundValid) {
			result = installApp(context, serviceAppId, member, "portal", true, studyConfirm, null);
		}
		AccessLog.logEnd("end refresh or install service:"+serviceAppId);
		return result;
	}
	
	public static Set<MidataId> getObserversForApp(Set<StudyAppLink> links) throws InternalServerException {
		if (links == null) return null;
		Set<MidataId> observers = null;
		for (StudyAppLink link : links) {
			if (link.linkTargetType == LinkTargetType.SERVICE && link.isConfirmed() && link.active) {
				Plugin target = Plugin.getById(link.serviceAppId);
				if (target.consentObserving && target.status != PluginStatus.DELETED) {
					if (observers==null) observers = new HashSet<MidataId>();
					observers.add(target._id);
				}
			}
		}
		return observers;
	}
	
    public static Set<MidataId> getObserversForApp(MidataId appId) throws AppException {
    	if (appId == null) return null;
		return getObserversForApp(StudyAppLink.getByApp(appId));
	}

	public static MobileAppInstance createServiceApiKey(AccessContext context, ServiceInstance serviceInstance) throws AppException {
		AccessLog.logBegin("begin create service api key");
		Plugin app = Plugin.getById(serviceInstance.appId, Sets.create("name", "type", "pluginVersion", "defaultQuery", "predefinedMessages", "termsOfUse", "writes", "defaultSubscriptions"));
		if (app == null) throw new InternalServerException("error.internal", "App not found");

		String phrase = serviceInstance.name+serviceInstance._id;								

		// Create app instance *
		MobileAppInstance appInstance = createAppInstance(serviceInstance.executorAccount, phrase, app, serviceInstance._id, null);	
		
		// Create APS for AppInstance *
		// create APS *						
		RecordManager.instance.createPrivateAPS(context.getCache(), context.getAccessor(), appInstance._id);
		Set<MidataId> targetUsers = new HashSet<MidataId>();
		targetUsers.add(serviceInstance.managerAccount);
		targetUsers.add(serviceInstance.executorAccount);
		targetUsers.add(appInstance._id);
		if (serviceInstance.endpoint != null && serviceInstance.endpoint.length()>0) {
			targetUsers.add(RuntimeConstants.instance.publicUser);
		}
		appInstance.authorized = targetUsers;
		
		//RecordManager.instance.shareAPS(new ConsentAccessContext(appInstance, context), targetUsers);			
		
		// Write phrase into APS *
		Map<String, Object> meta = new HashMap<String, Object>();
		meta.put("phrase", phrase);		
		RecordManager.instance.setMeta(context, appInstance._id, "_app", meta);
								
		Map<String, Object> query = appInstance.sharingQuery;
		if (serviceInstance.linkedStudy != null) query.put("study", serviceInstance.linkedStudy.toString());
		if (serviceInstance.linkedStudyGroup != null) query.put("study-group", serviceInstance.linkedStudyGroup);
		
		if (serviceInstance.linkedStudy != null) query.put("target-study", serviceInstance.linkedStudy.toString());
		if (serviceInstance.linkedStudyGroup != null) query.put("target-study-group", serviceInstance.linkedStudyGroup);			
		if (serviceInstance.linkedStudy != null) query.put("link-study", serviceInstance.linkedStudy.toString());
		
		if (serviceInstance.studyRelatedOnly) query.put("study-related", "true");

		appInstance.sharingQuery = query;
		
		/*appInstance.set(appInstance._id, "sharingQuery", query);
		
		RecordManager.instance.shareByQuery(sharingContext, appInstance._id, appInstance.sharingQuery);
		*/
		// Confirm app consent *
		
		// Gain access to executor account
		if (serviceInstance.executorAccount.equals(serviceInstance._id)) {
			Map<String, Object> si = RecordManager.instance.getMeta(context, serviceInstance.executorAccount, "_si").toMap();
			MidataId keyId = MidataId.from(si.get("id"));
			byte[] key = (byte[]) si.get("key");
			KeyManager.instance.unlock(serviceInstance.executorAccount, keyId, key);
		}
		
		AccessContext sharingContext = context.forServiceInstance(serviceInstance);
		Circles.consentStatusChange(sharingContext, appInstance, null);
		

		// Link with executor account
		//linkMobileConsentWithExecutorAccount(context, serviceInstance.executorAccount, appInstance._id);

		//appInstance.status = ConsentStatus.ACTIVE;	
		//if (!app.type.equals("endpoint")) {
		//  SubscriptionManager.activateSubscriptions(serviceInstance.executorAccount, app, appInstance._id, true);
		//}
		// protokoll app installation
		UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.INSTALL);
		
		// confirm audit entries
		AuditManager.instance.success();
		AccessLog.logEnd("end create service api key");
		return appInstance;
	}

	public static ServiceInstance createServiceInstance(AccessContext context, Plugin app, Study study, String group, String endpoint, boolean studyRelatedOnly) throws AppException {
		AccessLog.log("create service instance");

		if (!app.type.equals("analyzer") && !app.type.equals("endpoint")) throw new InternalServerException("error.internal", "Wrong app type");
        if (study.anonymous && !app.pseudonymize) throw new BadRequestException("error.invalid.anonymous", "Aggregator may read unpseudonymized data");
		
        if (app.type.equals("endpoint")) {
        	if (endpoint == null || endpoint.trim().length()==0) throw new BadRequestException("error.missing.endpoint", "Endpoint missing");
        	ServiceInstance old = ServiceInstance.getByEndpoint(endpoint, ServiceInstance.ALL);
        	if (old != null) throw new BadRequestException("error.exists.endpoint", "Endpoint already exists.");
        }
        
		ServiceInstance si = new ServiceInstance();
		si._id = new MidataId();
		si.appId = app._id;
		si.endpoint = endpoint;
		si.studyRelatedOnly = studyRelatedOnly;
		si.linkedStudy = study._id;
		si.linkedStudyGroup = group;
		si.managerAccount = study._id;
		si.status = UserStatus.ACTIVE;
		si.executorAccount = study._id;
		if (app.type.equals("endpoint")) {
		  si.name = app.name+": "+study.name + (group != null ? (" - " + group) : "")+" -> /opendata/"+endpoint+"/fhir";
		} else {
		  si.name = app.name+": "+study.name + (group != null ? (" - " + group) : "");
		}
		si.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(si._id, null);
		si.add();

		// Create service instance APS and store key
		RecordManager.instance.createAnonymizedAPS(si.executorAccount, si.managerAccount, si._id, false, false, true);
		MidataId keyId = new MidataId();
		byte[] splitKey = KeyManager.instance.generateAlias(si._id, keyId);
		Map<String, Object> obj = new HashMap<String, Object>();
		obj.put("key", splitKey);
		obj.put("id", keyId.toString());
		RecordManager.instance.setMeta(context, si._id, "_si", obj);
		
		// app first use audit entry
		//AuditManager.instance.addAuditEvent(AuditEventType.APP_FIRST_USE, app._id, member, null, appInstance, null, null);		    	    	
		
		// protokoll app installation
		UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.INSTALL);
		
		// confirm audit entries
		AuditManager.instance.success();

		return si;
	}

	public static ServiceInstance createServiceInstance(AccessContext context, Plugin app, MidataId managerId) throws AppException {
		AccessLog.log("create service instance");
		ServiceInstance si = new ServiceInstance();
		si._id = new MidataId();
		si.appId = app._id;
		si.linkedStudy = null;
		si.linkedStudyGroup = null;
		si.managerAccount = managerId;
		si.status = UserStatus.ACTIVE;
		si.executorAccount = si._id;
		si.name = app.name;
		si.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(si._id, null);
		si.add();
		RecordManager.instance.getMeta(context, context.getAccessor(), "_");
		// Create service instance APS and store key
		RecordManager.instance.createAnonymizedAPS(si._id, managerId, si._id, false, false, true);
		MidataId keyId = new MidataId();
		byte[] splitKey = KeyManager.instance.generateAlias(si._id, keyId);
		Map<String, Object> obj = new HashMap<String, Object>();
		obj.put("key", splitKey);
		obj.put("id", keyId.toString());
		RecordManager.instance.setMeta(context, si._id, "_si", obj);

		// protokoll app installation
		UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.INSTALL);
		
		// confirm audit entries
		AuditManager.instance.success();

		return si;
	}



	public static ServiceInstance checkServiceInstanceOwner(AccessContext context, MidataId serviceId) throws AppException {
		ServiceInstance instance = ServiceInstance.getById(serviceId, ServiceInstance.ALL);
		if (instance == null) throw new BadRequestException("error.unknown.service", "Service Instance does not exist");
		if (instance.managerAccount.equals(context.getAccessor())) return instance;

		UserGroupMember ugm = UserGroupMember.getByGroupAndActiveMember(instance.managerAccount, context.getAccessor());
		if (ugm != null) {
			Feature_UserGroups.loadKey(context, ugm);
			return instance;
		}

		throw new BadRequestException("error.unknown.service", "Service Instance does not exist");
	}

	private static MobileAppInstance createAppInstance(MidataId owner, String phrase, Plugin app, MidataId serviceId, Set<MidataId> observers)
			throws InternalServerException, BadRequestException, AppException {
		MobileAppInstance appInstance = new MobileAppInstance();
		appInstance._id = new MidataId();
		AccessLog.log("create new app instance id="+appInstance._id);
		appInstance.owner = owner;
		appInstance.deviceId = phrase.substring(0,3);
		if (app.type.equals("service")) {
			appInstance.name = "Service: "+ app.name;		
		} else {
		    appInstance.name = "App: "+ app.name+" (Device: "+phrase.substring(0, 3)+")";
		}
		appInstance.applicationId = app._id;	
		appInstance.serviceId = serviceId;
		appInstance.appVersion = app.pluginVersion;
		appInstance.observers = observers;
		appInstance.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(appInstance._id, null);    			
		appInstance.passcode = MobileAppInstance.hashDeviceId(phrase); 
		appInstance.dateOfCreation = new Date();
		appInstance.lastUpdated = appInstance.dateOfCreation;
		appInstance.writes = app.writes;
		appInstance.status = ConsentStatus.ACTIVE;
		if (appInstance.writes==null) appInstance.writes = WritePermissionType.NONE;
		
		if (app.defaultQuery != null && !app.defaultQuery.isEmpty()) {			
		    Feature_FormatGroups.convertQueryToContents(app.defaultQuery);		    
		    appInstance.sharingQuery = Feature_QueryRedirect.simplifyAccessFilter(app._id, app.defaultQuery);						   
        } else appInstance.sharingQuery = ConsentQueryTools.getEmptyQuery();
		
		return appInstance;
	}

	private static MobileAppInstance createServiceUseInstance(AccessContext context, MidataId owner, Plugin app, ServiceInstance si)
			throws InternalServerException, BadRequestException, AppException {
		MobileAppInstance appInstance = new MobileAppInstance();
		appInstance._id = new MidataId();
		appInstance.name = "External: "+ app.name;
		appInstance.type = ConsentType.API;					
		appInstance.applicationId = app._id;			
		appInstance.appVersion = app.pluginVersion;	
		appInstance.owner = owner;									
		appInstance.dateOfCreation = new Date();
		appInstance.lastUpdated = appInstance.dateOfCreation;
		appInstance.writes = app.writes;
		appInstance.status = ConsentStatus.ACTIVE;
		appInstance.authorized = Collections.singleton(si.executorAccount);
		appInstance.entityType = EntityType.SERVICES;
		
		if (app.defaultQuery != null && !app.defaultQuery.isEmpty()) {			
		    Feature_FormatGroups.convertQueryToContents(app.defaultQuery);		    
		    appInstance.sharingQuery = Feature_QueryRedirect.simplifyAccessFilter(app._id, app.defaultQuery);						   
		} else appInstance.sharingQuery = ConsentQueryTools.getEmptyQuery();
		
		//Circles.addConsent(context, appInstance, true, null, false);
		
		return appInstance;
	}

	private static void createAppInstanceAPS(AccessContext context, MidataId owner, String phrase, boolean autoConfirm, Plugin app,
			MobileAppInstance appInstance) throws AppException, JsonValidationException {
		AccessLog.log("create new app instance APS id="+appInstance._id);
		// create APS *				
		RecordManager.instance.createAnonymizedAPS(owner, appInstance._id, appInstance._id, true);
		
		// Write phrase into APS *
		Map<String, Object> meta = new HashMap<String, Object>();
		meta.put("phrase", phrase);
		if (context == null) context = RecordManager.instance.createLoginOnlyContext(appInstance._id);
		RecordManager.instance.setMeta(context, appInstance._id, "_app", meta);
		
		// Write access filter into APS *
		if (app.defaultQuery != null && !app.defaultQuery.isEmpty()) {
			appInstance.sharingQuery = app.defaultQuery;
			//AccessContext sharingContext = RecordManager.instance.createSharingContext(context, owner);
		    //RecordManager.instance.shareByQuery(sharingContext, appInstance._id, appInstance.sharingQuery);
		}						
		
		// Confirm app consent *
		/*if (autoConfirm) {
		   HealthProvider.confirmConsent(context, appInstance._id);
		   appInstance.status = ConsentStatus.ACTIVE;
		}*/
	}

    private static void sendFirstUseMessage(User member, Plugin app) throws AppException {
        if (app.predefinedMessages!=null) {
			AccessLog.log("send first use message");
			if (!app._id.equals(member.initialApp)) {
				Messager.sendMessage(app._id, MessageReason.FIRSTUSE_EXISTINGUSER, null, Collections.singleton(member._id), member.language, new HashMap<String, String>());	
			} 
			Messager.sendMessage(app._id, MessageReason.FIRSTUSE_ANYUSER, null, Collections.singleton(member._id), member.language, new HashMap<String, String>());								
		}
    }

    private static void createConsentsForProjects(AccessContext context, User member, Set<MidataId> studyConfirm, Plugin app,
            Set<StudyAppLink> links) throws AppException, InternalServerException {
        for (StudyAppLink sal : links) {
			if (sal.isConfirmed()) {	
				if (sal.linkTargetType == LinkTargetType.ORGANIZATION) {
					if (sal.type.contains(StudyAppLinkType.REQUIRE_P) || (sal.type.contains(StudyAppLinkType.OFFER_P) && studyConfirm.contains(sal.userId))) {
						
						if (LinkTools.findConsentForAppLink(context.getAccessor(),sal)==null) {
							RecordManager.instance.clearCache();
							Set<MidataId> observers = ApplicationTools.getObserversForApp(links);
							LinkTools.createConsentForAppLink(context, sal, observers);
						}
					}
				} else if (sal.linkTargetType == LinkTargetType.SERVICE) {
					if (sal.type.contains(StudyAppLinkType.REQUIRE_P) || (sal.type.contains(StudyAppLinkType.OFFER_P) && studyConfirm.contains(sal.serviceAppId))) {					
						RecordManager.instance.clearCache();
						refreshOrInstallService(context, sal.serviceAppId, member, studyConfirm);											
					}
				} else
				if (sal.type.contains(StudyAppLinkType.REQUIRE_P) || (sal.type.contains(StudyAppLinkType.OFFER_P) && studyConfirm.contains(sal.studyId))) {
					RecordManager.instance.clearCache();
			        controllers.members.Studies.requestParticipation(new ExecutionInfo(context.getAccessor(), member.getRole()), member._id, sal.studyId, app._id, sal.dynamic ? JoinMethod.API : JoinMethod.APP, null);
				}
			}
		}
    }

    private static void checkProjectConsentsAccepted(User member, Set<MidataId> studyConfirm, Set<StudyAppLink> links)
            throws InternalServerException, BadRequestException, AppException {
        for (StudyAppLink sal : links) {
			if (sal.isConfirmed()) {
												
				if (!sal.active) sal.type = Collections.emptySet();
				
				if (sal.linkTargetType == LinkTargetType.ORGANIZATION) {
					if (sal.type.contains(StudyAppLinkType.REQUIRE_P) && sal.type.contains(StudyAppLinkType.OFFER_P) && !studyConfirm.contains(sal.userId)) {
						Consent consent = LinkTools.findConsentForAppLink(member._id, sal);						
						if (consent==null) throw new BadRequestException("error.missing.consent_accept", "Consent belonging to app must be accepted.");
					}
				} else if (sal.linkTargetType == LinkTargetType.SERVICE) {
						if (sal.type.contains(StudyAppLinkType.REQUIRE_P) && sal.type.contains(StudyAppLinkType.OFFER_P) && !studyConfirm.contains(sal.serviceAppId)) {
							Consent consent = LinkTools.findConsentForAppLink(member._id, sal);						
							if (consent==null) throw new BadRequestException("error.missing.consent_accept", "Consent belonging to app must be accepted.");
						}									
				} else {
				
					if (sal.type.contains(StudyAppLinkType.REQUIRE_P) && sal.type.contains(StudyAppLinkType.OFFER_P) && !studyConfirm.contains(sal.studyId)) {
						StudyParticipation sp = StudyParticipation.getByStudyAndMember(sal.studyId, member._id, Sets.create("status", "pstatus"));
			        	if (sp == null || 
			        		sp.pstatus.equals(ParticipationStatus.MEMBER_RETREATED) || 
			        		sp.pstatus.equals(ParticipationStatus.MEMBER_REJECTED) || 
			        		sp.pstatus.equals(ParticipationStatus.RESEARCH_REJECTED)) {
			        		throw new BadRequestException("error.missing.study_accept", "Study belonging to app must be accepted.");        		
			        	}
					}
					
					if (sal.type.contains(StudyAppLinkType.REQUIRE_P) || (sal.type.contains(StudyAppLinkType.OFFER_P) && studyConfirm.contains(sal.studyId))) {
						controllers.members.Studies.precheckRequestParticipation(member._id, sal.studyId);
					}
				}
			}
		}
    }

	public static void linkMobileConsentWithExecutorAccount(AccessContext context, MidataId targetAccountId, MidataId consentId) throws AppException {
		AccessLog.log("link app instance to account from="+consentId+" to="+targetAccountId);
		BSONObject meta = RecordManager.instance.getMeta(context, consentId, "_app");
		if (meta == null) throw new InternalServerException("error.internal", "_app object not found,");
		MidataId alias = new MidataId();
		byte[] key = KeyManager.instance.generateAlias(targetAccountId, alias);
		meta.put("alias", alias.toString());
		meta.put("aliaskey", key);
		meta.put("targetAccount", targetAccountId.toString());
		RecordManager.instance.setMeta(context, consentId, "_app", meta.toMap());
	}
	
	public static void linkRepresentativeConsentWithExecutorAccount(AccessContext context, MidataId targetAccountId, MidataId consentId) throws AppException {
		AccessLog.log("link representative consent to account from="+consentId+" to="+targetAccountId);
		Map<String, Object> meta = new HashMap<String, Object>();
		MidataId alias = new MidataId();
		byte[] key = KeyManager.instance.generateAlias(targetAccountId, alias);
		meta.put("alias", alias.toString());
		meta.put("aliaskey", key);
		meta.put("targetAccount", targetAccountId.toString());
		RecordManager.instance.setMeta(context, consentId, "_representative", meta);
	}
	
	public static AccessContext actAsRepresentative(AccessContext context, MidataId targetUser, boolean useOriginalContextOnFail) throws AppException {
		
		
		if (context.getAccessor().equals(targetUser)) return context;
		if (context.getCache().hasSubCache(targetUser)) return new RepresentativeAccessContext(context.getCache().getSubCache(targetUser), context);
		
		Consent consent = Consent.getRepresentativeActiveByAuthorizedAndOwner(context.getActor(), targetUser);
		
		if (consent != null) {				
			Map<String, Object> meta = RecordManager.instance.getMeta(context, consent._id, "_representative").toMap();
			if (meta.containsKey("aliaskey") && meta.containsKey("alias")) {
				AccessLog.log("Act as representative: unlock "+targetUser);
				MidataId alias = new MidataId(meta.get("alias").toString());
				byte[] key = (byte[]) meta.get("aliaskey");
				KeyManager.instance.unlock(targetUser, alias, key);			
				//RecordManager.instance.clearCache();
				
				return new RepresentativeAccessContext(context.getCache().getSubCache(targetUser), context);
			}
		}

		if (useOriginalContextOnFail) return context;
		return null;
	}
									
									
				
	

	public static MobileAppInstance refreshApp(MobileAppInstance appInstance, MidataId executor, MidataId appId, User member, String phrase) throws AppException {
		AccessLog.logBegin("start refresh app id="+appInstance._id);
		long tStart = System.currentTimeMillis();
		Plugin app = Plugin.getById(appId, Sets.create("name", "type", "pluginVersion", "defaultQuery", "predefinedMessages", "termsOfUse", "writes", "defaultSubscriptions"));
							
		appInstance = MobileAppInstance.getById(appInstance._id, Sets.create(MobileAppInstance.APPINSTANCE_ALL, "fhirConsent"));
		
		//RecordManager.instance.unshareAPS(appInstance._id, executor, Collections.singleton(appInstance._id));
		
	    appInstance.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(appInstance._id, null);
	    if (appInstance.deviceId == null || !appInstance.deviceId.equals(phrase.substring(0,3))) {
	      AccessLog.log("create passcode");
		  appInstance.passcode = MobileAppInstance.hashDeviceId(phrase);     	
	    }
		appInstance.lastUpdated = new Date();
		
		RecordManager.instance.shareAPS(RecordManager.instance.createContextFromApp(executor, appInstance), appInstance.publicKey);
		appInstance.upsert();
					
		AuditManager.instance.success();
		AccessLog.logEnd("end refresh app time="+(System.currentTimeMillis()-tStart));
		return appInstance;
	
	}

	public static void removeAppInstance(AccessContext context, MidataId executorId, MobileAppInstance appInstance) throws AppException {
		AccessLog.logBegin("start remove app instance: "+appInstance._id+" context="+context.toString());
		// Device or password changed, regenerates consent				
		Circles.consentStatusChange(context, appInstance, ConsentStatus.EXPIRED);
		Plugin app = Plugin.getById(appInstance.applicationId);
		if (app!=null) SubscriptionManager.deactivateSubscriptions(appInstance.owner, app, appInstance._id);
		RecordManager.instance.deleteAPS(appInstance._id, executorId);									
		//Removing queries from user account should not be necessary
		if (appInstance.serviceId == null) Circles.removeQueries(appInstance.owner, appInstance._id);										
		
		MobileAppInstance.delete(appInstance.owner, appInstance._id);
		AccessLog.logEnd("end remove app instance");
	}

	public static void deleteServiceInstance(AccessContext context, ServiceInstance instance) throws InternalServerException {
        Set<MobileAppInstance> appInstances = MobileAppInstance.getByService(instance._id, MobileAppInstance.ALL);
        for (MobileAppInstance appInstance : appInstances) {
            try {
              ApplicationTools.removeAppInstance(context, context.getAccessor(), appInstance);            
            } catch (Exception e) {
            	AccessLog.logEnd("end remove app instance (exception)");
                MobileAppInstance.delete(instance.executorAccount, appInstance._id);
            }
        }

        ServiceInstance.delete(instance._id);
    }

    
}