package models;

import java.util.Date;
import java.util.Set;

import models.enums.ParticipationCodeStatus;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

/**
 * data model for a study participation code.
 * Participation codes need to be remodelled.
 *
 */
public class ParticipationCode extends Model {
	
	private static final String collection = "codes";
	
	/**
	 * the code that needs to be entered 
	 */
	public String code; // unique code that needs to be entered by member to participate in study
	
	/**
	 * the id of the study this code belongs to
	 */
	public ObjectId study; // references Study.	study this code belongs to
	
	/**
	 * the id of the recruiter
	 */
	public ObjectId recruiter; // references User. recruiter who owns this code and may give this code to someone
	
	/**
	 * firstname and lastname of the recruiter
	 */
	public String recruiterName; // replication of recruiter name

	/**
	 * group of participants where the member should be added to
	 */
	public String group; // If study has separate groups of participants
	
	/**
	 * status of this code
	 */
	public ParticipationCodeStatus status;
	
	/**
	 * date of creation of this code
	 */
	public Date createdAt;
	
	public static void add(ParticipationCode participationCode) throws InternalServerException {
		Model.insert(collection, participationCode);
	}
	
	public static Set<ParticipationCode> getByStudy(ObjectId study) throws InternalServerException {
		return Model.getAll(ParticipationCode.class, collection, CMaps.map("study", study), Sets.create("code", "group", "recruiter", "recruiterName", "status", "study", "createdAt"));
	}
	
	public static ParticipationCode getByCode(String code) throws InternalServerException {
		return Model.get(ParticipationCode.class, collection, CMaps.map("code", code), Sets.create("code", "createdAt", "group", "recruiter", "recruiterName", "status", "study"));
	}
	
	public void setStatus(ParticipationCodeStatus newstatus) throws InternalServerException {
		Model.set(ParticipationCode.class, collection, this._id, "status", newstatus);
	}
}
