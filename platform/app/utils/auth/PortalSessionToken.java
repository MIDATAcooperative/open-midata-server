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
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import models.User;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

public class PortalSessionToken {

	public final static long LIFETIME = 1000l * 60l * 60l * 8l;

	/**
	 * id of user
	 */
	public MidataId ownerId;
	
	public MidataId orgId;
	
	public MidataId developerId;

	public UserRole userRole;
	
	public String handle;

	/**
	 * Creation time of token
	 */
	public long created;

	/**
	 * IP Address for which the token is valid
	 */
	public String remoteAddress = "all";
	
	

	public MidataId getOwnerId() {
		return ownerId;
	}

	public MidataId getOrgId() {
		return orgId;
	}
	
	public MidataId getDeveloperId() {
		return developerId;
	}

	public UserRole getRole() {
		return userRole;
	}

	public long getCreated() {
		return created;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}
	
	public String getHandle() {
		return handle;
	}

	public PortalSessionToken() {
		
	}
	
	public PortalSessionToken(String handle, MidataId userId, UserRole role, MidataId org, MidataId developer) {
		this.handle = handle;
		this.ownerId = userId;
		this.userRole = role;
		this.orgId = org;
		this.developerId = developer;
		this.created = System.currentTimeMillis();
	}

	public PortalSessionToken(String handle, MidataId userId, UserRole role, MidataId org, MidataId developer, long created, String remoteAddr) {
		this.handle = handle;
		this.ownerId = userId;
		this.userRole = role;
		this.orgId = org;
		this.developerId = developer;
		this.created = created;
		this.remoteAddress = remoteAddr;
	}

	private static String remoteAddr(Request req) {
		Optional<String> ra = req.header("X-Real-IP-LB");
		if (ra.isPresent()) return ra.get();
		ra = req.header("X-Real-IP");
		if (ra.isPresent()) return ra.get();		
		return req.remoteAddress();
	}

	public void setRemoteAddress(Request req) {
		this.remoteAddress = remoteAddr(req);
	}
	
	public void setTimeout(long timeout) {
		this.created = System.currentTimeMillis() - LIFETIME + timeout;
	}
					
	protected void populate(Map<String, Object> map) {		
		if (userRole == null && ownerId != null) throw new NullPointerException();
		
		if (handle != null) map.put("h", handle);
		if (ownerId != null) map.put("o", ownerId.toString());		
		if (orgId != null) map.put("or", orgId.toString());		
		if (developerId != null) map.put("d", developerId.toString());						
		if (userRole != null) map.put("r", userRole.toString());
		if (!remoteAddress.equals("all")) map.put("ra", remoteAddress);
				
	}

	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("c", created);
		
		populate(map);
				
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}
	
	protected static final String getstr(JsonNode json, String n) {
		JsonNode jn = json.get(n);
		if (jn == null) return null;
		return jn.asText();
	}

	
	protected void fetch(JsonNode json) {
		this.created = json.get("c").asLong();
		
		this.handle = getstr(json, "h");
		this.ownerId = MidataId.from(getstr(json, "o"));
		this.orgId = MidataId.from(getstr(json, "or"));
		this.developerId = MidataId.from(getstr(json, "d"));
		
		String userRole = getstr(json, "r");
		if (userRole != null) this.userRole = UserRole.valueOf(userRole); 
		String ra = getstr(json, "ra");
		if (ra != null) this.remoteAddress = ra;				

	}
	
	/**
	 * The secret passed here can be an arbitrary string, so check all possible
	 * exceptions.
	 */
	public static PortalSessionToken decrypt(Request request) {
		try {
			String secret = request.header("X-Session-Token").orElse(null);
			if (secret == null || secret.length()==0) {
				String[] token = request.queryString().get("token");
				if (token!=null && token.length == 1) secret = token[0];
			}
			if (secret == null || secret.length()==0) return null;
									
			PortalSessionToken result = PortalSessionToken.decrypt(secret);
			if (result == null) return null;			
			
			if (System.currentTimeMillis() > result.created + LIFETIME) {				
				return null;
			}
			if (!result.remoteAddress.equals("all")) {
				if (!remoteAddr(request).equals(result.remoteAddress)) {
					AccessLog.log("bad ip address tk="+result.remoteAddress+" f="+remoteAddr(request));
					return null;
				}
			}
						
            session.set(result);           
			return result;
		} catch (Exception e) {
			AccessLog.logException("token decrypt", e);
			return null;
		}
	}
	
	public void set() {
		session.set(this);
	}
	
	public static PortalSessionToken decrypt(String unsafeSecret) {
		try {			
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			PortalSessionToken result = json.has("f") ? new ExtendedSessionToken() : new PortalSessionToken();
			result.fetch(json);
			return result;
		} catch (Exception e) {
			return null;
		}
	}
	
	public PortalSessionToken asPortalSession() {
		PortalSessionToken result = new PortalSessionToken();
		result.ownerId = this.ownerId;
		result.orgId = this.orgId;		
		result.developerId = this.developerId;		
		result.userRole = this.userRole;
		result.handle = this.handle;		
		result.created = this.created;
		result.remoteAddress = this.remoteAddress;
				
		return result;
	}
	
	public boolean is2FAVerified(User user) {
		return true;
	}
	
	private static ThreadLocal<PortalSessionToken> session = new ThreadLocal<PortalSessionToken>();
	
	public static PortalSessionToken session() {
		return session.get();
	}
	
	public static void clear() {
		session.set(null);
	}
}
