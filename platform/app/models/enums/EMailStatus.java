package models.enums;

/**
 * status of an email address used in MIDATA
 *
 */
public enum EMailStatus {
	
	  /**
	   * email address is new
	   */
	  UNVALIDATED,
	  
	  /**
	   * email address has been validated
	   */
	  VALIDATED,
	  
	  /**
	   * email address has been rejected
	   */
	  REJECTED,
	  
	  /**
	   * email address validated by external party
	   */
	  EXTERN_VALIDATED
	}