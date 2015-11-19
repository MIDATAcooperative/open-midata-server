package models.enums;

/**
 * Status of the study regarding participant search
 *
 */
public enum ParticipantSearchStatus {
	
   /**
    * Study does not yet search for participants
    */
   PRE,
   
   /**
    * Study is currently searching for participants
    */
   SEARCHING,
   
   /**
    * Study participant search has been finished
    */
   CLOSED
}
