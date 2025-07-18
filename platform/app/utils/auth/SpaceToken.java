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
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.collections.ChainedMap;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * Authorization token for plugins to access a user's records assigned to a space.
 */
public class SpaceToken {
	
	public final static long LIFETIME = 1000 * 60 * 60 * 8;

	/**
	 * id of space
	 */
	public MidataId spaceId;
		
	/**
	 * id of user
	 */
	public MidataId userId;
	
	/**
	 * optional id of record if only access to a single record is allowed
	 */
	public MidataId recordId;
	
	/**
	 * optional id of plugin
	 */
	public MidataId pluginId;
	
	public boolean autoimport;
	
	/**
	 * Creation time of token
	 */
	public long created;
	
	/**
	 * IP Address for which the token is valid
	 */
	public String remoteAddress = "all";
	
	/**
	 * Different executing person
	 */
	public MidataId executorId;
	
	public UserRole role;
	
	public String handle;
	
	/**
	 * Just used for auto import tokens
	 */
	public MidataId userGroup;

	public SpaceToken(String handle, MidataId spaceId, MidataId userId, UserRole role) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = null;
		this.role = role;
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId executorId, MidataId userId, UserRole role, boolean dummy) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.executorId = executorId;
		this.recordId = null;
		this.role = role;
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId userId, UserRole role, MidataId recordId, MidataId pluginId) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
		this.role = role;
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId userId, UserRole role, MidataId recordId, MidataId pluginId, MidataId executorId, MidataId userGroupId) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
		this.executorId = executorId;	
		this.autoimport = true;
		this.created = System.currentTimeMillis();
		this.role = role;
		this.userGroup = userGroupId;
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId userId, UserRole role, MidataId recordId, MidataId pluginId, MidataId executorId, long created, String remoteAddr, boolean autoimport, MidataId userGroup) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
		this.executorId = executorId;
		this.created = created;
		this.remoteAddress = remoteAddr;
		this.autoimport = autoimport;
		this.role = role;
		this.userGroup = userGroup;
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId userId, UserRole role, MidataId recordId) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.role = role;
	}
	
	public static String remoteAddr(Request req) {
		if (req.hasHeader("X-Real-IP-LB")) {
			return req.header("X-Real-IP-LB").get();
		}
		if (req.hasHeader("X-Real-IP")) {
			return req.header("X-Real-IP").get();
		}
		return req.remoteAddress();
	}

	public String encrypt(Request req) throws InternalServerException {
		this.created = System.currentTimeMillis();
		this.remoteAddress = remoteAddr(req);
		return encrypt();
	}
	
	public String encrypt() throws InternalServerException  {		
		Map<String, String> map = new ChainedMap<String, String>().put("instanceId", spaceId.toString()).put("userId", userId.toString())
				.get();
		if (recordId != null) map.put("r", recordId.toString());
		if (pluginId != null) map.put("p", pluginId.toString());
		map.put("R", role.toShortString());
		if (executorId != null && !executorId.equals(userId)) map.put("e", executorId.toString());
		if (autoimport) map.put("a", "1");
		if (userGroup != null) map.put("g", userGroup.toString());
		map.put("c", Long.toString(this.created));
		map.put("i", this.remoteAddress);
		map.put("h", handle);
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	
	public static SpaceToken decryptAndSession(Request request, String unsafeSecret) throws AppException {
		SpaceToken res = decrypt(request, unsafeSecret);
		if (res == null) return null;
		KeyManager.instance.continueSession(res.handle, res.executorId);
		return res;
	}
	
	public static SpaceToken decrypt(Request request, String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
								
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);			
			JsonNode json = Json.parse(plaintext);
			return decrypt(request, json);
		} catch (Exception e) {			
			AccessLog.logException("decrypt", e);
			return null;
		}
	}
	
	public static SpaceToken decrypt(Request request, JsonNode json) {
		try {
			MidataId spaceId = new MidataId(json.get("instanceId").asText());
			MidataId userId = new MidataId(json.get("userId").asText());
			MidataId recordId = json.has("r") ? new MidataId(json.get("r").asText()) : null;
			MidataId pluginId = json.has("p") ? new MidataId(json.get("p").asText()) : null;
			MidataId executorId = json.has("e") ? new MidataId(json.get("e").asText()) : userId;
			MidataId userGroup = json.has("g") ? new MidataId(json.get("g").asText()) : null;
			String handle = json.get("h").asText();
			long created = json.get("c").asLong();
			String remoteAddr = json.get("i").asText();
			
			if (System.currentTimeMillis() > created + LIFETIME) return null;
			if (!remoteAddr.equals("all") && !remoteAddr.equals("::1")) {				
			  if (!remoteAddr(request).equals(remoteAddr)) return null;
			}
			UserRole role = UserRole.fromShortString(json.get("R").asText());
			
			return new SpaceToken(handle, spaceId, userId, role, recordId, pluginId, executorId, created, remoteAddr, json.has("a"), userGroup);
		} catch (Exception e) {
			return null;
		}
	}
}
