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

package utils.messaging;

import java.time.Duration;
import java.util.Date;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.AbstractActor.Receive;
import controllers.Circles;
import models.AccessPermissionSet;
import models.Circle;
import models.Consent;
import models.HPUser;
import models.HealthcareProvider;
import models.KeyInfoExtern;
import models.KeyRecoveryData;
import models.KeyRecoveryProcess;
import models.MidataId;
import models.MobileAppInstance;
import models.Research;
import models.ResearchUser;
import models.Space;
import models.StudyParticipation;
import models.SubscriptionData;
import models.User;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.ParticipationStatus;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.FutureLogin;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.exceptions.AppException;
import utils.fhir.OrganizationResourceProvider;
import utils.stats.ActionRecorder;

/**
 * Wipe accounts from DB
 *
 */
public class AccountWiper extends AbstractActor {
	

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(AccountWipeMessage.class, this::accountWipe)			   
			   .build();
	}
	
	private void sceduleNextPhase(AccountWipeMessage msg) {
		getContext().getSystem().scheduler().scheduleOnce(
			   Duration.ofSeconds(30),
			   getSelf(), new AccountWipeMessage(msg.getAccountToWipe(), msg.getHandle(), msg.getExecutorId(), msg.getPhase() + 1, msg.getAudit()), getContext().dispatcher(), getSender());
	
	}
	
	void accountWipe(AccountWipeMessage msg) throws Exception {
		String path = "AccountWiper/phase "+msg.getPhase();
		long st = ActionRecorder.start(path);
		
		try {
			AccessLog.logStart("jobs", "Account Wipe");
			AccessLog.logBegin("START ACCOUNT WIPE: "+msg.getAccountToWipe()+" / "+msg.getPhase());		
			KeyManager.instance.continueSession(msg.getHandle(), msg.getExecutorId());
			AccessContext context = ContextManager.instance.createSessionForDownloadStream(msg.getExecutorId(), UserRole.ANY);			
	
			switch(msg.getPhase()) {
			case 0: retreatPhase(context, msg.getAccountToWipe());
                    appLeavePhase(context, msg.getAccountToWipe());
			        sceduleNextPhase(msg);
			        AuditManager.instance.success();
			        break;
			case 1: serviceLeavePhase(context, msg.getAccountToWipe());
			        sceduleNextPhase(msg);
			        AuditManager.instance.success();
			        break;
			case 2: consentRemovalPhase(context, msg.getAccountToWipe());
			        cleanUpPhase(context, msg.getAccountToWipe());
			        AuditManager.instance.success();
			        AuditManager.instance.resumeAsyncEvent(msg.getAudit());
			        AuditManager.instance.success();
			        break;			
			}
		
		} catch (Exception e) {
			AuditManager.instance.resumeAsyncEvent(msg.getAudit());
			AuditManager.instance.fail(500, e.toString(), "error.internal");
			ErrorReporter.report("AccountWiper", null, e);	
			throw e;
		} finally {
			AccessLog.logEnd("END ACCOUNT WIPE");
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
		}
	}
	
	private void retreatPhase(AccessContext context, MidataId userId) throws AppException {
        MidataId executorId = context.getAccessor();
		AccessLog.logBegin("begin expire all consents with healthcare, friends and delegation");
		Set<Consent> allconsents = Consent.getAllByOwner(userId, CMaps.map("type", Sets.createEnum(ConsentType.CIRCLE, ConsentType.HCRELATED, ConsentType.HEALTHCARE, ConsentType.REPRESENTATIVE)), Consent.ALL, Integer.MAX_VALUE);
		for (Consent consent : allconsents) {
			if (consent.status == ConsentStatus.ACTIVE) {
			  Circles.consentStatusChange(context, consent, ConsentStatus.EXPIRED);
			} 
		}
		AccessLog.logEnd("end expire all consents");
		AccessLog.logBegin("begin retreat from all projects");
		Set<StudyParticipation> studies = StudyParticipation.getAllByMember(userId, Sets.create("_id", "study", "status", "pstatus"));
		for (StudyParticipation study : studies) {
			if (study.pstatus == ParticipationStatus.MEMBER_REJECTED || study.pstatus == ParticipationStatus.MEMBER_RETREATED || study.pstatus == ParticipationStatus.RESEARCH_REJECTED) continue;
			try {
			  controllers.members.Studies.retreatParticipation(context, userId, study.study);
			} catch (Exception e) {
			  AccessLog.logException("Error retreating from project", e);
			  ErrorReporter.report("Account wiper", null, e);
			}			
		}
		AccessLog.logEnd("end retreat from all projects");
	}
	
	private void appLeavePhase(AccessContext context, MidataId userId) throws AppException {
		AccessLog.logBegin("begin expire all application consents");
		Set<MobileAppInstance> allconsents = MobileAppInstance.getByOwner(userId, MobileAppInstance.APPINSTANCE_ALL);
		for (MobileAppInstance consent : allconsents) {
			if (consent.type == ConsentType.EXTERNALSERVICE && consent.status == ConsentStatus.ACTIVE) {
			  ApplicationTools.removeAppInstance(context, context.getAccessor(), consent);			  
			} 
		}
		AccessLog.logEnd("end expire all application consents");
	}
	
	private void serviceLeavePhase(AccessContext context, MidataId userId) throws AppException {
		AccessLog.logBegin("begin expire all service uses");
		Set<MobileAppInstance> allconsents = MobileAppInstance.getByOwner(userId, MobileAppInstance.APPINSTANCE_ALL);
		for (MobileAppInstance consent : allconsents) {
			if (consent.type == ConsentType.API && consent.status == ConsentStatus.ACTIVE) {
			  ApplicationTools.leaveInstalledService(context, consent, false);			  
			} 
		}
		AccessLog.logEnd("end expire all service uses");
	}
	
				
	private void consentRemovalPhase(AccessContext context, MidataId userId) throws AppException {
		MidataId executorId = context.getAccessor();
        SubscriptionData.deleteByOwner(userId);
        AccessLog.logBegin("begin remove all spaces");
		Set<Space> spaces = Space.getAllByOwner(userId, Space.ALL);
		for (Space space : spaces) {
			if (executorId.equals(userId)) {
			  RecordManager.instance.deleteAPS(context, space._id);
			} else AccessPermissionSet.delete(space._id);		
			
			Space.delete(userId, space._id);
		}
		AccessLog.logEnd("end remove all spaces");
		AccessLog.logBegin("begin wipe consents");
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map("type", Sets.createEnum(ConsentType.CIRCLE, ConsentType.EXTERNALSERVICE, ConsentType.HCRELATED, ConsentType.HEALTHCARE, ConsentType.API, ConsentType.REPRESENTATIVE)), Consent.ALL, Integer.MAX_VALUE);
		for (Consent consent : consents) {
			if (executorId.equals(userId)) {
			RecordManager.instance.deleteAPS(context, consent._id);
			} else AccessPermissionSet.delete(consent._id);
			Circle.delete(userId, consent._id);
		}
		AccessLog.logEnd("end wipe consents");
		AccessLog.logBegin("begin remove authorization from other consents");				
		Set<Consent> consents2 = Consent.getAllByAuthorized(userId);
		for (Consent consent : consents2) {
			consent = Consent.getByIdAndAuthorized(consent._id, userId, Sets.create("authorized"));
			consent.authorized.remove(userId);
			Consent.set(consent._id, "authorized", consent.authorized);	
			Consent.set(consent._id, "lastUpdated", new Date());
		}
		AccessLog.logEnd("end remove authorization");
		Set<UserGroupMember> ugs = UserGroupMember.getAllByMember(userId);
		for (UserGroupMember ug : ugs) {
			AccessPermissionSet.delete(ug._id);
			ug.delete();
		}
		
		if (executorId.equals(userId)) {
		  RecordManager.instance.clearIndexes(context, userId);
		}
	}
				
	private void cleanUpPhase(AccessContext context, MidataId userId) throws AppException {
	    AccessLog.logBegin("begin cleanup");
        KeyRecoveryProcess.delete(userId);
        KeyRecoveryData.delete(userId);
        FutureLogin.delete(userId);
		KeyManager.instance.deleteKey(userId);
		KeyInfoExtern.delete(userId);
		AccessPermissionSet.delete(userId);
						
		User user = User.getByIdAlsoDeleted(userId, User.ALL_USER_INTERNAL);
		if (user != null) {
			if (user.role == UserRole.PROVIDER) {
				HPUser hp = HPUser.getById(userId, Sets.create("provider"));
				user.delete();
				if (!User.exists(CMaps.map("provider", hp.provider).map("status", User.NON_DELETED))) {
					OrganizationResourceProvider.deleteOrganization(context, hp.provider);
					HealthcareProvider.delete(hp.provider);
				}			
			} else
			if (user.role == UserRole.RESEARCH) {
				ResearchUser ru = ResearchUser.getById(userId, Sets.create("organization"));
				user.delete();
				if (!User.exists(CMaps.map("organization", ru.organization).map("status", User.NON_DELETED))) {
				  OrganizationResourceProvider.deleteOrganization(context, ru.organization);
				  Research.delete(ru.organization);	
				}
			} else {
				user.delete();
			} 						
		}
		AccessLog.logEnd("end cleanup");
	}
}
