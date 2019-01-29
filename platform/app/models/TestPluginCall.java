package models;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

@JsonFilter("TestPluginCall")
public class TestPluginCall extends Model {

	protected @NotMaterialized static final String collection = "testcalls";
	
	public @NotMaterialized final static int NOTANSWERED = -999;
	
	public @NotMaterialized final static Set<String> ALL = 
			 Sets.create("_id", "handle", "path", "token", "lang", "owner", "resource", "resourceId", "created", "answer", "answerStatus");
	
	
	public String handle;
	
	public String path;
	
	public String token;
	
	public String lang;
	
	public String owner;
	
	public String resource;
	
	public String resourceId;
	
	public String answer;
	
	public int answerStatus;
	
	public long created;
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
	
	public static List<TestPluginCall> getForHandle(String handle) throws InternalServerException {
		return Model.getAllList(TestPluginCall.class, collection, CMaps.map("handle", handle).map("answerStatus", NOTANSWERED), ALL, 1000, "created", 1);
	}
	
	public static TestPluginCall getForHandleAndId(String handle, MidataId id) throws InternalServerException {
		return Model.get(TestPluginCall.class, collection, CMaps.map("_id",id).map("handle", handle), ALL);
	}
	
	public static TestPluginCall getById(MidataId id) throws InternalServerException {
		return Model.get(TestPluginCall.class, collection, CMaps.map("_id",id), ALL);
	}
	
	public static void delete(String handle) throws InternalServerException {
		Model.delete(TestPluginCall.class, collection, CMaps.map("handle", handle));
	}
	
	public static void delete(String handle, MidataId id) throws InternalServerException {
		Model.delete(TestPluginCall.class, collection, CMaps.map("_id",id).map("handle", handle));
	}
	
	public void setAnswer(int status, String content) throws InternalServerException {
		this.answerStatus = status;
		this.answer = content;
		this.setMultiple(collection, Sets.create("answerStatus", "answer"));
	}
}
