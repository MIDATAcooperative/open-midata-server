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
	public boolean noBackshare;
	
	public StudyRelated() {
		this.type = ConsentType.STUDYRELATED;
	}
	
	public void add() throws InternalServerException {
		assertNonNullFields();
		Model.insert(collection, this);	
	}
	
	public static Set<StudyRelated> getActiveByOwnerGroupAndStudy(MidataId owner, String group, MidataId studyId, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.mapNotEmpty("owner", owner).map("type", ConsentType.STUDYRELATED).mapNotEmpty("group", group).map("study", studyId).map("status", ConsentStatus.ACTIVE), fields);
	}
	
	public static Set<StudyRelated> getActiveByOwnerGroupAndStudyPublic(MidataId owner, String group, MidataId studyId, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.mapNotEmpty("owner", owner).map("type", ConsentType.STUDYRELATED).mapNotEmpty("group", group).map("study", studyId).map("status", ConsentStatus.ACTIVE).map("noBackshare", CMaps.map("$ne", true)), fields);
	}
	
	public static Set<StudyRelated> getActiveByOwnerGroupAndStudyPrivate(MidataId owner, String group, MidataId studyId, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.mapNotEmpty("owner", owner).map("type", ConsentType.STUDYRELATED).mapNotEmpty("group", group).map("study", studyId).map("status", ConsentStatus.ACTIVE).map("noBackshare", true), fields);
	}
	
	public static Set<StudyRelated> getActiveByAuthorizedGroupAndStudy(MidataId authorized, Set<String> group, Set<MidataId> studyId, Set<MidataId> owners, Set<String> fields, long since) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.map("authorized", authorized).map("type", ConsentType.STUDYRELATED).mapNotEmpty("group", group).map("study", studyId).map("status", ConsentStatus.ACTIVE).mapNotEmpty("owner", owners).map("dataupdate", CMaps.map("$gte", since)), fields);
	}
	
	public static Set<StudyRelated> getActiveByAuthorizedGroupAndStudyPublic(MidataId authorized, Set<String> group, Set<MidataId> studyId, Set<MidataId> owners, Set<String> fields, long since) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.map("authorized", authorized).map("type", ConsentType.STUDYRELATED).mapNotEmpty("group", group).map("study", studyId).map("status", ConsentStatus.ACTIVE).mapNotEmpty("owner", owners).map("dataupdate", CMaps.map("$gte", since)).map("noBackshare", CMaps.map("$ne", true)), fields);
	}
	
	public static Set<StudyRelated> getActiveByAuthorizedAndIds(MidataId authorized, Set<MidataId> ids) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.map("authorized", authorized).map("type", ConsentType.STUDYRELATED).map("_id", ids).map("status", ConsentStatus.ACTIVE), Consent.SMALL);
	}
	
	public static Set<StudyRelated> getByStudy(MidataId studyId, Set<String> fields) throws InternalServerException {
		return Model.getAll(StudyRelated.class, collection, CMaps.map("type", ConsentType.STUDYRELATED).map("study", studyId).map("status", NOT_DELETED), fields);
	}
	
	public static void deleteByStudyAndParticipant(MidataId studyId, MidataId partId) throws InternalServerException {	
		Model.delete(StudyRelated.class, collection, CMaps.map("_id", partId).map("study", studyId));
	}
	
}
