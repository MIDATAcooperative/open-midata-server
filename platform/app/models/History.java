package models;

import java.util.Date;

import models.MidataId;

import models.enums.EventType;
import models.enums.UserRole;

/**
 * Several classes store their modification history. This class encapsulates a single history entry.
 *
 */
public class History implements JsonSerializable {

	/**
	 * the type of event this history entry documents
	 */
	public EventType event;
	
	/**
	 * when did the event occur
	 */
	public Date timestamp;
	
	/**
	 * id of person triggering the event
	 */
	public MidataId who;
	
	/**
	 * public name of person triggering the event. This may be not the persons real name but a pseudonym for studies.
	 */
	public String whoName;
	
	/**
	 * the role of the person triggering the event
	 */
	public UserRole whoRole;
	
	/**
	 * a string further describing the event
	 */
	public String message;
	
	public History() {}
	
	public History(EventType event, User who, String message) {
		this.event = event;
		this.timestamp = new Date();
		this.who = who._id;
		this.whoRole = who.getRole();
		this.whoName = who.lastname+", "+who.firstname;
		this.message = message;
	}
	
	public History(EventType event, StudyParticipation who, String message) {
		this.event = event;
		this.timestamp = new Date();
		this.who = who._id;
		this.whoRole = UserRole.MEMBER;
		this.whoName = who.ownerName;
		this.message = message;
	}
}
