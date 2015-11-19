package utils.auth;

import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.ChainedMap;

import com.fasterxml.jackson.databind.JsonNode;

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

	public String encrypt() {		
		String json = Json.stringify(Json.toJson(this));
		return Crypto.encryptAES(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static RecordToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = Crypto.decryptAES(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			String appId = json.get("recordId").asText();
			String userId = json.get("apsId").asText();
			return new RecordToken(appId, userId);
		} catch (Exception e) {
			return null;
		}
	}
}
