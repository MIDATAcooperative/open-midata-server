package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import models.enums.AuditEventType;
import models.enums.EventType;
import models.enums.UserRole;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class MidataAuditEvent extends Model {
	
	private static final String collection = "auditevents";
	
	/**
     * constant with all fields
     */
	public @NotMaterialized final static Set<String> ALL = Sets.create("_id", "event", "timestamp", "who", "whoName", "whoRole", "message","status", "statusDescription", "targetAccount", "targetConsent", "fhirAuditEvent", "appUsed" );

	
	/**
	 * the type of event this history entry documents
	 */
	public AuditEventType event;
	
	/**
	 * when did the event occur
	 */
	public Date timestamp;
	
	/**
	 * id of person triggering the event
	 */
	public MidataId who;
	
	/**
	 * public name of person triggering the event. This may be not the persons real name but a pseudonym for studies.
	 */
	public String whoName;
	
	/**
	 * the role of the person triggering the event
	 */
	public UserRole whoRole;
	
	/**
	 * a string further describing the event
	 */
	public String message;
	
	/**
	 * http status of request
	 */
	public int status = 0;
	
	/**
	 * description for failures
	 */
	public String statusDescription;
	
	/**
	 * id of user account that was changed or null if none
	 */
	public MidataId targetAccount;
			
	/**
	 * id of consent that was changed or null if none
	 */
	public MidataId targetConsent;
	
	/**
	 * id of app that triggered the event
	 */
	public MidataId appUsed;
	
	/**
	 * FHIR audit event
	 */
    public BSONObject fhirAuditEvent;
    
    public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
    
    public void setStatus(int status, String description) throws InternalServerException {
    	Model.set(MidataAuditEvent.class, collection, _id, "status", status);
    	if (description != null) {
    		Model.set(MidataAuditEvent.class, collection, _id, "statusDescription", description);
    	}
    }
    
    public static MidataAuditEvent getById(MidataId id) throws InternalServerException {
		return Model.get(MidataAuditEvent.class, collection, CMaps.map("_id", id), ALL);
	}
    
    public static Set<MidataAuditEvent> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(MidataAuditEvent.class, collection, properties, fields);
	}
    
}
