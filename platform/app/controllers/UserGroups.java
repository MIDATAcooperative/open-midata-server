package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.History;
import models.MidataId;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.UserGroupType;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.KeyManager;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.fhir.GroupResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;

/**
 * Management functions of user groups
 *
 */
public class UserGroups extends APIController {	

	/**
	 * Search for user groups
	 * @return
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result search() throws AppException {
		JsonNode json = request().body().asJson();				
		MidataId executorId = new MidataId(request().username());
		
        JsonValidation.validate(json, "properties", "fields");
		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		ObjectIdConversion.convertMidataIds(properties, "_id", "developer", "creator");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
				
		if (properties.containsKey("member")) {
			properties.remove("member");
			Rights.chk("UserGroups.searchOwn", getRole(), properties, fields);
		    
			Set<UserGroupMember> ugms = UserGroupMember.getAllByMember(executorId);
			Set<MidataId> ids = new HashSet<MidataId>();
			for (UserGroupMember ugm : ugms) ids.add(ugm.userGroup);
			properties.put("_id", ids);
		} else {
			Rights.chk("UserGroups.search", getRole(), properties, fields);
			//if (!properties.containsKey("_id")) properties.put("searchable", true);
		}
		
		Set<UserGroup> groups = UserGroup.getAllUserGroup(properties, fields);
		
		return ok(JsonOutput.toJson(groups, "UserGroup", fields)); 		
	}
	
	/**
	 * List members of a user group
	 * @return
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result listUserGroupMembers() throws AppException {
		JsonNode json = request().body().asJson();				
		MidataId executorId = new MidataId(request().username());
		
        JsonValidation.validate(json, "usergroup");
        MidataId groupId = JsonValidation.getMidataId(json, "usergroup");
				
		Set<String> fields = UserGroupMember.ALL;
										
		Set<UserGroupMember> members = listUserGroupMembers(groupId);
		Map<MidataId, UserGroupMember> idmap = new HashMap<MidataId, UserGroupMember>();
		for (UserGroupMember member : members) idmap.put(member.member, member);
		Set<User> users = User.getAllUser(CMaps.map("_id", idmap.keySet()), Sets.create("firstname", "lastname", "email"));
		for (User user : users) idmap.get(user._id).user = user;
		Map<String, Set<String>> fieldMap = new HashMap<String, Set<String>>();
		fieldMap.put("UserGroupMember", fields);
		fieldMap.put("User", Sets.create("firstname", "lastname", "email"));
		return ok(JsonOutput.toJson(members, fieldMap)); 		
	}
	
	public static Set<UserGroupMember> listUserGroupMembers(MidataId groupId) throws AppException {
		Set<UserGroupMember> members = UserGroupMember.getAllByGroup(groupId);
		Map<MidataId, UserGroupMember> idmap = new HashMap<MidataId, UserGroupMember>();
		for (UserGroupMember member : members) idmap.put(member.member, member);
		Set<User> users = User.getAllUser(CMaps.map("_id", idmap.keySet()), Sets.create("firstname", "lastname", "email"));
		for (User user : users) idmap.get(user._id).user = user;
		return members;
	}
	
	/**
	 * Creates a new user group
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result createUserGroup() throws AppException {
        JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "name");
		MidataId executorId = new MidataId(request().username());
		
		UserGroup userGroup = new UserGroup();
		
		userGroup.name = JsonValidation.getString(json, "name");
		
		userGroup.type = UserGroupType.CARETEAM;
		userGroup.status = UserStatus.ACTIVE;
		userGroup.creator = executorId;
		
		userGroup._id = new MidataId();
		userGroup.nameLC = userGroup.name.toLowerCase();
		userGroup.history = new ArrayList<History>();
		userGroup.keywordsLC = new HashSet<String>();
		userGroup.registeredAt = new Date();
		
		userGroup.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(userGroup._id, null);
				
		GroupResourceProvider.updateMidataUserGroup(userGroup);
		userGroup.add();
		
		UserGroupMember member = new UserGroupMember();
		member._id = new MidataId();
		member.member = executorId;
		member.userGroup = userGroup._id;
		member.status = ConsentStatus.ACTIVE;
																				
		Map<String, Object> accessData = new HashMap<String, Object>();
		accessData.put("aliaskey", KeyManager.instance.generateAlias(userGroup._id, member._id));
		RecordManager.instance.createPrivateAPS(executorId, member._id);
		RecordManager.instance.setMeta(executorId, member._id, "_usergroup", accessData);
						
		member.add();
				
		RecordManager.instance.createPrivateAPS(userGroup._id, userGroup._id);
		
		return ok(JsonOutput.toJson(userGroup, "UserGroup", UserGroup.ALL));
	}
	
	public static Result editUserGroup() {
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result addMembersToUserGroup() throws AppException {
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "members", "group");
		MidataId executorId = new MidataId(request().username());
	
		MidataId groupId = JsonValidation.getMidataId(json, "group");
		Set<MidataId> targetUserIds = JsonExtraction.extractMidataIdSet(json.get("members"));
		
		UserGroupMember self = UserGroupMember.getByGroupAndMember(groupId, executorId);
		if (self == null) throw new AuthException("error.internal", "User not member of group");
		
		BSONObject meta = RecordManager.instance.getMeta(executorId, self._id, "_usergroup");
		byte[] key = (byte[]) meta.get("aliaskey");
		KeyManager.instance.unlock(groupId, self._id, key);
		
		for (MidataId targetUserId : targetUserIds) {
			UserGroupMember old = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
			if (old == null) {			
				UserGroupMember member = new UserGroupMember();
				member._id = new MidataId();
				member.member = targetUserId;
				member.userGroup = groupId;
				member.status = ConsentStatus.ACTIVE;
																									
				Map<String, Object> accessData = new HashMap<String, Object>();
				accessData.put("aliaskey", KeyManager.instance.generateAlias(groupId, member._id));
				RecordManager.instance.createAnonymizedAPS(targetUserId, executorId, member._id, false);
				RecordManager.instance.setMeta(executorId, member._id, "_usergroup", accessData);
				
				member.add();
			}
		}
				 
		return ok();
	}
	
	public static Result changeUserGroupMembership() {
		return ok();
	}
		
}
