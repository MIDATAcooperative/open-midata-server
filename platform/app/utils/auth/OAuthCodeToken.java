package utils.auth;

import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Json;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

import com.fasterxml.jackson.databind.JsonNode;

public class OAuthCodeToken {
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
	
	public String state;
	
	

	public OAuthCodeToken(ObjectId appInstanceId, String phrase, long created, String state) {
		this.appInstanceId = appInstanceId;
		this.passphrase = phrase;
		this.created = created;
		this.state = state;
	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("a", appInstanceId.toString()).map("p", passphrase).map("c", created).map("s", state);						
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static OAuthCodeToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId appInstanceId = new ObjectId(json.get("a").asText());
			String phrase = json.get("p").asText();	
			long created = json.get("c").asLong();
			String state = json.get("s").asText();
			return new OAuthCodeToken(appInstanceId, phrase, created, state);
		} catch (Exception e) {
			return null;
		}
	}
}
