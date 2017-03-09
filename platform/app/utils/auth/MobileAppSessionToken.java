package utils.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import play.libs.Json;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

/**
 * Session authentication token for mobile apps
 *
 */
public class MobileAppSessionToken {

	/**
	 * the id of the application instance
	 */
	public MidataId appInstanceId;
	
	/**
	 * the secret passphrase for this application instance
	 */
	public String passphrase;
	
	/**
	 * the expiration timestamp of the token
	 */
	public long expiration;

	public MobileAppSessionToken(MidataId appInstanceId, String phrase, long expirationTime) {
		this.appInstanceId = appInstanceId;
		this.passphrase = phrase;
		this.expiration = expirationTime;
	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("a", appInstanceId.toString()).map("p", passphrase).map("c", expiration);						
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
			return decrypt(json);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static MobileAppSessionToken decrypt(JsonNode json) {
		try {			
			MidataId appInstanceId = new MidataId(json.get("a").asText());
			String phrase = json.get("p").asText();	
			long expirationTime = json.get("c").asLong();
		    if (expirationTime < System.currentTimeMillis()) return null;
			return new MobileAppSessionToken(appInstanceId, phrase, expirationTime);
		} catch (Exception e) {
			return null;
		}
	}
}
