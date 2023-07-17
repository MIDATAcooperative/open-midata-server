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

import java.security.SecureRandom;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import models.enums.AuditEventType;
import play.libs.Json;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation;

public class ActionToken {
	
	/**
	 * optional id of the user, if existing
	 */
	public MidataId userId;
	
	/**
	 * id of resource for this action, if existing
	 */
	public MidataId resourceId;
	
	/**
	 * optional handle of action if existing
	 */
	public String handle;
	
	/**
	 * action that this token may execute
	 */
	public AuditEventType action;
	
	/**
	 * expiration time
	 */
	public long expiration;
	
	static SecureRandom random = new SecureRandom();

	public ActionToken(MidataId userId, MidataId resourceId, String handle, AuditEventType action, long expiration) {		
		this.userId = userId;
		this.resourceId = resourceId;
		this.handle = handle;
		this.action = action;
		this.expiration = expiration;
	}
		

	public String encrypt() throws InternalServerException {
		Map<String, Object> map = 
				CMaps.mapNotEmpty("u", userId != null ? userId.toString() : null)
				     .mapNotEmpty("r", resourceId != null ? resourceId.toString() : null)
				     .mapNotEmpty("h", handle)
				     .mapNotEmpty("action", action.toString())
				     .map("e", expiration);
				
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	public static ActionToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			MidataId userId = JsonValidation.getMidataId(json, "u");
			MidataId resourceId = JsonValidation.getMidataId(json, "r");
			String handle = JsonValidation.getString(json, "h");
			AuditEventType action = JsonValidation.getEnum(json, "action", AuditEventType.class);
			long expiration = JsonValidation.getLong(json, "e");	
			if (expiration > 0 && expiration < System.currentTimeMillis()) return null;
			return new ActionToken(userId, resourceId, handle, action, expiration);
		} catch (Exception e) {
			return null;
		}
	}
}


