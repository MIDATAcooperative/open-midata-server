package models.enums;

import models.Member;
import models.User;
import utils.InstanceConfig;

/**
 * Requirements that a user must fulfill to do some action (like participate in a study)
 * @author alexander
 *
 */
public enum UserFeature {

	/**
	 * the user has entered his email address
	 */
	EMAIL_ENTERED,
	
	/**
	 * the email address has been verified
	 */
	EMAIL_VERIFIED,
	
	/**
	 * the user has entered his phone number
	 */
	PHONE_ENTERED,
	
	/**
	 * the phone number has been verified
	 */
	PHONE_VERIFIED,
	
	/**
	 * the user has entered his address
	 */
	ADDRESS_ENTERED,
	
	/**
	 * the address of the user has been verified
	 */
	ADDRESS_VERIFIED,
	
	/**
	 * the user has been identified by passport
	 */
	PASSPORT_VERIFIED,
	
	/**
	 * the user is member of the MIDATA cooperative
	 */
	MIDATA_COOPERATIVE_MEMBER,
	
	/**
	 * the user account has been unlocked by an MIDATA admin
	 */
	ADMIN_VERIFIED,
	
	/**
	 * the user account has a password
	 */
	PASSWORD_SET,
	
	/**
	 * the user account has a birthday
	 */
	BIRTHDAY_SET,
			
	/**
	 * the user has agreed to the newest version of terms 
	 */
	NEWEST_TERMS_AGREED,
		
	/**
	 * the user has agreed to the newest version of privacy policy
	 */
	NEWEST_PRIVACY_POLICY_AGREED,
	
	/**
	 * user has been verified with 2-factory-authentication
	 */
	AUTH2FACTOR;
	
	/**
	 * Does a user satisfy this feature?
	 * @param user
	 * @return
	 */
	public boolean isSatisfiedBy(User user) {
		switch (this) {
			case EMAIL_ENTERED: return true;
			case EMAIL_VERIFIED: return (user.emailStatus.equals(EMailStatus.VALIDATED) || user.emailStatus.equals(EMailStatus.EXTERN_VALIDATED));
			case PHONE_ENTERED: return user.mobile != null && user.mobile.length() > 0;
			case PHONE_VERIFIED: return false;
			case ADDRESS_ENTERED: return user.city != null && user.city.length() > 0 && user.zip != null && user.zip.length() > 0 && user.address1 != null;
			case ADDRESS_VERIFIED: return user.agbStatus.equals(ContractStatus.SIGNED);
			case PASSPORT_VERIFIED: return false;
			case MIDATA_COOPERATIVE_MEMBER: return user.contractStatus.equals(ContractStatus.SIGNED);
			case ADMIN_VERIFIED: return user.status.equals(UserStatus.ACTIVE);
			case PASSWORD_SET: return user.password != null;
			case BIRTHDAY_SET: return (!(user instanceof Member)) || ((Member) user).birthday != null;
			case NEWEST_TERMS_AGREED:
				InstanceConfig ic = InstanceConfig.getInstance();
				return user.termsAgreed != null && user.termsAgreed.contains(ic.getTermsOfUse());
			case NEWEST_PRIVACY_POLICY_AGREED:
				ic = InstanceConfig.getInstance();
				return user.termsAgreed != null && user.termsAgreed.contains(ic.getPrivacyPolicy());
			case AUTH2FACTOR:
				if (user.authType == null || user.authType.equals(SecondaryAuthType.NONE)) return true;
				// AUTH2FACTOR is handeled outside. If no phone is present this can be skipped. Use PHONE_ENTERED to force phone number to be present.
				return user.mobile == null && user.phone == null;
		}
		return false;
	}
}
