package controllers.research;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import models.MidataId;
import models.Study;
import models.StudyParticipation;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationStatus;
import models.enums.StudyExecutionStatus;
import play.libs.Akka;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.messaging.MailUtils;
import utils.messaging.Message;

public class AutoJoiner {

private static ActorRef autoJoiner;
	
	public static void init() {
		autoJoiner = Akka.system().actorOf(Props.create(AutoJoinerActor.class), "autoJoiner");
	}
	
	public static void autoJoin(MidataId app, MidataId user, MidataId study) {		
		autoJoiner.tell(new JoinMessage(app, user, study), ActorRef.noSender());
	}
	       
    protected static void autoApproveSingle(List<StudyParticipation> participants, MidataId userId, MidataId app, Study study, String group, MidataId targetUser) throws AppException {			   		  		
		//controllers.research.Studies.joinSharing(userId, study, group, true, participants);
		
		Set<MidataId> ids = new HashSet<MidataId>();
		  
		for (StudyParticipation participation : participants) {
			if (participation.group == null || !participation.group.equals(group)) {
			  AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_GROUP_ASSIGNED, app, userId, participation, study);
			}
			participation.group = group;
			ids.add(participation._id);
		}
		  
		StudyParticipation.setManyGroup(ids, group);
		  
		for (StudyParticipation participation : participants) {
			AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_APPROVED, app, userId, participation, study);																				
		}
		  
		StudyParticipation.setManyStatus(ids, ParticipationStatus.ACCEPTED);
		  					
		AuditManager.instance.success();
				
	}
    
    public static void approve(Study theStudy, MidataId participant, MidataId app, String group) throws AppException {
    	Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByGroup(theStudy._id);

	    Set<String> fields = Sets.create("owner", "ownerName", "group", "recruiter", "recruiterName", "pstatus", "gender", "country", "yearOfBirth", "partName");	    
		List<StudyParticipation> participants = StudyParticipation.getParticipantsByStudy(theStudy._id, CMaps.map("pstatus", ParticipationStatus.REQUEST).map("owner", participant), fields, 0);

		MidataId executor = null;
		for (UserGroupMember ugm : ugms) {		   
		  if  (ugm.role.manageParticipants()) {
			executor = ugm.member;
			
			try {
				String handle = KeyManager.instance.login(1000l*60l, false);
				KeyManager.instance.unlock(ugm.member, null);
				
					
				controllers.research.Studies.joinSharing(ugm.member, theStudy, group, true, participants);
																		
			} finally {
				
			    KeyManager.instance.logout();
			    ServerTools.endRequest();
			}
		
		  }
		}
		
		try {
			String handle = KeyManager.instance.login(1000l*60l, false);
			KeyManager.instance.unlock(executor, null);																														
			AutoJoiner.autoApproveSingle(participants, executor, app, theStudy, group, participant);
		} finally {						
		    KeyManager.instance.logout();
		    ServerTools.endRequest();
		}
    }
	
}

class AutoJoinerActor extends UntypedActor {
	
	public AutoJoinerActor() {							    
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		try {
		if (message instanceof JoinMessage) {
			
			Study theStudy = Study.getById(((JoinMessage) message).getStudy(), Sets.create("_id", "participantSearchStatus", "executionStatus", "autoJoinGroup", "name", "code"));				
			if (theStudy != null && theStudy.autoJoinGroup != null) {
				if (theStudy.participantSearchStatus.equals(ParticipantSearchStatus.SEARCHING) && (theStudy.executionStatus.equals(StudyExecutionStatus.PRE) || theStudy.executionStatus.equals(StudyExecutionStatus.RUNNING))) {					
					AutoJoiner.approve(theStudy, ((JoinMessage) message).getUser(), ((JoinMessage) message).getApp(), theStudy.autoJoinGroup);									
				}
			}
			
		} else {
		    unhandled(message);
	    }	
		} catch (Exception e) {
			ErrorReporter.report("AutoJoiner", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();			
		}
	}
	
}