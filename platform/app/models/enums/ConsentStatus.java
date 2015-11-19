package models.enums;

/**
 * status of a consent
 *
 */
public enum ConsentStatus {
	
	/**
	 * the consent is currently active
	 */
   ACTIVE,
   
   /**
    * the consent was not created by the member and therefore needs confirmation by him
    */
   UNCONFIRMED,
   
   /**
    * the consent is no longer active. it has expired.
    */
   EXPIRED,
   
   /**
    * the consent has been rejected by the member
    */
   REJECTED
}
