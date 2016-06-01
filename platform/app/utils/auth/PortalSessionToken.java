package utils.auth;

import java.util.Map;

import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.collections.ChainedMap;
import utils.exceptions.InternalServerException;

import com.fasterxml.jackson.databind.JsonNode;

public class PortalSessionToken {

	public final static long LIFETIME = 1000 * 60 * 60 * 8;

	/**
	 * id of user
	 */
	public ObjectId userId;
	
	public ObjectId org;

	public UserRole role;

	/**
	 * Creation time of token
	 */
	public long created;

	/**
	 * IP Address for which the token is valid
	 */
	public String remoteAddress = "all";
	
	

	public ObjectId getUserId() {
		return userId;
	}

	public ObjectId getOrg() {
		return org;
	}

	public UserRole getRole() {
		return role;
	}

	public long getCreated() {
		return created;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public PortalSessionToken(ObjectId userId, UserRole role, ObjectId org) {
		this.userId = userId;
		this.role = role;
		this.org = org;
	}

	public PortalSessionToken(ObjectId userId, UserRole role, ObjectId org, long created, String remoteAddr) {
		this.userId = userId;
		this.role = role;
		this.org = org;
		this.created = created;
		this.remoteAddress = remoteAddr;
	}

	private static String remoteAddr(Request req) {
		if (req.hasHeader("X-Real-IP")) {
			return req.getHeader("X-Real-IP");
		}
		return req.remoteAddress();
	}

	public String encrypt(Request req) throws InternalServerException {
		this.created = System.currentTimeMillis();
		this.remoteAddress = remoteAddr(req);
		return encrypt();
	}
	
	public String encrypt(Request req, long timeout) throws InternalServerException {
		this.created = System.currentTimeMillis() - LIFETIME + timeout;
		this.remoteAddress = remoteAddr(req);
		return encrypt();
	}

	public String encrypt() throws InternalServerException {
		Map<String, String> map = new ChainedMap<String, String>().put("u", userId.toString()).put("r", role.toString()).get();
		if (org != null) map.put("o", org.toString());
		map.put("c", Long.toString(this.created));
		map.put("i", this.remoteAddress);
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible
	 * exceptions.
	 */
	public static PortalSessionToken decrypt(Request request) {
		try {
			String secret = request.getHeader("X-Session-Token");
			if (secret == null || secret.length()==0) {
				String[] token = request.queryString().get("token");
				if (token!=null && token.length == 1) secret = token[0];
			}
			if (secret == null || secret.length()==0) return null;
			
			String plaintext = TokenCrypto.decryptToken(secret);		
			JsonNode json = Json.parse(plaintext);
			ObjectId userId = new ObjectId(json.get("u").asText());
			UserRole role = UserRole.valueOf(json.get("r").asText());
			long created = json.get("c").asLong();
			String remoteAddr = json.get("i").asText();
			ObjectId org = null;
			if (json.has("o")) {
			  org = new ObjectId(json.get("o").asText()); 
			}

			if (System.currentTimeMillis() > created + LIFETIME) {				
				return null;
			}
			if (!remoteAddr.equals("all")) {
				if (!remoteAddr(request).equals(remoteAddr)) {
					AccessLog.log("bad ip address");
					return null;
				}
			}
			
			PortalSessionToken currentSession = new PortalSessionToken(userId, role, org, created, remoteAddr);
            session.set(currentSession);           
			return currentSession;
		} catch (Exception e) {
			AccessLog.logException("token decrypt", e);
			return null;
		}
	}
	
	private static ThreadLocal<PortalSessionToken> session = new ThreadLocal<PortalSessionToken>();
	
	public static PortalSessionToken session() {
		return session.get();
	}
}
