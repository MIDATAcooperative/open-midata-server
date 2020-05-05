package models.enums;

/**
 * How may be study be joined
 *
 */
public enum JoinMethod {

	/**
	 * Participant joins by installing an app
	 */
	APP,
	
	/**
	 * Participant joins by manually applying in the portal
	 */
	PORTAL,
	
	/**
	 * Participant is added by a member of the research team
	 */
	RESEARCHER,
	
	/**
	 * Participant is added using FHIR API
	 */
	API,
	
	/**
	 * Participant was proposed by an algorithm
	 */
	ALGORITHM,
	
	/**
	 * Participant joined by entering a participation code
	 */
	CODE,
	
	/**
	 * Participant joined by app provided participation code
	 */
	APP_CODE,
	
	/**
	 * Data is transferred from another study
	 */
	TRANSFER
	
}
