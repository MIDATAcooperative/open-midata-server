/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package models.enums;

import java.util.Date;

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
	AUTH2FACTOR,
	
	/**
	 * two factor authentication needs to be setup
	 */
	AUTH2FACTORSETUP,
	
	/**
	 * A valid licence is required
	 */
	VALID_LICENCE;
	
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
			case PHONE_VERIFIED: return user.mobileStatus != null && user.mobileStatus == EMailStatus.VALIDATED;
			case ADDRESS_ENTERED: return user.city != null && user.city.length() > 0 && user.zip != null && user.zip.length() > 0 && user.address1 != null;
			case ADDRESS_VERIFIED: return user.agbStatus.equals(ContractStatus.SIGNED);
			case PASSPORT_VERIFIED: return false;
			case MIDATA_COOPERATIVE_MEMBER: return user.contractStatus.equals(ContractStatus.SIGNED);
			case ADMIN_VERIFIED: return user.status.equals(UserStatus.ACTIVE);
			case PASSWORD_SET: return user.password != null && (user.flags==null || !user.flags.contains(AccountActionFlags.CHANGE_PASSWORD));
			case BIRTHDAY_SET: 
				if (!(user instanceof Member)) return true;
				Member member = (Member) user;
				return member.birthday != null && member.birthday.before(new Date(System.currentTimeMillis()-60l*60l*24l*365l*3l));
			case NEWEST_TERMS_AGREED:
				InstanceConfig ic = InstanceConfig.getInstance();
				return user.termsAgreed != null && user.termsAgreed.contains(ic.getTermsOfUse(user.role));
			case NEWEST_PRIVACY_POLICY_AGREED:
				ic = InstanceConfig.getInstance();
				return user.termsAgreed != null && user.termsAgreed.contains(ic.getPrivacyPolicy(user.role));
			case AUTH2FACTOR:
				if (user.authType == null || user.authType.equals(SecondaryAuthType.NONE)) return true;
				// AUTH2FACTOR is handeled outside. If no phone is present this can be skipped. Use PHONE_ENTERED to force phone number to be present.
				return user.mobile == null && user.phone == null;
			case AUTH2FACTORSETUP:				
				if (user.authType == null || (user.authType.equals(SecondaryAuthType.SMS) && user.mobile == null)) return false;
				return true;
			case VALID_LICENCE:
				return true;
		}
		return false;
	}
}
