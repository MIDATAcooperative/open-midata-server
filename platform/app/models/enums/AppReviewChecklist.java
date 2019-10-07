package models.enums;

public enum AppReviewChecklist {

	/**
	 * Application concept as been reviewed
	 */
	CONCEPT,
	
	/**
	 * Data model has been reviewed
	 */
	DATA_MODEL,
	
	/**
	 * Access filter is correct and shares only necessary data
	 */
	ACCESS_FILTER,
	
	/**
	 * Queries are correct, contain all necessary restrictions
	 */
	QUERIES,
	
	/**
	 * App has a proper description 
	 */
	DESCRIPTION,
	
	/**
	 * App has acceptable icons
	 */
	ICONS,
	
	/**
	 * Mail texts are ok and available in all required languages
	 */
	MAILS,
	
	/**
	 * App is properly linked to projects 
	 */
	PROJECTS,
	
	/**
	 * Source code has been reviewed
	 */
	CODE_REVIEW,
	
	/**
	 * functional and ui tests have been formulated
	 */
	TEST_CONCEPT,
	
	/**
	 * tests have been run by 3rd party
	 */
	TEST_PROTOKOLL,
	
	/**
	 * contracts have been made
	 */
	CONTRACT
		
}
