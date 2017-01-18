package models;

import org.bson.types.ObjectId;

public class TypedMidataId {

	private MidataId midataId;
	private String type;
	public String name;
	
	public TypedMidataId(MidataId midataId, String type) {
		this.midataId = midataId;
		this.type = type;
	}
	
	public MidataId getMidataId() {
		return midataId;
	}
	
	public String getType() {
		return type;
	}
		
}
