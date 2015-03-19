package models;

import models.enums.UserRole;

import org.bson.types.ObjectId;

public abstract class Plugin extends Model {

	public ObjectId creator;
	public String filename;
	public String name;
	public String description;
	public String category;
	public UserRole targetUserRole;
	public boolean spotlighted;

}
