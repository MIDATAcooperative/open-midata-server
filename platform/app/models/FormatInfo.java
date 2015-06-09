package models;

import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;

import models.enums.APSSecurityLevel;

public class FormatInfo extends Model {

	private static final String collection = "formats";
	private static final Set<String> ALL = Sets.create("format","visualization","security");
	
	public String format;
	public ObjectId visualization;
	public APSSecurityLevel security;
	
	public static FormatInfo getByName(String name) throws ModelException {
		return Model.get(FormatInfo.class, collection, CMaps.map("format", name), ALL);
	}
}
