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

package utils.evolution;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import controllers.Circles;
import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.Circle;
import models.Consent;
import models.HealthcareProvider;
import models.MidataId;
import models.Record;
import models.Research;
import models.ResearchUser;
import models.Space;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.ResearcherRole;
import models.enums.UserGroupType;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ConsentAccessContext;
import utils.context.ContextManager;
import utils.exceptions.AppException;
import utils.fhir.MidataConsentResourceProvider;
import utils.fhir.GroupResourceProvider;
import utils.fhir.OrganizationResourceProvider;
import utils.fhir.PatientResourceProvider;

public class AccountPatches {

	public static final int currentAccountVersion = 20200221;
	
	public static boolean check(AccessContext context, User user) throws AppException {
		boolean isold = user.accountVersion < currentAccountVersion;
		
		if (user.accountVersion < 20160324) { formatPatch20160324(context,user); }	
		if (user.accountVersion < 20160407) { formatPatch20160407(context,user); }
		if (user.accountVersion < 20160902) { formatPatch20160902(context,user); }
		if (user.accountVersion < 20161205) { formatPatch20161205(context,user); }
		if (user.accountVersion < 20171206) { formatPatch20171206(context, user); }
		if (user.accountVersion < 20190206) { formatPatch20190206(context, user); }
		if (user.accountVersion < 20200221) { formatPatch20200221(context,user); }
		//if (user.accountVersion < 20180130) { formatPatch20180130(user); }
		//if (user.accountVersion < 20170206) { formatPatch20170206(user); }
		
		return isold;
	}
	
	public static void makeCurrent(User user, int currentAccountVersion) throws AppException {
		if (user.accountVersion < currentAccountVersion) {
			user.accountVersion = currentAccountVersion;
			User.set(user._id, "accountVersion", user.accountVersion);
		}
	}
	
	public static void formatPatch20160324(AccessContext context, User user) throws AppException {
		AccessLog.logBegin("start patch 2016 03 24");
	   Set<String> formats = Sets.create("fhir/Observation/String", "fhir/Observation/Quantity", "fhir/Observation/CodeableConcept");
	   List<Record> recs = RecordManager.instance.list(UserRole.ANY, context, CMaps.map("format", formats).map("owner", "self"), RecordManager.COMPLETE_DATA);
	   for (Record r : recs) {
		   MidataId oldId = r._id;
		   r._id = new MidataId();
		   
		   r.format = "fhir/Observation";
		   try {
		     RecordManager.instance.addRecord(context,  r, null);
		   } catch (AppException e) {}
		   
	   }
	   RecordManager.instance.wipe(context, CMaps.map("format", formats).map("owner", "self"));
	   Set<Space> spaces = Space.getAllByOwner(user._id, Sets.create("_id", "type"));
	   for (Space space : spaces) {	   					
		   if (space.type != null && space.type.equals("visualization")) {
			  RecordManager.instance.deleteAPS(context, space._id);
			  Space.delete(user._id, space._id);
		   }
	   }	   
	   RecordManager.instance.fixAccount(context);
	   makeCurrent(user, 20160324);
	   AccessLog.logEnd("end patch 2016 03 24");
	}
	
	public static void formatPatch20160407(AccessContext context, User user) throws AppException {
		AccessLog.logBegin("start patch 2016 04 07");
		RecordManager.instance.patch20160407(context); 		      
		RecordManager.instance.fixAccount(context);
		makeCurrent(user, 20160407);
		AccessLog.logEnd("end patch 2016 04 07");
	}
	
	public static void formatPatch20160902(AccessContext context, User user) throws AppException {
		AccessLog.logBegin("start patch 2016 09 02");
		
		RecordManager.instance.fixAccount(context);
		if (user.role.equals(UserRole.MEMBER)) {
		  PatientResourceProvider.updatePatientForAccount(context, user._id);
		}
		
		/*Set<Consent> consents = Consent.getAllByOwner(user._id, CMaps.map("type", ), Consent.ALL);
		for (Consent consent : consents) {
		  Circles.autosharePatientRecord(consent);
		}*/
				
		makeCurrent(user, 20160902);
		AccessLog.logEnd("end patch 2016 09 02");
	}
	
	public static void formatPatch20161205(AccessContext context, User user) throws AppException {
		AccessLog.logBegin("start patch 2016 12 05");
		
		Set<Consent> consents = Consent.getAllByAuthorized(user._id);
		for (Consent c : consents) {
			AccessPermissionSet.setConsent(c._id);					
			Consent.set(c._id, "dataupdate", System.currentTimeMillis());
		}
		consents = Consent.getAllByOwner(user._id, CMaps.map(), Sets.create("_id"), Integer.MAX_VALUE);
		for (Consent c : consents) {
			AccessPermissionSet.setConsent(c._id);					
			Consent.set(c._id, "dataupdate", System.currentTimeMillis());
		}
		makeCurrent(user, 20161205);
		
		AccessLog.logEnd("end patch 2016 12 05");
	}
	
