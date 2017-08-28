package models.enums;

public enum MessageReason {

	/**
	 * Message sent upon registration of a new user
	 */
	REGISTRATION,
	
	/**
	 * Message sent upon first use of app 
	 */
	FIRSTUSE_ANYUSER,
	
	/**
	 * Message sent upon first use of app of existing user
	 */
	FIRSTUSE_EXISTINGUSER,
	
	/**
	 * Message sent for a proposed consent to a non MIDATA user consent owner
	 */
	CONSENT_REQUEST_OWNER_INVITED,
	
	/**
	 * Message sent for a proposed consent to an existing MIDATA user consent owner
	 */
	CONSENT_REQUEST_OWNER_EXISTING,
	
	/**
	 * Message sent for a proposed consent to a non MIDATA user authorized by consent
	 */
	CONSENT_REQUEST_AUTHORIZED_INVITED,
	
	/**
	 * Message sent for a proposed consent to an existing MIDATA user authorized by consent
	 */
	CONSENT_REQUEST_AUTHORIZED_EXISTING,
	
	/**
	 * Message sent to consent owner upon confirmation of consent
	 */
	CONSENT_CONFIRM_OWNER,
	
	/**
	 * Message sent to authorized person upon confirmation of consent
	 */
	CONSENT_CONFIRM_AUTHORIZED,
	
	/**
	 * Message send to consent owner upon rejection of consent
	 */
	CONSENT_REJECT_OWNER,
	
	/**
	 * Message send to authorized person upon rejection of consent
	 */
	CONSENT_REJECT_AUTHORIZED,
	
	/**
	 * Message sent because account has been unlocked by admin
	 */
	ACCOUNT_UNLOCK
}
