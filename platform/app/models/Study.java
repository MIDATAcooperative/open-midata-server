package models;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.AssistanceType;
import models.enums.InformationType;
import models.enums.ParticipantSearchStatus;
import models.enums.ResearcherRole;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;
import models.enums.UserFeature;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
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
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "name", "code", "owner", "createdBy", "createdAt", "description", "infos", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "groups", "requirements", "termsOfUse", "startDate", "endDate", "dataCreatedBefore", "processFlags", "autoJoinGroup", "anonymous");
	
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
	 * start date of study
	 */
	public Date startDate;
	
	/**
	 * end date of study
	 */
	public Date endDate;
	
	/**
	 * do not share data created after
	 */
	public Date dataCreatedBefore;
	
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
	 * If set no one is allowed to see mapping
	 */
	public boolean anonymous;
	
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
     * Definition of groups of participants
     */
    public List<StudyGroup> groups;
    
    /**
     * Requirements to user account to participate in study
     */
    public Set<UserFeature> requirements;
    
    /**
     * Role of current researcher
     */
    public @NotMaterialized ResearcherRole myRole;
    
    /**
     * Terms of use for study
     */
    public String termsOfUse;
    
    /**
     * mark process steps as done/unnecessary
     */
    public Set<String> processFlags;
    
    /**
     * A StudyGroup new participants should automatically join
     */
    public String autoJoinGroup;
    
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
    
    public static Study getById(MidataId studyid, Set<String> fields) throws InternalServerException {
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
    
    public void setAnonymous(boolean anonymous) throws InternalServerException {
		Model.set(Study.class, collection, this._id, "anonymous", anonymous);
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
    
    public void setRequirements(Set<UserFeature> requirements) throws InternalServerException {
    	this.requirements = requirements;
    	Model.set(Study.class, collection, this._id, "requirements", requirements);
    }
    
    public void setTermsOfUse(String termsOfUse) throws InternalServerException {
    	this.termsOfUse = termsOfUse;
    	Model.set(Study.class, collection, this._id, "termsOfUse", termsOfUse);
    }
    
    public void setStartDate(Date startDate) throws InternalServerException {
    	this.startDate = startDate;
    	Model.set(Study.class, collection, this._id, "startDate", startDate);
    }
    
    public void setEndDate(Date endDate) throws InternalServerException {
    	this.endDate = endDate;
    	Model.set(Study.class, collection, this._id, "endDate", endDate);
    }
    
    public void setDataCreatedBefore(Date dataCreatedBefore) throws InternalServerException {
    	this.dataCreatedBefore = dataCreatedBefore;
    	Model.set(Study.class, collection, this._id, "dataCreatedBefore", dataCreatedBefore);
    }
    
    public void setName(String name) throws InternalServerException {
    	this.name = name;
    	Model.set(Study.class, collection, this._id, "name", name);
    }
    
    public void setDescription(String description) throws InternalServerException {
    	this.description = description;
    	Model.set(Study.class, collection, this._id, "description", description);
    }
    
    public void setAutoJoinGroup(String autoJoinGroup) throws InternalServerException {
    	this.autoJoinGroup = autoJoinGroup;
    	Model.set(Study.class, collection, this._id, "autoJoinGroup", autoJoinGroup);
    }
    
    public void setProcessFlags(Set<String> processFlags) throws InternalServerException {
    	this.processFlags = processFlags;
    	Model.set(Study.class, collection, this._id, "processFlags", processFlags);
    }
        
    
    public static void delete(MidataId studyId) throws InternalServerException {	
		Model.delete(Study.class, collection, CMaps.map("_id", studyId));
	}
	
    public static long count() throws AppException {
		return Model.count(Study.class, collection, CMaps.map("executionStatus", StudyExecutionStatus.RUNNING));
	}
}
