package controllers;

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
import models.Record;
import models.RecordsInfo;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.AggregationType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EntityType;
import models.enums.SubUserRole;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.PasswordHash;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
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
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for managing consents
 *
 */
public class Circles extends APIController {	

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
	public static Result get() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
						
		List<Circle> circles = null;

		if (json.has("owner")) {
			MidataId owner = new MidataId(request().username());
			circles = new ArrayList<Circle>(Circle.getAllByOwner(owner));
		} else if (json.has("member")) {
			MidataId member = new MidataId(request().username());
			circles = new ArrayList<Circle>(Circle.getAllByMember(member));
			ReferenceTool.resolveOwners(circles, true);
		} else JsonValidation.validate(json, "owner");
		Collections.sort(circles);
		return ok(JsonOutput.toJson(circles, "Consent", Sets.create("name", "order", "owner", "authorized")));
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
	public static Result listConsents() throws JsonValidationException, AppException, AuthException {
		// validate json
		JsonNode json = request().body().asJson();					
		JsonValidation.validate(json, "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));	
		ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "authorized");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("Circles.listConsents", getRole(), properties, fields);
		
		List<Consent> consents = null;
	
		MidataId owner = new MidataId(request().username());
		if (properties.containsKey("_id")) {
			MidataId target = MidataId.from(properties.get("_id"));
			Consent c = getConsentById(owner, target, fields);
			consents = (c!=null) ? Collections.singletonList(c) : Collections.<Consent>emptyList();
		} else if (properties.containsKey("member")) {
		  properties.remove("member");
		  consents = new ArrayList<Consent>(getConsentsAuthorized(owner, properties, fields));
		} else {
		  consents = new ArrayList<Consent>(Consent.getAllByOwner(owner, properties, fields));
		}
		
		if (fields.contains("ownerName")) ReferenceTool.resolveOwners(consents, true);
		
		if (fields.contains("records")) {
			Map<String, Object> all = new HashMap<String,Object>();
			for (Consent consent : consents) {
				if (!consent.status.equals(ConsentStatus.EXPIRED)) {
				  Collection<RecordsInfo> summary = RecordManager.instance.info(owner, consent._id, all, AggregationType.ALL);
				  if (summary.isEmpty()) consent.records = 0; else consent.records = summary.iterator().next().count;
				} else consent.records = 0;
			}
		}
		
		if (fields.contains("createdBefore") || fields.contains("validUntil")) {
			for (Consent consent : consents) {
				BasicBSONObject obj = (BasicBSONObject) RecordManager.instance.getMeta(owner, consent._id, "_filter");
				if (obj != null) {
					consent.validUntil = obj.getDate("valid-until");
					consent.createdBefore = obj.getDate("created-before");
				}
			}
		}
		
		if (fields.contains("passcode") && !properties.containsKey("member")) {
			for (Consent consent : consents) {
				if (consent.type == null || consent.type.equals(ConsentType.EXTERNALSERVICE)) {
				   BSONObject obj = RecordManager.instance.getMeta(owner, consent._id, "_app");
				   if (obj != null) consent.passcode = obj.get("phrase").toString();
				}
				if (consent.type == null || consent.type.equals(ConsentType.HEALTHCARE)) {
				   BSONObject obj = RecordManager.instance.getMeta(owner, consent._id, "_config");
				   if (obj != null) consent.passcode = obj.get("passcode").toString();
				}
			}
		}
				
		
		//Collections.sort(circles);
		return ok(JsonOutput.toJson(consents, "Consent", fields));
	}
	
	public static Collection<Consent> getConsentsAuthorized(MidataId user, Map<String, Object> properties, Set<String> fields) throws AppException {
		Set<UserGroupMember> groups = UserGroupMember.getAllActiveByMember(user);
		Set<MidataId> auth = new HashSet<MidataId>();
		auth.add(user);
		for (UserGroupMember group : groups) auth.add(group.userGroup);
		Collection<Consent> consents = Consent.getAllByAuthorized(auth, properties, fields);
		return consents;
	}
	
	public static Consent getConsentById(MidataId user, MidataId consentId, Set<String> fields) throws AppException {
		fields.add("owner");
		fields.add("authorized");
		Consent consent = Consent.getByIdUnchecked(consentId, fields);		
		if (consent == null) return null;
		if (consent.owner.equals(user)) return consent;
		if (consent.authorized.contains(user)) return consent;
		Set<UserGroupMember> groups = UserGroupMember.getAllActiveByMember(user);
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
	public static Result add() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "type");
		
