package models.enums;

/**
 * What should be done when a participant does no longer want to participate
 * @author alexander
 *
 */
public enum ProjectLeavePolicy {

	/**
	 * data is frozen
	 */
	FREEZE,
	
	/**
	 * participation is expired
	 */
	REJECT,
	
	/**
	 * participation is deleted
	 */
	DELETE
		
}
