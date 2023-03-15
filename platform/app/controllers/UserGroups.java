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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Consent;
import models.MidataId;
import models.Study;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ResearcherRole;
import models.enums.UserGroupType;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.ProjectTools;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.KeyManager;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
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
	public Result search(Request request) throws AppException {
		JsonNode json = request.body().asJson();				
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
        JsonValidation.validate(json, "properties", "fields");
		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		ObjectIdConversion.convertMidataIds(properties, "_id", "developer", "creator");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
				
		if (properties.containsKey("member")) {
			properties.remove("member");
			Rights.chk("UserGroups.searchOwn", getRole(), properties, fields);
		    
			Set<UserGroupMember> ugms = null; UserGroupMember.getAllByMember(executorId);
			
			if (properties.containsKey("active")) {
				properties.remove("active");
				ugms = UserGroupMember.getAllActiveByMember(executorId);
			} else {
			   ugms = UserGroupMember.getAllByMember(executorId);
			}
			Set<MidataId> ids = new HashSet<MidataId>();
			for (UserGroupMember ugm : ugms) ids.add(ugm.userGroup);
			properties.put("_id", ids);
		} else {
			Rights.chk("UserGroups.search", getRole(), properties, fields);
			if (!properties.containsKey("_id") && !getRole().equals(UserRole.ADMIN)) properties.put("searchable", true);
		}
		
		Set<UserGroup> groups = UserGroup.getAllUserGroup(properties, fields);
		
		return ok(JsonOutput.toJson(groups, "UserGroup", fields)).as("application/json"); 		
	}
	
	/**
	 * List members of a user group
	 * @return
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result listUserGroupMembers(Request request) throws AppException {
		JsonNode json = request.body().asJson();				
		//MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
        JsonValidation.validate(json, "usergroup");
        MidataId groupId = JsonValidation.getMidataId(json, "usergroup");
				
		Set<String> fields = UserGroupMember.ALL;
										
		Set<UserGroupMember> members = listUserGroupMembers(groupId);
		Map<MidataId, UserGroupMember> idmap = new HashMap<MidataId, UserGroupMember>();
		for (UserGroupMember member : members) idmap.put(member.member, member);
		Set<User> users = User.getAllUser(CMaps.map("_id", idmap.keySet()), Sets.create("firstname", "lastname", "email", "role"));
		for (User user : users) idmap.get(user._id).user = user;
		Map<String, Set<String>> fieldMap = new HashMap<String, Set<String>>();
		fieldMap.put("UserGroupMember", fields);
		fieldMap.put("User", Sets.create("firstname", "lastname", "email", "role"));
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
	public Result createUserGroup(Request request) throws AppException {
        JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "name");
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		
		UserGroup userGroup = new UserGroup();
		
		userGroup.name = JsonValidation.getString(json, "name");
		
		userGroup.type = UserGroupType.CARETEAM;
		userGroup.status = UserStatus.ACTIVE;
		userGroup.creator = executorId;
		
		userGroup._id = new MidataId();
		userGroup.nameLC = userGroup.name.toLowerCase();	
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
		member.startDate = new Date();
																				
		Map<String, Object> accessData = new HashMap<String, Object>();
		accessData.put("aliaskey", KeyManager.instance.generateAlias(userGroup._id, member._id));
		RecordManager.instance.createPrivateAPS(context.getCache(), executorId, member._id);
		RecordManager.instance.setMeta(context, member._id, "_usergroup", accessData);
						
		member.add();
				
		RecordManager.instance.createPrivateAPS(context.getCache(), userGroup._id, userGroup._id);
		
		return ok(JsonOutput.toJson(userGroup, "UserGroup", UserGroup.ALL)).as("application/json");
	}
	
	/**
	 * delete a new user group
	 * @return 200 ok
	 * @throws AppException
	 */	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result deleteUserGroup(Request request, String groupIdStr) throws AppException {       
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId groupId = MidataId.from(groupIdStr);
		AccessContext context = portalContext(request);
		
		UserGroupMember execMember = UserGroupMember.getByGroupAndActiveMember(groupId, executorId);
		if (execMember == null) throw new BadRequestException("error.invalid.usergroup", "Only members may delete a group");
		
		UserGroup userGroup = UserGroup.getById(groupId, Sets.create("_id", "status"));
		
		Set<Consent> consents = Consent.getAllByAuthorized(groupId);
		
		if (consents.isEmpty()) {		
			Set<UserGroupMember> allMembers = UserGroupMember.getAllByGroup(groupId);		
			for (UserGroupMember member : allMembers) {
				RecordManager.instance.deleteAPS(context, member._id);
				member.delete();
			}
			
			RecordManager.instance.deleteAPS(context, groupId);
			UserGroup.delete(groupId);
		} else {
			Set<UserGroupMember> allMembers = UserGroupMember.getAllByGroup(groupId);		
			for (UserGroupMember member : allMembers) {
				if (member.status == ConsentStatus.ACTIVE) {
					member.status = ConsentStatus.EXPIRED;
					member.endDate = new Date();
					UserGroupMember.set(member._id, "status" , member.status);
					UserGroupMember.set(member._id, "endDate" , member.endDate);
				}
			}
			
			userGroup.status = UserStatus.DELETED;
			userGroup.searchable = false;
			UserGroup.set(userGroup._id, "searchable", userGroup.searchable);
			UserGroup.set(userGroup._id, "status", userGroup.status);
			
			GroupResourceProvider.updateMidataUserGroup(userGroup);
		}
					
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result editUserGroup(Request request, String groupIdStr) throws AppException {
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId groupId = MidataId.from(groupIdStr);
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "name");
		
		UserGroupMember execMember = UserGroupMember.getByGroupAndActiveMember(groupId, executorId);
		if (execMember == null) throw new BadRequestException("error.invalid.usergroup", "Only members may edit a group");
		
		UserGroup userGroup = UserGroup.getById(groupId, UserGroup.ALL);

		boolean searchable = JsonValidation.getBoolean(json, "searchable");
		userGroup.set("searchable", searchable);
		
		return ok(JsonOutput.toJson(userGroup, "UserGroup", UserGroup.ALL)).as("application/json");
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result addMembersToUserGroup(Request request) throws AppException {
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "members", "group");
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
	
		MidataId groupId = JsonValidation.getMidataId(json, "group");
		Set<MidataId> targetUserIds = JsonExtraction.extractMidataIdSet(json.get("members"));
		
		UserGroupMember self = UserGroupMember.getByGroupAndActiveMember(groupId, executorId);
		if (self == null) throw new AuthException("error.notauthorized.action", "User not member of group");
		if (!self.getRole().mayChangeTeam()) throw new BadRequestException("error.notauthorized.action", "User is not allowed to change team.");
		
		Study study = Study.getById(groupId, Sets.create("anonymous"));
		boolean anonymous = study != null && study.anonymous;
		
		ResearcherRole role = null;
		if (json.has("roleName")) {
			role = new ResearcherRole();
			role.readData = JsonValidation.getBoolean(json, "readData");
			role.writeData = JsonValidation.getBoolean(json, "writeData");
			role.auditLog = JsonValidation.getBoolean(json, "auditLog");
			role.changeTeam = JsonValidation.getBoolean(json, "changeTeam");
			role.export = JsonValidation.getBoolean(json, "export");
			role.pseudo = !JsonValidation.getBoolean(json, "unpseudo");
			role.participants = JsonValidation.getBoolean(json, "participants");
			role.setup = JsonValidation.getBoolean(json, "setup");
			role.applications = JsonValidation.getBoolean(json, "applications");
			role.roleName = JsonValidation.getString(json, "roleName");			
			role.id = JsonValidation.getString(json, "id");	
		}
		
		if (anonymous) {
			if (role == null || !role.pseudo) throw new BadRequestException("error.invalid.anonymous", "Unpseudonymized access not allowed");
		}
		
		ProjectTools.addToUserGroup(context, self, role, targetUserIds);
		
		AuditManager.instance.success();
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result deleteUserGroupMembership(Request request) throws AppException  {
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "member", "group");
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
	
		MidataId groupId = JsonValidation.getMidataId(json, "group");
		MidataId targetUserId = JsonValidation.getMidataId(json, "member");
		
		AuditManager.instance.addAuditEvent(AuditEventType.REMOVED_FROM_TEAM, null, executorId, targetUserId, null, groupId);
		
		if (targetUserId.equals(executorId)) throw new BadRequestException("error.invalid.user", "You cannot remove yourself from group.");
		UserGroupMember self = UserGroupMember.getByGroupAndActiveMember(groupId, executorId);
		if (self == null) throw new AuthException("error.internal", "User not member of group");
						
		UserGroupMember target = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
		if (target == null) throw new BadRequestException("error.invalid.user", "User is not member of group");
		target.status = ConsentStatus.EXPIRED;
		target.endDate = new Date();
		UserGroupMember.set(target._id, "status", target.status);
		UserGroupMember.set(target._id, "endDate", target.endDate);

		AuditManager.instance.success();
		return ok();
	}
		
}
