package utils.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import play.libs.Json;
import play.mvc.Http.Request;
import utils.collections.ChainedMap;
import utils.exceptions.AuthException;
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
	
	public String handle;

	public SpaceToken(String handle, MidataId spaceId, MidataId userId) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = null;
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId userId, MidataId recordId, MidataId pluginId) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId userId, MidataId recordId, MidataId pluginId, MidataId executorId) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
		this.executorId = executorId;	
		this.created = System.currentTimeMillis();
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId userId, MidataId recordId, MidataId pluginId, MidataId executorId, long created, String remoteAddr) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
		this.executorId = executorId;
		this.created = created;
		this.remoteAddress = remoteAddr;
	}
	
	public SpaceToken(String handle, MidataId spaceId, MidataId userId, MidataId recordId) {
		this.handle = handle;
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
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
	
	public String encrypt() throws InternalServerException  {		
		Map<String, String> map = new ChainedMap<String, String>().put("instanceId", spaceId.toString()).put("userId", userId.toString())
				.get();
		if (recordId != null) map.put("r", recordId.toString());
		if (pluginId != null) map.put("p", pluginId.toString());
		if (executorId != null && !executorId.equals(userId)) map.put("e", executorId.toString());
		map.put("c", Long.toString(this.created));
		map.put("i", this.remoteAddress);
		map.put("h", handle);
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	
	public static SpaceToken decryptAndSession(Request request, String unsafeSecret) throws AuthException {
		SpaceToken res = decrypt(request, unsafeSecret);
		KeyManager.instance.continueSession(res.handle);
		return res;
	}
	
	public static SpaceToken decrypt(Request request, String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			return decrypt(request, json);
		} catch (Exception e) {
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
			String handle = json.get("h").asText();
			long created = json.get("c").asLong();
			String remoteAddr = json.get("i").asText();
			
			if (System.currentTimeMillis() > created + LIFETIME) return null;
			if (!remoteAddr.equals("all")) {
			  if (!remoteAddr(request).equals(remoteAddr)) return null;
			}
			
			return new SpaceToken(handle, spaceId, userId, recordId, pluginId, executorId, created, remoteAddr);
		} catch (Exception e) {
			return null;
		}
	}
}
