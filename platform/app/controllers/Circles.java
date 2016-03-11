package controllers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Consent;
import models.HCRelated;
import models.Member;
import models.MemberKey;
import models.RecordsInfo;
import models.enums.AggregationType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;

import org.bson.types.ObjectId;

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
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

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
			ObjectId owner = new ObjectId(request().username());
			circles = new ArrayList<Circle>(Circle.getAllByOwner(owner));
		} else if (json.has("member")) {
			ObjectId member = new ObjectId(request().username());
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
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("Circles.listConsents", getRole(), properties, fields);
		
		List<Consent> consents = null;
	
		ObjectId owner = new ObjectId(request().username());
		if (properties.containsKey("member")) {
		  consents = new ArrayList<Consent>(Consent.getAllByAuthorized(owner));
		} else {
		  consents = new ArrayList<Consent>(Consent.getAllByOwner(owner, properties, fields));
		}
		
		if (fields.contains("ownerName")) ReferenceTool.resolveOwners(consents, true);
		
		if (fields.contains("records")) {
			Map<String, Object> all = new HashMap<String,Object>();
			for (Consent consent : consents) {
				Collection<RecordsInfo> summary = RecordManager.instance.info(owner, consent._id, all, AggregationType.ALL);
				if (summary.isEmpty()) consent.records = 0; else consent.records = summary.iterator().next().count;
			}
		}
		
		//Collections.sort(circles);
		return ok(JsonOutput.toJson(consents, "Consent", fields));
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
		
		// validate request
		ConsentType type = JsonValidation.getEnum(json, "type", ConsentType.class);
		ObjectId executorId = new ObjectId(request().username());
		String name = JsonValidation.getString(json, "name");
		ObjectId userId = JsonValidation.getObjectId(json, "owner");
		if (userId == null) userId = executorId;
		String passcode = json.has("passcode") ? JsonValidation.getPassword(json, "passcode") : null;
						
		if (Consent.existsByOwnerAndName(userId, name)) {
		  return badRequest("A consent with this name already exists.");
		}
		
		if (passcode != null && !executorId.equals(userId)) {
		  return badRequest("Only owner may create consent with passcode");
		}
		
		Consent consent;
		switch (type) {
		case CIRCLE : 
			consent = new Circle();
			((Circle) consent).order = Circle.getMaxOrder(userId) + 1;
			break;
		case HEALTHCARE :
			consent = new MemberKey();
			break;
		case HCRELATED :
			consent = new HCRelated();
			break;		
		default :
			return badRequest("Unsupported consent type");
		}
		
		if (passcode != null) {
			try{
				String hpasscode = PasswordHash.createHashGivenSalt(passcode.toCharArray(), userId.toByteArray());
				consent.passcode = hpasscode;
				if (Consent.getByOwnerAndPasscode(userId, hpasscode, Sets.create("name")) != null) {
					return badRequest("Please choose a different passcode!");
				}
			}  catch (NoSuchAlgorithmException e) {
				throw new InternalServerException("error.internal.cryptography", e);
			} catch (InvalidKeySpecException e) {
				throw new InternalServerException("error.internal.cryptography", e);
			}
		}
			
		consent._id = new ObjectId();
		consent.owner = userId;
		consent.name = name;		
		consent.authorized = new HashSet<ObjectId>();
		consent.status = userId.equals(executorId) ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
		if (! userId.equals(executorId)) consent.authorized.add(executorId);
							
		RecordManager.instance.createAnonymizedAPS(userId, executorId, consent._id);
		
		if (passcode != null) {			  
			  byte[] pubkey = KeyManager.instance.generateKeypairAndReturnPublicKey(consent._id, passcode);
		      RecordManager.instance.shareAPS(consent._id, userId, consent._id, pubkey);
		      RecordManager.instance.setMeta(userId, consent._id, "_config", CMaps.map("passcode", passcode));			  		
		}
		
		consent.add();
				
		return ok(JsonOutput.toJson(consent, "Consent", Consent.ALL));
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
		ObjectId executorId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "passcode", "owner");
		String passcode = JsonValidation.getString(json, "passcode");
		ObjectId ownerId = JsonValidation.getObjectId(json, "owner");
		
		try {
		   String hpasscode = PasswordHash.createHashGivenSalt(passcode.toCharArray(), ownerId.toByteArray());
		   
		   Consent consent = Consent.getByOwnerAndPasscode(ownerId, hpasscode, Sets.create("name","authorized"));
		   if (consent == null) return badRequest("Bad passcode");
		   
		   KeyManager.instance.unlock(consent._id, passcode);		  
		   RecordManager.instance.shareAPS(consent._id, consent._id, Collections.singleton(executorId));		   
		   consent.authorized.add(executorId);
		   Consent.set(consent._id, "authorized", consent.authorized);
		
		   return ok(JsonOutput.toJson(consent, "Consent", Sets.create("_id", "authorized")));
		} catch (NoSuchAlgorithmException e) {
	    	throw new InternalServerException("error.internal.cryptography", e);
	    } catch (InvalidKeySpecException e) {
	    	throw new InternalServerException("error.internal.cryptography", e);
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
	@Security.Authenticated(MemberSecured.class)
	public static Result delete(String circleIdString) throws JsonValidationException, AppException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("owner", "authorized", "type"));
		if (consent == null) {
			return badRequest("No consent with this id exists.");
		}
		if (consent.type != ConsentType.CIRCLE && consent.type != ConsentType.EXTERNALSERVICE) return badRequest("Operation not supported");
		
		// Remove APS
		consent.setStatus(ConsentStatus.EXPIRED);
		consentStatusChange(userId, consent);
		RecordManager.instance.deleteAPS(consent._id, consent.owner);
		
		// Remove Rules
		removeQueries(userId, circleId);
		
		// delete circle		
		switch (consent.type) {
		case CIRCLE: Circle.delete(userId, circleId);break;
		case EXTERNALSERVICE: Circle.delete(userId, circleId);break;
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
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("authorized","type"));
		if (consent == null) {
			return badRequest("No consent with this id belonging to user exists.");
		}
		
		// add users to circle (implicit: if not already present)
		Set<ObjectId> newMemberIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("users")));
				
		consent.authorized.addAll(newMemberIds);
		Consent.set(consent._id, "authorized", consent.authorized);
		
		RecordManager.instance.shareAPS(consent._id, userId, newMemberIds);
					
		return ok();
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
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("authorized","type"));
		if (consent == null) {
			return badRequest("No consent with this id exists.");
		}
		
		// remove member from circle (implicit: if present)
		ObjectId memberId = new ObjectId(memberIdString);
		
		consent.authorized.remove(memberId);
		Consent.set(consent._id, "authorized", consent.authorized);

		Set<ObjectId> memberIds = new HashSet<ObjectId>();
		memberIds.add(memberId);
		
		RecordManager.instance.unshareAPSRecursive(consent._id, userId, memberIds);
		
		return ok();
	}
	
	public static void consentStatusChange(ObjectId executor, Consent consent) throws AppException {
		boolean active = consent.status.equals(ConsentStatus.ACTIVE);
		if (active) {
			RecordManager.instance.shareAPS(consent._id, consent.owner, consent.authorized);
		} else {
			Set<ObjectId> auth = consent.authorized;
			if (auth.contains(consent.owner)) { auth.remove(consent.owner); }
			RecordManager.instance.unshareAPSRecursive(consent._id, consent.owner, consent.authorized);
		}
	}
	
	/**
	 * return query for automatic record adding for a consent
	 * @param userId ID of user
	 * @param apsId ID of consent
	 * @return query
	 * @throws InternalServerException
	 */
	public static Map<String, Object> getQueries(ObjectId userId, ObjectId apsId) throws InternalServerException {
		Member member = Member.getById(userId, Sets.create("queries"));
		if (member.queries!=null) return member.queries.get(apsId.toString());
		return null;
	}
	
	/**
	 * set query for automatic record adding for a consent 
	 * @param userId ID of user
	 * @param apsId ID of consent
	 * @param query query to be set
	 * @throws AppException
	 */
	public static void setQuery(ObjectId userId, ObjectId apsId, Map<String, Object> query) throws AppException {
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
		} else {
			RecordManager.instance.removeMeta(userId, apsId, "_exclude");
		}
	}
			
	/**
	 * remove query for automatic record adding for a consent from user account
	 * @param userId ID of user
	 * @param targetaps ID of content
	 * @throws InternalServerException
	 */
	protected static void removeQueries(ObjectId userId, ObjectId targetaps) throws InternalServerException {
        Member member = Member.getById(userId, Sets.create("queries"));
		
		if (member.queries == null) return;
		 
		String key = targetaps.toString();
	    if (member.queries.containsKey(key)) {
	    	member.queries.remove(key);
	    	Member.set(userId, "queries", member.queries);
	    }
	}
		
}
