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
	    Set<String> fields = Sets.create("owner", "ownerName", "group", "recruiter", "recruiterName", "pstatus", "gender", "country", "yearOfBirth", "partName");	    
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
		
			
			Study theStudy = Study.getById(message.getStudy(), Sets.create("_id", "participantSearchStatus", "executionStatus", "autoJoinGroup", "name", "code"));				
			if (theStudy != null && theStudy.autoJoinGroup != null) {
				if (theStudy.participantSearchStatus.equals(ParticipantSearchStatus.SEARCHING) && (theStudy.executionStatus.equals(StudyExecutionStatus.PRE) || theStudy.executionStatus.equals(StudyExecutionStatus.RUNNING))) {
					
					Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByGroup(theStudy._id);
					UserGroupMember ugm = null;
					for (UserGroupMember ugmx : ugms) {		   
					  if  (ugmx.role.manageParticipants()) { ugm = ugmx; }
					}
					
					if (ugm != null) {
						AccessLog.log("START AUTOJOIN");		
						try {
							String handle = KeyManager.instance.login(1000l*60l, false);
							KeyManager.instance.unlock(ugm.member, null);
							RecordManager.instance.setAccountOwner(ugm.member, ugm.member);
							
							AutoJoiner.approve(ugm.member, theStudy, message.getUser(), message.getApp(), theStudy.autoJoinGroup);
							
							AccessLog.log("END AUTOJOIN");
						} finally {
							
						    KeyManager.instance.logout();
						    ServerTools.endRequest();
						}
					
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