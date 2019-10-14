package models;

import java.util.Set;

import models.enums.EntityType;

/**
 * Licence requirements for an app or plugin
 *
 */
public class LicenceDefinition implements JsonSerializable {

	public Set<EntityType> allowedEntities;
	
	// To be extended
	
}
