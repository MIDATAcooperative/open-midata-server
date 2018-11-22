package models.enums;

/**
 * Action flags for login
 *
 */
public enum AccountActionFlags {

	/**
	 * Update of FHIR Patient Record required
	 */
	UPDATE_FHIR,
	
	/**
	 * Recovery of account private key
	 */
	KEY_RECOVERY
}