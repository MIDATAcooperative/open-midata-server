package controllers.research;

import java.util.List;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import models.MidataId;
import models.Study;
import models.StudyParticipation;
import models.UserGroupMember;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationStatus;
import models.enums.StudyExecutionStatus;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.messaging.MailUtils;
import utils.messaging.ServiceHandler;

public class AutoJoiner {

private static ActorRef autoJoiner;

private static ActorSystem system;
	
	public static void init(ActorSystem system1) {
		system = system1;
		autoJoiner = system.actorOf(Props.create(AutoJoinerActor.class), "autoJoiner");
	}
	
	public static void autoJoin(MidataId app, MidataId user, MidataId study) {		
		autoJoiner.tell(new JoinMessage(app, user, study), ActorRef.noSender());
	}
	       
  
    
    public static void approve(MidataId executor, Study theStudy, MidataId participant, MidataId app, String group) throws AppException {    	
	    Set<String> fields = Sets.create("owner", "ownerName", "group", "recruiter", "recruiterName", "pstatus", "partName");	    
		List<StudyParticipation> participants = StudyParticipation.getParticipantsByStudy(theStudy._id, CMaps.map("pstatus", ParticipationStatus.REQUEST).map("owner", participant), fields, 0);
		
		Studies.autoApprove(app, theStudy, executor, theStudy.autoJoinGroup, participants);			
    }
	
}

class AutoJoinerActor extends AbstractActor {
	
	public AutoJoinerActor() {							    
	}
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(JoinMessage.class, this::join)	      
	      .build();
	}
	
		
	public void join(JoinMessage message) throws Exception {
		try {
		
			
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
							RecordManager.instance.setAccountOwner(theStudy.autoJoinExecutor, theStudy.autoJoinExecutor);							
							AutoJoiner.approve(theStudy.autoJoinExecutor, theStudy, message.getUser(), message.getApp(), theStudy.autoJoinGroup);
							
							AccessLog.log("END AUTOJOIN");
						} finally {
							
						    KeyManager.instance.logout();
						    ServerTools.endRequest();
						}
					
					
										
				}
			}
					
		} catch (Exception e) {
			ErrorReporter.report("AutoJoiner", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();			
		}
	}
	
}