	public static void formatPatch20170206(AccessContext context, User user) throws AppException {
		AccessLog.logBegin("start patch 2017 02 06");
		
		accountReset(context, user);
		try {
		  RecordManager.instance.wipe(context, CMaps.map("owner", "self").map("app", "fitbit"));
		} catch (AppException e) {};
		try {
		  RecordManager.instance.wipe(context, CMaps.map("owner", "self").map("app", "57a476e679c72190248a135d"));
		} catch (AppException e) {};
		try {
		  RecordManager.instance.wipe(context, CMaps.map("owner", "self").map("app", "withings"));
		} catch (AppException e) {};
		
				
		makeCurrent(user, 20170206);
						
		AccessLog.logEnd("end patch 2017 02 06");
	}
	
	public static void formatPatch20171206(AccessContext context, User user) throws AppException {
		AccessLog.logBegin("start patch 2017 12 06");
		
		if (user.role.equals(UserRole.RESEARCH)) {
			ResearchUser ru = ResearchUser.getById(user._id, Sets.create("organization"));
			Set<Study> studies = Study.getByOwner(ru.organization, Sets.create("_id", "name", "createdAt"));
			for (Study study : studies) {
				UserGroup grp = UserGroup.getById(study._id, Sets.create("_id"));
				if (grp == null) {
					
					  UserGroup userGroup = new UserGroup();
						
						userGroup.name = study.name;		
						userGroup.type = UserGroupType.RESEARCHTEAM;
						userGroup.status = UserStatus.ACTIVE;
						userGroup.creator = ru._id;		
						userGroup._id = study._id;
						userGroup.nameLC = userGroup.name.toLowerCase();	
						userGroup.keywordsLC = new HashSet<String>();
						userGroup.registeredAt = study.createdAt;		
						userGroup.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(userGroup._id, null);				
						GroupResourceProvider.updateMidataUserGroup(userGroup);		
						userGroup.add();
								
						UserGroupMember member = new UserGroupMember();
						member._id = new MidataId();
						member.member = ru._id;
						member.userGroup = userGroup._id;
						member.status = ConsentStatus.ACTIVE;
						member.startDate = new Date();		
						member.role = ResearcherRole.SPONSOR();
						Map<String, Object> accessData = new HashMap<String, Object>();
						accessData.put("aliaskey", KeyManager.instance.generateAlias(userGroup._id, member._id));
						RecordManager.instance.createPrivateAPS(context.getCache(), ru._id, member._id);
						RecordManager.instance.setMeta(context, member._id, "_usergroup", accessData);						
						member.add();
								
						RecordManager.instance.createPrivateAPS(context.getCache(), userGroup._id, userGroup._id);
					
						Set<StudyParticipation> parts = StudyParticipation.getParticipantsByStudy(study._id, StudyParticipation.ALL);
						
						for (StudyParticipation part : parts) {
							part.authorized.add(study._id);
							part.authorized.remove(ru._id);
							StudyParticipation.set(part._id, "authorized", part.authorized);
							if (part.status == ConsentStatus.ACTIVE) {
								AccessContext partContext = new ConsentAccessContext(part, context);
							  RecordManager.instance.shareAPS(partContext, Collections.singleton(study._id));
							  RecordManager.instance.unshareAPS(partContext, part._id, Collections.singleton(ru._id));
						    }
						}
						
					
				}
			}
		} else if (user.role.equals(UserRole.MEMBER)) {
			Set<Consent> parts = Consent.getAllByOwner(user._id, CMaps.map("type", ConsentType.STUDYPARTICIPATION), Sets.create("_id", "owner"), 0);
			for (Consent c : parts) {
			  Circles.autosharePatientRecord(context, c);
			}
			
		}
				
		makeCurrent(user, 20171206);
						
		AccessLog.logEnd("end patch 2017 12 06");
	}
	
	public static void formatPatch20190206(AccessContext context, User user) throws AppException {
		AccessLog.logBegin("start patch 2019 02 06");
		
		if (user.role.equals(UserRole.ADMIN)) {
			try {
		      RecordManager.instance.getMeta(context, user._id, "test");
			} catch (APSNotExistingException e) {
			  RecordManager.instance.createPrivateAPS(context.getCache(), user._id, user._id);
			  PatientResourceProvider.updatePatientForAccount(context, user._id);
			}
			makeCurrent(user, 20190206);
		}
		
		AccessLog.logEnd("end patch 2019 02 06");
	}
	
