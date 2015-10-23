package models;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.AssistanceType;
import models.enums.InformationType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

@JsonFilter("Study")
public class Study extends Model {
	
	private static final String collection = "studies";
	public static final Set<String> ALL = Sets.create("_id", "name", "code", "owner", "createdBy", "createdAt", "description", "infos", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "history", "groups");
	
	public String name;
	public String code;
	public ObjectId owner; // references Research
	public ObjectId createdBy; // references ResearchUser	 
	public Date createdAt;	 
	public String description; //short public description of goals of study
	public List<Info> infos;
	public Set<ObjectId> studyKeywords; //references StudyKeyword. Used to identify members as candidates if their participationInterest is set to 'some'
	public Set<FilterRule> participantRules; //List of rules that members must satisfy to become participants
	public Map<String, Object> recordQuery; // Query that determines which records a member must share during study execution	
	public InformationType requiredInformation;
	public AssistanceType assistance;
	public StudyValidationStatus validationStatus;	
	public ParticipantSearchStatus participantSearchStatus;
	public StudyExecutionStatus executionStatus;
    public List<History> history;
    public List<StudyGroup> groups;
    
    public static void add(Study study) throws InternalServerException {
		Model.insert(collection, study);
	 }
 
    public static boolean existsByName(String name) throws InternalServerException {
	   return Model.exists(Study.class, collection, CMaps.map("name", name));
    }
    
    public static Set<Study> getByOwner(ObjectId research, Set<String> fields) throws InternalServerException {
		return Model.getAll(Study.class, collection, CMaps.map("owner", research), fields);
	}
    
    public static Set<Study> getAll(ObjectId research, Map<String,Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Study.class, collection, properties, fields);
	}
    
    public static Study getByIdFromOwner(ObjectId studyid, ObjectId owner, Set<String> fields) throws InternalServerException {
		return Model.get(Study.class, collection, CMaps.map("_id", studyid).map("owner",  owner), fields);
	}
    
    public static Study getByIdFromMember(ObjectId studyid, Set<String> fields) throws InternalServerException {
		return Model.get(Study.class, collection, CMaps.map("_id", studyid), fields);
	}
    
    public void setValidationStatus(StudyValidationStatus newstatus) throws InternalServerException {
		Model.set(Study.class, collection, this._id, "validationStatus", newstatus);
	}
    
    public void setParticipantSearchStatus(ParticipantSearchStatus newstatus) throws InternalServerException {
		Model.set(Study.class, collection, this._id, "participantSearchStatus", newstatus);
	}
    
    public void setExecutionStatus(StudyExecutionStatus newstatus) throws InternalServerException {
		Model.set(Study.class, collection, this._id, "executionStatus", newstatus);
	}
    
    public void setRequiredInformation(InformationType inf) throws InternalServerException {
		Model.set(Study.class, collection, this._id, "requiredInformation", inf);
	}
    
    public void setAssistance(AssistanceType inf) throws InternalServerException {
		Model.set(Study.class, collection, this._id, "assistance", inf);
	}
    
    public void setGroups(List<StudyGroup> groups) throws InternalServerException {
    	this.groups = groups;
		Model.set(Study.class, collection, this._id, "groups", groups);
	}
    
    public void setRecordQuery(Map<String, Object> recordQuery) throws InternalServerException {
    	this.recordQuery = recordQuery;
		Model.set(Study.class, collection, this._id, "recordQuery", recordQuery);
	}
    
    public void addHistory(History newhistory) throws InternalServerException {
    	this.history.add(newhistory);
    	Model.set(Study.class, collection, this._id, "history", this.history);
    }
	
}
