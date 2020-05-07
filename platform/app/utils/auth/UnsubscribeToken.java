package utils.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import play.libs.Json;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
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
