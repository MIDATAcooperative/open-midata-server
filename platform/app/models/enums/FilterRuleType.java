package models.enums;

/**
 * Study participant search rules may search for charcteristics of the user or a record of the user
 *
 */
public enum FilterRuleType {
	
  /**
   * filter rule selects records of users
   */
  RECORD, 
  
  /**
   * filter rule selects users
   */
  USER
}
