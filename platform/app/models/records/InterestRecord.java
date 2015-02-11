package models.records;

import java.util.Set;

import org.bson.types.ObjectId;


public class InterestRecord {

	public Set<ObjectId> studyKeywords; // Keywords for studies
	public Set<ObjectId> tags; // Tags describing account
}
