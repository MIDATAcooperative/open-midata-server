package utils.auth;

import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.ChainedMap;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Authorization token for plugins to access a user's records assigned to a space.
 */
public class SpaceToken {

	/**
	 * id of space
	 */
	public ObjectId spaceId;
		
	/**
	 * id of user
	 */
	public ObjectId userId;
	
	/**
	 * optional id of record if only access to a single record is allowed
	 */
	public ObjectId recordId;
	
	/**
	 * optional id of plugin
	 */
	public ObjectId pluginId;
	
	/**
	 * Different executing person
	 */
	public ObjectId executorId;

	public SpaceToken(ObjectId spaceId, ObjectId userId) {
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = null;
	}
	
	public SpaceToken(ObjectId spaceId, ObjectId userId, ObjectId recordId, ObjectId pluginId) {
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
	}
	
	public SpaceToken(ObjectId spaceId, ObjectId userId, ObjectId recordId, ObjectId pluginId, ObjectId executorId) {
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
		this.executorId = executorId;
	}
	
	public SpaceToken(ObjectId spaceId, ObjectId userId, ObjectId recordId) {
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
	}

	public String encrypt() {
		Map<String, String> map = new ChainedMap<String, String>().put("instanceId", spaceId.toString()).put("userId", userId.toString())
				.get();
		if (recordId != null) map.put("r", recordId.toString());
		if (pluginId != null) map.put("p", pluginId.toString());
		if (executorId != null) map.put("e", executorId.toString());
		String json = Json.stringify(Json.toJson(map));
		return Crypto.encryptAES(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static SpaceToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = Crypto.decryptAES(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId spaceId = new ObjectId(json.get("instanceId").asText());
			ObjectId userId = new ObjectId(json.get("userId").asText());
			ObjectId recordId = json.has("r") ? new ObjectId(json.get("r").asText()) : null;
			ObjectId pluginId = json.has("p") ? new ObjectId(json.get("p").asText()) : null;
			ObjectId executorId = json.has("e") ? new ObjectId(json.get("e").asText()) : null;
			return new SpaceToken(spaceId, userId, recordId, pluginId, executorId);
		} catch (Exception e) {
			return null;
		}
	}
}
