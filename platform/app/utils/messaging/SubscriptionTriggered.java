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

package utils.messaging;

import java.util.Map;

import models.MidataId;

/**
 * Message: A subscription of a user has been triggered by a resource change
 *
 */
public class SubscriptionTriggered {
	/**
	 * id of user who is owner of subscription
	 */
	final MidataId affected;
	
	/**
	 * app of triggered subscription 
	 */
	final MidataId app;
	
	/**
	 * optional for autoimport: _id of triggered subscription
	 */
	final MidataId byId;
	
	/**
	 * Type of resource that has changed
	 */
	final String type;
	
	/**
	 * The resource that was changed
	 */
	final String resource;
	
	/**
	 * The id of the resource that was changed
	 */
	final MidataId resourceId;
	
	/**
	 * The version of the resource
	 */
	final String resourceVersion;
	
	/**
	 * Optional event code
	 */
	final String eventCode;
	
	/**
	 * Parameters of trigger
	 */
    final Map<String, String> params;
            
    /**
     * FHIR version of resource
     */
    final String fhirVersion;
    
    /**
     * ID of user who is owner of the event that triggered the subscription
     */
    final MidataId sourceOwner;
    
    /**
     * link to trigger status for determining if all is done
     */
    final MidataId transactionId;
    
    /**
     * user group involved (for data brokers)
     */
    final MidataId userGroupId;
	
	public SubscriptionTriggered(MidataId affected, MidataId app, String type, String eventCode, String fhirVersion, String resource, MidataId resourceId, Map<String, String> params, MidataId sourceOwner, String resourceVersion, MidataId transactionId, MidataId userGroupId) {
		this.affected = affected;
		this.app = app;
		this.type = type;
		this.fhirVersion = fhirVersion;
		this.resource = resource;
		this.eventCode = eventCode;
		this.resourceId = resourceId;
		this.params = params;
		this.byId = null;
		this.sourceOwner = sourceOwner;
		this.resourceVersion = resourceVersion;
		this.transactionId = transactionId;
		this.userGroupId = userGroupId;
	}
	
	public SubscriptionTriggered(MidataId id, MidataId affected, MidataId app, String type, String eventCode, String fhirVersion, String resource, MidataId resourceId, Map<String, String> params, MidataId sourceOwner, String resourceVersion) {
		this.affected = affected;
		this.app = app;
		this.type = type;
		this.fhirVersion = fhirVersion;
		this.resource = resource;
		this.eventCode = eventCode;
		this.resourceId = resourceId;
		this.params = params;
		this.byId = id;
		this.sourceOwner = sourceOwner;
		this.resourceVersion = resourceVersion;
		this.transactionId = null;
		this.userGroupId = null;
	}

	public MidataId getAffected() {
		return affected;
	}
		
	public MidataId getApp() {
		return app;
	}
	
	public MidataId getById() {
		return byId;
	}

	public String getType() {
		return type;
	}

	public String getResource() {
		return resource;
	}
	
	public MidataId getResourceId() {
		return resourceId;
	}
	
	public MidataId getSourceOwner() {
		return sourceOwner;
	}

	public String getEventCode() {
		return eventCode;
	}
	
	public String getDescription() {
		if (eventCode != null) return type+" ["+eventCode+"]";
		return type;
	}
		
	public Map<String, String> getParams() {
		return params;
	}

	public String getFhirVersion() {
		return fhirVersion;
	}	

	public String getResourceVersion() {
		return resourceVersion;
	}
	
	public MidataId getTransactionId() {
		return transactionId;
	}
	
	public MidataId getUserGroupId() {
		return userGroupId;
	}

	public String toString() {
		return "trigger user="+affected+" type="+type;
	}
			
}