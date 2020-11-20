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

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import utils.exceptions.InternalServerException;

/**
 * A token to identify a record including its access permission set
 *
 */
public class RecordToken {
	
	/**
	 * id of record
	 */
	public String recordId;
	
	/**
	 * id of APS that allows access to the record
	 */
	public String apsId;

	public RecordToken(String recordId, String apsId) {
		this.recordId = recordId;
		this.apsId = apsId;
	}

	public String encrypt() throws InternalServerException {		
		String json = Json.stringify(Json.toJson(this));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static RecordToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			String appId = json.get("recordId").asText();
			String userId = json.get("apsId").asText();
			return new RecordToken(appId, userId);
		} catch (Exception e) {
			return null;
		}
	}
}
