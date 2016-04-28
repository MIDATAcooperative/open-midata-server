package utils.auth;

import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.collections.ChainedMap;
import utils.exceptions.InternalServerException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Authorization token for plugins to access a user's records assigned to a space.
 */
public class SpaceToken {
	
	public final static long LIFETIME = 1000 * 60 * 60 * 8;

	/**
	 * id of space
	 */
	public ObjectId spaceId;
		
	/**
	 * id of user
	 */
	public ObjectId userId;
	
	/**
	 * optional id of record if only access to a single record is allowed
	 */
	public ObjectId recordId;
	
	/**
	 * optional id of plugin
	 */
	public ObjectId pluginId;
	
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
	public ObjectId executorId;

	public SpaceToken(ObjectId spaceId, ObjectId userId) {
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = null;
	}
	
	public SpaceToken(ObjectId spaceId, ObjectId userId, ObjectId recordId, ObjectId pluginId) {
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
	}
	
	public SpaceToken(ObjectId spaceId, ObjectId userId, ObjectId recordId, ObjectId pluginId, ObjectId executorId) {
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
		this.executorId = executorId;	
		this.created = System.currentTimeMillis();
	}
	
	public SpaceToken(ObjectId spaceId, ObjectId userId, ObjectId recordId, ObjectId pluginId, ObjectId executorId, long created, String remoteAddr) {
		this.spaceId = spaceId;
		this.userId = userId;
		this.recordId = recordId;
		this.pluginId = pluginId;
		this.executorId = executorId;
		this.created = created;
		this.remoteAddress = remoteAddr;
	}
	
	public SpaceToken(ObjectId spaceId, ObjectId userId, ObjectId recordId) {
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
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static SpaceToken decrypt(Request request, String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId spaceId = new ObjectId(json.get("instanceId").asText());
			ObjectId userId = new ObjectId(json.get("userId").asText());
			ObjectId recordId = json.has("r") ? new ObjectId(json.get("r").asText()) : null;
			ObjectId pluginId = json.has("p") ? new ObjectId(json.get("p").asText()) : null;
			ObjectId executorId = json.has("e") ? new ObjectId(json.get("e").asText()) : userId;
			long created = json.get("c").asLong();
			String remoteAddr = json.get("i").asText();
			
			if (System.currentTimeMillis() > created + LIFETIME) return null;
			if (!remoteAddr.equals("all")) {
			  if (!remoteAddr(request).equals(remoteAddr)) return null;
			}
			
			return new SpaceToken(spaceId, userId, recordId, pluginId, executorId, created, remoteAddr);
		} catch (Exception e) {
			return null;
		}
	}
}
