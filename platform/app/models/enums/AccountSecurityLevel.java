package models.enums;

/**
 * Security level of a user account
 *
 */
public enum AccountSecurityLevel {
	/**
	 *  Account is not protected by keypair
	 */
	NONE,            
	
	/**
	 * Account is protected by key in keystore
	 */
	KEY,             
	
	/**
	 * Account is protected by key in keystore and passphrase
	 */
	KEY_PASSPHRASE,  
	
	/**
	 * Account is protected by externally stored keyfile (currently not supported)
	 */
	KEY_FILE         	
}
