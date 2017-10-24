package utils.evolution;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.AccessPermissionSet;
import models.Circle;
import models.Consent;
import models.MidataId;
import models.MobileAppInstance;
import models.Record;
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
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.fhir.GroupResourceProvider;
import utils.fhir.PatientResourceProvider;

public class AccountPatches {

	public static final int currentAccountVersion = 20161205;
	
	public static void check(User user) throws AppException {		
		if (user.accountVersion < 20160324) { formatPatch20160324(user); }	
		if (user.accountVersion < 20160407) { formatPatch20160407(user); }
		if (user.accountVersion < 20160902) { formatPatch20160902(user); }
		if (user.accountVersion < 20161205) { formatPatch20161205(user); }
		//if (user.accountVersion < 20170206) { formatPatch20170206(user); }
	}
	
	public static void makeCurrent(User user, int currentAccountVersion) throws AppException {
		if (user.accountVersion < currentAccountVersion) {
			user.accountVersion = currentAccountVersion;
			User.set(user._id, "accountVersion", user.accountVersion);
		}
	}
	
	public static void formatPatch20160324(User user) throws AppException {
		AccessLog.logBegin("start patch 2016 03 24");
	   Set<String> formats = Sets.create("fhir/Observation/String", "fhir/Observation/Quantity", "fhir/Observation/CodeableConcept");
	   List<Record> recs = RecordManager.instance.list(user._id, user._id, CMaps.map("format", formats).map("owner", "self"), RecordManager.COMPLETE_DATA);
	   for (Record r : recs) {
		   MidataId oldId = r._id;
		   r._id = new MidataId();
		   
		   r.format = "fhir/Observation";
		   try {
		     RecordManager.instance.addRecord(user._id, r);
		   } catch (AppException e) {}
		   
	   }
	   RecordManager.instance.wipe(user._id, CMaps.map("format", formats).map("owner", "self"));
	   Set<Space> spaces = Space.getAllByOwner(user._id, Sets.create("_id", "type"));
	   for (Space space : spaces) {	   					
		   if (space.type != null && space.type.equals("visualization")) {
			  RecordManager.instance.deleteAPS(space._id, user._id);
			  Space.delete(user._id, space._id);
		   }
	   }	   
	   RecordManager.instance.fixAccount(user._id);
	   makeCurrent(user, 20160324);
	   AccessLog.logEnd("end patch 2016 03 24");
	}
	
	public static void formatPatch20160407(User user) throws AppException {
		AccessLog.logBegin("start patch 2016 04 07");
		RecordManager.instance.patch20160407(user._id); 		      
		RecordManager.instance.fixAccount(user._id);
		makeCurrent(user, 20160407);
		AccessLog.logEnd("end patch 2016 04 07");
	}
	
	public static void formatPatch20160902(User user) throws AppException {
		AccessLog.logBegin("start patch 2016 09 02");
		
		RecordManager.instance.fixAccount(user._id);
		if (user.role.equals(UserRole.MEMBER)) {
		  PatientResourceProvider.updatePatientForAccount(user._id);
		}
		
		/*Set<Consent> consents = Consent.getAllByOwner(user._id, CMaps.map("type", ), Consent.ALL);
		for (Consent consent : consents) {
		  Circles.autosharePatientRecord(consent);
		}*/
				
		makeCurrent(user, 20160902);
		AccessLog.logEnd("end patch 2016 09 02");
	}
	
	public static void formatPatch20161205(User user) throws AppException {
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
	
	public static void formatPatch20170206(User user) throws AppException {
		AccessLog.logBegin("start patch 2017 02 06");
		
		accountReset(user);
		try {
		  RecordManager.instance.wipe(user._id, CMaps.map("owner", "self").map("app", "fitbit"));
		} catch (AppException e) {};
		try {
		  RecordManager.instance.wipe(user._id, CMaps.map("owner", "self").map("app", "57a476e679c72190248a135d"));
		} catch (AppException e) {};
		try {
		  RecordManager.instance.wipe(user._id, CMaps.map("owner", "self").map("app", "withings"));
		} catch (AppException e) {};
		
				
		makeCurrent(user, 20170206);
						
		AccessLog.logEnd("end patch 2017 02 06");
	}
	
	public static void formatPatch20171020(User user) throws AppException {
		AccessLog.logBegin("start patch 2017 10 20");
		
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
						member.role = ResearcherRole.INVESTIGATOR();
						Map<String, Object> accessData = new HashMap<String, Object>();
						accessData.put("aliaskey", KeyManager.instance.generateAlias(userGroup._id, member._id));
						RecordManager.instance.createPrivateAPS(ru._id, member._id);
						RecordManager.instance.setMeta(ru._id, member._id, "_usergroup", accessData);						
						member.add();
								
						RecordManager.instance.createPrivateAPS(userGroup._id, userGroup._id);
					
						Set<StudyParticipation> parts = StudyParticipation.getParticipantsByStudy(study._id, StudyParticipation.ALL);
						
						for (StudyParticipation part : parts) {
							part.authorized.add(study._id);
							part.authorized.remove(ru._id);
							StudyParticipation.set(part._id, "authorized", part.authorized);
							if (part.status == ConsentStatus.ACTIVE) {
							  RecordManager.instance.shareAPS(part._id, ru._id, Collections.singleton(study._id));
							  RecordManager.instance.unshareAPS(part._id, ru._id, Collections.singleton(ru._id));
						    }
						}
						
					
				}
			}
		}
				
		makeCurrent(user, 20171020);
						
		AccessLog.logEnd("end patch 2017 10 20");
	}
	
	public static void accountReset(User user) throws AppException {
		MidataId userId = user._id;
		
		Set<Space> spaces = Space.getAllByOwner(userId, Space.ALL);
		for (Space space : spaces) {
			RecordManager.instance.deleteAPS(space._id, userId);			
			Space.delete(userId, space._id);
		}
		
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map("type", ConsentType.EXTERNALSERVICE), Consent.ALL, Integer.MAX_VALUE);
		for (Consent consent : consents) {
			RecordManager.instance.deleteAPS(consent._id, userId);
			Circle.delete(userId, consent._id);
		}
	}
	
	
}
