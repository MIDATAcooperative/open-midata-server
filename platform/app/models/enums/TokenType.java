/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	OTP_OTHER;
	
	public boolean isReusableFor(TokenType other) {
		if (this == other) return true;
		if (this == WELCOME_MAIL && (other == OTP_MAIL || other == PWRESET_MAIL)) return true;		
		return false;
	}
}
