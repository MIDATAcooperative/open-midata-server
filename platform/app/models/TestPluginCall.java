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
	
	public @NotMaterialized final static Set<String> ALL = 
			 Sets.create("_id", "handle", "path", "token", "lang", "owner", "resource", "resourceId", "created", "returnPath");

	public String returnPath;
	
	public String handle;
	
	public String path;
	
	public String token;
	
	public String lang;
	
	public String owner;
	
	public String resource;
	
	public String resourceId;
	
	public long created;
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
	
	public static List<TestPluginCall> getForHandle(String handle) throws InternalServerException {
		return Model.getAllList(TestPluginCall.class, collection, CMaps.map("handle", handle), ALL, 1000, "created", 1);
	}
	
	public static void delete(String handle) throws InternalServerException {
		Model.delete(TestPluginCall.class, collection, CMaps.map("handle", handle));
	}
}
