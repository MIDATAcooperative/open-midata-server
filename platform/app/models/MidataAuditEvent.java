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

package models;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import models.enums.AuditEventType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class MidataAuditEvent extends Model {
	
	private static final String collection = "auditevents";
	
	/**
     * constant with all fields
     */
	public @NotMaterialized final static Set<String> ALL = Sets.create("_id", "event", "timestamp", "status", "statusKey", "statusDescription","fhirAuditEvent", "authorized", "about" );

	
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
	
	/**
	 * translatation key for failures
	 */
	public String statusKey;
		
	/**
	 * authorized users or organizations
	 */
	public Set<MidataId> authorized;
			
	
	/**
	 * id of entity this record is about
	 */
	public MidataId about;
	
	/**
	 * FHIR audit event
	 */
    public BSONObject fhirAuditEvent;
    
    public @NotMaterialized MidataAuditEvent next;
    
    public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
    
    public void setStatus(int status, String description, String key) throws InternalServerException {
    	this.status = status;
    	Model.set(MidataAuditEvent.class, collection, _id, "status", status);
    	if (description != null) {
    		this.statusDescription = description;
    		Model.set(MidataAuditEvent.class, collection, _id, "statusDescription", description);
    	}
    	if (key != null) {
    		this.statusKey = key;
    		Model.set(MidataAuditEvent.class, collection, _id, "statusKey", key);
    	}
    }
    
    public static MidataAuditEvent getById(MidataId id) throws InternalServerException {
		return Model.get(MidataAuditEvent.class, collection, CMaps.map("_id", id), ALL);
	}
    
    public static List<MidataAuditEvent> getAll(Map<String, ? extends Object> properties, Set<String> fields, int limit) throws InternalServerException {
		return Model.getAllList(MidataAuditEvent.class, collection, properties, fields, limit, "_id", -1);
	}
    
    public static long count() throws AppException {
		return Model.count(MidataAuditEvent.class, collection, CMaps.map());
	}
    
}
