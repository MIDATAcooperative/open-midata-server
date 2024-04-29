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

package models;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.AssistanceType;
import models.enums.InformationType;
import models.enums.JoinMethod;
import models.enums.ParticipantSearchStatus;
import models.enums.ProjectDataFilter;
import models.enums.ProjectLeavePolicy;
import models.enums.RejoinPolicy;
import models.enums.ResearcherRole;
import models.enums.StudyExecutionStatus;
import models.enums.StudyType;
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
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "name", "code", "identifiers", "type", "joinMethods", "owner", "createdBy", "createdAt", "description", "infos", "infosPart", "infosInternal", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "groups", "requirements", "termsOfUse", "startDate", "endDate", "dataCreatedBefore", "processFlags", "autoJoinGroup", "anonymous", "consentObserver", "leavePolicy", "rejoinPolicy", "forceClientCertificate", "dataFilters");
	
	public @NotMaterialized static final Set<String> LINK_FIELDS = Sets.create("_id", "name", "code", "type", "joinMethods", "description", "infos", "infosPart", "infosInternal", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "groups", "requirements", "termsOfUse", "startDate", "endDate", "dataCreatedBefore", "processFlags", "autoJoinGroup", "anonymous");
	
	/**
	 * name of study
	 */
	public String name;
	
	/**
	 * code of study 
	 */
	public String code;
	
	/**
	 * additional identifiers
	 */
	public List<String> identifiers;
	
	/**
	 * id of research organization which does the study
	 */
	public MidataId owner;
	
	/**
	 * name of research organization
	 */
	@NotMaterialized
	public String ownerName;
	
	/**
	 * id of researcher who created the study
	 */
	public MidataId createdBy; // references ResearchUser
	
	/**
	 * Type of research project
	 */
	public StudyType type;
	
	/**
	 * Supported join methods
	 */
	public Set<JoinMethod> joinMethods;
	
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
	
	public List<Info> infosPart;
	
	public List<Info> infosInternal;
	
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
	 * Information to be filtered out for pseudonymization
	 */
	public Set<ProjectDataFilter> dataFilters;
	
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
    
    public byte[] autoJoinKey;
    
    public MidataId autoJoinExecutor;
    
    /**
     * set of external services that monitor consent changes of this project
     */
    public Set<MidataId> consentObserver;
    
    public ProjectLeavePolicy leavePolicy;
    
    public RejoinPolicy rejoinPolicy;
    
    public @NotMaterialized Set<String> consentObserverNames;
    
    public boolean forceClientCertificate;
    
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
    
    public void setForceClientCertificate(boolean forceClientCertificate) throws InternalServerException {
    	this.forceClientCertificate = forceClientCertificate;
		Model.set(Study.class, collection, this._id, "forceClientCertificate", forceClientCertificate);
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
    
    public void setIdentifiers(List<String> identifiers) throws InternalServerException {
    	this.identifiers = identifiers;
    	Model.set(Study.class, collection, this._id, "identifiers", identifiers);
    }
    
    public void setType(StudyType type) throws InternalServerException {
    	this.type = type;
    	Model.set(Study.class, collection, this._id, "type", type);
    }
    
    public void setJoinMethods(Set<JoinMethod> joinMethods) throws InternalServerException {
    	this.joinMethods = joinMethods;
    	Model.set(Study.class, collection, this._id, "joinMethods", joinMethods);
    }
    
    public void setLeavePolicy(ProjectLeavePolicy leavePolicy) throws InternalServerException {
    	this.leavePolicy = leavePolicy;
    	Model.set(Study.class, collection, this._id, "leavePolicy", leavePolicy);
    }
    
    public void setRejoinPolicy(RejoinPolicy rejoinPolicy) throws InternalServerException {
    	this.rejoinPolicy = rejoinPolicy;
    	Model.set(Study.class, collection, this._id, "rejoinPolicy", rejoinPolicy);
    }
    
    public void setconsentObserver(Set<MidataId> consentObserver) throws InternalServerException {
    	this.consentObserver = consentObserver;
    	Model.set(Study.class, collection, this._id, "consentObserver", consentObserver);
    }
    
    public void setAutoJoinGroup(String autoJoinGroup, MidataId executor, byte[] key) throws InternalServerException {
    	this.autoJoinGroup = autoJoinGroup;
    	this.autoJoinExecutor = executor;
    	this.autoJoinKey = key;
    	this.setMultiple(collection, Sets.create("autoJoinGroup", "autoJoinExecutor", "autoJoinKey"));
    }
    
    public void setProcessFlags(Set<String> processFlags) throws InternalServerException {
    	this.processFlags = processFlags;
    	Model.set(Study.class, collection, this._id, "processFlags", processFlags);
    }
    
    public void setInfos(List<Info> infos) throws InternalServerException {
    	this.infos = infos;
    	Model.set(Study.class, collection, this._id, "infos", infos);
    }
    
    public void setInfosPart(List<Info> infosPart) throws InternalServerException {
    	this.infosPart = infosPart;
    	Model.set(Study.class, collection, this._id, "infosPart", infosPart);
    }
    
    public void setInfosInternal(List<Info> infosInternal) throws InternalServerException {
    	this.infosInternal = infosInternal;
    	Model.set(Study.class, collection, this._id, "infosInternal", infosInternal);
    }
        
    
    public static void delete(MidataId studyId) throws InternalServerException {	
		Model.delete(Study.class, collection, CMaps.map("_id", studyId));
	}
	
    public static long count() throws AppException {
		return Model.count(Study.class, collection, CMaps.map("executionStatus", StudyExecutionStatus.RUNNING));
	}
}
