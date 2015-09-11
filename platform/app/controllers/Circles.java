package controllers;

import java.security.NoSuchAlgorithmException;
import java.security.acl.Owner;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Consent;
import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.MemberKey;
import models.ModelException;
import models.enums.ConsentStatus;
import models.enums.ConsentType;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.PasswordHash;
import utils.access.AccessLog;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;


public class Circles extends Controller {	

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result get() throws JsonValidationException, ModelException {
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
		return ok(Json.toJson(circles));
	}
	


	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result add() throws JsonValidationException, ModelException {
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
				throw new ModelException(e);
			} catch (InvalidKeySpecException e) {
				throw new ModelException(e);
			}
		}
			
		consent._id = new ObjectId();
		consent.owner = userId;
		consent.name = name;		
		consent.authorized = new HashSet<ObjectId>();
		consent.status = userId.equals(executorId) ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
		if (! userId.equals(executorId)) consent.authorized.add(executorId);
							
		RecordSharing.instance.createAnonymizedAPS(userId, executorId, consent._id);
		
		if (passcode != null) {			  
			  byte[] pubkey = KeyManager.instance.generateKeypairAndReturnPublicKey(consent._id, passcode);
		      RecordSharing.instance.shareAPS(consent._id, userId, consent._id, pubkey);
		      RecordSharing.instance.setMeta(userId, consent._id, "_config", CMaps.map("passcode", passcode));			  		
		}
		
		consent.add();
				
		return ok(Json.toJson(consent));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result joinByPasscode() throws JsonValidationException, ModelException {
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
		   RecordSharing.instance.shareAPS(consent._id, consent._id, Collections.singleton(executorId));		   
		   consent.authorized.add(executorId);
		   Consent.set(consent._id, "authorized", consent.authorized);
		
		   return ok(Json.toJson(consent));
		} catch (NoSuchAlgorithmException e) {
	    	throw new ModelException(e);
	    } catch (InvalidKeySpecException e) {
	    	throw new ModelException(e);
	    }
	}

	@APICall
	@Security.Authenticated(Secured.class)
	public static Result delete(String circleIdString) throws JsonValidationException, ModelException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("authorized", "type"));
		if (consent == null) {
			return badRequest("No consent with this id exists.");
		}
		if (consent.type != ConsentType.CIRCLE) return badRequest("Operation not supported");
		
		// Remove APS
		RecordSharing.instance.deleteAPS(consent._id, consent.owner);
		
		// Remove Rules
		removeQueries(userId, circleId);
		
		// delete circle		
		switch (consent.type) {
		case CIRCLE: Circle.delete(userId, circleId);break;		
		}
		
		
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result addUsers(String circleIdString) throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "users");
		
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("authorized","type"));
		if (consent == null) {
			return badRequest("No consent with this id exists.");
		}
		
		// add users to circle (implicit: if not already present)
		Set<ObjectId> newMemberIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("users")));
				
		consent.authorized.addAll(newMemberIds);
		Consent.set(consent._id, "authorized", consent.authorized);
		
		RecordSharing.instance.shareAPS(consent._id, userId, newMemberIds);
					
		return ok();
	}

	@Security.Authenticated(Secured.class)
	@APICall
	public static Result removeMember(String circleIdString, String memberIdString) throws JsonValidationException, ModelException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Consent consent = Consent.getByIdAndOwner(circleId, userId, Sets.create("authorized","type"));
		if (consent == null) {
			return badRequest("No circle with this id exists.");
		}
		
		// remove member from circle (implicit: if present)
		ObjectId memberId = new ObjectId(memberIdString);
		
		consent.authorized.remove(memberId);
		Consent.set(consent._id, "authorized", consent.authorized);

		Set<ObjectId> memberIds = new HashSet<ObjectId>();
		memberIds.add(memberId);
		
		RecordSharing.instance.unshareAPS(consent._id, userId, memberIds);
		
		return ok();
	}
	
	public static Map<String, Object> getQueries(ObjectId userId, ObjectId apsId) throws ModelException {
		Member member = Member.getById(userId, Sets.create("queries"));
		if (member.queries!=null) return member.queries.get(apsId.toString());
		return null;
	}
	
	public static void setQuery(ObjectId userId, ObjectId apsId, Map<String, Object> query) throws ModelException {
		Member member = Member.getById(userId, Sets.create("queries"));
		if (member.queries==null) {
			member.queries = new HashMap<String, Map<String, Object>>();
		}
		member.queries.put(apsId.toString(), query);
		Member.set(userId, "queries", member.queries);
		if (query.containsKey("exclude-ids")) {
			Map<String, Object> ids = new HashMap<String,Object>();
			ids.put("ids", query.get("exclude-ids"));
			RecordSharing.instance.setMeta(userId, apsId, "_exclude", ids);
		}
	}
	
	public static Map<String, Object> mergeQueries(Map<String, Object> query1, Map<String, Object> query2) {
		return query1;
	}
	
	public static void removeQueries(ObjectId userId, ObjectId targetaps) throws ModelException {
        Member member = Member.getById(userId, Sets.create("queries"));
		
		if (member.queries == null) return;
		 
		String key = targetaps.toString();
	    if (member.queries.containsKey(key)) {
	    	member.queries.remove(key);
	    	Member.set(userId, "queries", member.queries);
	    }
	}
	
	/*
	public static ObjectId getOrCreateMemberKey(HPUser hpuser, Member member) throws ModelException {
		MemberKey key = MemberKey.getByOwnerAndAuthorizedPerson(member._id, hpuser._id);
		if (key!=null) return key.aps;
		
		HealthcareProvider prov = HealthcareProvider.getById(hpuser.provider);
		
		key = new MemberKey();
		key._id = new ObjectId();
		key.owner = member._id;
		key.organization = hpuser.provider;
		key.authorized = new HashSet<ObjectId>();
		key.authorized.add(hpuser._id);
		key.status = ConsentStatus.UNCONFIRMED;
		key.name = prov.name+": "+hpuser.firstname+" "+hpuser.sirname;
		key.aps = RecordSharing.instance.createAnonymizedAPS(member._id, hpuser._id, key._id);
		key.add();
		
		return key.aps;
		
	}
    */
}
