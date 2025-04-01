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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import actions.APICall;
import models.Admin;
import models.Consent;
import models.MidataId;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.Permission;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.DBRecord;
import utils.access.EncryptedAPS;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation.JsonValidationException;
import utils.stats.RequestMonitoring;

/**
 * used for debugging. Reading of APS content is not allowed on productive system.
 * @author alexander
 *
 */
public class Debug extends Controller {

	/**
	 * return APS content for debugging on a non productive system
	 * @param id ID of APS
	 * @return
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result get(Request request, String id) throws JsonValidationException, AppException {
				
		if (InstanceConfig.getInstance().getInstanceType().getDebugFunctionsAvailable()) {
		
			MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
			MidataId apsId = id.equals("-") ? userId : new MidataId(id);
			
			EncryptedAPS enc = new EncryptedAPS(apsId, userId);
									   			
			return ok(Json.toJson(enc.getPermissions()));
		
		} else return ok();
		
	}
				
	
	/**
	 * Do a database access for testing
	 * @return
	 * @throws AppException
	 */
	@APICall	
	public Result ping() throws AppException {
	  if (Admin.getByEmail(RuntimeConstants.BACKEND_SERVICE, Sets.create("_id")) == null) throw new InternalServerException("error.db", "Database error");
	  return ok();
	}
	
	@APICall	
	public Result test() throws AppException {
	  RequestMonitoring.flush();
		/*try {
	      Thread.sleep(10000);
		} catch (InterruptedException e) {}*/
	  return ok("ok");
	}
	
	// wget --header="Authorization: Bearer MZCFc1SVgd59WBYwpjy6sF2nlpnRWdvU3WsATbxMSMvCx4p8UYou7ggQd2wr2Hy04Nl0Pu9suurXy5hRmHtsihBU2I23CFcSs8aJam0Y0kLQTZLHyWt8_zxMjUzhTrjS4iJ48Q2GxuRaDL_u2M1RIk6jKYmDZNrugGHoQCGzsujnyhB_ltFJjXTdE3EK8Ts7GkoxzO8BzI03e025w5rZ2Q" http://localhost:9001/debug/patch/false
	@APICall	
	public Result patch(Request request, String exec) throws AppException {
		String param = request.header("Authorization").get();
		AccessContext brokerContext = ExecutionInfo.checkToken(request, param.substring("Bearer ".length()), false, false);
				  
		  boolean execute = exec != null && exec.equals("true");
		  
		  // get target project
	      MidataId project = MidataId.from("65d4cfc46d842c65d959559d");
	      
	      // source organizations
		  Set<MidataId> ugs = new HashSet<MidataId>();
		  // prod
		  ugs.add(MidataId.from("6756a6a41712c556381a48d1"));
		  ugs.add(MidataId.from("6756a6a61712c556381a48db"));
		  
		  //ugs.add(MidataId.from("67eab5c147e7e069cc492248"));
		  //ugs.add(MidataId.from("67eab59e47e7e069cc492244"));
		  
		  // for each sub org
		  for (MidataId ug : ugs) {
		      AccessLog.log("for organization: "+ug.toString());
			  AccessContext suborgContext = brokerContext.forUserGroup(ug, Permission.READ_DATA);
			  
			  // get patients
			  Set<Consent> patientConsents = Consent.getAllByAuthorized(ug, CMaps.map("type", ConsentType.HEALTHCARE).map("status", Sets.create(ConsentStatus.ACTIVE, ConsentStatus.PRECONFIRMED)), Consent.ALL);
			  AccessLog.log("found patients: "+patientConsents.size());
			  
			  // for each patient
			  for (Consent from : patientConsents) {
				 
			  // get consent for project
				 Set<Consent> targets = Consent.getAllActiveByAuthorizedAndOwners(project, Collections.singleton(from.owner));
				 AccessLog.log("patient: "+from.owner.toString()+" targets="+targets.size());
				 if (targets.size()>1) throw new NullPointerException();
				 // get records
			     if (targets.size() == 1) {
			    	 Consent target = targets.iterator().next();
			    	 AccessContext consentContext = suborgContext.forConsent(from); 
			    	 List<models.Record> recs = RecordManager.instance.list(UserRole.PROVIDER, consentContext, target.sharingQuery, RecordManager.SHARING_FIELDS);
			    	 Set<MidataId> recIds = new HashSet<MidataId>();
			    	 for (models.Record rec : recs) {
			    		 if (!rec.owner.equals(from.owner)) throw new NullPointerException();
			    		 recIds.add(rec._id);
			    	 }
			    	 AccessLog.log("#records="+recIds.size());
			    	 
			    	// share records with project 
			    	 if (execute) {
			    	   RecordManager.instance.share(suborgContext, from._id, target._id, target.owner, recIds, false);
			    	 }
			     }
			  
			  }
			
		  }
		  AccessLog.log("all done");
		  
		  return ok("ok");
	
	}
	
	
}