	/*public static void formatPatch20180130(User user) throws AppException {
		AccessLog.logBegin("start patch 2018 01 30");
		
	     if (user.role.equals(UserRole.MEMBER)) {	    	 
			Set<StudyParticipation> parts = StudyParticipation.getAllByMember(user._id, Sets.create("ownerName", "study", "studyName", "pstatus", "group", "recruiter", "recruiterName", "providers", "yearOfBirth", "country", "gender","name", "authorized", "entityType", "type", "status", "categoryCode", "creatorApp", "sharingQuery", "validUntil", "createdBefore", "dateOfCreation", "sharingQuery", "externalOwner", "externalAuthorized", "writes", "dataupdate", "owner"));
			for (StudyParticipation c : parts) {
				Study study = Study.getById(c.study, Study.ALL);
				Member member = Member.getById(user._id, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));
				if (study.requiredInformation.equals(InformationType.RESTRICTED)) {
					//List<Record> recs = RecordManager.instance.list(user._id, c._id, CMaps.map("format", "fhir/Patient").map("content", "PseudonymizedPatient"), Sets.create("_id", "data", "owner"));
					//if (recs.size()==0) {
  					  List<Record> old = RecordManager.instance.list(user._id, c._id, CMaps.map("format", "fhir/Patient"), Sets.create("_id", "data", "owner"));
					  RecordManager.instance.unshare(user._id, c._id, old);
					  PatientResourceProvider.createPatientForStudyParticipation(new ExecutionInfo(user._id), c, member);
					  Circles.autosharePatientRecord(user._id, c);
					//}
				}
			
			}
			
		}
				
		//makeCurrent(user, 20180130);
						
		AccessLog.logEnd("end patch 2018 01 30");
	}*/
	
	public static void accountReset(AccessContext context, User user) throws AppException {
		MidataId userId = user._id;
		
		Set<Space> spaces = Space.getAllByOwner(userId, Space.ALL);
		for (Space space : spaces) {
			RecordManager.instance.deleteAPS(context, space._id);			
			Space.delete(userId, space._id);
		}
		
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map("type", ConsentType.EXTERNALSERVICE), Consent.ALL, Integer.MAX_VALUE);
		for (Consent consent : consents) {
			RecordManager.instance.deleteAPS(context, consent._id);
			Circle.delete(userId, consent._id);
		}
	}
	
	public static void fixFhirConsents() throws AppException {
		boolean foundSome = true;
		while (foundSome) {
		    List<Consent> consents = Consent.getBroken();
		    if (consents.size() == 0) foundSome = false;
			for (Consent consent : consents) {				
				MidataConsentResourceProvider.updateMidataConsent(consent, null);		
				Consent.set(consent._id, "fhirConsent", consent.fhirConsent);
			}
		}
	}
	
	public static void formatPatch20200221(AccessContext context, User user) throws AppException {
		AccessLog.logBegin("start patch 2020 02 21");
		
		if (user.role.equals(UserRole.MEMBER)) {
		  PatientResourceProvider.updatePatientForAccount(context, user._id);
		}
							
		makeCurrent(user, 20200221);
		AccessLog.logEnd("end patch 2020 02 21");
	}
	
	public static void fixOrgs() throws AppException {
		/*AccessLog.logBegin("start fix organization records");
		KeyManager.instance.login(1000l*60l*60l, false);
		KeyManager.instance.unlock(RuntimeConstants.instance.publicUser, null);
		MidataId executor = RuntimeConstants.instance.publicUser;
		AccessContext session = ContextManager.instance.createRootPublicGroupContext();
		Set<Research> res = Research.getAll(CMaps.map(), Research.ALL);
		for (Research research : res) {
			RecordManager.instance.wipeFromPublic(session, CMaps.map("_id", research._id).map("format","fhir/Organization"));
			OrganizationResourceProvider.updateFromResearch(session, research);
		}
		
		Set<HealthcareProvider> hps = HealthcareProvider.getAll(CMaps.map(), HealthcareProvider.ALL);
		for (HealthcareProvider provider : hps) {
			RecordManager.instance.wipeFromPublic(session, CMaps.map("_id", provider._id).map("format","fhir/Organization"));
			OrganizationResourceProvider.updateFromHP(session, provider);
		}
		AccessLog.logEnd("end fix organization records");
		ServerTools.endRequest();*/
	}
	
}
