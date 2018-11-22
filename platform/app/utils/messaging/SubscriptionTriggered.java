package utils.messaging;

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
	 * Optional event code
	 */
	final String eventCode;
	
	public SubscriptionTriggered(MidataId affected, MidataId app, String type, String eventCode, String resource) {
		this.affected = affected;
		this.app = app;
		this.type = type;
		this.resource = resource;
		this.eventCode = eventCode;
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

	public String getEventCode() {
		return eventCode;
	}
	
	
			
}