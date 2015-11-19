package models;

import java.util.Set;

import models.enums.ConsentType;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.exceptions.InternalServerException;


/**
 * A consent that shares data from a study to a group of study participants.
 *
 */
public class StudyRelated extends Consent {

	public ObjectId study;
	public String group;
	
	public StudyRelated() {
		this.type = ConsentType.STUDYRELATED;
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static StudyRelated getByGroupAndStudy(String group, ObjectId studyId, Set<String> fields) throws InternalServerException {
		return Model.get(StudyRelated.class, collection, CMaps.map("type", ConsentType.STUDYRELATED).map("group", group).map("study", studyId), fields);
	}
	
	
}
