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

import models.MidataId;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model class for a study
 *
 */
@JsonFilter("Study")
public class Study extends Model {
	
	private static final String collection = "studies";
	
	/**
	 * constant set containing all fields
	 */
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "name", "code", "owner", "createdBy", "createdAt", "description", "infos", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "history", "groups");
	
	/**
	 * name of study
	 */
	public String name;
	
	/**
	 * code of study (currently not used)
	 */
	public String code;
	
	/**
	 * id of research organization which does the study
	 */
	public MidataId owner;
	
	/**
	 * id of researcher who created the study
	 */
	public MidataId createdBy; // references ResearchUser
	
	/**
	 * date of creation of the study
	 */
	public Date createdAt;	 
	
	/**
	 * textual description of the study
	 */
	public String description; 
	
	/**
	 * additional informations about the study
	 */
	public List<Info> infos;
	
	/**
	 * ids of keywords describing the study
	 * 
	 * Used to identify members as candidates if their participationInterest is set to 'some' 
	 */
	public Set<MidataId> studyKeywords; 
	
	/**
	 * Filter rules that a members account must satisfy to become participant in this study
	 */
	public Set<FilterRule> participantRules; 
	
	/**
	 * Query that determines which records a member must share during study execution 
	 */
	public Map<String, Object> recordQuery; 
	
	/**
	 * Level of information required
	 */
	public InformationType requiredInformation;
	
	/**
	 * Type of member assistance required
	 */
	public AssistanceType assistance;
	
	/**
	 * Status of validation process
	 */
	public StudyValidationStatus validationStatus;
	
	/**
	 * Status of participant search process
	 */
	public ParticipantSearchStatus participantSearchStatus;
	
	/**
	 * Status of study execution
	 */
	public StudyExecutionStatus executionStatus;
	
	/**
	 * Study change history
	 */
    public List<History> history;
    
    /**
     * Definition of groups of participants
     */
    public List<StudyGroup> groups;
    
    public static void add(Study study) throws InternalServerException {
		Model.insert(collection, study);
	 }
 
    public static boolean existsByName(String name) throws InternalServerException {
	   return Model.exists(Study.class, collection, CMaps.map("name", name));
    }
    
    public static boolean existsByCode(String code) throws InternalServerException {
 	   return Model.exists(Study.class, collection, CMaps.map("code", code));
     }
    
    public static Set<Study> getByOwner(MidataId research, Set<String> fields) throws InternalServerException {
		return Model.getAll(Study.class, collection, CMaps.map("owner", research), fields);
	}
    
    public static Set<Study> getAll(MidataId research, Map<String,Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Study.class, collection, properties, fields);
	}
    
    public static Study getByIdFromOwner(MidataId studyid, MidataId owner, Set<String> fields) throws InternalServerException {
		return Model.get(Study.class, collection, CMaps.map("_id", studyid).map("owner",  owner), fields);
	}
    
    public static Study getByIdFromMember(MidataId studyid, Set<String> fields) throws InternalServerException {
		return Model.get(Study.class, collection, CMaps.map("_id", studyid), fields);
	}
    
    public static Study getByCodeFromMember(String code, Set<String> fields) throws InternalServerException {
		return Model.get(Study.class, collection, CMaps.map("code", code), fields);
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
    
    public static void delete(MidataId studyId) throws InternalServerException {	
		Model.delete(Study.class, collection, CMaps.map("_id", studyId));
	}
	
}
