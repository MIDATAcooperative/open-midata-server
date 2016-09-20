package utils.auth;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;

import models.MidataId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Authorization token to reset a users password.
 */
public class PasswordResetToken {

	/**
	 * the id of the user
	 */
	public MidataId userId;
	
	/**
	 * the token
	 */
	public String token;
	
	/**
	 * the role of the user
	 */
	public String role;
	
	static SecureRandom random = new SecureRandom();

	public PasswordResetToken(MidataId userId, String role, String token) {		
		this.userId = userId;
		this.role = role;
		this.token = token;
	}
	
	public PasswordResetToken(MidataId userId, String role) {
		this.userId = userId;
		this.role = role;
	    this.token = new BigInteger(130, random).toString(32);
	}

	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("userId", userId.toString()).map("token", token).map("role", role);
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static PasswordResetToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			MidataId userId = new MidataId(json.get("userId").asText());
			String token = json.get("token").asText();
			String role = json.get("role").asText();
			return new PasswordResetToken(userId, role, token);
		} catch (Exception e) {
			return null;
		}
	}
}
