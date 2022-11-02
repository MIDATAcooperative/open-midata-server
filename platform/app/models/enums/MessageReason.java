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

public enum MessageReason {

	/**
	 * Message sent upon registration of a new user
	 */
	REGISTRATION,
	
	/**
	 * Message send upon login
	 */
	LOGIN,
	
	/**
	 * Message sent upon registration by another user
	 */
	REGISTRATION_BY_OTHER_PERSON, 
	
	/**
	 * Message sent upon first use of app 
	 */
	FIRSTUSE_ANYUSER,
	
	/**
	 * Message sent upon first use of app of existing user
	 */
	FIRSTUSE_EXISTINGUSER,
	
	/**
	 * Message sent if user withdraws consent from a service (also on account deletion)
	 */
	SERVICE_WITHDRAW,
	
	/**
	 * Message sent for a proposed consent to a non MIDATA user consent owner
	 */
	CONSENT_REQUEST_OWNER_INVITED,
	
	/**
	 * Message sent for a proposed consent to an existing MIDATA user consent owner
	 */
	CONSENT_REQUEST_OWNER_EXISTING,
	
	/**
	 * Message sent for a proposed consent to a non MIDATA user authorized by consent
	 */
	CONSENT_REQUEST_AUTHORIZED_INVITED,
	
	/**
	 * Message sent for a proposed consent to an existing MIDATA user authorized by consent
	 */
	CONSENT_REQUEST_AUTHORIZED_EXISTING,
	
	/**
	 * Message sent to consent owner upon confirmation of consent
	 */
	CONSENT_CONFIRM_OWNER,
	
	/**
	 * Message sent to authorized person upon confirmation of consent
	 */
	CONSENT_CONFIRM_AUTHORIZED,
	
	/**
	 * Message send to consent owner upon rejection of consent
	 */
	CONSENT_REJECT_OWNER,
	
	/**
	 * Message send to authorized person upon rejection of consent
	 */
	CONSENT_REJECT_AUTHORIZED,
	
	/**
	 * Message sent because account has been unlocked by admin
	 */
	ACCOUNT_UNLOCK,
	
	/**
	 * Account email has been changed (sent to old address)
	 */
	EMAIL_CHANGED_OLDADDRESS,
	
	/**
	 * Account email has been changed (sent to new address)
	 */
	EMAIL_CHANGED_NEWADDRESS,
	
	/**
	 * User has forgotten his password
	 */
	PASSWORD_FORGOTTEN,
	
	/**
	 * Users private key has been recovered
	 */
	USER_PRIVATE_KEY_RECOVERED,
	
	/**
	 * A resource has been changed
	 */
	RESOURCE_CHANGE,
	
	/**
	 * A FHIR message should be processed
	 */
	PROCESS_MESSAGE
}
