package models;

import java.util.Date;
import java.util.Map;

import models.enums.MemberKeyStatus;

import org.bson.types.ObjectId;

public class MemberKey extends Model {
	
	private static final String collection = "memberkeys";

	public ObjectId member;
	public ObjectId provider;
	public MemberKeyStatus status;	
	public Map<String,String> key; //key used to identify this member
	public Date confirmDate;
	public String comment;
}
