package models.enums;

/**
 * Type of information used to describe a Study.
 *
 */
public enum InfoType {
	
   /**
    * Short Summary
    */
   SUMMARY,
   
   /**
    * What is this study doing?
    */
   DESCRIPTION,
   
   /**
	 * Link to project homepage
	 */
   HOMEPAGE,
   
   /**
    * Contact information
    */
   CONTACT,
   
   /**
    * Instructions for participants
    */
   INSTRUCTIONS,
   
    /**
     * What is the purpose of this research
     */
  PURPOSE,
  
    /**
     * Limitations on who should participate
     */
  AUDIENCE,
  
  /**
   * What is the timeframe for this study
   */
  DURATION,
  
   /**
    * State or region where this study is taking place
    */
  LOCATION,
  
  /**
   * What is the state of progression of the research taking place
   */
  PHASE,

  /**
   * Who is initiator of the research and who is legally responsible
   */
  SPONSOR, 

  /**
   * At which site are activities beeing conducted?
   */
  SITE, 

  /**
   * Information about equipment required by a participant
   */
  DEVICES,
  
  /**
   * Additional notes
   */
  COMMENT
}
