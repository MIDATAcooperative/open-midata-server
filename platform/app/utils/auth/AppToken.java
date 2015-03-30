package utils.auth;

import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.CMaps;
import utils.collections.ChainedMap;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Authorization token for apps to push records to a user's repository.
 */
public class AppToken {

	public ObjectId appId;
	public ObjectId userId;
	public ObjectId ownerId;

	public AppToken(ObjectId appId, ObjectId userId) {
		this.appId = appId;
		this.userId = userId;
	}
	
	public AppToken(ObjectId appId, ObjectId ownerId, ObjectId userId) {
		this.appId = appId;
		this.userId = userId;
		this.ownerId = ownerId;
	}

	public String encrypt() {
		Map<String, Object> map;
		if (this.ownerId != null && !this.ownerId.equals(this.userId)) {
			map = CMaps.map("appId", appId.toString()).map("userId", userId.toString()).map("ownerId", ownerId.toString());
		} else {
			map = CMaps.map("appId", appId.toString()).map("userId", userId.toString());
		}
		String json = Json.stringify(Json.toJson(map));
		return Crypto.encryptAES(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static AppToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = Crypto.decryptAES(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId appId = new ObjectId(json.get("appId").asText());
			ObjectId userId = new ObjectId(json.get("userId").asText());
			String ownerIdStr = json.path("ownerId").asText();
			ObjectId ownerId = (ownerIdStr != null && !ownerIdStr.equals("")) ? new ObjectId(ownerIdStr) : userId;
			return new AppToken(appId, ownerId, userId);
		} catch (Exception e) {
			return null;
		}
	}
}
