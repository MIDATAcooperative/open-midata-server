package controllers;

import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.MidataId;
import models.MobileAppInstance;
import models.ServiceInstance;
import models.Study;
import models.UserGroupMember;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.auth.AnyRoleSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.json.JsonOutput;
import utils.json.JsonValidation;

/**
 * Services
 */
public class Services extends APIController {
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result listServiceInstancesStudy(String studyIdStr) throws AppException {

        MidataId managerId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        MidataId studyId = MidataId.from(studyIdStr);
        Study study = Study.getById(studyId, Sets.create("name", "type", "executionStatus", "participantSearchStatus", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");
				
		UserGroupMember self = UserGroupMember.getByGroupAndActiveMember(studyId, managerId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		//if (!self.role.maySetup())
		//	throw new BadRequestException("error.notauthorized.action", "User is not allowed to manage participants.");

        Set<ServiceInstance> instances = ServiceInstance.getByManager(studyId, ServiceInstance.ALL);
        return ok(JsonOutput.toJson(instances, "ServiceInstance", ServiceInstance.ALL)).as("application/json");
    }
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result listServiceInstances() throws AppException {

        MidataId managerId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));     
        Set<ServiceInstance> instances = ServiceInstance.getByManager(managerId, ServiceInstance.ALL);
        return ok(JsonOutput.toJson(instances, "ServiceInstance", ServiceInstance.ALL)).as("application/json");
    }
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result removeServiceInstance(String instanceIdStr) throws AppException {

        MidataId instanceId = MidataId.from(instanceIdStr);
        
        ServiceInstance instance = ServiceInstance.getById(instanceId, ServiceInstance.ALL);
        if (instance == null) throw new BadRequestException("error.unknown.service", "Service Instance does not exist");

        ServiceInstance.delete(instanceId);
        return ok();
    }
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result listApiKeys(String serviceIdStr) throws AppException {
        MidataId serviceId = MidataId.from(serviceIdStr);
        Set<MobileAppInstance> instances = MobileAppInstance.getByService(serviceId, MobileAppInstance.ALL);
        return ok(JsonOutput.toJson(instances, "MobileAppInstance", MobileAppInstance.ALL)).as("application/json");
    }
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result addApiKey(String serviceIdStr) throws AppException {
        AccessLog.log("add api key!");
        ObjectNode obj = Json.newObject();	 

        obj.put("access_token", "ABCD" /*session.encrypt()*/);
		///obj.put("token_type", "Bearer");
		//obj.put("scope", "user/*.*");		
		obj.put("expires_in", MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME / 1000l);
		//obj.put("patient", appInstance.owner.toString());
		obj.put("refresh_token", "EFGH" /*refresh.encrypt()*/);
				
		response().setHeader("Cache-Control", "no-store");
		response().setHeader("Pragma", "no-cache"); 
		
		return ok(obj).as("application/json");
    }
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result removeApiKey(String serviceIdStr, String apikeyIdStr) throws AppException {
        return ok();
    }
}