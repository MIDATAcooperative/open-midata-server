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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import models.MobileAppInstance;
import models.User;
import models.enums.EMailStatus;
import models.enums.SecondaryAuthType;
import models.enums.UserRole;
import play.libs.Json;
import utils.collections.CMaps;
import utils.context.AccessContext;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;

public class ExtendedSessionToken extends PortalSessionToken {
	
	public final static long PORTAL_LIFETIME = 1000l * 60l * 60l * 8l;

	public MidataId appInstanceId;
	
	//public MidataId ownerId;
	
	//public MidataId orgId;
	
	//public MidataId developerId;
	
	public MidataId appId;
	
	public MidataId studyId;
	
	//public UserRole userRole;
	
	//public long created;
	
	public String device;
	
	public String aeskey;
	
	//public String handle;
	
	//public String remoteAddress = "all";
	
    public String state;
	
	public String codeChallenge;
	
	public String codeChallengeMethod;
	
	public String securityToken;
	
	public int flags;
	
	public Set<MidataId> confirmations;
	
	public AccessContext currentContext;
	
	public String joinCode;
	
	public ExtendedSessionToken() {
		
	}
	
	public void setAppConfirmed() {
	  flags  |= (1 << 0);	
	}
	
	public boolean getAppConfirmed() {
		return (flags & (1 << 0)) > 0;
	}
	
	public ExtendedSessionToken withAppUnlockedWithCode() {
		setAppUnlockedWithCode();
		return this;
	}
	
	public void setAppUnlockedWithCode() {
		flags |= (1 << 1);
	}
	
	public boolean getAppUnlockedWithCode() {
		return (flags & (1 << 1)) > 0;
	}
	
	public void setPortal() {
		flags |= (1 << 2);
	}
	
	public boolean getPortal() {
		return (flags & (1 << 2)) > 0;
	}
	
	public void setFake() {
		flags |= (1 << 3);
	}
	
	public boolean getFake() {
		return (flags & (1 << 3)) > 0;
	}
	
	public void setIsChallengeResponse() {
		flags |= (1 << 4);
	}
	
	public boolean getIsChallengeResponse() {
		return (flags & (1 << 4)) > 0;
	}
	
	public void setIsAuthenticated() {
		flags |= (1 << 5);
	}
	
	public boolean getIsAuthenticated() {
		return (flags & (1 << 5)) > 0;
	}
	
	protected void populate(Map<String, Object> map) {
		super.populate(map);
		if (appInstanceId != null) map.put("i", appInstanceId.toString());
		if (appId != null) map.put("a", appId.toString());
		if (device != null) map.put("dv", device);
		if (aeskey != null) map.put("p", aeskey);
		
		if (studyId != null) map.put("st", studyId.toString());
				
		if (state != null) map.put("s", state);
		
		if (securityToken != null) map.put("t", securityToken);
								
		if (codeChallenge != null) map.put("cs", codeChallenge);
		if (codeChallengeMethod != null) map.put("csm", codeChallengeMethod);
		
		map.put("f", flags);
		
		if (confirmations != null && !confirmations.isEmpty()) {
			Set<String> conf = new HashSet<String>();
			for (MidataId id : confirmations) conf.add(id.toString());
			map.put("co", conf);
		}
		
		if (joinCode != null) map.put("jc", joinCode);

	}
	
	public String encrypt() throws InternalServerException {
		Map<String, Object> map = CMaps.map("c", created);
		
		populate(map);
				
		String json = Json.stringify(Json.toJson(map));
		return TokenCrypto.encryptToken(json);
	}

	protected void fetch(JsonNode json) {
		super.fetch(json);
		
		this.appInstanceId = MidataId.from(getstr(json, "i"));
		this.appId = MidataId.from(getstr(json, "a"));
		this.device = getstr(json, "dv");
		this.aeskey = getstr(json ,"p");		
		this.studyId = MidataId.from(getstr(json, "st"));		
		this.state = getstr(json, "s");
	    this.securityToken = getstr(json, "t");
		this.codeChallenge = getstr(json, "cs");
		this.codeChallengeMethod = getstr(json, "csm");
		this.joinCode = getstr(json,"jc");
		String flags = getstr(json, "f");
		if (flags != null) this.flags = Integer.parseInt(flags);
		if (json.has("co")) {
			this.confirmations = JsonExtraction.extractMidataIdSet(json.get("co"));
		}

	}
	
	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static ExtendedSessionToken decrypt(String unsafeSecret) {
		try {			
			String plaintext = TokenCrypto.decryptToken(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ExtendedSessionToken result = new ExtendedSessionToken();
			result.fetch(json);
			return result;
		} catch (Exception e) {
			return null;
		}
	}
	
	public ExtendedSessionToken asCodeExchangeToken() {
		ExtendedSessionToken result = new ExtendedSessionToken();
		result.aeskey = this.aeskey;
		result.appInstanceId = this.appInstanceId;
		result.codeChallenge = this.codeChallenge;
		result.codeChallengeMethod = this.codeChallengeMethod;
		result.created = System.currentTimeMillis();
		result.device = this.device;
		result.state = this.state;		
		
		return result;
	}
		
	
	public OAuthRefreshToken asRefreshToken() {		
		if (!getIsAuthenticated()) throw new NullPointerException();
		return new OAuthRefreshToken(this.appId, this.appInstanceId, this.ownerId, this.aeskey, System.currentTimeMillis());
	}
		
    public ExtendedSessionToken forUser(User user) {    	
    	this.ownerId = user._id;
    	this.userRole = user.role;
    	this.developerId = user.developer;
    	this.created = System.currentTimeMillis();
    	setIsAuthenticated();
    	return this;
    }
    
    public ExtendedSessionToken forFake() {    	
    	this.userRole = UserRole.ANY;
    	this.ownerId = new MidataId();    	
    	this.created = System.currentTimeMillis();
    	setFake();
    	return this;
    }
    
    public ExtendedSessionToken withSession(String handle) {
    	this.handle = handle;    	
    	return this;
    }
    
    public ExtendedSessionToken withApp(MidataId appId, String device) {    	
    	this.appId = appId;
    	this.device = device;
		//Disabled for two page registration
		//setAppConfirmed();
    	return this;
    }
    
    public ExtendedSessionToken withJoinCode(String joinCode) {
    	this.joinCode = joinCode;
    	return this;
    }
    
    public ExtendedSessionToken withAppInstance(MobileAppInstance appInstance) {
    	this.appInstanceId = appInstance._id;
    	this.appId = appInstance.applicationId;    	
    	return this;
    }
    
    public ExtendedSessionToken withConfirmations(Set<MidataId> confirmations) {
    	this.confirmations = confirmations;
    	return this;
    }
    
    public boolean is2FAVerified(User user) {
    	if (user.mobileStatus == EMailStatus.VALIDATED && user.authType != null && user.authType != SecondaryAuthType.NONE && this.securityToken==null) return false;
    	return true;
	}
    
    @Override
    public PortalSessionToken asPortalSession() {
    	if (!getIsAuthenticated()) throw new NullPointerException();
	    return super.asPortalSession();	
    }
	
}
