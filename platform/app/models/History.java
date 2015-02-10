package models;

import java.util.Date;

import org.bson.types.ObjectId;

import models.enums.EventType;
import models.enums.UserRole;

public class History implements JsonSerializable {

	public EventType event;
	public Date timestamp;
	public ObjectId who;
	public String whoName;
	public UserRole whoRole;
	public String message;
	
	public History() {}
	
	public History(EventType event, User who, String message) {
		this.event = event;
		this.timestamp = new Date();
		this.who = who._id;
		this.whoRole = who.getRole();
		this.whoName = who.sirname+", "+who.firstname;
		this.message = message;
	}
	
	public History(EventType event, StudyParticipation who, String message) {
		this.event = event;
		this.timestamp = new Date();
		this.who = who._id;
		this.whoRole = UserRole.MEMBER;
		this.whoName = who.memberName;
		this.message = message;
	}
}
