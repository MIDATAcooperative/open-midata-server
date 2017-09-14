package utils.audit;

import java.util.Date;

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
		addAuditEvent(type, who, null, null);
	}
	
	public void addAuditEvent(AuditEventType type, User who, String message) throws AppException {
		addAuditEvent(type, who, message, null);
	}
	public void addAuditEvent(AuditEventType type, User who, MidataId app) throws AppException {
		addAuditEvent(type, who, null, app);
	}
	
	public void addAuditEvent(AuditEventType type, User who, String message, MidataId app) throws AppException {
		MidataAuditEvent mae = new MidataAuditEvent();
		mae.event = type;
		mae.timestamp = new Date();
		mae.who = who._id;
		mae.whoRole = who.getRole();
		mae.whoName = who.lastname+", "+who.firstname;
		mae.message = message;
		mae.appUsed = app;
		addAuditEvent(mae);
	}
	
	public void addAuditEvent(MidataAuditEvent event) throws AppException {
		MidataAuditEvent old = running.get();
		if (old != null) throw new InternalServerException("error.internal", "Last audit event not finished");
		AuditEventResourceProvider.updateMidataAuditEvent(event);
		running.set(event);
		event.add();
	}
	
	public void success() throws AppException {
		MidataAuditEvent event = running.get();
		if (event != null) {
			event.setStatus(200, null);
			running.remove();
		}
	}
	
	public void fail(int status, String error) {
		MidataAuditEvent event = running.get();
		if (event != null) {
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
