package utils.audit;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import models.Consent;
import models.MidataAuditEvent;
import models.MidataId;
import models.Study;
import models.User;
import models.enums.AuditEventType;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.fhir.AuditEventResourceProvider;

public class AuditManager {

	public static AuditManager instance = new AuditManager();
	
	private ThreadLocal<MidataAuditEvent> running = new ThreadLocal<MidataAuditEvent>();

	public void addAuditEvent(AuditEventType type, MidataId who) throws AppException {
		User executingUser = User.getById(who, User.ALL_USER);
		addAuditEvent(type, null, executingUser, null, null, null, null);
	}
	
	public void addAuditEvent(AuditEventType type, User who) throws AppException {
		addAuditEvent(type, null, who, null, null, null, null);
	}
	
	/*public void addAuditEvent(AuditEventType type, User who, String message) throws AppException {
		addAuditEvent(type, who, message, null, null, null);
	}*/
	public void addAuditEvent(AuditEventType type, User who, MidataId app) throws AppException {
		addAuditEvent(type, app, who, null, null, null, null);
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

	public void addAuditEvent(AuditEventType type, MidataId app, MidataId who, MidataId modifiedUser, String message, MidataId userGroupId) throws AppException {
		User executingUser = User.getById(who, User.ALL_USER);
		User modifiedUserObj = User.getById(modifiedUser, User.ALL_USER);
		Study study = Study.getByIdFromMember(userGroupId, Study.ALL);		
		addAuditEvent(type, app, executingUser, modifiedUserObj, null, message, study);
	}
	
	public void addAuditEvent(AuditEventType type, MidataId app, User who, User modifiedUser, Consent consent, String message, Study study) throws AppException {
		MidataAuditEvent mae = new MidataAuditEvent();
		mae._id = new MidataId();
		mae.event = type;
		mae.timestamp = new Date();
		mae.status = 12;
		mae.authorized = new HashSet<MidataId>();
		if (who != null) mae.authorized.add(who._id);
		if (modifiedUser != null) {
			mae.authorized.add(modifiedUser._id);
			mae.about = modifiedUser._id;
		}
		if (study != null) {
			mae.about = study._id;
			mae.authorized.add(study.createdBy);
			mae.authorized.add(study._id);
			mae.authorized.add(study.owner);
		}
		if (consent != null) {
			mae.about = consent._id;
			if (consent.owner != null) mae.authorized.add(consent.owner);
			if (consent.authorized != null) mae.authorized.addAll(consent.authorized);
		}
		AuditEventResourceProvider.updateMidataAuditEvent(mae, app, who, modifiedUser, consent, message, study);
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
