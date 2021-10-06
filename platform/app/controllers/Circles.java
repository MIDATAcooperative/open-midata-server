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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Circle;
import models.Consent;
import models.HCRelated;
import models.Member;
import models.MemberKey;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.RecordsInfo;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.AggregationType;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EntityType;
import models.enums.MessageReason;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.WritePermissionType;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.PasswordHash;
import utils.RuntimeConstants;
import utils.access.APS;
import utils.access.AccessContext;
import utils.access.ConsentAccessContext;
import utils.access.Feature_Streams;
import utils.access.PasscodeAccessContext;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.MemberSecured;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.RequestTooLargeException;
import utils.fhir.ConsentResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;
import utils.messaging.SubscriptionManager;

/**
 * functions for managing consents
 *    
 */
public class Circles extends APIController {	

	public final static int RETURNED_CONSENT_LIMIT = 1000;
	/**
	 * list either all Circles of a user or all consents of others where the user is authorized 
	 * @return list of circles
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@Deprecated
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public Result get(Request request) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request.body().asJson();
						
		List<Circle> circles = null;

		if (json.has("owner")) {
			MidataId owner = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
			circles = new ArrayList<Circle>(Circle.getAllByOwner(owner));
		} else if (json.has("member")) {
			MidataId member = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
			if (json.has("status")) {
			  circles = new ArrayList<Circle>(Circle.getAllActiveByMember(member));
			} else {
			  circles = new ArrayList<Circle>(Circle.getAllByMember(member));
			}
			ReferenceTool.resolveOwners(circles, true);
		} else {
			JsonValidation.validate(json, "owner");
			return ok();
		}
		Collections.sort(circles);
		return ok(JsonOutput.toJson(circles, "Consent", Sets.create("name", "order", "owner", "authorized"))).as("application/json");
	}
	
	/**
	 * list either all consents of a user or all consents of others where the user is authorized 
	 * @return list of consents
	 * @throws JsonValidationException
	 * @throws AppException
	 * @throws AuthException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result listConsents(Request request) throws JsonValidationException, AppException, AuthException {
		// validate json
		JsonNode json = request.body().asJson();					
		JsonValidation.validate(json, "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));	
		ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "authorized");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		fields.add("type");
		
		Rights.chk("Circles.listConsents", getRole(), properties, fields);
		
		List<Consent> consents = null;
	
		MidataId owner = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		
		if (properties.containsKey("_id")) {
			MidataId target = MidataId.from(properties.get("_id"));
			Consent c = getConsentById(context, target, fields);
			consents = (c!=null) ? Collections.singletonList(c) : Collections.<Consent>emptyList();
		} else if (properties.containsKey("member")) {
		  properties.remove("member");
		  consents = new ArrayList<Consent>(getConsentsAuthorized(owner, properties, Consent.ALL));
		} else {
		  consents = new ArrayList<Consent>(Consent.getAllByOwner(owner, properties, Consent.ALL, RETURNED_CONSENT_LIMIT));
		}
		
		fillConsentFields(context, consents, fields);
		
		if (fields.contains("passcode") && !properties.containsKey("member")) {
			for (Consent consent : consents) {
				if (consent.type == null || consent.type.equals(ConsentType.EXTERNALSERVICE)) {
				   BSONObject obj = RecordManager.instance.getMeta(context, consent._id, "_app");
				   if (obj != null) consent.passcode = obj.get("phrase").toString();
				}
				if (consent.type == null || consent.type.equals(ConsentType.HEALTHCARE)) {
				   BSONObject obj = RecordManager.instance.getMeta(context, consent._id, "_config");
				   if (obj != null) consent.passcode = obj.get("passcode").toString();
				}
			}
		}
				
		
		//Collections.sort(circles);
		return ok(JsonOutput.toJson(consents, "Consent", fields)).as("application/json");
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result listApps(Request request) throws JsonValidationException, AppException, AuthException {
		// validate json
		JsonNode json = request.body().asJson();					
		JsonValidation.validate(json, "fields");
				
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		fields.add("type");
						
		List<MobileAppInstance> consents = null;
	
		MidataId owner = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
		consents = new ArrayList<MobileAppInstance>(MobileAppInstance.getByOwner(owner, fields));											
		
		//Collections.sort(circles);
		return ok(JsonOutput.toJson(consents, "MobileAppInstance", fields)).as("application/json");
	}
	
	public static void fillConsentFields(AccessContext context, Collection<Consent> consents, Set<String> fields) throws AppException {
        
        AccessLog.log("consents size="+consents.size());
		if (fields.contains("ownerName")) ReferenceTool.resolveOwners(consents, true);
	
		if (fields.contains("records") && consents.size() <= RETURNED_CONSENT_LIMIT) {
			Map<String, Object> all = new HashMap<String,Object>();
			for (Consent consent : consents) {
				if (consent.status.equals(ConsentStatus.ACTIVE)) {
				  try {
				    Collection<RecordsInfo> summary = RecordManager.instance.info(UserRole.ANY, consent._id, new ConsentAccessContext(consent, context), all, AggregationType.ALL);
				    if (summary.isEmpty()) consent.records = 0; else consent.records = summary.iterator().next().count;
				  } catch (RequestTooLargeException e) { consent.records = -1; }
				  catch (AppException e) {
					ErrorReporter.report("consent info", null, e);
					consent.records = -1;
				  }
				  
				} else consent.records = 0;
			}
		}
		
		for (Consent consent : consents) {
			if (consent.type.equals(ConsentType.STUDYRELATED) && consent.authorized != null) consent.authorized.clear();
			
			if (consent.sharingQuery == null && (consent.status == ConsentStatus.ACTIVE || consent.status == ConsentStatus.FROZEN || (consent.owner != null && consent.owner.equals(context.getAccessor())))) {
				if (fields.contains("createdBefore") || fields.contains("validUntil")) {				
					BasicBSONObject obj = (BasicBSONObject) RecordManager.instance.getMeta(context, consent._id, "_filter");
					if (obj != null) {
						consent.validUntil = obj.getDate("valid-until");
						consent.createdBefore = obj.getDate("history-date");
					}				
				}
				
				if (fields.contains("sharingQuery")) {
					
					if (consent.type.equals(ConsentType.EXTERNALSERVICE)) {
						BSONObject b = RecordManager.instance.getMeta(context, consent._id, APS.QUERY);
						if (b!=null) {
							consent.sharingQuery = b.toMap();				
						}
					} else {
						consent.sharingQuery = Circles.getQueries(consent.owner, consent._id);
					}
					
				}		
			}
		}
					
								
	}
	
	public static Collection<Consent> getConsentsAuthorized(MidataId user, Map<String, Object> properties, Set<String> fields) throws AppException {
		Set<UserGroupMember> groups = UserGroupMember.getAllActiveByMember(user);
		Set<MidataId> auth = new HashSet<MidataId>();
		auth.add(user);
		for (UserGroupMember group : groups) auth.add(group.userGroup);
		Collection<Consent> consents = Consent.getAllByAuthorized(auth, properties, fields, RETURNED_CONSENT_LIMIT);
		return consents;
	}
	
	public static Consent getConsentById(AccessContext context, MidataId consentId, Set<String> fields) throws AppException {
		return getConsentById(context, consentId, null, fields); 
	}
	
	public static Consent getConsentById(AccessContext context, MidataId consentId, MidataId observerId, Set<String> fields) throws AppException {
		fields.add("owner");
		fields.add("authorized");
		Consent consent;
		if (fields.contains("applicationId")) consent = MobileAppInstance.getById(consentId, fields); 
		else consent = Consent.getByIdUnchecked(consentId, fields);		
		if (consent == null) return null;
		if (consent.owner != null && consent.owner.equals(context.getAccessor())) return consent;
		if (consent.authorized.contains(context.getAccessor())) return consent;
		if (observerId != null && consent.observers != null && consent.observers.contains(observerId)) return consent;
		if (consent.owner != null && ApplicationTools.actAsRepresentative(context, consent.owner) != null) return consent;
		
		Set<UserGroupMember> groups = UserGroupMember.getAllActiveByMember(context.getAccessor());
		for (UserGroupMember group : groups) if (consent.authorized.contains(group.userGroup)) return consent;
		return null;							
	}
	

    /**
     * create a new consent
     * @return Consent json
     * @throws JsonValidationException
     * @throws AppException
     */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result add(Request request) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "name", "type");
		
		requireUserFeature(request, UserFeature.EMAIL_VERIFIED);		
		
		// validate request
		ConsentType type = JsonValidation.getEnum(json, "type", ConsentType.class);
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		String name = JsonValidation.getString(json, "name");
		MidataId userId = JsonValidation.getMidataId(json, "owner");
		String externalOwner = JsonValidation.getEMail(json, "externalOwner");
		if (userId == null && externalOwner == null) userId = executorId;
		String passcode = json.has("passcode") ? JsonValidation.getPassword(json, "passcode") : null;
						
		/*if (Consent.existsByOwnerAndName(userId, name)) {
		  throw new BadRequestException("error.exists.consent",  "A consent with this name already exists.");
		}*/
		
		if (passcode != null && !executorId.equals(userId)) {
		  throw new BadRequestException("error.internal", "Only owner may create consent with passcode");
		}
		
		Date validUntil = JsonValidation.getDate(json, "validUntil");
		Date createdBefore = JsonValidation.getDate(json, "createdBefore");
		boolean patientRecord = false;
		Consent consent;
		switch (type) {
		case CIRCLE : 			
			consent = new Circle();
			if (userId!=null) ((Circle) consent).order = Circle.getMaxOrder(userId) + 1;
			patientRecord = true;
			consent.writes = WritePermissionType.NONE;
			break;
		case REPRESENTATIVE:
			consent = new Circle();
			consent.type = ConsentType.REPRESENTATIVE;
			if (userId!=null) ((Circle) consent).order = Circle.getMaxOrder(userId) + 1;
			patientRecord = true;
			consent.writes = WritePermissionType.UPDATE_AND_CREATE;
			break;
		case HEALTHCARE :
			consent = new MemberKey();
			patientRecord = true;
			consent.writes = WritePermissionType.UPDATE_AND_CREATE;
			break;
		case HCRELATED :
			consent = new HCRelated();
			consent.writes = WritePermissionType.NONE;
			break;		
		default :
			throw new BadRequestException("error.internal", "Unsupported consent type");
		}
		
		if (passcode != null) {
			try{
				String hpasscode = PasswordHash.createHashGivenSalt(passcode.toCharArray(), userId.toByteArray());
				consent.passcode = hpasscode;
				if (Consent.getByOwnerAndPasscode(userId, hpasscode, Sets.create("name")) != null) {
					throw new BadRequestException("error.exists.passcode", "Please choose a different passcode!");
				}
			}  catch (NoSuchAlgorithmException e) {
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (InvalidKeySpecException e) {
				throw new InternalServerException("error.internal", "Cryptography error");
			}
		}
					
		consent.owner = userId;
		consent.externalOwner = externalOwner;
		consent.name = name;		
		consent.authorized = new HashSet<MidataId>();
		consent.status = (userId!=null && userId.equals(executorId)) ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
		consent.validUntil = validUntil;
		consent.createdBefore = createdBefore;
		if (json.has("writes")) {
		  consent.writes = JsonValidation.getEnum(json, "writes", WritePermissionType.class);
		}
		
		if (json.has("authorized")) {
			Set<MidataId> newMemberIds = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("authorized")));
			if (!newMemberIds.isEmpty()) {
				EntityType entityType = json.has("entityType") ? JsonValidation.getEnum(json, "entityType", EntityType.class) : null;
				consent.entityType = entityType;
				consent.authorized = newMemberIds;
				//addUsers(executorId, entityType, consent, newMemberIds);
			}
		}
		if (json.has("externalAuthorized")) {
			Set<String> extMails = JsonExtraction.extractStringSet(json.get("externalAuthorized"));
			for (String s : extMails) if (!JsonValidation.isEMail(s)) throw new JsonValidationException("error.invalid.email", "externalAuthorized", "invalid", "Invalid email");
			consent.externalAuthorized = extMails;			
		}
		
		if ((userId==null || !userId.equals(executorId)) && consent.authorized.size()==0) consent.authorized.add(executorId);
			
		addConsent(context, consent, patientRecord, passcode, false);					
		
		return ok(JsonOutput.toJson(consent, "Consent", Consent.ALL)).as("application/json");
	}
	
	public static void addConsent(AccessContext context, Consent consent, boolean patientRecord, String passcode, boolean force) throws AppException {
		consent._id = new MidataId();
		consent.dateOfCreation = new Date();
		consent.lastUpdated = consent.dateOfCreation;
		consent.dataupdate = System.currentTimeMillis();
			
		AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.CONSENT_CREATE).withActorUser(context.getActor()).withModifiedUser(consent.owner).withConsent(consent));
		
		if (consent.externalOwner != null) {
			Member member = Member.getByEmail(consent.externalOwner, Sets.create("_id"));
			if (member != null) {
				consent.owner = member._id;
				consent.externalOwner = null;
			}
		}
		if (consent.externalAuthorized != null) {
			Set<String> externalAuthorized = new HashSet<String>();
			for (String ext : consent.externalAuthorized) {
				if (!JsonValidation.isEMail(ext)) throw new BadRequestException("error.invalid.email", "Invalid email");
				Member member = Member.getByEmail(ext, Sets.create("_id"));
				if (member != null) {
					consent.authorized.add(member._id);
				} else externalAuthorized.add(ext);
			}
			if (externalAuthorized.isEmpty()) consent.externalAuthorized = null;
			else {
				consent.externalAuthorized = externalAuthorized;
				consent.status = ConsentStatus.UNCONFIRMED;
			}
		}
		
		if (consent.status != ConsentStatus.DRAFT && consent.status != ConsentStatus.UNCONFIRMED && !force && consent.type != ConsentType.IMPLICIT) {
			if (consent.owner == null || !consent.owner.equals(context.getAccessor())) {
				if (ApplicationTools.actAsRepresentative(context, consent.owner)==null) throw new AuthException("error.invalid.consent", "You must be owner to create active consents!");
			}
		}		
		
		if (consent.owner != null) {
			RecordManager.instance.createAnonymizedAPS(consent.owner, context.getAccessor(), consent._id, true);
			
			if (passcode != null) {			  
				  byte[] pubkey = KeyManager.instance.generateKeypairAndReturnPublicKey(consent._id, passcode);
			      RecordManager.instance.shareAPS(new ConsentAccessContext(consent, context), pubkey);
			      RecordManager.instance.setMeta(context, consent._id, "_config", CMaps.map("passcode", passcode));			  		
			}
		}
								
		
		//consent.add();
		
		consentStatusChange(context, consent, null, patientRecord);		
		if (consent.status.equals(ConsentStatus.ACTIVE) && patientRecord) autosharePatientRecord(context, consent);
		
		
								
		if (consent.status == ConsentStatus.UNCONFIRMED) {
			sendConsentNotifications(context.getAccessor(), consent, consent.status);
		} else if (consent.status == ConsentStatus.ACTIVE) {
			sendConsentNotifications(context.getAccessor(), consent, consent.status);
		}
				
		AuditManager.instance.success();

	}
	
	/**
	 * Retrieves or creates an implicit consent between a sender and a receiver for the purpose of sending messages.
	 * @param executorId executing person
	 * @param sender sender of message
	 * @param receiver receiver of message
	 * @param subject subject of message is the "owner" of the message
	 * @param groupReceiver receiver is a group
	 * @return the consent for the message
	 * @throws AppException
	 */
	public static Consent getOrCreateMessagingConsent(AccessContext context, MidataId sender, MidataId receiver, MidataId subject, boolean groupReceiver) throws AppException {
		if (context == null || sender == null || receiver == null || subject == null) throw new NullPointerException();
		MidataId other = receiver.equals(subject) ? sender : receiver;
		Consent consent = Consent.getMessagingActiveByAuthorizedAndOwner(other, subject);
		if (consent != null) return consent;
		
		//if (!executorId.equals(subject) && !executorId.equals(other)) throw new InternalServerException("error.internal", "Executor differs from message subject and other person");
		
		consent = new Consent();		
		consent.type = ConsentType.IMPLICIT;		
		consent.owner = subject;
		consent.authorized = Collections.singleton(other);
		consent.status = ConsentStatus.ACTIVE;
		consent.entityType = groupReceiver ? EntityType.USERGROUP : EntityType.USER;
		consent.writes = WritePermissionType.WRITE_ANY;
		
		if (groupReceiver && other.equals(receiver)) {
			UserGroup othergroup = UserGroup.getById(receiver, Sets.create("name"));
			consent.name="Msg: "+othergroup.name;
		} else {
			User otheruser = User.getById(other, Sets.create("firstname", "lastname"));
			consent.name="Msg: "+otheruser.firstname+" "+otheruser.lastname;
		}
		
		addConsent(context, consent, false, null, true);
						
		return consent;		
	}
	
	/**
	 * Each member has a FHIR patient record. That record should be shared with each consent so that a FHIR plugin can query for the patient. 
	 * @param consent consent with which the patient record should be shared
	 * @throws AppException
	 */
	//public static void autosharePatientRecord(AccessContext context1, Consent consent) throws AppException {
	//	AccessContext context = RecordManager.instance.createSharingContext(context1, consent.owner);
	//	autosharePatientRecord(context, consent);		
	//}
	
	public static void autosharePatientRecord(AccessContext executorContext, Consent consent) throws AppException {
		AccessContext context = RecordManager.instance.createSharingContext(executorContext, consent.owner);
		int recs = RecordManager.instance.share(context, consent._id, consent.owner, CMaps.map("owner", consent.owner).map("format", "fhir/Patient").map("data", CMaps.map("id", consent.owner.toString())), true);
		if (recs == 0) throw new InternalServerException("error.internal", "Patient Record not found!");
	}
	
	/**
	 * allows a user to get authorized for a consent by providing 
	 * a passphrase that has been specified during consent creation 
	 * @return consent authorized list
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result joinByPasscode(Request request) throws JsonValidationException, AppException {
		// validate json
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		MidataId groupExecutorId = executorId;
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "passcode", "owner");
		String passcode = JsonValidation.getString(json, "passcode");
		MidataId ownerId = JsonValidation.getMidataId(json, "owner");
		MidataId userGroupId = JsonValidation.getMidataId(json, "usergroup");
		
		if (userGroupId!=null) {
			UserGroupMember ugm = UserGroupMember.getByGroupAndActiveMember(userGroupId, executorId);
			if (ugm==null) throw new BadRequestException("error.invalid.usergroup", "Bad Usergroup");
			groupExecutorId = ugm.userGroup;
		}
		
		try {
		   String hpasscode = PasswordHash.createHashGivenSalt(passcode.toCharArray(), ownerId.toByteArray());
		   
		   Consent consent = Consent.getByOwnerAndPasscode(ownerId, hpasscode, Consent.ALL);
		   if (consent == null) throw new BadRequestException("error.invalid.passcode", "Bad passcode");
		   
		   KeyManager.instance.unlock(consent._id, passcode);
		   		   		   
		   // Maybe executor problem consent._id vs executor RecordManager.instance.shareAPS(consent._id, RecordManager.instance.createContextFromConsent(executorId, consent),consent._id, Collections.singleton(groupExecutorId));		   
		   AccessContext passcodeContext = new PasscodeAccessContext(context, consent, consent._id);
		   RecordManager.instance.shareAPS(passcodeContext, Collections.singleton(groupExecutorId));
		   
		   consent.authorized.add(groupExecutorId);
		   if (!groupExecutorId.equals(executorId)) {
		     consent.entityType = EntityType.USERGROUP;
		     Consent.set(consent._id, "entityType", consent.entityType);
		   }
		   Consent.set(consent._id, "authorized", consent.authorized);
		   Consent.set(consent._id, "lastUpdated", new Date());
		
		   return ok(JsonOutput.toJson(consent, "Consent", Sets.create("_id", "authorized"))).as("application/json");
		} catch (NoSuchAlgorithmException e) {
	    	throw new InternalServerException("error.internal", "Cryptography error");
	    } catch (InvalidKeySpecException e) {
	    	throw new InternalServerException("error.internal", "Cryptography error");
	    }
	}

	/**
	 * delete a consent
	 * @param circleIdString ID of consent
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result delete(Request request, String circleIdString) throws JsonValidationException, AppException {
		// validate request
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		MidataId circleId = new MidataId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Consent.FHIR);
		if (consent == null) {
			throw new BadRequestException("error.unknown.consent", "No consent with this id exists.");
		}
		if (consent.type != ConsentType.REPRESENTATIVE && consent.type != ConsentType.CIRCLE && consent.type != ConsentType.EXTERNALSERVICE && consent.type != ConsentType.API && consent.type != ConsentType.IMPLICIT) throw new BadRequestException("error.unsupported", "Operation not supported");
				
		switch (consent.type) {
		case EXTERNALSERVICE:
		case API:
			AuditManager.instance.addAuditEvent(AuditEventType.APP_DELETED, userId, consent);break;
		default: AuditManager.instance.addAuditEvent(AuditEventType.CONSENT_DELETE, userId, consent);break;
		}
		
		consentStatusChange(context, consent, ConsentStatus.EXPIRED);
		RecordManager.instance.deleteAPS(consent._id, userId);
		
		// Remove Rules
		removeQueries(userId, circleId);
		
		// delete circle		
		switch (consent.type) {
		case CIRCLE: Circle.delete(userId, circleId);break;
		case API:
		case EXTERNALSERVICE: Consent.delete(userId, circleId);break;
		case IMPLICIT: consent.setStatus(ConsentStatus.DELETED);break;
		//case STUDYRELATED : StudyRelated.delete(userId, circleId);break;
		default:break;
		}
		AuditManager.instance.success();
		
		return ok();
	}

	/**
	 * add users to the authorized people list of a consent
	 * @param circleIdString ID of consent
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result addUsers(Request request, String circleIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "users");
		
		// validate request
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		MidataId circleId = new MidataId(circleIdString);
		
		Consent consent = getConsentById(context, circleId, Sets.create("owner", "authorized","authorizedTypes", "type", "status"));
		if (consent == null) {
			throw new BadRequestException("error.unknown.consent", "No consent with this id belonging to user exists.");
		}
		if (!consent.owner.equals(userId) && !consent.status.equals(ConsentStatus.UNCONFIRMED)) {
			throw new BadRequestException("error.invalid.consent", "This consent may not be modified by current user.");
		}
		
		// add users to circle (implicit: if not already present)
		Set<MidataId> newMemberIds = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("users")));
				
		EntityType type = json.has("entityType") ? JsonValidation.getEnum(json, "entityType", EntityType.class) : null;
		addUsers(context, context.getAccessor(), type, consent, newMemberIds);		
					
		return ok();
	}

	
	
	public static void addUsers(AccessContext baseContext, MidataId userGroupExecutor, EntityType type, Consent consent, Set<MidataId> newMemberIds) throws AppException {
		if (type != null) {			
			if (consent.entityType == null) {
				consent.entityType = type;				
				Consent.set(consent._id, "entityType", type);
				
			} else if (!consent.entityType.equals(type)) {
				throw new BadRequestException("error.invalid.consent", "Bad consent entity type");
			}			
		}
		if (newMemberIds.contains(null)) throw new NullPointerException();
		
		consent.authorized.addAll(newMemberIds);
		if (!consent.type.equals(ConsentType.STUDYRELATED)) {
		  AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.CONSENT_PERSONS_CHANGE).withActorUser(baseContext.getActor()).withModifiedUser(consent.owner).withConsent(consent));
		}
		Consent.set(consent._id, "authorized", consent.authorized);
		Consent.set(consent._id, "lastUpdated", new Date());
		
		if (consent.status == ConsentStatus.ACTIVE) {
		  AccessContext context = baseContext.forConsentReshare(consent);
		  RecordManager.instance.reshareAPS(context, userGroupExecutor, newMemberIds);
		}
		
		AuditManager.instance.success();
	}

	/**
	 * remove a user from the authorized people lift of a consent
	 * @param circleIdString ID of consent
	 * @param memberIdString ID of user
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@Security.Authenticated(MemberSecured.class)
	@APICall
	public Result removeMember(Request request, String circleIdString, String memberIdString) throws JsonValidationException, AppException {
		// validate request
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		MidataId circleId = new MidataId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("authorized","type"));
		if (consent == null) {
			throw new BadRequestException("error.unknown.consent", "No consent with this id exists.");
		}
		
		// remove member from circle (implicit: if present)
		MidataId memberId = new MidataId(memberIdString);
		
        removeMember(context, consent, memberId);
		 
		return ok();
	}
	
	public static void removeMember(AccessContext context, Consent consent, MidataId memberId) throws AppException {
		consent.authorized.remove(memberId);
		Consent.set(consent._id, "authorized", consent.authorized);
		Consent.set(consent._id, "lastUpdated", new Date());

		Set<MidataId> memberIds = new HashSet<MidataId>();
		memberIds.add(memberId);
		
		RecordManager.instance.unshareAPSRecursive(context.forConsentReshare(consent), consent._id, memberIds);

	}
	
	public static void consentExpired(AccessContext context, MidataId consentId) throws AppException {
		Consent consent = getConsentById(context, consentId, Consent.FHIR);
		if (consent != null && !consent.status.equals(ConsentStatus.EXPIRED)) {			
			consentStatusChange(context, consent, ConsentStatus.EXPIRED);
			
		}
	}
	/**
	 * Call this method after the status of a consent has changed in order to activate or deactivate sharing as required. 
	 * @param executor id of executing user
	 * @param consent consent to check
	 * @throws AppException
	 */
	public static void consentStatusChange(AccessContext context, Consent consent, ConsentStatus newStatus) throws AppException {
	  Circles.consentStatusChange(context, consent, newStatus, true);	
	}
	
	public static void consentStatusChange(AccessContext context, Consent consent, ConsentStatus newStatus, boolean patientRecord) throws AppException {
		
		ConsentStatus oldStatus = consent.status;
		boolean wasActive = oldStatus.equals(ConsentStatus.ACTIVE) || oldStatus.equals(ConsentStatus.FROZEN);
		boolean active = (newStatus == null) ? wasActive : (newStatus.equals(ConsentStatus.ACTIVE) || newStatus.equals(ConsentStatus.FROZEN));
		if (context!=null) context = ApplicationTools.actAsRepresentative(context, consent.owner);
		
		if (newStatus == null) {
		  wasActive = false;
		} else {
		  consent.setStatus(newStatus);
		}
		
		if (active && !wasActive) {
			
			if (context == null) throw new InternalServerException("error.internal", "Cannot set consent to active state without proper context");
			RecordManager.instance.shareAPS(context.forConsentReshare(consent), consent.authorized);
			consentSettingChange(context, consent);
			
			Map<String, Object> query = consent.sharingQuery;
			if (consent.type.equals(ConsentType.REPRESENTATIVE)) {
				query = CMaps.map("group","all").map("group-system","v1");
			}			
			
			if (query == null) query = Circles.getQueries(consent.owner, consent._id);			
			if (query!=null) {
				if (consent.type.equals(ConsentType.EXTERNALSERVICE)) {
				  RecordManager.instance.shareByQuery(context, consent._id, query);				  
				} else {
				  Circles.setQuery(context, consent.owner, consent._id, query);
				  RecordManager.instance.applyQuery(context, query, consent.owner, consent, true);
				}
			}
			
			if (patientRecord && (consent.type.equals(ConsentType.REPRESENTATIVE) || consent.type.equals(ConsentType.CIRCLE) || consent.type.equals(ConsentType.HEALTHCARE) || consent.type.equals(ConsentType.STUDYPARTICIPATION))) autosharePatientRecord(context, consent);
			
			if (consent.type.equals(ConsentType.REPRESENTATIVE)) {
				ApplicationTools.linkRepresentativeConsentWithExecutorAccount(context, consent.owner, consent._id);
			}
		} else if (!active && wasActive) {
			
			Set<MidataId> auth = consent.authorized;
			if (auth != null && auth.contains(consent.owner)) { auth.remove(consent.owner); }
			
			if (context == null && (consent.type.equals(ConsentType.REPRESENTATIVE) || !auth.isEmpty())) throw new InternalServerException("error.internal", "Cannot set consent to passive state without proper context");
			
			if (consent.type.equals(ConsentType.REPRESENTATIVE)) {
				RecordManager.instance.removeMeta(context, consent._id, "_representative");
			}
			
			
			if (consent.authorized != null && consent.authorized.size()>0) RecordManager.instance.unshareAPSRecursive(context.forConsentReshare(consent), consent._id, consent.authorized);
			
			if (consent.type.equals(ConsentType.EXTERNALSERVICE)) {
				Plugin app;
				if (consent instanceof MobileAppInstance) {
				  app = Plugin.getById(((MobileAppInstance) consent).applicationId);
				} else {
				  MobileAppInstance mai = MobileAppInstance.getById(consent._id, Sets.create("applicationId"));
				  app = Plugin.getById(mai.applicationId);
				}
				
				SubscriptionManager.deactivateSubscriptions(consent.owner, app, consent._id);
				//Circles.removeQueries(consent.owner, consent._id);

			} else Circles.removeQueries(consent.owner, consent._id);
		}
		if (newStatus != null && newStatus.equals(ConsentStatus.FROZEN)) {
			if (context == null) throw new InternalServerException("error.internal", "Cannot set consent to frozen state without proper context");
			Date now = new Date();
			if (consent.createdBefore == null || consent.createdBefore.after(now)) {				
				consent.createdBefore = now;
				consent.set(consent._id, "createdBefore", consent.createdBefore);
				consentSettingChange(context, consent);
			}
			Circles.removeQueries(consent.owner, consent._id);
		}
		
		prepareConsent(context, consent, newStatus == null);
	}
	 
	
	/**
	 * Updates APS of consent after settings of the consent have been changed.
	 * @param executor id of executing user
	 * @param consent consent to check
	 * @throws AppException
	 */
	public static void consentSettingChange(AccessContext context, Consent consent) throws AppException {
		if (consent.status == ConsentStatus.ACTIVE || consent.status == ConsentStatus.FROZEN || context.getAccessor().equals(consent.owner)) {
			BasicBSONObject dat = (BasicBSONObject) RecordManager.instance.getMeta(context, consent._id, "_filter");
			Map<String, Object> restrictions = (dat == null) ? new HashMap<String, Object>() : dat.toMap();
			if (consent.validUntil != null) {
				restrictions.put("valid-until", consent.validUntil);
			} else {
				restrictions.remove("valid-until");
			}
			if (consent.createdBefore != null) {
				restrictions.put("history-date", consent.createdBefore);
			} else {
				restrictions.remove("history-date");
			}
			
			RecordManager.instance.setMeta(context, consent._id, "_filter", restrictions);
		}
	}
	
	/**
	 * Prepares a consent 
	 * @param consent
	 * @throws AppException
	 */
	public static void prepareConsent(AccessContext context, Consent consent, boolean isNew) throws AppException {
		ConsentResourceProvider.updateMidataConsent(consent, null);		
		if (consent.authorized == null && consent.type != ConsentType.EXTERNALSERVICE) throw new InternalServerException("error.internal", "Missing authorized");
		if (isNew) {
			consent.add();
		} else {
			Consent.set(consent._id, "fhirConsent", consent.fhirConsent);
		}
		SubscriptionManager.resourceChange(context, consent);
	}
	
	public static void sendConsentNotifications(MidataId executorId, Consent consent, ConsentStatus reason) throws AppException {
		MidataId sourcePlugin = consent.creatorApp != null ? consent.creatorApp : RuntimeConstants.instance.portalPlugin;
		Map<String, String> replacements = new HashMap<String, String>();
		Set<Object> targets = new HashSet<Object>();
		targets.addAll(consent.authorized);
		targets.remove(executorId);
						
		String category = consent.categoryCode;
		if (category == null) category = consent.type.toString();
		
		User sender = User.getByIdAlsoDeleted(executorId, Sets.create("firstname", "lastname", "role", "email", "language"));
		replacements.put("executor-firstname", sender.firstname);
		replacements.put("executor-lastname", sender.lastname);
		replacements.put("executor-email", sender.email != null ? sender.email : "none");
		try {
			if (executorId.equals(consent.owner)) {
				replacements.put("grantor-firstname", sender.firstname);
				replacements.put("grantor-lastname", sender.lastname);
				replacements.put("grantor-email", sender.email != null ? sender.email : "none");
			} else if (consent.owner != null) {
				User owner = User.getByIdAlsoDeleted(consent.owner, Sets.create("firstname", "lastname", "role", "email", "language"));
				replacements.put("grantor-firstname", owner.firstname);
				replacements.put("grantor-lastname", owner.lastname);
				replacements.put("grantor-email", owner.email != null ? owner.email : "none");
				replacements.put("confirm-url", InstanceConfig.getInstance().getServiceURL()+"?consent="+consent._id+(owner.email != null ? ("&login="+URLEncoder.encode(owner.email, "UTF-8")) : ""));
			} else {
				replacements.put("grantor-firstname", "");
				replacements.put("grantor-lastname", "");
				replacements.put("grantor-email", consent.externalOwner);
				replacements.put("confirm-url", InstanceConfig.getInstance().getServiceURL()+"?consent="+consent._id+"&isnew=true&login="+URLEncoder.encode(consent.externalOwner, "UTF-8"));
			}
			replacements.put("consent-name", consent.name);		
					
			if (reason == ConsentStatus.UNCONFIRMED) {
				Messager.sendMessage(sourcePlugin, MessageReason.CONSENT_REQUEST_AUTHORIZED_INVITED, category, consent.externalAuthorized, sender.language, replacements);
				Messager.sendMessage(sourcePlugin, MessageReason.CONSENT_REQUEST_AUTHORIZED_EXISTING, category, targets, sender.language, replacements);
				Messager.sendMessage(sourcePlugin, MessageReason.CONSENT_REQUEST_OWNER_INVITED, category, Collections.singleton(consent.externalOwner), sender.language, replacements);
				if (!executorId.equals(consent.owner)) Messager.sendMessage(sourcePlugin, MessageReason.CONSENT_REQUEST_OWNER_EXISTING, category, Collections.singleton(consent.owner), sender.language, replacements);
			} else if (reason == ConsentStatus.ACTIVE) {
				Messager.sendMessage(sourcePlugin, MessageReason.CONSENT_CONFIRM_AUTHORIZED, category, targets, sender.language, replacements);
				if (!executorId.equals(consent.owner)) Messager.sendMessage(sourcePlugin, MessageReason.CONSENT_CONFIRM_OWNER, category, Collections.singleton(consent.owner), sender.language, replacements);			
			} else if (reason == ConsentStatus.REJECTED) {
				Messager.sendMessage(sourcePlugin, MessageReason.CONSENT_REJECT_AUTHORIZED, category, targets, sender.language, replacements);
				if (!executorId.equals(consent.owner)) Messager.sendMessage(sourcePlugin, MessageReason.CONSENT_REJECT_OWNER, category, Collections.singleton(consent.owner), sender.language, replacements);
			}
		} catch (UnsupportedEncodingException e) {}
		
	}
	
	public static void fetchExistingConsents(AccessContext context, String emailLC) throws AppException {
		Set<Consent> consents = Consent.getByExternalEmail(emailLC);
		for (Consent consent : consents) {
			addUsers(context, context.getAccessor(), EntityType.USER, consent, Collections.singleton(context.getAccessor()));
			consent.externalAuthorized.remove(emailLC);
			Consent.set(consent._id, "externalAuthorized", consent.externalAuthorized);
			
			prepareConsent(context, consent, false);
		}
		
		consents = Consent.getByExternalOwnerEmail(emailLC);
		for (Consent consent : consents) {
			
			consent.owner = context.getAccessor();
			consent.externalOwner = null;
			
			RecordManager.instance.createAnonymizedAPS(consent.owner, context.getAccessor(), consent._id, true);
			
			Consent.set(consent._id, "owner", consent.owner);
			Consent.set(consent._id, "externalOwner", consent.externalOwner);
			
			prepareConsent(context, consent, false);
		}
	}
	
	/**
	 * return query for automatic record adding for a consent
	 * @param userId ID of user
	 * @param apsId ID of consent
	 * @return query
	 * @throws InternalServerException
	 */
	public static Map<String, Object> getQueries(MidataId userId, MidataId apsId) throws InternalServerException {
		Member member = Member.getById(userId, Sets.create("queries"));
		if (member!=null && member.queries!=null) return member.queries.get(apsId.toString());
		return null;
	}
	
	/**
	 * set query for automatic record adding for a consent 
	 * @param excecutor ID of executor
	 * @param userId ID of owner
	 * @param apsId ID of consent
	 * @param query query to be set
	 * @throws AppException
	 */
	public static void setQuery(AccessContext context, MidataId userId, MidataId apsId, Map<String, Object> query) throws AppException {
		Member member = Member.getById(userId, Sets.create("queries", "rqueries"));
		Pair<Map<String, Object>, Map<String, Object>> pair = Feature_Streams.convertToQueryPair(query);
		if (pair.getLeft() != null && member.queries==null) {
			member.queries = new HashMap<String, Map<String, Object>>();
		}
		if (pair.getRight() != null && member.rqueries==null) {
			member.rqueries = new HashMap<String, Map<String, Object>>();
		}
		if (pair.getLeft() != null) {
			member.queries.put(apsId.toString(), pair.getLeft());
			Member.set(userId, "queries", member.queries);
		}
		if (pair.getRight() != null) {
			member.rqueries.put(apsId.toString(), pair.getRight());
			Member.set(userId, "rqueries", member.rqueries);
		}				
		if (query.containsKey("exclude-ids")) {
			Map<String, Object> ids = new HashMap<String,Object>();
			ids.put("ids", query.get("exclude-ids"));
			RecordManager.instance.setMeta(context, apsId, "_exclude", ids);
		} else if (context.getAccessor().equals(userId)) {
			RecordManager.instance.removeMeta(context, apsId, "_exclude");
		}
	}
			
	/**
	 * remove query for automatic record adding for a consent from user account
	 * @param userId ID of user
	 * @param targetaps ID of content
	 * @throws InternalServerException
	 */
	public static void removeQueries(MidataId userId, MidataId targetaps) throws InternalServerException {
		Member member = Member.getById(userId, Sets.create("queries", "rqueries"));
		if (member == null) return;

        String key = targetaps.toString();
        
		if (member.queries != null) {		 	
		    if (member.queries.containsKey(key)) {
		    	member.queries.remove(key);
		    	Member.set(userId, "queries", member.queries);
		    }	    
		}
		if (member.rqueries != null) {
			if (member.rqueries.containsKey(key)) {
		    	member.rqueries.remove(key);
		    	Member.set(userId, "rqueries", member.rqueries);
		    }	
		}
	}
		
}
