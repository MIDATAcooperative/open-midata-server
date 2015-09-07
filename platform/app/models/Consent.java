package models;

import java.util.Set;

import models.enums.ConsentType;

import org.bson.types.ObjectId;

import utils.db.NotMaterialized;

public class Consent extends Model {

	public ObjectId owner;
	public @NotMaterialized String ownerName;
	public String name;	
	public Set<ObjectId> authorized;
	public ConsentType type;
}
