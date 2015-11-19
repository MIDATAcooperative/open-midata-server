package models.enums;

/**
 * General interest of a MIDATA member to participate in a study
 *
 */
public enum ParticipationInterest {
	
  /**
   * MIDATA member is generally not interested in study participation
   */
  NONE,
  
  /**
   * MIDATA member is generally interested to participate in a study
   */
  ALL,
  
  /**
   * MIDATA member is interested to participate in studies with specific topics
   */
  SOME,
  
  /**
   * MIDATA member has not selected any preference about study participation yet.
   */
  UNSET
}
