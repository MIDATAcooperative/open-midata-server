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

package utils.audit;

import models.Consent;
import models.MidataId;
import models.Study;
import models.User;
import models.enums.AuditEventType;
import utils.exceptions.InternalServerException;

public class AuditEventBuilder {
	private AuditEventType type;
	private MidataId app;
	private User who;
	private User modifiedUser;
	private Consent consent;
	private String message;
	private Study study;
	
	public AuditEventType getType() {
		return type;
	}
		
	
	public MidataId getApp() {
		return app;
	}
	
	public AuditEventBuilder withApp(MidataId app) {
		this.app = app;
		return this;
	}
	
	public User getActorUser() {
		return who;
	}
	
	public AuditEventBuilder withActorUser(User who) {
		this.who = who;
		return this;
	}
	
	public AuditEventBuilder withActorUser(MidataId who) throws InternalServerException {		
		this.who = User.getById(who, User.ALL_USER);
		return this;
	}
	
	public User getModifiedUser() {
		return modifiedUser;
	}
	
	public AuditEventBuilder withModifiedUser(User modifiedUser) {
		this.modifiedUser = modifiedUser;
		return this;
	}
	
	public AuditEventBuilder withModifiedUser(MidataId modifiedUser) throws InternalServerException {		
		this.modifiedUser = User.getById(modifiedUser, User.ALL_USER);
		return this;
	}
	
	public Consent getConsent() {
		return consent;
	}
	
	public AuditEventBuilder withConsent(Consent consent) {
		this.consent = consent;
		return this;
	}
	
	public String getMessage() {
		return message;
	}
	
	public AuditEventBuilder withMessage(String message) {
		this.message = message;
		return this;
	}
	
	public Study getStudy() {
		return study;
	}
	
	public AuditEventBuilder withStudy(Study study) {
		this.study = study;
		return this;
	}
	
	public AuditEventBuilder withStudy(MidataId studyId) throws InternalServerException {
		this.study = Study.getById(studyId, Study.ALL);
		return this;
	}
	
	public static AuditEventBuilder withType(AuditEventType type) {
		AuditEventBuilder builder = new AuditEventBuilder();
		builder.type = type;
		return builder;
	}
}
