package models;

import java.util.Set;

import models.enums.ConsentStatus;
import models.enums.ConsentType;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;


/**
 * A consent that shares data from a study to a group of study participants.
 *
 */
public class StudyRelated extends Consent {

	public MidataId study;
	public String group;
	
	public StudyRelated() {
		this.type = ConsentType.STUDYRELATED;
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static Set<StudyRelated> getActiveByOwnerGroupAndStudy(MidataId owner, String group, MidataId studyId, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.map("owner", owner).map("type", ConsentType.STUDYRELATED).mapNotEmpty("group", group).map("study", studyId).map("status", ConsentStatus.ACTIVE), fields);
	}
	
	public static Set<StudyRelated> getActiveByAuthorizedGroupAndStudy(MidataId authorized, Set<String> group, Set<MidataId> studyId, Set<String> fields, long since) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.map("authorized", authorized).map("type", ConsentType.STUDYRELATED).mapNotEmpty("group", group).map("study", studyId).map("status", ConsentStatus.ACTIVE).map("dataupdate", CMaps.map("$gte", since)), fields);
	}
	
	public static Set<StudyRelated> getByStudy(MidataId studyId, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.map("type", ConsentType.STUDYRELATED).map("study", studyId), fields);
	}
	
	public static void deleteByStudyAndParticipant(MidataId studyId, MidataId partId) throws InternalServerException {	
		Model.delete(StudyRelated.class, collection, CMaps.map("_id", partId).map("study", studyId));
	}
	
}
