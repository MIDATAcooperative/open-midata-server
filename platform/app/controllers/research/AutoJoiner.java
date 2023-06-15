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

package controllers.research;

import java.util.List;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import controllers.Circles;
import models.Consent;
import models.MidataId;
import models.Study;
import models.StudyParticipation;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationStatus;
import models.enums.StudyExecutionStatus;
import models.enums.UserRole;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.messaging.ServiceHandler;
import utils.stats.ActionRecorder;

public class AutoJoiner {

private static ActorRef autoJoiner;

private static ActorSystem system;
	
	public static void init(ActorSystem system1) {
		system = system1;
		autoJoiner = system.actorOf(Props.create(AutoJoinerActor.class).withDispatcher("medium-work-dispatcher"), "autoJoiner");
	}
	
	public static void autoJoin(MidataId app, MidataId user, MidataId study) {		
		autoJoiner.tell(new JoinMessage(app, user, study), ActorRef.noSender());
	}
	       
  
    
    public static void approve(AccessContext context, Study theStudy, MidataId participant, MidataId app, String group) throws AppException {    	
	    Set<String> fields = StudyParticipation.STUDY_EXTRA;	    
		List<StudyParticipation> participants = StudyParticipation.getParticipantsByStudy(theStudy._id, CMaps.map("pstatus", ParticipationStatus.REQUEST).map("owner", participant), fields, 0);
		
		Studies.autoApprove(app, theStudy, context, theStudy.autoJoinGroup, participants);			
    }
    
    public static void autoConfirm(MidataId consentId) {
    	autoJoiner.tell(new AutoConfirmMessage(consentId), ActorRef.noSender());
    }
	
}

class AutoJoinerActor extends AbstractActor {
	
	public AutoJoinerActor() {							    
	}
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(JoinMessage.class, this::join)	   
	      .match(AutoConfirmMessage.class, this::autoconfirm)
	      .build();
	}
	
		
	public void join(JoinMessage message) throws Exception {
	    long st = ActionRecorder.start("AutoJoiner/join");
		try {
		    AccessLog.logStart("jobs", message.toString());
			
			Study theStudy = Study.getById(message.getStudy(), Sets.create("_id", "participantSearchStatus", "executionStatus", "autoJoinGroup", "autoJoinExecutor", "autoJoinKey", "name", "code", "type"));				
			if (theStudy != null && theStudy.autoJoinGroup != null && theStudy.autoJoinExecutor != null && theStudy.autoJoinKey != null) {
				if (theStudy.participantSearchStatus.equals(ParticipantSearchStatus.SEARCHING) && (theStudy.executionStatus.equals(StudyExecutionStatus.PRE) || theStudy.executionStatus.equals(StudyExecutionStatus.RUNNING))) {
					
					
						AccessLog.log("START AUTOJOIN");		
						try {
							String handle = ServiceHandler.decrypt(theStudy.autoJoinKey);
							
							if (handle == null) { // Main background service key not valid anymore
								theStudy.autoJoinGroup = null;
								theStudy.setAutoJoinGroup(null, null, null);
								try {
									throw new InternalServerException("error.internal", "Missing service key for study:"+theStudy.code+"("+theStudy.name+")");
								} catch (InternalServerException e) {
									ErrorReporter.report("Auto-Join", null, e);
								}
								return;
							}
							
							KeyManager.instance.continueSession(handle, theStudy.autoJoinExecutor);	
							AccessContext context = ContextManager.instance.createSessionForDownloadStream(theStudy.autoJoinExecutor, UserRole.ANY);
							ContextManager.instance.setAccountOwner(theStudy.autoJoinExecutor, theStudy.autoJoinExecutor);							
							AutoJoiner.approve(context, theStudy, message.getUser(), message.getApp(), theStudy.autoJoinGroup);
							
							AccessLog.log("END AUTOJOIN");
						} finally {
							
						    KeyManager.instance.logout();
						    //ServerTools.endRequest();
						}
					
					
										
				}
			}
					
		} catch (Exception e) {
			ErrorReporter.report("AutoJoiner", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();
			ActionRecorder.end("AutoJoiner/join", st);
		}
	}
	
	public void autoconfirm(AutoConfirmMessage message) throws Exception {
		 long st = ActionRecorder.start("AutoJoiner/autoconfirm");
			try {
			    AccessLog.logStart("jobs", message.toString());
				
			    Consent consent = Consent.getByIdUnchecked(message.getConsentId(), Sets.create(Consent.FHIR, "autoConfirmHandle"));
			    							
				if (consent != null && consent.autoConfirmHandle != null && consent.owner != null && consent.status == ConsentStatus.UNCONFIRMED) {
																	
					AccessLog.log("START AUTOCONFIRM");		
					try {
						String handle = ServiceHandler.decrypt(consent.autoConfirmHandle);
						
						if (handle == null) { // Main background service key not valid anymore								
							return;
						}
						
						KeyManager.instance.continueSession(handle, consent.owner);	
						AccessContext context = ContextManager.instance.createSessionForDownloadStream(consent.owner, UserRole.ANY);
						ContextManager.instance.setAccountOwner(consent.owner, consent.owner);							
						
						AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.CONSENT_APPROVED).withActorUser(RuntimeConstants.instance.backendService).withModifiedUser(consent.owner).withConsent(consent));
						Circles.consentStatusChange(context, consent, ConsentStatus.ACTIVE);
						Circles.sendConsentNotifications(context.getAccessor(), consent, ConsentStatus.ACTIVE, false);
						
						Consent.set(consent._id, "autoConfirmHandle", null);
						consent.autoConfirmHandle = null;
						
						AuditManager.instance.success();
						AccessLog.log("END AUTOCONFIRM");
					} finally {
						
					    KeyManager.instance.logout();
					    //ServerTools.endRequest();
					}
				
																						
				}
						
			} catch (Exception e) {
				ErrorReporter.report("AutoJoiner", null, e);	
				throw e;
			} finally {
				ServerTools.endRequest();
				ActionRecorder.end("AutoJoiner/autoconfirm", st);
			}
	}
	
}

class AutoConfirmMessage {	
	private final MidataId consentId;
	
    AutoConfirmMessage(MidataId consentId) {
    	this.consentId = consentId;
    }

	public MidataId getConsentId() {
		return consentId;
	}
    
    
}