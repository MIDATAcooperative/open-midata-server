package models;

import java.util.List;
import java.util.Set;

import models.enums.ParticipationStatus;

import org.bson.types.ObjectId;

public class StudyParticipation extends Model {
	
	private static final String collection = "participation";

	public ObjectId member; //member that is related to a study
	public ObjectId study; //study that the member is related to
	public ParticipationStatus status; //how is the member related to the study?
	public String group; // If study has multiple separate groups of participants
	public ObjectId recruiter; // if member has been recruited through someone (by entering a participation code)
	public Set<ObjectId> providers; // (Optional) List of healthcare providers monitoring the member for this study.
	public List<History> history; // History of participation process
	public Set<ObjectId> shared; // Records of member shared for this study
}
