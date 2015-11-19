package models.enums;

/**
 * Status of a study participation code.
 *
 */
public enum ParticipationCodeStatus {
	
  /**
   * Code has not been used yet 
   */
   UNUSED,
   
   /**
    * Code has been given away but has not been entered by anyone.
    */
   SHARED,
   
   /**
    * Code has been used.
    */
   USED,
   
   /**
    * Code has been blocked and cannot be used any longer.
    */
   BLOCKED,
   
   /**
    * Code is infinitely reusable
    */
   REUSEABLE
}
