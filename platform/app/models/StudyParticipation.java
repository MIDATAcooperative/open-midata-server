package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.ConsentType;
import models.enums.Gender;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationStatus;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.NotMaterialized;
import utils.db.OrderOperations;
import utils.exceptions.InternalServerException;
import utils.search.Search;

/**
 * A consent that shares data from a MIDATA member with a study.
 * 
 */
public class StudyParticipation extends Consent {
		
	public String ownerName;
	public ObjectId study; //study that the member is related to
	public String studyName; // replication of study name
	public ParticipationStatus pstatus; //how is the member related to the study?
	public String group; // If study has multiple separate groups of participants
	public ObjectId recruiter; // if member has been recruited through someone (by entering a participation code)
	public String recruiterName; // replication of recruiter name
	public Set<ObjectId> providers; // (Optional) List of healthcare providers monitoring the member for this study.
	public List<History> history; // History of participation process
	
	public int yearOfBirth;
	public String country;
	public Gender gender;
	
	public StudyParticipation() {
		this.type = ConsentType.STUDYPARTICIPATION;
	}
	
	public static void add(StudyParticipation studyparticipation) throws InternalServerException {
		Model.insert(collection, studyparticipation);
	}
	
	public static Set<StudyParticipation> getAllByMember(ObjectId member, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("owner", member), fields);
	}
	
	public static Set<StudyParticipation> getParticipantsByStudy(ObjectId study, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.RESEARCH_REJECTED)), fields);
	}
	
	public static Set<StudyParticipation> getParticipantsByStudyAndGroup(ObjectId study, String group, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.RESEARCH_REJECTED)), fields);
	}
	
	public static Set<StudyParticipation> getActiveParticipantsByStudy(ObjectId study, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST)), fields);
	}
	
	public static Set<StudyParticipation> getActiveParticipantsByStudyAndGroup(ObjectId study, String group, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST)), fields);
	}
	
	public static StudyParticipation getById(ObjectId id, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static StudyParticipation getByStudyAndMember(ObjectId study, ObjectId member, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("owner", member), fields);
	}
	
	public static boolean existsByStudyAndMemberName(ObjectId study, String name) throws InternalServerException {
		return Model.exists(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("ownerName", name));
	}
	
	public static StudyParticipation getByStudyAndId(ObjectId study, ObjectId id, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("_id", id).map("study", study), fields);
	}
	
	public void setPStatus(ParticipationStatus newstatus) throws InternalServerException {
		Model.set(StudyParticipation.class, collection, this._id, "pstatus", newstatus);
	}
    
    public void addHistory(History newhistory) throws InternalServerException {
    	this.history.add(newhistory);
    	Model.set(StudyParticipation.class, collection, this._id, "history", this.history);
    }
    
    public static void delete(ObjectId studyId, ObjectId partId) throws InternalServerException {	
		Model.delete(StudyParticipation.class, collection, CMaps.map("_id", partId).map("study", studyId));
	}

}
