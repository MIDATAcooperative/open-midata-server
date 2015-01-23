package models;

import java.util.Date;
import java.util.List;
import java.util.Set;

import models.enums.ParticipantSearchStatus;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;

import org.bson.types.ObjectId;

public class Study {
	
	private static final String collection = "studies";
	
	public String name;
	public ObjectId owner; // references Research
	public ObjectId createdBy; // references ResearchUser	 
	public Date createdAt;	 
	public String description; //short public description of goals of study
	public List<Info> infos;
	public Set<ObjectId> studyKeywords; //references StudyKeyword. Used to identify members as candidates if their participationInterest is set to 'some'
	public Set<FilterRule> participantRules; //List of rules that members must satisfy to become participants
	public Set<FilterRule> recordRules; //Rule set that determines which records a member must share during study execution
	public StudyValidationStatus validationStatus;	
	public ParticipantSearchStatus participantSearchStatus;
	public StudyExecutionStatus executionStatus;
    public List<History> history;
	
}
