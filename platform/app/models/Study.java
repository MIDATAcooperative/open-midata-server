package models;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.InformationType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;

import org.bson.types.ObjectId;

import utils.collections.CMaps;

public class Study extends Model {
	
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
	public Set<InformationType> requiredInformation;
	public StudyValidationStatus validationStatus;	
	public ParticipantSearchStatus participantSearchStatus;
	public StudyExecutionStatus executionStatus;
    public List<History> history;
    
    public static void add(Study study) throws ModelException {
		Model.insert(collection, study);
	 }
 
    public static boolean existsByName(String name) throws ModelException {
	   return Model.exists(Study.class, collection, CMaps.map("name", name));
    }
    
    public static Set<Study> getByOwner(ObjectId research, Set<String> fields) throws ModelException {
		return Model.getAll(Study.class, collection, CMaps.map("owner", research), fields);
	}
    
    public static Study getByIdFromOwner(ObjectId studyid, ObjectId owner, Set<String> fields) throws ModelException {
		return Model.get(Study.class, collection, CMaps.map("_id", studyid).map("owner",  owner), fields);
	}
    
    public static Study getByIdFromMember(ObjectId studyid, Set<String> fields) throws ModelException {
		return Model.get(Study.class, collection, CMaps.map("_id", studyid), fields);
	}
    
    public void setValidationStatus(StudyValidationStatus newstatus) throws ModelException {
		Model.set(Study.class, collection, this._id, "validationStatus", newstatus);
	}
    
    public void setParticipantSearchStatus(ParticipantSearchStatus newstatus) throws ModelException {
		Model.set(Study.class, collection, this._id, "participantSearchStatus", newstatus);
	}
    
    public void setExecutionStatus(StudyExecutionStatus newstatus) throws ModelException {
		Model.set(Study.class, collection, this._id, "executionStatus", newstatus);
	}
    
    public void setRequiredInformation(Set<InformationType> inf) throws ModelException {
		Model.set(Study.class, collection, this._id, "requiredInformation", inf);
	}
    
    public void addHistory(History newhistory) throws ModelException {
    	this.history.add(newhistory);
    	Model.set(Study.class, collection, this._id, "history", this.history);
    }
	
}
