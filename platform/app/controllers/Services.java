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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.Circle;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.ServiceInstance;
import models.Study;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.OAuthRefreshToken;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonOutput;
import utils.json.JsonValidation;

/**
 * Services
 */
public class Services extends APIController {
    
    public static final long SERVICE_EXPIRATION_TIME = 1000l * 60l * 60l * 24l * 365l * 5l;

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
        Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByMember(managerId);
        Set<MidataId> managers = new HashSet<MidataId>();
        managers.add(managerId);
        for (UserGroupMember ugm : ugms) managers.add(ugm.userGroup);
        Set<ServiceInstance> instances = ServiceInstance.getByManager(managers, ServiceInstance.ALL);
        return ok(JsonOutput.toJson(instances, "ServiceInstance", ServiceInstance.ALL)).as("application/json");
    }
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result removeServiceInstance(String instanceIdStr) throws AppException {

        MidataId managerId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        MidataId instanceId = MidataId.from(instanceIdStr);
        
        ServiceInstance instance = ApplicationTools.checkServiceInstanceOwner(managerId, instanceId);        

        ApplicationTools.deleteServiceInstance(managerId, instance);
        return ok();
    }

    
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result listApiKeys(String serviceIdStr) throws AppException {
        MidataId managerId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        MidataId instanceId = MidataId.from(serviceIdStr);
        
        ServiceInstance instance = ApplicationTools.checkServiceInstanceOwner(managerId, instanceId);        
        Plugin app = Plugin.getById(instance.appId);
        
        Set<MobileAppInstance> instances = MobileAppInstance.getByService(instance._id, MobileAppInstance.APPINSTANCE_ALL);
        for (MobileAppInstance inst : instances) {
            if (inst.appVersion != app.pluginVersion) inst.status = ConsentStatus.FROZEN;
        }
        return ok(JsonOutput.toJson(new ArrayList(instances), "Consent", MobileAppInstance.APPINSTANCE_ALL)).as("application/json");
    }
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result addApiKey(String serviceIdStr) throws AppException {
        AccessLog.log("add api key!");

        MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        MidataId instanceId = MidataId.from(serviceIdStr);
        
        ServiceInstance serviceInstance = ApplicationTools.checkServiceInstanceOwner(executorId, instanceId);  
        
        MobileAppInstance appInstance = ApplicationTools.createServiceApiKey(executorId, serviceInstance);
        
        String aeskey = KeyManager.instance.newAESKey(appInstance._id);	

        MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, aeskey, System.currentTimeMillis() + SERVICE_EXPIRATION_TIME, UserRole.ANY); 
        OAuthRefreshToken refresh = OAuth2.createRefreshToken(executorId, appInstance, aeskey);
        
        ObjectNode obj = Json.newObject();	 

        obj.put("access_token", session.encrypt());		
		obj.put("expires_in", SERVICE_EXPIRATION_TIME / 1000l);		
		obj.put("refresh_token", refresh.encrypt());
		obj.put("cn", appInstance._id+"."+aeskey);
				
		response().setHeader("Cache-Control", "no-store");
		response().setHeader("Pragma", "no-cache"); 
		
		return ok(obj).as("application/json");
    }
    
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
    public Result removeApiKey(String serviceIdStr, String apikeyIdStr) throws AppException {
        MidataId managerId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        MidataId instanceId = MidataId.from(serviceIdStr);
        MidataId apikeyId = MidataId.from(apikeyIdStr);
        
        ServiceInstance serviceInstance = ApplicationTools.checkServiceInstanceOwner(managerId, instanceId);          
        MobileAppInstance appInstance = MobileAppInstance.getById(apikeyId, MobileAppInstance.APPINSTANCE_ALL);

        if (appInstance == null || appInstance.serviceId == null || !appInstance.serviceId.equals(serviceInstance._id)) throw new InternalServerException("error.internal", "User not authorized to do action.");

        ApplicationTools.removeAppInstance(managerId, appInstance);
        return ok();
    }
}