		forbidSubUserRole(SubUserRole.TRIALUSER, SubUserRole.NONMEMBERUSER);
		
		// validate request
		ConsentType type = JsonValidation.getEnum(json, "type", ConsentType.class);
		MidataId executorId = new MidataId(request().username());
		String name = JsonValidation.getString(json, "name");
		MidataId userId = JsonValidation.getMidataId(json, "owner");
		if (userId == null) userId = executorId;
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
			forbidSubUserRole(SubUserRole.STUDYPARTICIPANT, SubUserRole.MEMBEROFCOOPERATIVE);
			forbidSubUserRole(SubUserRole.APPUSER, SubUserRole.MEMBEROFCOOPERATIVE);
			consent = new Circle();
			((Circle) consent).order = Circle.getMaxOrder(userId) + 1;
			patientRecord = true;
			break;
		case HEALTHCARE :
			consent = new MemberKey();
			patientRecord = true;
			break;
		case HCRELATED :
			consent = new HCRelated();
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
				throw new InternalServerException("error.internal", e);
			} catch (InvalidKeySpecException e) {
				throw new InternalServerException("error.internal", e);
			}
		}
			
		consent._id = new MidataId();
		consent.owner = userId;
		consent.name = name;		
		consent.authorized = new HashSet<MidataId>();
		consent.status = userId.equals(executorId) ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
		consent.validUntil = validUntil;
		consent.createdBefore = createdBefore;
		if (! userId.equals(executorId)) consent.authorized.add(executorId);
							
		RecordManager.instance.createAnonymizedAPS(userId, executorId, consent._id, true);
		
		if (passcode != null) {			  
			  byte[] pubkey = KeyManager.instance.generateKeypairAndReturnPublicKey(consent._id, passcode);
		      RecordManager.instance.shareAPS(consent._id, userId, consent._id, pubkey);
		      RecordManager.instance.setMeta(userId, consent._id, "_config", CMaps.map("passcode", passcode));			  		
		}
						
		consentSettingChange(executorId, consent);
		consent.add();
				
		if (consent.status.equals(ConsentStatus.ACTIVE) && patientRecord) autosharePatientRecord(consent);
		
		if (json.has("authorized")) {
			Set<MidataId> newMemberIds = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("authorized")));
			if (!newMemberIds.isEmpty()) {
				EntityType entityType = json.has("entityType") ? JsonValidation.getEnum(json, "entityType", EntityType.class) : null;
				addUsers(executorId, entityType, consent, newMemberIds);
			}
		}
		
		return ok(JsonOutput.toJson(consent, "Consent", Consent.ALL));
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
	public static Consent getOrCreateMessagingConsent(MidataId executorId, MidataId sender, MidataId receiver, MidataId subject, boolean groupReceiver) throws AppException {
		if (executorId == null || sender == null || receiver == null || subject == null) throw new NullPointerException();
		MidataId other = sender.equals(subject) ? receiver : sender;
		Consent consent = Consent.getMessagingActiveByAuthorizedAndOwner(other, subject);
		if (consent != null) return consent;
		
		if (!executorId.equals(subject) && !executorId.equals(other)) throw new InternalServerException("error.internal", "Executor differs from message subject and other person");
		
		consent = new Consent();
		consent.type = ConsentType.IMPLICIT;
		consent._id = new MidataId();
		consent.owner = subject;
		consent.authorized = Collections.singleton(other);
		consent.status = ConsentStatus.ACTIVE;
		
		if (groupReceiver && other.equals(receiver)) {
			UserGroup othergroup = UserGroup.getById(receiver, Sets.create("name"));
			consent.name="Msg: "+othergroup.name;
		} else {
			User otheruser = User.getById(other, Sets.create("firstname", "lastname"));
			consent.name="Msg: "+otheruser.firstname+" "+otheruser.lastname;
		}
		
		RecordManager.instance.createAnonymizedAPS(subject, other, consent._id, true);
		consentSettingChange(executorId, consent);
		consent.add();
		
		return consent;		
	}
	
	/**
	 * Each member has a FHIR patient record. That record should be shared with each consent so that a FHIR plugin can query for the patient. 
	 * @param consent consent with which the patient record should be shared
	 * @throws AppException
	 */
	public static void autosharePatientRecord(Consent consent) throws AppException {
		List<Record> recs = RecordManager.instance.list(consent.owner, consent.owner, CMaps.map("owner", "self").map("format", "fhir/Patient"), Sets.create("_id"));
		if (recs.size()>0) {
		  RecordManager.instance.share(consent.owner, consent.owner, consent._id, Collections.singleton(recs.get(0)._id), true);
		}
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
	public static Result joinByPasscode() throws JsonValidationException, AppException {
		// validate json
		MidataId executorId = new MidataId(request().username());
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "passcode", "owner");
		String passcode = JsonValidation.getString(json, "passcode");
		MidataId ownerId = JsonValidation.getMidataId(json, "owner");
		
		try {
		   String hpasscode = PasswordHash.createHashGivenSalt(passcode.toCharArray(), ownerId.toByteArray());
		   
		   Consent consent = Consent.getByOwnerAndPasscode(ownerId, hpasscode, Sets.create("name","authorized"));
		   if (consent == null) throw new BadRequestException("error.invalid.passcode", "Bad passcode");
		   
		   KeyManager.instance.unlock(consent._id, passcode);		  
		   RecordManager.instance.shareAPS(consent._id, consent._id, Collections.singleton(executorId));		   
		   consent.authorized.add(executorId);
		   Consent.set(consent._id, "authorized", consent.authorized);
		
		   return ok(JsonOutput.toJson(consent, "Consent", Sets.create("_id", "authorized")));
		} catch (NoSuchAlgorithmException e) {
	    	throw new InternalServerException("error.internal", e);
	    } catch (InvalidKeySpecException e) {
	    	throw new InternalServerException("error.internal", e);
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
	public static Result delete(String circleIdString) throws JsonValidationException, AppException {
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId circleId = new MidataId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("owner", "authorized", "type"));
		if (consent == null) {
			throw new BadRequestException("error.unknown.consent", "No consent with this id exists.");
		}
		if (consent.type != ConsentType.CIRCLE && consent.type != ConsentType.EXTERNALSERVICE && consent.type != ConsentType.IMPLICIT) throw new BadRequestException("error.unsupported", "Operation not supported");
		
		// Remove APS
		consent.setStatus(ConsentStatus.EXPIRED);
		consentStatusChange(userId, consent);
		RecordManager.instance.deleteAPS(consent._id, userId);
		
		// Remove Rules
		removeQueries(userId, circleId);
		
		// delete circle		
		switch (consent.type) {
		case CIRCLE: Circle.delete(userId, circleId);break;
		case EXTERNALSERVICE: Circle.delete(userId, circleId);break;
		case IMPLICIT: Consent.delete(userId, circleId);break;
		default:break;
		}
		
		
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
	public static Result addUsers(String circleIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "users");
		
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId circleId = new MidataId(circleIdString);
		
		Consent consent = getConsentById(userId, circleId, Sets.create("owner", "authorized","authorizedTypes", "type", "status"));
		if (consent == null) {
			throw new BadRequestException("error.unknown.consent", "No consent with this id belonging to user exists.");
		}
		if (!consent.owner.equals(userId) || !consent.status.equals(ConsentStatus.UNCONFIRMED)) {
			throw new BadRequestException("error.invalid.consent", "This consent may not be modified by current user.");
		}
		
		// add users to circle (implicit: if not already present)
		Set<MidataId> newMemberIds = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("users")));
				
		EntityType type = json.has("entityType") ? JsonValidation.getEnum(json, "entityType", EntityType.class) : null;
		addUsers(userId, type, consent, newMemberIds);		
					
		return ok();
	}
	
	public static void addUsers(MidataId executor, EntityType type, Consent consent, Set<MidataId> newMemberIds) throws AppException {
		if (type != null) {			
			if (consent.entityType == null) {
				consent.entityType = type;				
				Consent.set(consent._id, "entityType", type);
				
			} else if (!consent.entityType.equals(type)) {
				throw new BadRequestException("error.invalid.consent", "Bad consent entity type");
			}			
		}
		
		consent.authorized.addAll(newMemberIds);
		Consent.set(consent._id, "authorized", consent.authorized);
		
		RecordManager.instance.shareAPS(consent._id, executor, newMemberIds);
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
	public static Result removeMember(String circleIdString, String memberIdString) throws JsonValidationException, AppException {
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId circleId = new MidataId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("authorized","type"));
		if (consent == null) {
			throw new BadRequestException("error.unknown.consent", "No consent with this id exists.");
		}
		
		// remove member from circle (implicit: if present)
		MidataId memberId = new MidataId(memberIdString);
		
		consent.authorized.remove(memberId);
		Consent.set(consent._id, "authorized", consent.authorized);

		Set<MidataId> memberIds = new HashSet<MidataId>();
		memberIds.add(memberId);
		
		RecordManager.instance.unshareAPSRecursive(consent._id, userId, memberIds);
		
		return ok();
	}
	
	public static void consentExpired(MidataId executor, MidataId consentId) throws AppException {
		Consent consent = getConsentById(executor, consentId, Consent.ALL);
		if (consent != null && !consent.status.equals(ConsentStatus.EXPIRED)) {
			consent.setStatus(ConsentStatus.EXPIRED);
			consentStatusChange(executor, consent);
		}
	}
	/**
	 * Call this method after the status of a consent has changed in order to activate or deactivate sharing as required. 
	 * @param executor id of executing user
	 * @param consent consent to check
	 * @throws AppException
	 */
	public static void consentStatusChange(MidataId executor, Consent consent) throws AppException {
		boolean active = consent.status.equals(ConsentStatus.ACTIVE);
		if (active) {
			RecordManager.instance.shareAPS(consent._id, consent.owner, consent.authorized);
			if (consent.type.equals(ConsentType.CIRCLE) || consent.type.equals(ConsentType.HEALTHCARE)) autosharePatientRecord(consent);
			Map<String, Object> query = Circles.getQueries(consent.owner, consent._id);
			if (query!=null) RecordManager.instance.applyQuery(executor, query, consent.owner, consent._id, true);	 
		} else {
			Set<MidataId> auth = consent.authorized;
			if (auth.contains(consent.owner)) { auth.remove(consent.owner); }
			RecordManager.instance.unshareAPSRecursive(consent._id, consent.owner, consent.authorized);
		}
	}
	
	/**
	 * Updates APS of consent after settings of the consent have been changed.
	 * @param executor id of executing user
	 * @param consent consent to check
	 * @throws AppException
	 */
	public static void consentSettingChange(MidataId executor, Consent consent) throws AppException {
		BasicBSONObject dat = (BasicBSONObject) RecordManager.instance.getMeta(executor, consent._id, "_filter");
		Map<String, Object> restrictions = (dat == null) ? new HashMap<String, Object>() : dat.toMap();
		if (consent.validUntil != null) {
			restrictions.put("valid-until", consent.validUntil);
		} else {
			restrictions.remove("valid-until");
		}
		if (consent.createdBefore != null) {
			restrictions.put("created-before", consent.createdBefore);
		} else {
			restrictions.remove("created-before");
		}
		
		RecordManager.instance.setMeta(executor, consent._id, "_filter", restrictions);				
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
		if (member.queries!=null) return member.queries.get(apsId.toString());
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
	public static void setQuery(MidataId executor, MidataId userId, MidataId apsId, Map<String, Object> query) throws AppException {
		Member member = Member.getById(userId, Sets.create("queries"));
		if (member.queries==null) {
			member.queries = new HashMap<String, Map<String, Object>>();
		}
		member.queries.put(apsId.toString(), query);
		Member.set(userId, "queries", member.queries);
		if (query.containsKey("exclude-ids")) {
			Map<String, Object> ids = new HashMap<String,Object>();
			ids.put("ids", query.get("exclude-ids"));
			RecordManager.instance.setMeta(userId, apsId, "_exclude", ids);
		} else if (executor.equals(userId)) {
			RecordManager.instance.removeMeta(userId, apsId, "_exclude");
		}
	}
			
	/**
	 * remove query for automatic record adding for a consent from user account
	 * @param userId ID of user
	 * @param targetaps ID of content
	 * @throws InternalServerException
	 */
	protected static void removeQueries(MidataId userId, MidataId targetaps) throws InternalServerException {
        Member member = Member.getById(userId, Sets.create("queries"));
		
		if (member.queries == null) return;
		 
		String key = targetaps.toString();
	    if (member.queries.containsKey(key)) {
	    	member.queries.remove(key);
	    	Member.set(userId, "queries", member.queries);
	    }
	}
		
}
