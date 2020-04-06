package models.enums;

/**
 * Type of information needed by a study from any participant 
 *
 */
public enum InformationType {
	
	/**
	 * Only a pseudonym, year of birth, gender and country
	 */
	RESTRICTED,
	
	/**
	 * All demographic information of the participant. (Non anonymous study)
	 */
	DEMOGRAPHIC,
	
	/**
	 * No account information included
	 */
	NONE
}
