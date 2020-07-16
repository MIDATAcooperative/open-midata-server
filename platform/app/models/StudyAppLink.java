package models;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import models.enums.LinkTargetType;
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
 * Link between an application and research projects or providers
 *
 */
public class StudyAppLink extends Model {
	
	private @NotMaterialized static final String collection = "studyapplink";
	
	public @NotMaterialized static final Set<String> ALL_MATERIALIZED = Collections.unmodifiableSet(Sets.create("_id", "linkTargetType", "studyId", "appId", "type", "validationResearch", "validationDeveloper", "usePeriod", "shareToStudy" ,"restrictRead", "studyGroup", "userId", "providerId", "active", "what", "identifier", "termsOfUse", "serviceAppId"));
	
	public @NotMaterialized static final Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "linkTargetType", "studyId", "appId", "type", "validationResearch", "validationDeveloper", "usePeriod", "shareToStudy" ,"restrictRead", "studyGroup", "study", "app", "userId", "userLogin", "providerId", "provider", "active", "what", "identifier", "termsOfUse", "serviceApp", "serviceAppId"));
	
	/**
	 * Type of link
	 */
	public LinkTargetType linkTargetType;
	
	/**
	 * An identifier for this link
	 */
	public String identifier;
	
	/**
	 * which study is linked (may be null)
	 */
	public MidataId studyId;
	
	/**
	 * the study that is linked (may be null)
	 */
	public @NotMaterialized Study study;
			
	/**
	 * which application is linked
	 */
	public MidataId appId;
	
	/**
	 * the application that is linked
	 */
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
	
	/**
	 * is this link currently active
	 */
	public boolean active;
	
	public boolean shareToStudy;
	
	public boolean restrictRead;
	
	public String studyGroup;
	
	/**
	 * which user account is linked
	 */
	public MidataId userId;
	
	/**
	 * which user account is linked
	 */	
	public @NotMaterialized String userLogin;
	
	/**
	 * which service is linked (may be null)
	 */
	public MidataId serviceAppId;

	/**
	 * service that is linked (may be null)
	 */
	public Plugin serviceApp;

	/**
	 * which provider is linked (may be null)
	 */
	public MidataId providerId;
	
	/**
	 * the provider that is linked
	 */
	public @NotMaterialized HealthcareProvider provider;
	
	/**
	 * the access filter to be used (may be null for default)
	 */
	public Map<String, Object> what;
	
	/**
	 * Terms of use (if not taken from research project)
	 */
	public String termsOfUse;
	
	/**
	 * Has been dynamically added during login
	 */
	public @NotMaterialized boolean dynamic;
		
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
	
	public Study getStudy() throws InternalServerException {
		if (this.study != null) return this.study;
		if (this.studyId == null) return null;
		this.study = Study.getById(this.studyId, Study.LINK_FIELDS);
		return this.study;
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
	
	public StudyAppLink() {}
	
	public StudyAppLink(MidataId studyId, MidataId appId) {		
		this.active = true;
		this.dynamic = true;
		this.appId = appId;
		this.studyId = studyId;
		this.linkTargetType=LinkTargetType.STUDY;
		this.type=EnumSet.of(StudyAppLinkType.OFFER_P, StudyAppLinkType.OFFER_EXTRA_PAGE, StudyAppLinkType.REQUIRE_P, StudyAppLinkType.OFFER_INLINE_AGB);
		this.usePeriod=EnumSet.allOf(StudyExecutionStatus.class);	
		this.validationDeveloper=StudyValidationStatus.VALIDATED;
		this.validationResearch=StudyValidationStatus.VALIDATED;
	}
		
}
