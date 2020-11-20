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
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

public class OAuthCodeToken {
	/**
	 * the id of the application instance
	 */
	public MidataId appInstanceId;
	
	/**
	 * the secret passphrase for this application instance
	 */
	public String passphrase;
	
	public String aeskey;
	
	/**
	 * the creation timestamp of the token
	 */
	public long created;
	
	public String state;
	
	public String codeChallenge;
	
	public String codeChallengeMethod;
	
	

	public OAuthCodeToken(MidataId appInstanceId, String phrase, String aeskey, long created, String state, String cs, String csm) {
		this.appInstanceId = appInstanceId;
		this.passphrase = phrase;
		this.aeskey = aeskey;
		this.created = created;
		this.state = state;
		this.codeChallenge = cs;
		this.codeChallengeMethod = csm;
	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("a", appInstanceId.toString()).map("p", passphrase).map("k", aeskey).map("c", created).map("s", state);
		if (codeChallenge != null) map.put("cs", codeChallenge);
		if (codeChallengeMethod != null) map.put("csm", codeChallengeMethod);
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
			MidataId appInstanceId = new MidataId(json.get("a").asText());
			String phrase = json.get("p").asText();	
			String aeskey = json.get("k").asText();
			long created = json.get("c").asLong();
			String state = json.get("s").asText();
			String cs = json.has("cs") ? json.get("cs").asText() : null;
			String csm = json.has("csm") ? json.get("csm").asText() : null;
			return new OAuthCodeToken(appInstanceId, phrase, aeskey, created, state, cs, csm);
		} catch (Exception e) {
			return null;
		}
	}
}
