package models;

import models.enums.ConsentType;

import org.bson.types.ObjectId;



public class StudyRelated extends Consent {

	public ObjectId study;
	
	public StudyRelated() {
		this.type = ConsentType.STUDYRELATED;
	}
	
	public void add() throws ModelException {
		Model.insert(collection, this);	
	}
}
