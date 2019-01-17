package utils.auth.auth2factor;

import models.enums.SecondaryAuthType;

/**
 * Factory for two factor authentication providers
 *
 */
public class Authenticators {

	/**
	 * Return authenticator of requested type
	 * @param authType
	 * @return
	 */
	public static Authenticator getInstance(SecondaryAuthType authType) {
		if (authType == SecondaryAuthType.SMS) return new SMSAuthenticator();
		return null;
	}
}
