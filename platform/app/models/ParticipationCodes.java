package models;

import models.enums.ParticipationCodeStatus;

import org.bson.types.ObjectId;

public class ParticipationCodes extends Model {
	
	private static final String collection = "codes";
	
	public String code; // unique code that needs to be entered by member to participate in study
	public ObjectId study; // references Study.	study this code belongs to
	public ObjectId recruiter; // references User. recruiter who owns this code and may give this code to someone
	public String group; // If study has separate groups of participants
	public ParticipationCodeStatus status; 
}
