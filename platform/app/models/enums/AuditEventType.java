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

import org.hl7.fhir.r4.model.AuditEvent.AuditEventAction;
import org.hl7.fhir.r4.model.Coding;


public enum AuditEventType {
	
	 
	USER_REGISTRATION(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-registration", "User registration"), AuditEventAction.C), 
	
	TRIED_USER_REREGISTRATION(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "tried-user-reregistration", "Attempted User reregistration"), AuditEventAction.E),
	
	NON_PERFECT_ACCOUNT_MATCH(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "non-perfect-account-match", "Non perfect account match during access from 3rd party"), AuditEventAction.E),
	
	USER_AUTHENTICATION(new Coding(System.DCM,"110114","User Authentication"), new Coding(System.DCM, "110122", "Login"), AuditEventAction.E),
	
	USER_PASSWORD_CHANGE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-password-change", "Password changed"), AuditEventAction.U),
	
	USER_PASSWORD_CHANGE_REQUEST(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-password-change-request", "Password change request"), AuditEventAction.E),
	
	USER_PASSPHRASE_CHANGE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-passphrase-change", "Passphrase changed"), AuditEventAction.U),
	
	USER_ADDRESS_CHANGE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-address-change", "User address changed"), AuditEventAction.U),
	
	USER_SETTINGS_CHANGE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-settings-change", "User settings changed"), AuditEventAction.U),
	
	USER_PHONE_CHANGE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-phone-change", "User settings changed"), AuditEventAction.U),
	
	USER_EMAIL_CHANGE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-email-change", "Email changed"), AuditEventAction.U),
	
	USER_BIRTHDAY_CHANGE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-birthday-change", "Birthday changed"), AuditEventAction.U),
	
	USER_GENDER_CHANGE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-gender-change", "Gender updated"), AuditEventAction.U),
	
	// Not done
	USER_STATUS_CHANGE_ACTIVE(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-status-change-active", "User status changed to active"), AuditEventAction.U),
	
	USER_STATUS_CHANGE_BLOCKED(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-status-change-blocked", "User status changed to blocked"), AuditEventAction.U),
	
	USER_EMAIL_CONFIRMED(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-email-confirmed", "User email confirmed"), AuditEventAction.U),
	
	USER_EMAIL_REJECTED(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-email-rejected", "User email rejected"), AuditEventAction.U),
	
	/**
	 * an admin changed the user account status
	 */
	USER_ACCOUNT_CHANGE_BY_ADMIN(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-account-change-by-admin", "User account changed by admin"), AuditEventAction.U),
	
	/**
	 * the account has been marked as deleted
	 */
	USER_ACCOUNT_DELETED(new Coding(System.DCM,"110110","Patient Record"), new Coding(System.MIDATA, "user-account-deleted", "User account deleted"), AuditEventAction.D),
	
	
	APP_FIRST_USE(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "app-first-use", "First use of application"), AuditEventAction.E),
	
	APP_REJECTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "app-rejected", "Application rejected"), AuditEventAction.E),
	
	APP_DELETED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "app-deleted", "Application consent deleted"), AuditEventAction.D),

	CONSENT_CREATE(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "consent-create", "Consent created"), AuditEventAction.C),
	
	CONSENT_PERSONS_CHANGE(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "consent-persons-change", "Consent persons changed"), AuditEventAction.U),
	
	// TODO
	CONSENT_CHANGE(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "consent-change", "Consent changed"), AuditEventAction.U),
	
	CONSENT_APPROVED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "consent-approved", "Consent approved"), AuditEventAction.U),
	
	CONSENT_REJECTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "consent-rejected", "Consent rejected"), AuditEventAction.U),
	
	CONSENT_DELETE(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "consent-deleted", "Consent deleted"), AuditEventAction.D),
	
    COMMUNICATION_APPROVED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "communication-approved", "Communication approved"), AuditEventAction.U),
	
	COMMUNICATION_REJECTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "communication-rejected", "Communication rejected"), AuditEventAction.U),
	
			
	/**
	 * user requested participation to a study
	 */
	STUDY_PARTICIPATION_REQUESTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-participation-requested", "Study participation requested"), AuditEventAction.C),
	
	/**
	 * user will not participate in a study
	 */
	STUDY_PARTICIPATION_MEMBER_REJECTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-participation-member-rejected", "Study participation rejected by member"), AuditEventAction.U),
	
	/**
	 * user will not participate in a study
	 */
	STUDY_PARTICIPATION_MEMBER_RETREAT(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-participation-member-retreat", "Account holder retreated from study"), AuditEventAction.U),
	
	
	/**
	 * study participation of a user has been rejected
	 */
	STUDY_PARTICIPATION_RESEARCH_REJECTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-participation-research-rejected", "Study participation rejected by research"), AuditEventAction.U),
	
	/**
	 * study participation of a user has been approved
	 */
	STUDY_PARTICIPATION_APPROVED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-participation-approved", "Study participation approved"), AuditEventAction.U),
	
	/**
	 * a group has been assigned to the study participant
	 */
	STUDY_PARTICIPATION_GROUP_ASSIGNED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-participation-group-assigned", "Study participation group assigned"), AuditEventAction.U),
	
	/**
	 * validation of a study has been requested
	 */
	STUDY_VALIDATION_REQUESTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-validation-requested", "Study validation requested"), AuditEventAction.U),
	
	/**
	 * a study has been validated
	 */
	STUDY_VALIDATED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-validated", "Study validated"), AuditEventAction.U),
	
	/**
	 * a study has been rejected
	 */
	STUDY_REJECTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-rejected", "Study rejected"), AuditEventAction.U),
	
	/**
	 * a study has started to search for participants
	 */
	STUDY_PARTICIPANT_SEARCH_STARTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-participant-search-started", "Study participant search started"), AuditEventAction.U),
	
	/**
	 * a study has finished its search for participants
	 */
	STUDY_PARTICIPANT_SEARCH_CLOSED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-participant-search-closed", "Study participant search closed"), AuditEventAction.U),
	
		
	/**
	 * a study has been started
	 */
	STUDY_STARTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-started", "Study started"), AuditEventAction.U),
	
	/**
	 * a study has been finished
	 */
	STUDY_FINISHED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-finished", "Study finished"), AuditEventAction.U),
	
	/**
	 * a study has been aborted
	 */
	STUDY_ABORTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-aborted", "Study aborted"), AuditEventAction.U),
	
	
	STUDY_DELETED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.DCM, "110105", "Entire Study has been deleted"), AuditEventAction.D),
			
	/**
	 * the setup of a study has been changed
	 */
	STUDY_SETUP_CHANGED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "study-setup-changed", "Study has been changed"), AuditEventAction.U),
	
	DATA_DELETION(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "data-deletion", "Data has been deleted"), AuditEventAction.D),
	
	DATA_EXPORT(new Coding(System.DCM,"110106","Export"), new Coding(System.MIDATA, "data-export", "Data exported"), AuditEventAction.R),
	
	DATA_IMPORT(new Coding(System.DCM,"110107","Import"), new Coding(System.MIDATA, "data-import", "Data import"), AuditEventAction.C),
	
	
	/**
	 * a contract has been send to a user
	 */
	CONTRACT_REQUESTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "contract-requested", "Contract requested by user"), AuditEventAction.E),
	
	/**
	 * a contract has been send to a user
	 */
	CONTRACT_SEND(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "contract-send", "Contract send to user"), AuditEventAction.E),
	
	/**
	 * a comment from the administrator
	 */
	INTERNAL_COMMENT(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "internal-comment", "Internal comment"), AuditEventAction.E),
	
	/**
	 * a user agreed to terms of use
	 */
	USER_TERMS_OF_USE_AGREED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "user-terms-of-use-agreed", "Agreed to terms of use"), AuditEventAction.E),
	
	USER_TERMS_OF_USE_NOTED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "user-terms-of-use-noted", "Noted terms of use"), AuditEventAction.E),
	
	AUDIT_LOG_USE(new Coding(System.DCM,"110101","Audit Log Used"), new Coding(System.MIDATA, "audit-log-used", "Audit Log Used"), AuditEventAction.R),
	
	REST_CREATE(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.HL7REST, "create", "Resource created"), AuditEventAction.C),
	
	REST_UPDATE(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.HL7REST, "update", "Resource updated"), AuditEventAction.U),
	
    REST_READ(new Coding(System.DCM,"110112","Query"), new Coding(System.HL7REST, "read", "Resource read"), AuditEventAction.R),
    
    REST_VREAD(new Coding(System.DCM,"110112","Query"), new Coding(System.HL7REST, "vread", "Resource version read"), AuditEventAction.R),
    
    REST_HISTORY(new Coding(System.DCM,"110112","Query"), new Coding(System.HL7REST, "history", "Resource history read"), AuditEventAction.R),
    
    REST_SEARCH(new Coding(System.DCM,"110112","Query"), new Coding(System.HL7REST, "search", "Searched for resources"), AuditEventAction.R),
	
	/**
	 * a user has searched for the Person record of another user
	 */
	USER_SEARCHED(new Coding(System.DCM,"110112","Query"), new Coding(System.MIDATA, "user-searched", "User searched"), AuditEventAction.R),
	
	WELCOME_SENT(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "welcome-sent", "Welcome mail sent"), AuditEventAction.E),
	
	EMAIL_SENT(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "email-sent", "Email sent"), AuditEventAction.E),
	
	SMS_SENT(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "sms-sent", "SMS sent"), AuditEventAction.E),
	
	RESTHOOK(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "resthook-called", "Resthook called"), AuditEventAction.E),
	
	SCRIPT_INVOCATION(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "script-invocation", "Script invoced"), AuditEventAction.E),
	
	ADDED_AS_TEAM_MEMBER(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "added-as-team-member", "Added as team member"), AuditEventAction.C),
	
	UPDATED_ROLE_IN_TEAM(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "updated-role-in-team", "Updated role in team"), AuditEventAction.U),
	
	REMOVED_FROM_TEAM(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "removed-from-team", "Removed from team"), AuditEventAction.D),
	
	APP_DEFINITION_CHANGED(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "app-definition-changed", "App Definition Changed"), AuditEventAction.U),
	
	SIGNATURE_FAILURE(new Coding(System.DCM,"110100","Application Activity"), new Coding(System.MIDATA, "signature-failure", "Signature failure"), AuditEventAction.E);
	
	private AuditEventAction action;
	private Coding type;
	private Coding subtype;
	
	AuditEventType(Coding type, Coding subtype, AuditEventAction action) {
		this.type = type;
		this.subtype = subtype;
		this.action = action;
	}
	
	public Coding getFhirType() {
		return type;
	}
	
	public Coding getFhirSubType() {
		return subtype;
	}
	
	public AuditEventAction getAction() {
		return action;
	}
	
	private static class System {
		public final static String DCM = "http://dicom.nema.org/resources/ontology/DCM";
		public final static String MIDATA = "http://midata.coop/codesystems/event-types";
		public final static String HL7REST = "http://hl7.org/fhir/restful-interaction";
    }
}
