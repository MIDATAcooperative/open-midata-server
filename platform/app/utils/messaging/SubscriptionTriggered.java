/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
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
	
	public SubscriptionTriggered(MidataId affected, MidataId app, String type, String eventCode, String fhirVersion, String resource, MidataId resourceId, Map<String, String> params) {
		this.affected = affected;
		this.app = app;
		this.type = type;
		this.fhirVersion = fhirVersion;
		this.resource = resource;
		this.eventCode = eventCode;
		this.resourceId = resourceId;
		this.params = params;
	}

	public MidataId getAffected() {
		return affected;
	}
		
	public MidataId getApp() {
		return app;
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

	public String toString() {
		return "trigger user="+affected+" type="+type;
	}
			
}