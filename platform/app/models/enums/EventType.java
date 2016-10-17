package models.enums;

/**
 * type of event to be logged in a history
 *
 */
public enum EventType {
	/**
	 * user entered a code
	 */
	CODE_ENTERED,
	
	/**
	 * user requested participation to a study
	 */
	PARTICIPATION_REQUESTED,
	
	/**
	 * user will not participate in a study
	 */
	NO_PARTICIPATION,
	
	/**
	 * study participation of a user has been rejected
	 */
	PARTICIPATION_REJECTED,
	
	/**
	 * study participation of a user has been approved
	 */
	PARTICIPATION_APPROVED,
	
	/**
	 * a group has been assigned to the study participant
	 */
	GROUP_ASSIGNED,
	
	/**
	 * validation of a study has been requested
	 */
	VALIDATION_REQUESTED,
	
	/**
	 * a study has been validated
	 */
	STUDY_VALIDATED,
	
	/**
	 * a study has been rejected
	 */
	STUDY_REJECTED,
	
	/**
	 * a study has started to search for participants
	 */
	PARTICIPANT_SEARCH_STARTED,
	
	/**
	 * a study has finished its search for participants
	 */
	PARTICIPANT_SEARCH_CLOSED,
	
	/**
	 * participation codes have been generated
	 */
	CODES_GENERATED,
	
	/**
	 * a study has been started
	 */
	STUDY_STARTED,
	
	/**
	 * a study has been finished
	 */
	STUDY_FINISHED,
	
	/**
	 * a study has been aborted
	 */
	STUDY_ABORTED,
	
	/**
	 * the required information for a study has been changed
	 */
	REQUIRED_INFORMATION_CHANGED,
	
	/**
	 * the setup of a study has been changed
	 */
	STUDY_SETUP_CHANGED,
	
	
	/**
	 * a user has requested Membership
	 */
	MEMBERSHIP_REQUEST,
	
	/**
	 * a user has changed his contact address
	 */
	CONTACT_ADDRESS_CHANGED
}
