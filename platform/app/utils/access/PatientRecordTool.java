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

package utils.access;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Consent;
import models.Member;
import models.MidataId;
import models.User;
import models.enums.APSSecurityLevel;
import models.enums.AccountSecurityLevel;
import models.enums.UserRole;
import models.enums.UserStatus;
import models.Record;
import utils.AccessLog;
import utils.QueryTagTools;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.evolution.AccountPatches;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.messaging.SubscriptionManager;

public class PatientRecordTool {

	public static boolean keepPatientRecordKey(AccessContext context, MidataId targetUser) throws AppException {
		List<DBRecord> recs = QueryEngine.listInternal(context.getCache(), context.getTargetAps(), context, CMaps.map("owner", targetUser).map("format","fhir/Patient").map("content", "Patient").map("data", CMaps.map("id", targetUser.toString())), RecordManager.SHARING_FIELDS);
		if (recs.size() == 1) {
			Set<String> tags = RecordConversion.instance.getTags(recs.get(0));
			if (tags != null && tags.contains(QueryTagTools.SECURITY_LOCALCOPY)) return false;
			
			AccessLog.log("stored patient record key");
			Member member = Member.getById(targetUser, Sets.create("_id", "patientRecordKey", "patientRecordId"));
			member.setPatientRecord(recs.get(0)._id, recs.get(0).key);
			return true;
		} else if (recs.size()==0) {
			return false;
		} else {
			throw new InternalServerException("error.internal", "Could not store patient record key.");
		}
	}
	
	public static void patchMissingPatientRecords() throws AppException {
		System.out.println("patch missing patient records");
		Set<Member> all = Member.getAll(CMaps.map("role", UserRole.MEMBER).map("status", Sets.createEnum(UserStatus.ACTIVE, UserStatus.BLOCKED, UserStatus.NEW)).map("security", AccountSecurityLevel.KEY).map("patientRecordId", CMaps.map("$exists", false)), Sets.create("_id"), 2000);
		Set<MidataId> failed = new HashSet<MidataId>();
		while (all.size() > failed.size()) {
			System.out.println("found "+all.size()+" user records where patient record can be added. (failed: "+failed.size()+")");
			for (Member member : all) {
			   if (!failed.contains(member._id) && !recoverPatientRecordKey(member._id)) { failed.add(member._id); };	
			}
			all = Member.getAll(CMaps.map("role", UserRole.MEMBER).map("status", Sets.createEnum(UserStatus.ACTIVE, UserStatus.BLOCKED, UserStatus.NEW)).map("security", AccountSecurityLevel.KEY).map("patientRecordId", CMaps.map("$exists", false)), Sets.create("_id"), 2000);			
		}
		System.out.println("finished patching missing patient records failed="+failed.size());
		ServerTools.endRequest();
	}
	
	private static boolean recoverPatientRecordKey(MidataId targetUser) {
		try {
			Member user = Member.getById(targetUser, Sets.create(User.ALL_USER_INTERNAL, "status", "role", "security", "patientRecordKey", "patientRecordId"));			
			if (user.role == UserRole.MEMBER && user.security == AccountSecurityLevel.KEY && !user._id.equals(RuntimeConstants.instance.publicUser)) {
				KeyManager.instance.login(1000l*60l*60l, false);
				KeyManager.instance.unlock(targetUser, null);
				AccessContext temp = ContextManager.instance.createInitialSession(targetUser, UserRole.MEMBER, RuntimeConstants.instance.portalPlugin).forAccount();
				AccountPatches.check(temp, user);
				return keepPatientRecordKey(temp, targetUser);						
			}
		} catch (Exception e) {
			System.out.println("patient record patch failed for patient: "+targetUser.toString());
			e.printStackTrace();
			AccessLog.log("patient record patch failed for patient: "+targetUser.toString());
			AccessLog.logException("patient record patch error", e);
		} finally {
	        ServerTools.endRequest();
		}
		return false;
	}
	
	private static DBRecord getPatientRecordIntern(AccessContext context, MidataId targetUser) throws AppException {
		Member user = Member.getById(targetUser, Sets.create("patientRecordKey", "patientRecordId"));
		if (user == null) return null;
		byte[] key = user.patientRecordKey;
		if (key == null) {
			if (keepPatientRecordKey(context.forAccountReshare(), targetUser)) {
				user = Member.getById(targetUser, Sets.create("patientRecordKey", "patientRecordId"));
			} else return null;
		}
		
		DBRecord rec = DBRecord.getById(user.patientRecordId, Sets.create(QueryEngine.META_AND_DATA, "stream", "time", "document", "part", "direct"));		
		rec.key = user.patientRecordKey;
		rec.security = APSSecurityLevel.HIGH;
		rec.owner = user._id;
		rec.context = context.internal();
		QueryEngine.loadData(rec);
		
		return rec;	
	}
	
	public static Record getPatientRecord(AccessContext context, MidataId targetUser) throws AppException {		
		DBRecord rec = getPatientRecordIntern(context, targetUser);		
		if (rec == null) return null;
		return RecordConversion.instance.currentVersionFromDB(rec);	
	}
	
	public static void sharePatientRecord(AccessContext context, Consent target) throws AppException {
		AccessLog.log("share patient record context=", context.toString()," consent=", target._id.toString());
		DBRecord rec = getPatientRecordIntern(context, target.owner);
		if (rec == null) throw new BadRequestException("error.login.required", "User needs to login.");
		APS apswrapper = context.getCache().getAPS(target._id, target.owner);
		List<DBRecord> alreadyContained = QueryEngine.isContainedInAps(context, target._id, Collections.singletonList(rec));
        if (alreadyContained.isEmpty()) {		
		  RecordManager.instance.shareUnchecked(context.getCache(), Collections.singletonList(rec), alreadyContained, apswrapper, true);
        }      
	}
	
	public static void updatePatientRecord(AccessContext context, Record record) throws AppException {
		AccessLog.log("update patient record context=",context.toString()," user=",record.owner.toString());
		DBRecord current = getPatientRecordIntern(context, record.owner);
		if (current == null) throw new InternalServerException("error.internal", "Cannot access patient record.");
		DBRecord vrec = new VersionedDBRecord(current);		
		RecordEncryption.encryptRecord(vrec);
		record.lastUpdated = new Date(); 
		
		current.data = record.data;
		current.meta.put("lastUpdated", record.lastUpdated);
		current.time = Query.getTimeFromDate(record.lastUpdated);
		record.modifiedBy = context.getActor();
	   	   
	    String version = Long.toString(System.currentTimeMillis());
	    current.meta.put("version", version);
	    record.version = version;
	    
	    if (record.modifiedBy.equals(current.owner)) {
	      // use "O" for owner. No entry is same as creator for backwards compatibility
	      current.meta.put("modifiedBy", "O");
	    } else {
	      current.meta.put("modifiedBy", record.modifiedBy.toDb());
	    }
	    		
	    DBRecord clone = current.clone();	    
		RecordEncryption.encryptRecord(current);					
		VersionedDBRecord.add(vrec);				
	    DBRecord.upsert(current); 	  		    
	    RecordLifecycle.notifyOfChange(clone, context.getCache());	    
	    SubscriptionManager.resourceChange(record);
	}
}
