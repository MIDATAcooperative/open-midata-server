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

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.CommunicationChannelUseStatus;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.JoinMethod;
import models.enums.ParticipationStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * A consent that shares data from a MIDATA member with a study.
 * 
 */
public class StudyParticipation extends Consent {
	
	/**
	 * this fields marks superclass field in Consent. This is intentional and cannot be removed easily.
	 */
	public String ownerName;
	
	public MidataId study; //study that the member is related to
	public String studyName; // replication of study name
	public ParticipationStatus pstatus; //how is the member related to the study?
	public String group; // If study has multiple separate groups of participants
	public MidataId recruiter; // if member has been recruited through someone (by entering a participation code)
	public String recruiterName; // replication of recruiter name
	public Set<MidataId> providers; // (Optional) List of healthcare providers monitoring the member for this study.
	
	public JoinMethod joinMethod;
	public CommunicationChannelUseStatus projectEmails;
	
	public @NotMaterialized final static Set<String> SMALL_WITH_GROUP = Sets.create(Consent.SMALL, "group");
	public @NotMaterialized final static Set<String> STUDY_EXTRA = Sets.create(Consent.FHIR, "ownerName","study","studyName","pstatus","group","joinMethod","projectEmails");
	
	//public int yearOfBirth;
	//public String country;
	//public Gender gender;
	
	public @NotMaterialized String partName;
			
	public StudyParticipation() {
		this.type = ConsentType.STUDYPARTICIPATION;
	}
	
	public static void add(StudyParticipation studyparticipation) throws InternalServerException {
		Model.insert(collection, studyparticipation);
	}
	
	public static Set<StudyParticipation> getAllByMember(MidataId member, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("owner", member).map("status", NOT_DELETED), fields);
	}
	
	public static Set<StudyParticipation> getAllActiveByMember(MidataId member, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("owner", member).map("pstatus", ParticipationStatus.ACCEPTED), fields);
	}
	
	public static Set<StudyParticipation> getParticipantsByStudy(MidataId study, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.RESEARCH_REJECTED, ParticipationStatus.MEMBER_RETREATED)), fields);
	}
	
	public static List<StudyParticipation> getParticipantsByStudy(MidataId study, Map<String, Object> properties, Set<String> fields, int limit) throws InternalServerException {
		return Model.getAllList(StudyParticipation.class, collection, CMaps.map(properties).map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("status", NOT_DELETED), fields, limit);
	}
	
	public static long countParticipantsByStudy(MidataId study, Map<String, Object> properties) throws InternalServerException {
		return Model.count(StudyParticipation.class, collection, CMaps.map(properties).map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("status", NOT_DELETED));
	}
	
	public static Set<StudyParticipation> getParticipantsByStudyAndGroup(MidataId study, String group, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.RESEARCH_REJECTED, ParticipationStatus.MEMBER_RETREATED)), fields);
	}
	
	public static Set<StudyParticipation> getActiveParticipantsByStudy(MidataId study, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.MEMBER_RETREATED)), fields);
	}
	
	public static Set<StudyParticipation> getActiveParticipantsByStudyAndGroup(MidataId study, String group, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).mapNotEmpty("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST)), fields);
	}
	
	public static Set<StudyParticipation> getActiveOrRetreatedParticipantsByStudyAndGroup(MidataId study, String group, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.MEMBER_RETREATED)), fields);
	}
	
	public static List<StudyParticipation> getActiveOrRetreatedParticipantsByStudyAndGroupsAndParticipant(Set<MidataId> study, Set<String> group, MidataId member, Set<MidataId> owners, Set<String> fields, boolean alsoPseudonymized, long since, int maxReturn) throws InternalServerException {
		Map<String, Object> m = CMaps.map("type", ConsentType.STUDYPARTICIPATION).mapNotEmpty("study", study).mapNotEmpty("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.MEMBER_RETREATED)).map("authorized", member).mapNotEmpty("owner", owners).map("dataupdate", CMaps.map("$gte", since)).map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN));
		if (!alsoPseudonymized) m.put("ownerName", CMaps.map("$exists", false));
		return Model.getAllList(StudyParticipation.class, collection, m, fields, maxReturn);
	}
	
	public static Set<StudyParticipation> getActiveOrRetreatedParticipantsByStudyAndGroupsAndIds(Set<MidataId> study, Set<String> group, MidataId member, Set<MidataId> owners, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).mapNotEmpty("study", study).mapNotEmpty("group", group).map("pstatus", Sets.createEnum(ParticipationStatus.ACCEPTED, ParticipationStatus.REQUEST, ParticipationStatus.MEMBER_RETREATED)).map("authorized", member).mapNotEmpty("_id", owners).map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN)), fields);
	}
	
	public static StudyParticipation getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("_id", id).map("status", NOT_DELETED), fields);
	}
	
	public static StudyParticipation getByStudyAndMember(MidataId study, MidataId member, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("owner", member).map("status", NOT_DELETED), fields);
	}
	
	public static boolean existsByStudyAndMemberName(MidataId study, String name) throws InternalServerException {
		return Model.exists(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("study", study).map("ownerName", name).map("status", NOT_DELETED));
	}
	
	public static StudyParticipation getByStudyAndId(MidataId study, MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(StudyParticipation.class, collection, CMaps.map("type", ConsentType.STUDYPARTICIPATION).map("_id", id).map("study", study).map("status", NOT_DELETED), fields);
	}
	
	public static Set<StudyParticipation> getAllAuthorizedWithGroup(MidataId member, long since) throws InternalServerException {
		return Model.getAll(StudyParticipation.class, collection, CMaps.map("authorized", member).map("dataupdate", CMaps.map("$gte", since)), StudyParticipation.SMALL_WITH_GROUP);
	}
	
	public void setPStatus(ParticipationStatus newstatus) throws InternalServerException {
		Model.set(StudyParticipation.class, collection, this._id, "pstatus", newstatus);
		pstatus = newstatus;
	}
	
	public void setPStatus(ParticipationStatus newstatus, JoinMethod joinMethod) throws InternalServerException {
		this.pstatus = newstatus;
		this.joinMethod = joinMethod;
		this.setMultiple(collection, Sets.create("pstatus", "joinMethod"));		
	}
	
	public static void setManyGroup(Set<MidataId> ids, String group) throws InternalServerException {
		Model.setAll(StudyParticipation.class, collection, CMaps.map("_id", ids), "group", group);
	}
	
	public static void setManyStatus(Set<MidataId> ids, ParticipationStatus newStatus) throws InternalServerException {
		Model.setAll(StudyParticipation.class, collection, CMaps.map("_id", ids), "pstatus", newStatus);
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
