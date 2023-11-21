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

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import play.libs.Json;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

/**
 * Mobile app instance token
 * 
 * This token is used to uniquely identify an application instance.
 * This token can be exchanged into a session token when the user logs in on his mobile device
 *
 */
public class OAuthRefreshToken {

	/**
	 * the id of the application instance
	 */
	public MidataId appInstanceId;
	
	/**
	 * the id of the application
	 */
	public MidataId appId;
	
	/**
	 * the id of the owner of the application instance
	 */
	public MidataId ownerId;
	
	/**
	 * the secret passphrase of the application instance
	 */
	public String phrase;
	
	/**
	 * creation time
	 */
	public long created;

	public OAuthRefreshToken(MidataId appId, MidataId instanceId, MidataId ownerId, String phrase, long created) {
		this.appInstanceId = instanceId;
		this.appId = appId;
		this.ownerId = ownerId;
		this.phrase = phrase;
		this.created = created;
	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("i", appInstanceId.toString()).map("a", appId.toString()).map("p", phrase).map("c", created).map("o", ownerId.toString());						
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static OAuthRefreshToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			MidataId appId = new MidataId(json.get("a").asText());
			MidataId instanceId = new MidataId(json.get("i").asText());
			MidataId ownerId = new MidataId(json.get("o").asText());
			String phrase = json.get("p").asText();		
			long created = json.get("c").asLong();
			if (json.has("f") || json.has("h")) {
				AccessLog.log("Provided token is no refresh token.");
				return null;
			}
			return new OAuthRefreshToken(appId, instanceId, ownerId, phrase, created);
		} catch (Exception e) {
			AccessLog.log("Exception while decoding refresh token:");
			return null;
		}
	}
}