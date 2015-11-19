package models.enums;

/**
 * Status of validation process for a study
 *
 */
public enum StudyValidationStatus {
	
	/**
	 * Validation has not yet been requested
	 */
	DRAFT,
	
	/**
	 * Study validation is currently in progress
	 */
	VALIDATION,
	
	/**
	 * Study has been successfully validated.
	 */
	VALIDATED,
	
	/**
	 * Study has been rejected by MIDATA
	 */
	REJECTED
}
