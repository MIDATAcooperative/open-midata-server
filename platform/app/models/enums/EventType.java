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

/**
 * type of event to be logged in a history
 *
 */
public enum EventType {
	/**
	 * user entered a code
	 */
	CODE_ENTERED,
	
	/**
	 * user requested participation to a study
	 */
	PARTICIPATION_REQUESTED,
	
	/**
	 * user will not participate in a study
	 */
	NO_PARTICIPATION,
	
	/**
	 * study participation of a user has been rejected
	 */
	PARTICIPATION_REJECTED,
	
	/**
	 * study participation of a user has been approved
	 */
	PARTICIPATION_APPROVED,
	
	/**
	 * a group has been assigned to the study participant
	 */
	GROUP_ASSIGNED,
	
	/**
	 * validation of a study has been requested
	 */
	VALIDATION_REQUESTED,
	
	/**
	 * a study has been validated
	 */
	STUDY_VALIDATED,
	
	/**
	 * a study has been rejected
	 */
	STUDY_REJECTED,
	
	/**
	 * a study has started to search for participants
	 */
	PARTICIPANT_SEARCH_STARTED,
	
	/**
	 * a study has finished its search for participants
	 */
	PARTICIPANT_SEARCH_CLOSED,
	
	/**
	 * participation codes have been generated
	 */
	CODES_GENERATED,
	
	/**
	 * a study has been started
	 */
	STUDY_STARTED,
	
	/**
	 * a study has been finished
	 */
	STUDY_FINISHED,
	
	/**
	 * a study has been aborted
	 */
	STUDY_ABORTED,
	
	/**
	 * the required information for a study has been changed
	 */
	REQUIRED_INFORMATION_CHANGED,
	
	/**
	 * the setup of a study has been changed
	 */
	STUDY_SETUP_CHANGED,
	
	
	/**
	 * a user has requested Membership
	 */
	MEMBERSHIP_REQUEST,
	
	/**
	 * a user has changed his contact address
	 */
	CONTACT_ADDRESS_CHANGED,
	
	/**
	 * a contract has been send to a user
	 */
	CONTRACT_SEND,
	
	/**
	 * an admin changed the user account status
	 */
	ADMIN_ACCOUNT_CHANGE,
	
	/**
	 * the account has been marked as deleted
	 */
	ACCOUNT_DELETED,
	
	/**
	 * a comment from the administrator
	 */
	INTERNAL_COMMENT,
	
	/**
	 * a user agreed to terms of use
	 */
	TERMS_OF_USE_AGREED
}
