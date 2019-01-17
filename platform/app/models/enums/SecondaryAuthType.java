package models.enums;

/**
 * Which type of two factor authentication should be used?
 * @author alexander
 *
 */
public enum SecondaryAuthType {

	/**
	 * Do not use 2FA
	 */
	NONE,
	
	/**
	 * Use SMS for two factor authentication
	 */
	SMS
}
