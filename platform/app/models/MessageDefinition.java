package models;

import java.util.Map;

import models.enums.MessageReason;

/**
 * Template for a predefined message
 *
 */
public class MessageDefinition implements JsonSerializable {
	
	/**
     * reason for message
     */
	public MessageReason reason;
	
    /**
     * additional code used to distinguish between different types of consents etc.
     */
	public String code;
	
	/**
	 * localized texts for message
	 */
	public Map<String, String> text;
	
	/**
	 * localized titles for message
	 */
	public Map<String, String> title;
	
	
	
}
