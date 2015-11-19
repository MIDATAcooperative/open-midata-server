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
	public ObjectId instanceId;
	
	/**
	 * the id of the application
	 */
	public ObjectId appId;
	
	/**
	 * the secret passphrase of the application instance
	 */
	public String phrase;	

	public MobileAppToken(ObjectId appId, ObjectId instanceId, String phrase) {
		this.instanceId = instanceId;
		this.appId = appId;
		this.phrase = phrase;		
	}
	
	public String encrypt() {
		Map<String, Object> map = CMaps.map("i", instanceId.toString()).map("a", appId.toString()).map("p", phrase);						
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
			String phrase = json.get("p").asText();			
			return new MobileAppToken(appId, instanceId, phrase);
		} catch (Exception e) {
			return null;
		}
	}
}