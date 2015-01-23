package models;

import java.util.Date;

import models.enums.UserRole;

public class History {

	public String event;
	public Date timestamp;
	public String who;
	public UserRole whoRole;
	public String message;
}
