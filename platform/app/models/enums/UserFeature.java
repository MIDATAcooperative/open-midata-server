package models.enums;

import models.User;

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
	ADMIN_VERIFIED;
		
	/**
	 * Does a user satisfy this feature?
	 * @param user
	 * @return
	 */
	public boolean isSatisfiedBy(User user) {
		switch (this) {
			case EMAIL_ENTERED: return true;
			case EMAIL_VERIFIED: return user.emailStatus.equals(EMailStatus.VALIDATED);
			case PHONE_ENTERED: return user.mobile != null && user.mobile.length() > 0;
			case PHONE_VERIFIED: return false;
			case ADDRESS_ENTERED: return user.city != null && user.city.length() > 0 && user.zip != null && user.zip.length() > 0 && user.address1 != null;
			case ADDRESS_VERIFIED: return user.agbStatus.equals(ContractStatus.SIGNED);
			case PASSPORT_VERIFIED: return false;
			case MIDATA_COOPERATIVE_MEMBER: return user.contractStatus.equals(ContractStatus.SIGNED);
			case ADMIN_VERIFIED: return user.status.equals(UserStatus.ACTIVE);
		}
		return false;
	}
}
