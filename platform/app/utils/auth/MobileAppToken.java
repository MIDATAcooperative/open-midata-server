package utils.auth;

import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.CMaps;
import utils.collections.ChainedMap;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Mobile app instance token
 * 
 * This token is used to uniquely identify an application instance.
 * This token can be exchanged into a session token when the user logs in on his mobile device
 *
 */
public class MobileAppToken {

	/**
	 * the id of the application instance
	 */
	public ObjectId appInstanceId;
	
	/**
	 * the id of the application
	 */
	public ObjectId appId;
	
	/**
	 * the id of the owner of the application instance
	 */
	public ObjectId ownerId;
	
	/**
	 * the secret passphrase of the application instance
	 */
	public String phrase;
	
	/**
	 * creation time
	 */
	public long created;

	public MobileAppToken(ObjectId appId, ObjectId instanceId, ObjectId ownerId, String phrase, long created) {
		this.appInstanceId = instanceId;
		this.appId = appId;
		this.ownerId = ownerId;
		this.phrase = phrase;
		this.created = created;
	}
	
	public String encrypt() {
		Map<String, Object> map = CMaps.map("i", appInstanceId.toString()).map("a", appId.toString()).map("p", phrase).map("c", created).map("o", ownerId.toString());						
		String json = Json.stringify(Json.toJson(map));
		return Crypto.encryptAES(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static MobileAppToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = Crypto.decryptAES(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId appId = new ObjectId(json.get("a").asText());
			ObjectId instanceId = new ObjectId(json.get("i").asText());
			ObjectId ownerId = new ObjectId(json.get("o").asText());
			String phrase = json.get("p").asText();		
			long created = json.get("c").asLong();
			return new MobileAppToken(appId, instanceId, ownerId, phrase, created);
		} catch (Exception e) {
			return null;
		}
	}
}