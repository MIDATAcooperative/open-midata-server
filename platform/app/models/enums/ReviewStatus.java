package models.enums;

/**
 * Status of an application review
 *
 */
public enum ReviewStatus {

	/**
	 * App has passed the review
	 */
	ACCEPTED,
	
	/**
	 * Something needs to be changed
	 */
	NEEDS_FIXING,
	
	/**
	 * Changes have been performed after the review. Review needs to be repeated.
	 */
	OBSOLETE
}
