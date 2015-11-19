package models.enums;

/**
 * Status of execution of a study
 *
 */
public enum StudyExecutionStatus {
	
	/**
	 * Study execution has not started yet.
	 */
	PRE,
	
	/**
	 * Study is currently running.
	 */
	RUNNING,
	
	/**
	 * The study has been done.
	 */
	FINISHED,
	
	/**
	 * The study has been aborted.
	 */
	ABORTED
}
