package models.enums;

/**
 * the type of a consent
 *
 */
public enum ConsentType {

  /**
   * a consent where one MIDATA member shares records with other MIDATA members
   */
  CIRCLE,
  
  /**
   * a consent where one MIDATA member shares records with a study
   */
  STUDYPARTICIPATION,
  
  /**
   * a consent where one MIDATA member shares records with a healthcare provider
   */
  HEALTHCARE,
  
  /**
   * a consent where a research organization shares records with participants of a study
   */
  STUDYRELATED,
  
  /**
   * a consent where a healthcare provider shares records with one of his patients
   */
  HCRELATED,
  
  /**
   * a consent where one MIDATA member shares records with a mobile app instance
   */
  EXTERNALSERVICE,
  
  /**
   * a consent that has been created by the system (for message delivery)
   */
  IMPLICIT
}
