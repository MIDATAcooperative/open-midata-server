package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.Gender;
import models.enums.ParticipationStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

/**
 * A consent that shares data from a MIDATA member with a study.
 * 
 */
public class StudyParticipation extends Consent {
		
	public String ownerName;
	public MidataId study; //study that the member is related to
	public String studyName; // replication of study name
	public ParticipationStatus pstatus; //how is the member related to the study?
	public String group; // If study has multiple separate groups of participants
	public MidataId recruiter; // if member has been recruited through someone (by entering a participation code)
	public String recruiterName; // replication of recruiter name
	public Set<MidataId> providers; // (Optional) List of healthcare providers monitoring the member for this study.	
	
	public int yearOfBirth;
	public String country;
	public Gender gender;
			
	public StudyParticipation() {
		this.type = ConsentType.STUDYPARTICIPATION;
	}
	
	public static void add(StudyParticipation studyparticipation) throws InternalServerException {
		Model.insert(collection, studyparticipation);
	}
	
	public static Set<StudyParticipation> getAllByMember(MidataId member, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("owner", member), fields);
	}
	
	public static Set<StudyParticipation> getParticipantsByStudy(MidataId study, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.RESEARCH_REJECTED, ParticipationStatus.MEMBER_RETREATED)), fields);
	}
	
	public static Set<StudyParticipation> getParticipantsByStudyAndGroup(MidataId study, String group, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.RESEARCH_REJECTED, ParticipationStatus.MEMBER_RETREATED)), fields);
	}
	
	public static Set<StudyParticipation> getActiveParticipantsByStudy(MidataId study, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.MEMBER_RETREATED)), fields);
	}
	
	public static Set<StudyParticipation> getActiveParticipantsByStudyAndGroup(MidataId study, String group, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.MEMBER_RETREATED)), fields);
	}
	
	public static Set<StudyParticipation> getActiveParticipantsByStudyAndGroupsAndParticipant(Set<MidataId> study, Set<String> group, MidataId member, Set<MidataId> owners, Set<String> fields, boolean alsoPseudonymized) throws InternalServerException {
		Map<String, Object> m = CMaps.map("type", ConsentType.STUDYPARTICIPATION).mapNotEmpty("study", study).mapNotEmpty("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.MEMBER_RETREATED)).map("authorized", member).mapNotEmpty("owner", owners).map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN));
		if (!alsoPseudonymized) m.put("ownerName", CMaps.map("$exists", false));
		return Model.getAll(StudyParticipation.class, collection, m, fields);
	}
	
	public static Set<StudyParticipation> getActiveParticipantsByStudyAndGroupsAndIds(Set<MidataId> study, Set<String> group, MidataId member, Set<MidataId> owners, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).mapNotEmpty("study", study).mapNotEmpty("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.MEMBER_RETREATED)).map("authorized", member).mapNotEmpty("_id", owners).map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN)), fields);
	}
	
	public static StudyParticipation getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static StudyParticipation getByStudyAndMember(MidataId study, MidataId member, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("owner", member), fields);
	}
	
	public static boolean existsByStudyAndMemberName(MidataId study, String name) throws InternalServerException {
		return Model.exists(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("ownerName", name));
	}
	
	public static StudyParticipation getByStudyAndId(MidataId study, MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("_id", id).map("study", study), fields);
	}
	
	public void setPStatus(ParticipationStatus newstatus) throws InternalServerException {
		Model.set(StudyParticipation.class, collection, this._id, "pstatus", newstatus);
	}
       
    
    public static void delete(MidataId studyId, MidataId partId) throws InternalServerException {	
		Model.delete(StudyParticipation.class, collection, CMaps.map("_id", partId).map("study", studyId));
	}
    
    public void setOwnerName(String ownerName) {
    	super.setOwnerName(ownerName);
		this.ownerName = ownerName;
	}
    
    public String getOwnerName() {
		return ownerName;
	}

}
