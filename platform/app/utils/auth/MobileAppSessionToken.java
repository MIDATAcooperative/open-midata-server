package utils.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import models.enums.UserRole;
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
	public String aeskey;
	
	/**
	 * the expiration timestamp of the token
	 */
	public long expiration;
	
	public UserRole role;

	public MobileAppSessionToken(MidataId appInstanceId, String aeskey, long expirationTime, UserRole role) {
		this.appInstanceId = appInstanceId;
		this.aeskey = aeskey;
		this.expiration = expirationTime;
		this.role = role;
	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("a", appInstanceId.toString()).map("p", aeskey).map("c", expiration).map("r", role.toShortString());						
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
			String aeskey = json.get("p").asText();	
			long expirationTime = json.get("c").asLong();
		    if (expirationTime < System.currentTimeMillis()) return null;
		    String r = json.get("r").asText();
		    UserRole role = UserRole.fromShortString(r);		    
			return new MobileAppSessionToken(appInstanceId, aeskey, expirationTime, role);
		} catch (Exception e) {
			return null;
		}
	}
}
