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
	public @NotMaterialized final static Set<String> ALL = Sets.create("_id", "event", "timestamp", "status", "statusDescription","fhirAuditEvent", "authorized", "about" );

	
	/**
	 * the type of event this history entry documents
	 */
	public AuditEventType event;
	
	/**
	 * when did the event occur
	 */
	public Date timestamp;
		
	/**
	 * http status of request
	 */
	public int status = 0;
	
	/**
	 * description for failures
	 */
	public String statusDescription;
		
	
	public Set<MidataId> authorized;
	
	public MidataId about;
	
	/**
	 * FHIR audit event
	 */
    public BSONObject fhirAuditEvent;
    
    public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
    
    public void setStatus(int status, String description) throws InternalServerException {
    	this.status = status;
    	Model.set(MidataAuditEvent.class, collection, _id, "status", status);
    	if (description != null) {
    		this.statusDescription = description;
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
