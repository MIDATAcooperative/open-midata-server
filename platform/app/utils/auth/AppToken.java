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

	/**
	 * id of authorized app
	 */
	public ObjectId appId;
	
	/**
	 * id of authorized user
	 */
	public ObjectId userId;
	
	/**
	 * id of consent this form may push data into
	 */
	public ObjectId consentId;

	public AppToken(ObjectId appId, ObjectId userId) {
		this.appId = appId;
		this.userId = userId;
	}
	
	public AppToken(ObjectId appId, ObjectId userId, ObjectId consentId) {
		this.appId = appId;
		this.userId = userId;
		this.consentId = consentId;
	}

	public String encrypt() {
		Map<String, Object> map;
		if (this.consentId != null) {
			map = CMaps.map("appId", appId.toString()).map("userId", userId.toString()).map("consentId", consentId.toString());
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
			String consentIdStr = json.path("consentId").asText();
			ObjectId consentId = (consentIdStr != null && !consentIdStr.equals("")) ? new ObjectId(consentIdStr) : null;
			return new AppToken(appId, userId, consentId);
		} catch (Exception e) {
			return null;
		}
	}
}
