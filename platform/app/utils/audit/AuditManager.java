package utils.audit;

import java.util.Date;
import java.util.HashSet;

import models.Consent;
import models.MidataAuditEvent;
import models.MidataId;
import models.User;
import models.enums.AuditEventType;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.fhir.AuditEventResourceProvider;

public class AuditManager {

	public static AuditManager instance = new AuditManager();
	
	private ThreadLocal<MidataAuditEvent> running = new ThreadLocal<MidataAuditEvent>();

	public void addAuditEvent(AuditEventType type, User who) throws AppException {
		addAuditEvent(type, null, who, null, null, null);
	}
	
	/*public void addAuditEvent(AuditEventType type, User who, String message) throws AppException {
		addAuditEvent(type, who, message, null, null, null);
	}*/
	public void addAuditEvent(AuditEventType type, User who, MidataId app) throws AppException {
		addAuditEvent(type, app, who, null, null, null);
	}
	
	public void addAuditEvent(AuditEventType type, MidataId app, User who, User modifiedUser, Consent consent, String message) throws AppException {
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
		if (consent != null) {
			mae.about = consent._id;
			if (consent.owner != null) mae.authorized.add(consent.owner);
			if (consent.authorized != null) mae.authorized.addAll(consent.authorized);
		}
		AuditEventResourceProvider.updateMidataAuditEvent(mae, app, who, modifiedUser, consent, message);
		addAuditEvent(mae);
	}
	
	private void addAuditEvent(MidataAuditEvent event) throws AppException {
		MidataAuditEvent old = running.get();
		if (old != null) throw new InternalServerException("error.internal", "Last audit event not finished");
		
		running.set(event);
		event.add();
	}
	
	public void success() throws AppException {
		MidataAuditEvent event = running.get();
		if (event != null) {
			event.setStatus(0, null);
			running.remove();
		}
	}
	
	public void fail(int status, String error) {
		MidataAuditEvent event = running.get();
		if (event != null) {
			if (status >= 400 && status < 500) status = 4;
			if (status > 500) status = 8;
			try {
			  event.setStatus(status, error);
			} catch (AppException e) {}
			running.remove();
		}
	}
	
	public void clear() {				
		running.remove();
	}
	
}
