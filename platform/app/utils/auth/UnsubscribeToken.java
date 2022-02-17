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

public class UnsubscribeToken {

	private MidataId userId;
	
	private MidataId consentId;
	
	public UnsubscribeToken() {}
	
	public static String userToken(MidataId userId) throws InternalServerException {
		UnsubscribeToken tk = new UnsubscribeToken();
		tk.userId = userId;
		return tk.encrypt();
	}
	
	public static String consentToken(MidataId consentId) throws InternalServerException {
		UnsubscribeToken tk = new UnsubscribeToken();
		tk.consentId = consentId;
		return tk.encrypt();
	}
	
	public String encrypt() throws InternalServerException  {		
		Map<String, Object> map = CMaps.mapNotEmpty("u", userId!=null ? userId.toString() : null).mapNotEmpty("c", consentId!=null ? consentId.toString() : null);		
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}
	
	public static UnsubscribeToken decrypt(String unsafeSecret) {
		try {										
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);			
			JsonNode json = Json.parse(plaintext);
			UnsubscribeToken result = new UnsubscribeToken();
			if (json.has("u")) result.userId = MidataId.from(json.get("u").asText());
			if (json.has("c")) result.consentId = MidataId.from(json.get("c").asText());
			return result;
		} catch (Exception e) {			
			AccessLog.logException("decrypt", e);
			return null;
		}
	}

	public MidataId getUserId() {
		return userId;
	}

	public MidataId getConsentId() {
		return consentId;
	}
		
}
