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

import java.util.Date;
import java.util.HashSet;

import models.Actor;
import models.Consent;
import models.MidataAuditEvent;
import models.MidataId;
import models.Study;
import models.User;
import models.enums.AuditEventType;
import utils.exceptions.AppException;
import utils.fhir.AuditEventResourceProvider;

public class AuditManager {

	public static AuditManager instance = new AuditManager();
	
	private ThreadLocal<MidataAuditEvent> running = new ThreadLocal<MidataAuditEvent>();

	
	public void addAuditEvent(AuditEventType type, Actor who) throws AppException {
		addAuditEvent(AuditEventBuilder.withType(type).withActor(who));
	}
	
	/*public void addAuditEvent(AuditEventType type, User who, String message) throws AppException {
		addAuditEvent(type, who, message, null, null, null);
	}*/
	public void addAuditEvent(AuditEventType type, User who, MidataId app) throws AppException {
		addAuditEvent(AuditEventBuilder.withType(type).withActorUser(who).withApp(app));		
	}
	
	public void addAuditEvent(AuditEventType type, MidataId app, MidataId who, User modifiedUser) throws AppException {
		User executingUser = User.getById(who, User.ALL_USER);
		addAuditEvent(type, app, executingUser, modifiedUser, null, null, null);
	}
	
	public void addAuditEvent(AuditEventType type, MidataId app, MidataId who, User modifiedUser, String message) throws AppException {
		User executingUser = User.getById(who, User.ALL_USER);
		addAuditEvent(type, app, executingUser, modifiedUser, null, message, null);
	}
	
	public void addAuditEvent(AuditEventType type, MidataId who, Consent consent) throws AppException {
		User executingUser = User.getById(who, User.ALL_USER);
		addAuditEvent(type, null, executingUser, null, consent, null, null);
	}

	public void addAuditEvent(AuditEventType type, MidataId who, Consent consent, Study study) throws AppException {
		User executingUser = User.getById(who, User.ALL_USER);
		addAuditEvent(type, null, executingUser, null, consent, null, study);
	}
	
	public void addAuditEvent(AuditEventType type, MidataId app, MidataId who, Consent consent, Study study) throws AppException {
		User executingUser = User.getById(who, User.ALL_USER);
		addAuditEvent(type, app, executingUser, null, consent, null, study);
	}

	public void addAuditEvent(AuditEventType type, MidataId app, MidataId who, MidataId modifiedUser, String message, MidataId userGroupId) throws AppException {
		User executingUser = User.getById(who, User.ALL_USER);
		User modifiedUserObj = User.getById(modifiedUser, User.ALL_USER);
		Study study = Study.getById(userGroupId, Study.ALL);		
		addAuditEvent(type, app, executingUser, modifiedUserObj, null, message, study);
	}
	
	public void addAuditEvent(AuditEventBuilder builder) throws AppException {
		addAuditEvent(builder.getType(), builder.getApp(), builder.getActor(), builder.getModifiedUser(), builder.getConsent(), builder.getMessage(), builder.getStudy(), builder.getExtraInfo());
	}
	
	public void addAuditEvent(AuditEventType type, MidataId app, Actor who, User modifiedUser, Consent consent, String message, Study study) throws AppException {
	    addAuditEvent(type, app, who, modifiedUser, consent, message, study, null);	
	}
	
	public void addAuditEvent(AuditEventType type, MidataId app, Actor who, User modifiedUser, Consent consent, String message, Study study, AuditExtraInfo extra) throws AppException {
		MidataAuditEvent mae = new MidataAuditEvent();
		mae._id = new MidataId();
		mae.event = type;
		mae.timestamp = new Date();
		mae.status = 12;
		mae.authorized = new HashSet<MidataId>();
		if (who != null) mae.authorized.add(who.getId());
		if (modifiedUser != null) {
			mae.authorized.add(modifiedUser._id);
			mae.about = modifiedUser._id;
		}
		if (study != null) {
			mae.about = study._id;
			mae.authorized.add(study._id);
			if (consent==null || consent.getOwnerName()==null) {
				mae.authorized.add(study.createdBy);				
				if (study.owner != null) mae.authorized.add(study.owner);
			}
		}
		if (consent != null) {
			mae.about = consent._id;
			if (consent.owner != null) mae.authorized.add(consent.owner);
			if (consent.authorized != null) mae.authorized.addAll(consent.authorized);
		}
		AuditEventResourceProvider.updateMidataAuditEvent(mae, app, who, modifiedUser, consent, message, study, extra);
		addAuditEvent(mae);
	}
	
	private void addAuditEvent(MidataAuditEvent event) throws AppException {
		MidataAuditEvent old = running.get();
		if (old != null) {
			event.next = old;
		}
		
		running.set(event);
		event.add();
	}
	
	public MidataId convertLastEventToAsync() {
		MidataAuditEvent old = running.get();
		if (old != null) {
			MidataId result = old._id;			
			running.set(old.next);
			return result;
		}
		return null;
	}
	
	public void resumeAsyncEvent(MidataId eventId) {
		if (eventId==null) return;
		MidataAuditEvent resumed = new MidataAuditEvent();
		resumed._id = eventId;
		MidataAuditEvent old = running.get();
		if (old != null) {
			resumed.next = old;
		}
		
		running.set(resumed);
	}
	
	public void success() throws AppException {
		MidataAuditEvent event = running.get();
		
		while (event != null) {
		  event.setStatus(0, null, null);
		  event = event.next;
		  running.remove();
		}			
		
	}
	
	public void fail(int status, String error, String errorkey) {
		MidataAuditEvent event = running.get();
		while (event != null) {
			if (status >= 400 && status < 500) status = 4;
			if (status > 500) status = 8;
			try {
			  event.setStatus(status, error, errorkey);
			} catch (AppException e) {}
			running.remove();
			event = event.next;
		}
	}
	
	public void clear() {				
		running.remove();
	}
	
	
	
}
