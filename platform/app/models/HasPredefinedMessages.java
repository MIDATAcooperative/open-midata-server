package models;

import java.util.Map;

public interface HasPredefinedMessages {

	/**
	 * Predefined messages
	 */
	public Map<String, MessageDefinition> getPredefinedMessages();
}
