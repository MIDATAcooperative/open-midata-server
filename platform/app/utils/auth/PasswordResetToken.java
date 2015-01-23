package utils.auth;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.CMaps;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Authorization token to reset a users password.
 */
public class PasswordResetToken {

	public ObjectId userId;
	public String token;
	public String role;
	
	static SecureRandom random = new SecureRandom();

	public PasswordResetToken(ObjectId userId, String role, String token) {		
		this.userId = userId;
		this.role = role;
		this.token = token;
	}
	
	public PasswordResetToken(ObjectId userId, String role) {
		this.userId = userId;
		this.role = role;
	    this.token = new BigInteger(130, random).toString(32);
	}

	public String encrypt() {
		Map<String, Object> map = CMaps.map("userId", userId.toString()).map("token", token);
		String json = Json.stringify(Json.toJson(map));
		return Crypto.encryptAES(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static PasswordResetToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = Crypto.decryptAES(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId userId = new ObjectId(json.get("userId").asText());
			String token = json.get("token").asText();
			String role = json.get("role").asText();
			return new PasswordResetToken(userId, role, token);
		} catch (Exception e) {
			return null;
		}
	}
}
