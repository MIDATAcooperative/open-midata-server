package models.enums;

/**
 * Status of the participation of a MIDATA member in a study.
 *
 */
public enum ParticipationStatus {
	
	/**
	 * MIDATA member has been chosen as a candidate for the study by the platform
	 */
	MATCH, 
	
	/**
	 * MIDATA member requests to participate in the study.
	 */
    REQUEST,
    
    /**
     * MIDATA member has entered a participation code for the study.
     */
    CODE,
    
    /**
     * MIDATA member has been accepted as particpant
     */
	ACCEPTED,
	
	/**
	 * the MIDATA member rejected participation in the study.
	 */
    MEMBER_REJECTED,
    
    /**
     * The research organization rejected participation of the member in the study.
     */
    RESEARCH_REJECTED
}
