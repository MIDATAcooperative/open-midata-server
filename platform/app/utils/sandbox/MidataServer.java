package utils.sandbox;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import models.Record;
import models.RecordsInfo;

import org.bson.types.ObjectId;

import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.ws.WSResponse;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

import controllers.PluginsAPI;

import utils.DateTimeUtils;
import utils.auth.ExecutionInfo;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

public class MidataServer {
	
	private ExecutionInfo inf;
	
	public MidataServer(ExecutionInfo inf) {
	  this.inf = inf;	
	}

	public void createRecord(String name, String description, String content, String format, String data) throws AppException {
		Record record = new Record();
		record._id = new ObjectId();
		record.app = inf.pluginId;
		record.owner = inf.ownerId;
		record.creator = inf.executorId;
		record.created = DateTimeUtils.now();
						
		record.format = format;
		record.content = content;
					
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		
		PluginsAPI.createRecord(inf, record);
	};
	
	
	public Collection<Record> getRecords(Map<String,Object> properties, Set<String> fields) throws AppException {
		return PluginsAPI.getRecords(inf, properties, fields); 
	};
	
	public Collection<RecordsInfo> getSummary(String level, Map<String,Object> properties, Set<String> fields) {
		return null;
	};
	
	public JsonNode getConfig() {
		return null;
	};
	
	
	public void setConfig(JsonNode config) {
		 
	};
	
	public void cloneAs(String name, JsonNode config) {
		 
	};
	
	public Promise<JsonNode> oauth2Request(String url) throws AppException {	
	    Promise<WSResponse> response = PluginsAPI.oAuth2Call(inf, url);	
		
		Promise<JsonNode> promise = response.map(new Function<WSResponse, JsonNode>() {
			public JsonNode apply(WSResponse response) {
				return response.asJson();
			}
		});
		
		return promise;
	};
}
