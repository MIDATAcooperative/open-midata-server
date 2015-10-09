package models;

import java.util.Date;
import java.util.Set;

import models.enums.ParticipationCodeStatus;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.ModelException;

public class ParticipationCode extends Model {
	
	private static final String collection = "codes";
	
	public String code; // unique code that needs to be entered by member to participate in study
	public ObjectId study; // references Study.	study this code belongs to
	public ObjectId recruiter; // references User. recruiter who owns this code and may give this code to someone
	public String recruiterName; // replication of recruiter name
	//public String recruiterName; //replicated for performance
	public String group; // If study has separate groups of participants
	public ParticipationCodeStatus status; 
	public Date createdAt;
	
	public static void add(ParticipationCode participationCode) throws ModelException {
		Model.insert(collection, participationCode);
	}
	
	public static Set<ParticipationCode> getByStudy(ObjectId study) throws ModelException {
		return Model.getAll(ParticipationCode.class, collection, CMaps.map("study", study), Sets.create("code", "group", "recruiter", "recruiterName", "status", "study", "createdAt"));
	}
	
	public static ParticipationCode getByCode(String code) throws ModelException {
		return Model.get(ParticipationCode.class, collection, CMaps.map("code", code), Sets.create("code", "createdAt", "group", "recruiter", "recruiterName", "status", "study"));
	}
	
	public void setStatus(ParticipationCodeStatus newstatus) throws ModelException {
		Model.set(ParticipationCode.class, collection, this._id, "status", newstatus);
	}
}
