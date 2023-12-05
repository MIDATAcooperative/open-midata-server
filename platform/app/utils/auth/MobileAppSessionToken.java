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

import java.util.Collections;
import java.util.HashMap;
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
	
	public MidataId restrictedResourceId;
	
	private Map<String, String> extra;

	public MobileAppSessionToken(MidataId appInstanceId, String aeskey, long expirationTime, UserRole role, Map<String, String> extra) {
		this.appInstanceId = appInstanceId;
		this.aeskey = aeskey;
		this.expiration = expirationTime;
		this.role = role;
		this.extra = extra;
	}
	
	public MobileAppSessionToken(MidataId appInstanceId, String aeskey, long expirationTime, UserRole role, MidataId restrictedResourceId, Map<String, String> extra) {
		this.appInstanceId = appInstanceId;
		this.aeskey = aeskey;
		this.expiration = expirationTime;
		this.role = role;
		this.restrictedResourceId = restrictedResourceId;
		this.extra = extra;
	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("a", appInstanceId.toString()).map("p", aeskey).map("c", expiration).map("r", role.toShortString());						
		if (restrictedResourceId != null) map.put("i", restrictedResourceId.toString());
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static MobileAppSessionToken decrypt(String unsafeSecret) {
		try {
            
            Map<String, String> extra = parseExtra(unsafeSecret);            
			String plaintext = TokenCrypto.decryptToken(noExtra(unsafeSecret));
			JsonNode json = Json.parse(plaintext);
			return decrypt(json, extra);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static MobileAppSessionToken decrypt(JsonNode json, Map<String, String> extra) {
		try {			
			MidataId appInstanceId = new MidataId(json.get("a").asText());
			String aeskey = json.get("p").asText();	
			long expirationTime = json.get("c").asLong();
		    if (expirationTime < System.currentTimeMillis()) return null;
		    String r = json.get("r").asText();
		    UserRole role = UserRole.fromShortString(r);
		    MidataId restrictedResourceId = json.has("i") ? MidataId.from(json.get("i").asText()) : null;
			return new MobileAppSessionToken(appInstanceId, aeskey, expirationTime, role, restrictedResourceId, extra);
		} catch (Exception e) {
			return null;
		}
	}
	
	public Map<String, String> getExtra() {
		if (extra == null) return Collections.emptyMap();
		return extra;
	}
	
	protected static String noExtra(String in) {
		  int separator = in.indexOf(";");
          if (separator>0) {
          	return in.substring(0, separator);
          }
          return in;
	}
	
	public static Map<String, String> parseExtra(String in) {
		if (in == null) return null;
		if (in.startsWith("Bearer ")) in = in.substring("Bearer ".length());
		Map<String, String> extra = null;
		String[] parts = in.split(";");
		for (String part : parts) {
			if (part.contains("=")) {
				String[] v = part.split("=");
				if (v.length == 2) {
					if (extra == null) extra = new HashMap<String, String>();
					extra.put(v[0].trim(), v[1].trim());
				}
			}
		}
		return extra;
	}
}
