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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Consent;
import models.HealthcareProvider;
import models.MessageDefinition;
import models.MidataId;
import models.Plugin;
import models.ServiceInstance;
import models.Study;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.MessageChannel;
import models.enums.MessageReason;
import models.enums.Permission;
import models.enums.ResearcherRole;
import models.enums.StudyType;
import models.enums.SubUserRole;
import models.enums.UserGroupType;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.InstanceConfig;
import utils.ProjectTools;
import utils.RuntimeConstants;
import utils.UserGroupTools;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.ActionToken;
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
import utils.exceptions.InternalServerException;
import utils.fhir.GroupResourceProvider;
import utils.fhir.OrganizationResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.messaging.Messager;

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
		AccessContext context = portalContext(request);
        JsonValidation.validate(json, "properties", "fields");
		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		ObjectIdConversion.convertMidataIds(properties, "_id", "developer", "creator");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
				
		if (properties.containsKey("member")) {
			properties.remove("member");
			Rights.chk("UserGroups.searchOwn", getRole(), properties, fields);
		    
			Set<UserGroupMember> ugms = null; 
			
			if (properties.containsKey("active")) {
				properties.remove("active");
				ugms = context.getCache().getAllActiveByMember(Permission.ANY);
			} else {
			   ugms = UserGroupMember.getAllByMember(executorId);
			   ugms.addAll(context.getCache().getAllActiveByMember(Permission.ANY));			   
			}
			Set<MidataId> ids = new HashSet<MidataId>();
			if (properties.containsKey("setup")) {
				properties.remove("setup");
				for (UserGroupMember ugm : ugms) if (ugm.getRole().maySetup()) ids.add(ugm.userGroup);
			} else {
			    for (UserGroupMember ugm : ugms) ids.add(ugm.userGroup);
			}
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
		
		Map<String, Set<String>> fieldMap = new HashMap<String, Set<String>>();
		fieldMap.put("UserGroupMember", fields);
		fieldMap.put("User", Sets.create("firstname", "lastname", "email", "role"));
		return ok(JsonOutput.toJson(members, fieldMap)).as("application/json"); 		
	}
	
	/**
	 * List members of a user group
	 * @return
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result listUserGroupGroups(Request request) throws AppException {
		JsonNode json = request.body().asJson();				
		//MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
        JsonValidation.validate(json, "usergroup");
        MidataId groupId = JsonValidation.getMidataId(json, "usergroup");
				
		Set<String> fields = UserGroupMember.ALL;
										
		Set<UserGroupMember> members = UserGroupMember.getAllByGroup(groupId, Sets.createEnum(EntityType.USERGROUP, EntityType.ORGANIZATION, EntityType.SERVICES));
		Map<MidataId, UserGroupMember> idmap = new HashMap<MidataId, UserGroupMember>();
		for (UserGroupMember member : members) {
			idmap.put(member.member, member);
			if (member.entityType == EntityType.SERVICES) {
				ServiceInstance si = ServiceInstance.getById(member.member, ServiceInstance.LIMITED);
				if (si != null) member.entityName = si.name;
			}
		}
		Set<UserGroup> groups = UserGroup.getAllUserGroup(CMaps.map("_id", idmap.keySet()), UserGroup.ALL);
		for (UserGroup group : groups) idmap.get(group._id).entityName = group.name;
									
		return ok(JsonOutput.toJson(members, "UserGroupMember", UserGroupMember.ALL)).as("application/json"); 		
	}
	
	public static Set<UserGroupMember> listUserGroupMembers(MidataId groupId) throws AppException {
		Set<UserGroupMember> members = UserGroupMember.getAllUserByGroup(groupId);
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
		AccessContext context = portalContext(request);
		
		UserGroup userGroup = UserGroupTools.createUserGroup(context, UserGroupType.CARETEAM, new MidataId(), JsonValidation.getString(json, "name"));
		UserGroupMember member = UserGroupTools.createUserGroupMember(context, ResearcherRole.HC(), userGroup._id);
				
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
		MidataId groupId = MidataId.parse(groupIdStr);
		AccessContext context = portalContext(request);
		
		UserGroupTools.deleteUserGroup(context, groupId, false);
					
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result editUserGroup(Request request, String groupIdStr) throws AppException {
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		MidataId groupId = MidataId.from(groupIdStr);
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "name");
		
		if (!UserGroupTools.accessorIsMemberOfGroup(context, groupId, Permission.SETUP)) {
			requireSubUserRoleForRole(request, SubUserRole.USERADMIN, UserRole.ADMIN);
			if (context.getAccessorRole() != UserRole.ADMIN) new BadRequestException("error.notauthorized.action", "Only members may edit a group");		
		}
		
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
		EntityType memberType = EntityType.USER;
		
		if (json.has("type")) memberType = JsonValidation.getEnum(json, "type", EntityType.class);
		Permission permission = memberType.getChangePermission();
		
		Set<MidataId> targetUserIds1 = JsonExtraction.extractMidataIdSet(json.get("members"));
		
		if (!UserGroupTools.accessorIsMemberOfGroup(context, groupId, permission)) {
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change team.");		
		}
		
		ResearcherRole role = null;
		
		Set<MidataId> targetUserIds = new HashSet<MidataId>();
		for (MidataId targetUserId : targetUserIds1) {
			switch(memberType) {
			case USER:
				if (User.getById(targetUserId, Sets.create("_id")) == null) throw new BadRequestException("error.unknown.user", "Target user does not exist.");
				targetUserIds.add(targetUserId);
				break;
			case ORGANIZATION:
				if (HealthcareProvider.getById(targetUserId, Sets.create("_id")) == null) throw new BadRequestException("error.unknown.organization", "Target organization does not exist.");
				targetUserIds.add(targetUserId);
				break;
			case USERGROUP:
				if (targetUserIds.size()==1 && HealthcareProvider.getById(targetUserId, Sets.create("_id")) != null) memberType=EntityType.ORGANIZATION;
				if (UserGroup.getById(targetUserId, Sets.create("_id")) == null) throw new BadRequestException("error.unknown.group", "Target team does not exist.");
				targetUserIds.add(targetUserId);
				break;
			case PROJECT:
				Study targetStudy = Study.getById(targetUserId, Sets.create("_id", "type"));
			    if (targetStudy == null) throw new BadRequestException("error.unknown.project", "Target project does not exist.");
			    Study sStudy = Study.getById(groupId, Sets.create("_id", "type"));
			    if (sStudy == null || sStudy.type == StudyType.META) throw new BadRequestException("error.invalid.project", "No meta projects allowed as target");
			    targetUserIds.add(targetUserId);
			    role = ResearcherRole.SUBPROJECT();
			    break;
			case SERVICES:
				Plugin pl = Plugin.getById(targetUserId);
				if (pl==null) {
					targetUserIds.add(targetUserId);
				} else if (!pl.type.equals("broker")) throw new BadRequestException("error.unknown.plugin", "Target data broker does not exist.");
				else {
					if (pl.decentral) {
						ServiceInstance si = ApplicationTools.createServiceInstance(context, pl, groupId);
						targetUserIds.add(si._id);
					} else {
						Set<ServiceInstance> si = ServiceInstance.getByApp(targetUserId, ServiceInstance.LIMITED);
						boolean found = false;
						for (ServiceInstance s : si) {
							if (s.status == UserStatus.ACTIVE) {
								targetUserIds.add(s._id);
								found = true;
							}
						}
						if (!found) throw new BadRequestException("error.unknown.plugin", "Target data broker does not exist.");
					}
				}
		
			}
		}
		
		Study study = Study.getById(groupId, Sets.create("anonymous"));
		boolean anonymous = study != null && study.anonymous;
		
		Map<String, String> projectGroupMapping = null;
		if (json.has("projectGroupMapping")) {
		    projectGroupMapping = JsonExtraction.extractStringMap(json.get("projectGroupMapping"));
		}
				
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
		
		List<UserGroupMember> self = context.getCache().getByGroupAndActiveMember(groupId, context.getAccessor(), permission);
		ProjectTools.addToUserGroup(context, self.get(self.size()-1), role, memberType, targetUserIds, projectGroupMapping);
		
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
		AccessContext context = portalContext(request);
		
		MidataId groupId = JsonValidation.getMidataId(json, "group");
		MidataId targetUserId = JsonValidation.getMidataId(json, "member");
		
		AuditManager.instance.addAuditEvent(AuditEventType.REMOVED_FROM_TEAM, context, null, executorId, targetUserId, null, groupId);
		
		if (targetUserId.equals(executorId)) throw new BadRequestException("error.invalid.user", "You cannot remove yourself from group.");
		
		UserGroupTools.removeMemberFromUserGroup(context, targetUserId, groupId);
		
		AuditManager.instance.success();
		return ok();
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getUserGroup(Request request, String id) throws AppException {
			
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId providerid = MidataId.parse(id);
						
		UserGroup provider = UserGroup.getById(providerid, UserGroup.ALL);
		if (provider == null) return notFound();	
				
		UserGroupMember ugm = UserGroupMember.getByGroupAndActiveMember(providerid, executorId);
		//if (provider == null || (ugm == null && !provider.searchable)) return notFound();
		
		provider.currentUserAccessUntil = ugm != null ? ugm.confirmedUntil : null;
						
		return ok(JsonOutput.toJson(provider, "UserGroup", Sets.create(UserGroup.ALL,"currentUserAccessUntil")));		
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result requestConfirmation(Request request) throws AppException  {
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "group");
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		
		MidataId groupId = JsonValidation.getMidataId(json, "group");
		String reason = JsonValidation.getStringOrNull(json, "reason");
		
		UserGroupMember ugm = UserGroupMember.getByGroupAndActiveMember(groupId, executorId);
		if (ugm == null) throw new BadRequestException("error.notauthorized.action", "User is not allowed to change team.");		
		
		UserGroup group = UserGroup.getById(groupId, UserGroup.ALL);
		if (!group.protection) return ok();
		
		User requestor = context.getRequestCache().getUserById(executorId);
		Set<UserGroupMember> allMembers = UserGroupMember.getAllActiveUserByGroup(groupId);
		
		boolean mailSent = false;
		
		AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.ACCESS_CONFIRMATION_REQUEST).withActor(requestor));
		
		for (UserGroupMember member : allMembers) {
			if (!member.member.equals(executorId)) {
				
				String token = new ActionToken(member.member, ugm._id, null, AuditEventType.ACCESS_CONFIRMATION, System.currentTimeMillis() + 1000l * 60l * 10l).encrypt(); 
				Map<String, String> replacements = new HashMap<String, String>();
				try {
				  replacements.put("token", InstanceConfig.getInstance().getServiceURL()+"?token="+URLEncoder.encode(token, "UTF-8"));
				} catch (UnsupportedEncodingException e) {}
				replacements.put("reason", reason);
				replacements.put("group-name", group.name);
				replacements.put("requestor-firstname", requestor.firstname);
				replacements.put("requestor-lastname", requestor.lastname);
				replacements.put("requestor-email", requestor.email);
				
				MessageDefinition def = new MessageDefinition();
				def.reason = MessageReason.ACCESS_CONFIRMATION_REQUEST;
				def.title = new HashMap<String, String>();
				def.title.put(InstanceConfig.getInstance().getDefaultLanguage(), "Group Access request");
				def.text = new HashMap<String, String>();
				def.text.put(InstanceConfig.getInstance().getDefaultLanguage(), "Dear <firstname> <lastname>,\nthe user <requestor-firstname> <requestor-lastname> with email <requestor-email>\nhas requested access for the protected group <group-name>.\n\nGiven reason: <reason>.\n\nClick here to grant access for one hour:\n<token>\n\nRegards, your Midata Instance.");
				
				Messager.sendMessage(def, Collections.emptyMap(), User.getById(member.member, User.ALL_USER), replacements, MessageChannel.EMAIL, null);
								
				mailSent = true;
			}
		}
		
		// If there is no one to confirm, allow self confirmation
		if (!mailSent) confirmation(executorId, ugm._id);
		
		AuditManager.instance.success();
		return ok();
	}
	
	public static void confirmation(MidataId executor, MidataId userGroupMemberId) throws AppException {
		UserGroupMember ugm = UserGroupMember.getById(userGroupMemberId);
		if (ugm == null) throw new InternalServerException("error.internal", "User Group does not exist");
		
		UserGroupMember checkExecutor = UserGroupMember.getByGroupAndActiveMember(ugm.userGroup, executor);
		if (checkExecutor == null) throw new InternalServerException("error.internal", "Executor not member of group");
		
		ugm.confirmedUntil = new Date(System.currentTimeMillis() + 1000l * 60l * 60l);
		ugm.confirmedBy = executor;
		
		AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.ACCESS_CONFIRMATION).withActor(null, executor).withModifiedActor(null, ugm.member));
		
		UserGroupMember.set(ugm._id, "confirmedBy", ugm.confirmedBy);
		UserGroupMember.set(ugm._id, "confirmedUntil", ugm.confirmedUntil);
		
	}

		
}
