package models.enums;

/**
 * Type of platform installation
 *
 */
public enum InstanceType {

	/**
	 * Productive instance
	 */
	PROD,
	
	/**
	 * Test instance
	 */
	TEST,
	
	/**
	 * Demo instance
	 */
	DEMO,
	
	/**
	 * Local instance
	 */
	LOCAL;
		
	/**
	 * Users need status ACTIVE to log in
	 * @return 
	 */
	public boolean getUsersNeedValidation() {
	   return this == PROD;	
	}
	
	/**
	 * Debug functions like direct APS reads are allowed
	 * @return 
	 */
	public boolean getDebugFunctionsAvailable() {
	   return this != PROD;
	}
	
	/**
	 * Users may delete their account	
	 * @return 
	 */
	public boolean getAccountWipeAvailable() {
	   return this != PROD;
	}
	
	/**
	 * TRIAL ACCOUNT users may log into the platform
	 * @return 
	 */
	public boolean getTrialAccountsMayLogin() {
		return this != PROD;
	}
	
	/**
	 * Studies are immediately marked VALIDATED
	 * @return 
	 */
	public boolean getStudiesValidateAutomatically() {
		return this == TEST || this == DEMO;
	}
	
	/**
	 * Membership Requests are immediately granted
	 * @return 
	 */
	public boolean getAutoGrandMembership() {
		return this == TEST || this == DEMO;
	}
	
	/**
	 * Write log to file. 
	 * @return
	 */
	public boolean getLogToFile() {
		return this == LOCAL;
	}
}