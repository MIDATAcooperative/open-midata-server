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
	   return this != PROD;
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
		return this == LOCAL;
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
	
	public Set<UserFeature> defaultRequirementsPortalLogin(UserRole role) {
		if (this == PERFTEST) return EnumSet.of(UserFeature.EMAIL_ENTERED);
		if (role != UserRole.MEMBER) return EnumSet.of(UserFeature.EMAIL_VERIFIED, UserFeature.ADMIN_VERIFIED);
		if (this == TEST || this == DEMO || this == PROD) {
		   return EnumSet.of(UserFeature.EMAIL_VERIFIED, UserFeature.ADMIN_VERIFIED);
		}
		return EnumSet.of(UserFeature.EMAIL_VERIFIED);
	}
	
    public Set<UserFeature> defaultRequirementsOAuthLogin(UserRole role) {
    	if (this == PERFTEST) return EnumSet.of(UserFeature.EMAIL_ENTERED);
    	if (role != UserRole.MEMBER) return EnumSet.of(UserFeature.EMAIL_VERIFIED, UserFeature.ADMIN_VERIFIED);
    	if (this == TEST || this == DEMO) {
 		   return EnumSet.of(UserFeature.EMAIL_ENTERED, UserFeature.ADMIN_VERIFIED);
 		}
    	return EnumSet.of(UserFeature.EMAIL_ENTERED);
	}
}
