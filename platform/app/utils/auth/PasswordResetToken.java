/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.auth;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import play.libs.Json;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

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
	
	public PasswordResetToken(MidataId userId, String role, boolean dummy) {
		this.userId = userId;
		this.role = role;
	    this.token = new BigInteger(32, random).toString(32);
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
