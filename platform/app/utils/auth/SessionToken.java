package utils.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import models.enums.UserRole;
import play.libs.Json;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

public class SessionToken {

	public MidataId appInstanceId;
	
	public MidataId ownerId;
	
	public MidataId orgId;
	
	public MidataId developerId;
	
	public MidataId appId;
	
	public MidataId studyId;
	
	public UserRole userRole;
	
	public long created;
	
	public String device;
	
	public String aeskey;
	
	public String handle;
	
	public String remoteAddress = "all";
	
    public String state;
	
	public String codeChallenge;
	
	public String codeChallengeMethod;
	
	public String securityToken;
	
	public int flags;
	
	public SessionToken() {
		
	}
	
	public void setAppConfirmed() {
	  flags  |= (1 << 0);	
	}
	
	public boolean getAppConfirmed() {
		return (flags & (1 << 0)) > 0;
	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("c", created);
				
		if (appInstanceId != null) map.put("i", appInstanceId.toString());
		if (appId != null) map.put("a", appId.toString());
		if (device != null) map.put("dv", device);
		if (aeskey != null) map.put("p", aeskey);
		if (handle != null) map.put("h", handle);
		if (ownerId != null) map.put("o", ownerId.toString());
		
		if (orgId != null) map.put("or", orgId.toString());
		
		if (developerId != null) map.put("d", developerId.toString());
		
		if (studyId != null) map.put("st", studyId.toString());
		
		if (userRole != null) map.put("r", userRole.toString());
		
		if (!remoteAddress.equals("all")) map.put("ra", remoteAddress);
		
		if (state != null) map.put("s", state);
		
		if (securityToken != null) map.put("t", securityToken);
								
		if (codeChallenge != null) map.put("cs", codeChallenge);
		if (codeChallengeMethod != null) map.put("csm", codeChallengeMethod);
		
		if (flags != 0) map.put("f", flags);
		
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	private static final String getstr(JsonNode json, String n) {
		JsonNode jn = json.get(n);
		if (jn == null) return null;
		return jn.asText();
	}
	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static SessionToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			SessionToken result = new SessionToken();
			result.created = json.get("c").asLong();
			result.appInstanceId = MidataId.from(getstr(json, "i"));
			result.appId = MidataId.from(getstr(json, "a"));
			result.device = getstr(json, "dv");
			result.aeskey = getstr(json ,"p");
			result.handle = getstr(json, "h");
			result.ownerId = MidataId.from(getstr(json, "o"));
			result.orgId = MidataId.from(getstr(json, "or"));
			result.developerId = MidataId.from(getstr(json, "d"));
			result.studyId = MidataId.from(getstr(json, "st"));
			String userRole = getstr(json, "r");
			if (userRole != null) result.userRole = UserRole.valueOf(userRole); 
			String ra = getstr(json, "ra");
			if (ra != null) result.remoteAddress = ra;
			result.state = getstr(json, "s");
		    result.securityToken = getstr(json, "t");
			result.codeChallenge = getstr(json, "cs");
			result.codeChallengeMethod = getstr(json, "csm");
			String flags = getstr(json, "f");
			if (flags != null) result.flags = Integer.parseInt(flags);
														
			return result;
		} catch (Exception e) {
			return null;
		}
	}
}
