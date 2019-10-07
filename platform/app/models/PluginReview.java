package models;

import java.util.Date;
import java.util.List;
import java.util.Set;

import models.enums.AppReviewChecklist;
import models.enums.ReviewStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;

/**
 * Review of an application
 *
 */
public class PluginReview extends Model {

	@NotMaterialized
	private static final String collection = "pluginreviews";
	
	public @NotMaterialized final static Set<String> ALL  = 
			 Sets.create("_id", "pluginId", "check", "timestamp", "userId", "userLogin", "comment", "status");
	
	/**
	 * Plugin that has been reviewed
	 */
	public MidataId pluginId;
	
	/**
	 * Type of review that has been done
	 */
	public AppReviewChecklist check;
	
	/**
	 * Time/Date of review
	 */
	public Date timestamp;
	
	/**
	 * Id of user who performed the review
	 */
	public MidataId userId;
	
	/**
	 * Login of user who performed the review
	 */
	public String userLogin;
	
	/**
	 * Reviewers comment
	 */
	public String comment;
	
	/**
	 * Status of review
	 */
	public ReviewStatus status;
	
	/**
	 * Get all reviews for a specific application 
	 * @param pluginId id of application
	 * @return reviews sorted by timestamp ascending
	 * @throws AppException
	 */
	public static List<PluginReview> getReviews(MidataId pluginId) throws AppException {
		return Model.getAllList(PluginReview.class, collection, CMaps.map("pluginId", pluginId), ALL, 0, "timestmap", 1);
	}
	
	/**
	 * Get all reviews for a specific application and review type
	 * @param pluginId id of application
	 * @param check type of review
	 * @return reviews sorted by timestamp ascending
	 * @throws AppException
	 */
	public static List<PluginReview> getReviews(MidataId pluginId, AppReviewChecklist check) throws AppException {
		return Model.getAllList(PluginReview.class, collection, CMaps.map("pluginId", pluginId).map("check", check), ALL, 0, "timestmap", 1);
	}
	
	/**
	 * Add this review to the database	
	 */
	public void add() throws AppException {
		Model.insert(collection, this);
	}
	
	/**
	 * Mark this review as obsolete
	 * @throws AppException
	 */
	public void markObsolete() throws AppException {
		if (this.status == ReviewStatus.ACCEPTED) {
			this.status = ReviewStatus.OBSOLETE;
			Model.set(PluginReview.class, collection, _id, "status", this.status);
		}
	}
	
	
}
