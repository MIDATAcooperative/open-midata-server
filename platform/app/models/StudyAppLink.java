package models;

import java.util.Collections;
import java.util.Set;

import models.enums.StudyAppLinkType;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.sync.Instances;

/**
 * Link between a study and an application
 *
 */
public class StudyAppLink extends Model {
	
	private @NotMaterialized static final String collection = "studyapplink";
	
	public @NotMaterialized static final Set<String> ALL_MATERIALIZED = Collections.unmodifiableSet(Sets.create("_id", "studyId", "appId", "type", "validationResearch", "validationDeveloper", "usePeriod", "shareToStudy" ,"restrictRead", "studyGroup", "userId"));
	
	public @NotMaterialized static final Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "studyId", "appId", "type", "validationResearch", "validationDeveloper", "usePeriod", "shareToStudy" ,"restrictRead", "studyGroup", "study", "app", "userId", "userLogin"));
	
	/**
	 * which study is linked
	 */
	public MidataId studyId;
	
	public @NotMaterialized Study study;
			
	/**
	 * which application is linked
	 */
	public MidataId appId;
	
	public @NotMaterialized Plugin app;
			
	/**
	 * what type(s) of link
	 */
	public Set<StudyAppLinkType> type;
	
	/**
	 * has this link been validated by research?
	 */
	public StudyValidationStatus validationResearch;
	
	/**
	 * has this link been validated by the developer
	 */
	public StudyValidationStatus validationDeveloper;
	
	/**
	 * during which execution states is the link valid
	 */
	public Set<StudyExecutionStatus> usePeriod;
	
	public boolean shareToStudy;
	
	public boolean restrictRead;
	
	public String studyGroup;
	
	public MidataId userId;
	
	public @NotMaterialized String userLogin;
		

	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}

	public void delete() throws InternalServerException {				
		Model.delete(StudyAppLink.class, collection, CMaps.map("_id", this._id));
		Instances.cacheClear("studyapplink",  _id);
	}
	
	public void update() throws InternalServerException {
		this.setMultiple(collection, ALL_MATERIALIZED);
		Instances.cacheClear("studyapplink",  _id);
	}
	
	public static StudyAppLink getById(MidataId id) throws AppException {
		return Model.get(StudyAppLink.class, collection, CMaps.map("_id", id), ALL_MATERIALIZED);
	}
	
	public static Set<StudyAppLink> getByStudy(MidataId study) throws AppException {
		return Model.getAll(StudyAppLink.class, collection, CMaps.map("studyId", study), ALL_MATERIALIZED);
	}
	
	public static Set<StudyAppLink> getByApp(MidataId app) throws AppException {
		return Model.getAll(StudyAppLink.class, collection, CMaps.map("appId", app), ALL_MATERIALIZED);
	}
	
	public boolean isConfirmed() {
		return validationDeveloper.equals(StudyValidationStatus.VALIDATED) && validationResearch.equals(StudyValidationStatus.VALIDATED);
	}
		
}
