package models.enums;

/**
 * is a user able to rejoin a project
 *
 */
public enum RejoinPolicy {

	/**
	 * do not allow to rejoin a project
	 */
	NO_REJOIN,
	
	/**
	 * on rejoin delete last participation
	 */	
	DELETE_LAST
}
