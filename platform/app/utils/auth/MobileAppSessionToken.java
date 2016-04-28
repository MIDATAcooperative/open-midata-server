package utils.auth;

import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Session authentication token for mobile apps
 *
 */
public class MobileAppSessionToken {

	/**
	 * the id of the application instance
	 */
	public ObjectId appInstanceId;
	
	/**
	 * the secret passphrase for this application instance
	 */
	public String passphrase;
	
	/**
	 * the creation timestamp of the token
	 */
	public long created;

	public MobileAppSessionToken(ObjectId appInstanceId, String phrase, long created) {
		this.appInstanceId = appInstanceId;
		this.passphrase = phrase;
		this.created = created;
	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("a", appInstanceId.toString()).map("p", passphrase).map("c", created);						
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static MobileAppSessionToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId appInstanceId = new ObjectId(json.get("a").asText());
			String phrase = json.get("p").asText();	
			long created = json.get("c").asLong();
			return new MobileAppSessionToken(appInstanceId, phrase, created);
		} catch (Exception e) {
			return null;
		}
	}
}
