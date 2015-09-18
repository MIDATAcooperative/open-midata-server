package models;

import org.bson.types.ObjectId;



public class StudyRelated extends Consent {

	public ObjectId study;
	
	public void add() throws ModelException {
		Model.insert(collection, this);	
	}
}
