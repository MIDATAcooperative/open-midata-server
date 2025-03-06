package models.enums;

/**
 * Type of password reset token sent
 */
public enum TokenType {

	/**
	 * link or OTP was included in welcome mail
	 */
	WELCOME_MAIL,
	
	/**
	 * link was included in password reset mail
	 */
	PWRESET_MAIL,
	
	/**
	 * link was included in an email change notification
	 */
	MAILCHANGE_NOTIFICATION,
	
	/**
	 * One time password was sent by email
	 */
	OTP_MAIL,
	
	/**
	 * One time password was sent by SMS
	 */
	OTP_SMS,
	
	/**
	 * One time password was sent using another communication mechanism (letter, telephone call, API)
	 */
	OTP_OTHER
}
