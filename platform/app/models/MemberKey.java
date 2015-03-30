package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.MemberKeyStatus;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;

public class MemberKey extends Model {
	
	private static final String collection = "memberkeys";

	public ObjectId member;
	public ObjectId provider;
	public MemberKeyStatus status;	
	//public Map<String,String> key; //key used to identify this member
	public Date confirmDate;
	public String comment;
	public ObjectId aps;
	
	public static MemberKey getByMemberAndProvider(ObjectId memberId, ObjectId providerId) throws ModelException {
		return Model.get(MemberKey.class, collection, CMaps.map("member", memberId).map("provider", providerId), Sets.create("member", "provider", "status", "confirmDate", "aps", "comment"));
	}
	
	public static void add(MemberKey memberKey) throws ModelException {
		Model.insert(collection, memberKey);
	}
	
	
}
