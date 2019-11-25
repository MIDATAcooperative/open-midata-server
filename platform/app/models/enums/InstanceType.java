package models.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Type of platform installation
 *
 */
public enum InstanceType {

	/**
	 * Productive instance
	 */
	PROD,
	
	/**
	 * Test instance
	 */
	TEST,
	
	/**
	 * Demo instance
	 */
	DEMO,
	
	/**
	 * Local instance
	 */
	LOCAL,
	
	/**
	 * Performance test instance
	 */
	PERFTEST;
			
	
	/**
	 * Debug functions like direct APS reads are allowed
	 * @return 
	 */
	public boolean getDebugFunctionsAvailable() {
	   return this != PROD;
	}
	
	/**
	 * Users may delete their account	
	 * @return 
	 */
	public boolean getAccountWipeAvailable() {
	   return true; //this != PROD; Now allowed for all instances
	}
	
	/**
	 * TRIAL ACCOUNT users may log into the platform
	 * @return 
	 */
	public boolean getTrialAccountsMayLogin() {
		return this == LOCAL || this == PERFTEST;
	}
	
	/**
	 * Studies are immediately marked VALIDATED
	 * @return 
	 */
	public boolean getStudiesValidateAutomatically() {
		return this == TEST || this == DEMO || this == PERFTEST;
	}
	
	/**
	 * Membership Requests are immediately granted
	 * @return 
	 */
	public boolean getAutoGrandMembership() {
		return this == TEST || this == DEMO || this == PERFTEST;
	}
	
	/**
	 * Write log to file. 
	 * @return
	 */
	public boolean getLogToFile() {
		return this == LOCAL || this == TEST;
	}
	
	/**
	 * A developer may use his developer account to register new users
	 * @return
	 */
	public boolean developersMayRegisterTestUsers() {
		return this == TEST || this == LOCAL;
	}
	
	/**
	 * Send a mail to the MIDATA admin if a new user has been registered
	 * @return
	 */
	public boolean notifyAdminOnRegister() {
		return this == TEST || this == DEMO || this == PROD;
	}
	
	/**
	 * Users need to enter the confirmation code to complete registration.
	 * @return
	 */
	public boolean confirmationCodeRequired() {
		return this == PROD || this == LOCAL;
	}
	
	/**
	 * Record requests done by apps and plugins for debugging and performance optimizations
	 * @return
	 */
	public boolean doAppDeveloperStats() {
		return this == LOCAL || this == TEST || this == DEMO || this == PERFTEST;
	}
	
	/**
	 * Disable cross site scripting protection.
	 * @return
	 */
	public boolean disableCORSProtection() {
		return this == PERFTEST;
	}
	
	/**
	 * Do not send messages to users
	 * @return
	 */
	public boolean disableMessaging() {
		return this == PERFTEST;
	}
	
	/**
	 * Do not check if email address has been validated
	 * @return
	 */
	public boolean disableEMailValidation() {
		return this == PERFTEST;
	}
	
	/**
	 * Do not protect the background service key
	 * @return
	 */
	public boolean disableServiceKeyProtection() {
		return this == PERFTEST || this == LOCAL;
	}
	
	/**
	 * Do not use key protection on service user account
	 * @return
	 */
	public boolean simpleServiceKeyProtection() {
		return this == DEMO;
	}
		
	
	/**
	 * Automatically confirm changed consents on login from Midata API
	 * @return
	 */
	public boolean autoconfirmConsentsMidataApi() {
		return this == PERFTEST;
	}
	
	public Set<UserFeature> defaultRequirementsPortalLogin(UserRole role) {
		if (this == PERFTEST) return EnumSet.of(UserFeature.EMAIL_ENTERED);
		if (role != UserRole.MEMBER) return EnumSet.of(UserFeature.EMAIL_VERIFIED, UserFeature.ADMIN_VERIFIED, UserFeature.PASSWORD_SET, UserFeature.AUTH2FACTORSETUP, UserFeature.AUTH2FACTOR , UserFeature.NEWEST_PRIVACY_POLICY_AGREED, UserFeature.NEWEST_TERMS_AGREED);
		if (this == TEST || this == DEMO) {
		   return EnumSet.of(UserFeature.EMAIL_VERIFIED, UserFeature.ADMIN_VERIFIED, UserFeature.PASSWORD_SET, UserFeature.NEWEST_PRIVACY_POLICY_AGREED, UserFeature.NEWEST_TERMS_AGREED, UserFeature.AUTH2FACTORSETUP, UserFeature.AUTH2FACTOR);
		}
		if (this == PROD) return EnumSet.of(UserFeature.EMAIL_ENTERED, UserFeature.PASSWORD_SET, UserFeature.NEWEST_PRIVACY_POLICY_AGREED, UserFeature.NEWEST_TERMS_AGREED, UserFeature.AUTH2FACTORSETUP, UserFeature.AUTH2FACTOR);
		return EnumSet.of(UserFeature.EMAIL_VERIFIED, UserFeature.PASSWORD_SET, UserFeature.NEWEST_PRIVACY_POLICY_AGREED, UserFeature.NEWEST_TERMS_AGREED/*, UserFeature.AUTH2FACTORSETUP, UserFeature.AUTH2FACTOR*/);
	}
	
    public Set<UserFeature> defaultRequirementsOAuthLogin(UserRole role) {
    	if (this == PERFTEST) return EnumSet.of(UserFeature.EMAIL_ENTERED);
    	if (role != UserRole.MEMBER) return EnumSet.of(UserFeature.EMAIL_VERIFIED, UserFeature.ADMIN_VERIFIED, UserFeature.PASSWORD_SET, UserFeature.NEWEST_PRIVACY_POLICY_AGREED, UserFeature.NEWEST_TERMS_AGREED);
    	if (this == TEST || this == DEMO) {
 		   return EnumSet.of(UserFeature.EMAIL_ENTERED, UserFeature.ADMIN_VERIFIED, UserFeature.PASSWORD_SET, UserFeature.NEWEST_PRIVACY_POLICY_AGREED, UserFeature.NEWEST_TERMS_AGREED);
 		}
    	return EnumSet.of(UserFeature.EMAIL_ENTERED, UserFeature.PASSWORD_SET, UserFeature.NEWEST_PRIVACY_POLICY_AGREED, UserFeature.NEWEST_TERMS_AGREED);
	}
    
    public boolean is2FAMandatory(UserRole role) {    	
    	if (this == PERFTEST || this == LOCAL) return false;
    	if (this == TEST && role != UserRole.ADMIN) return false;
    	return role != UserRole.MEMBER;    	
    }
}
