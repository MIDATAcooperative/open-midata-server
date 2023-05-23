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

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Circles;
import controllers.OAuth2;
import models.Consent;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Space;
import models.User;
import models.enums.ConsentStatus;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.collections.CMaps;
import utils.collections.RequestCache;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ActionTokenAccessContext;
import utils.context.ContextManager;
import utils.context.SpaceAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class ExecutionInfo {
		
	public static AccessContext checkToken(Request request, String token, boolean allowInactive, boolean allowRestricted) throws AppException {
		String plaintext = TokenCrypto.decryptToken(token);
		if (plaintext == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");	
		JsonNode json = Json.parse(plaintext);
		if (json == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		
		if (json.has("instanceId")) {
			return checkSpaceToken(SpaceToken.decrypt(request, json));
		} else if (json.has("action"))  {
			return checkActionToken(request, token);
		} else {
			return checkMobileToken(request, MobileAppSessionToken.decrypt(json), allowInactive, allowRestricted);
		}
	}
	
	public static AccessContext checkSpaceToken(Request request, String token) throws AppException {
		// decrypt authToken 
		SpaceToken authToken = SpaceToken.decrypt(request, token);
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		
		return checkSpaceToken(authToken);
		
	}
	
	public static ActionTokenAccessContext checkActionToken(Request request, String token) throws AppException {
		ActionToken authToken = ActionToken.decrypt(token);
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		return ContextManager.instance.createActionTokenSession(authToken);
	}
	
	public static AccessContext checkSpaceToken(SpaceToken authToken) throws AppException {	
		if (authToken == null) throw new BadRequestException("error.notauthorized.account", "You are not authorized.");
		AccessLog.logBegin("begin check 'space' type session token");
		KeyManager.instance.continueSession(authToken.handle, authToken.executorId);
		
		AccessContext session;
			
		if (authToken.recordId != null) {
						
			session = ContextManager.instance.createSession(authToken.executorId, authToken.role, null, authToken.userId, null);
			 		
			Consent consent = Circles.getConsentById(session, authToken.spaceId, Consent.ALL);
			if (consent != null) {
			  session = session.forConsent(consent); 
			} else {
			  session = session.forAccount();
			}
						
			session = session.forSingleRecord(authToken.recordId);
			
		} else if (authToken.pluginId == null) {							
			Space space = Space.getByIdAndOwner(authToken.spaceId, authToken.autoimport ? authToken.userId : authToken.executorId, Sets.create("visualization", "app", "aps", "autoShare", "sharingQuery", "writes", "owner", "name"));
			if (space == null) throw new BadRequestException("error.unknown.space", "The current space does no longer exist.");
							
			MidataId ownerId = authToken.userId;
			
			
			session = ContextManager.instance.createSession(authToken.executorId, authToken.role, space.visualization, ownerId, null);
					
			User targetUser = Member.getById(authToken.userId,Sets.create("myaps", "tokens"));
			if (targetUser == null) {
				Consent c = Circles.getConsentById(session, authToken.userId, Sets.create("owner", "authorized"));
				if (c != null) {
					ownerId = c.owner;
				} else throw new BadRequestException("error.internal", "Invalid authToken.");
			}
			
			session = ContextManager.instance.createSession(authToken.executorId, authToken.role, space.visualization, ownerId, null);
			session = session.forSpace(space, ownerId);			
			
			if (session.getAccessorRole().equals(UserRole.PROVIDER) && !ownerId.equals(authToken.executorId)) {
				session = ((SpaceAccessContext) session).withRestrictions(CMaps.map("consent-type-exclude", "HEALTHCARE"));
			}
			
		} else if (authToken.autoimport) {
			MobileAppInstance appInstance = MobileAppInstance.getById(authToken.spaceId, MobileAppInstance.APPINSTANCE_ALL);
			if (appInstance == null) OAuth2.invalidToken(); 

		    if (!appInstance.status.equals(ConsentStatus.ACTIVE)) throw new BadRequestException("error.noconsent", "Consent needs to be confirmed before creating records!");
		        		        												                                              			
			session = ContextManager.instance.createSession(authToken.executorId, authToken.role, appInstance.applicationId, appInstance.owner, null);
			session = session.forApp(appInstance);
			
			if (appInstance.sharingQuery == null) {
				appInstance.sharingQuery = RecordManager.instance.getMeta(session, authToken.spaceId, "_query").toMap();
			}
			
		} else {
			// Unhandeled case. How can this happen?
			throw new NullPointerException();
		}
	   AccessLog.log("using as context:", session.toString());
	   AccessLog.logEnd("end check 'space' type session token");
	   return session;	
		
	}
	
	public static AccessContext checkMobileToken(Request request, String token, boolean allowInactive, boolean allowRestricted) throws AppException {		
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(token);
		if (authToken == null) OAuth2.invalidToken(); 
				
		return checkMobileToken(request, authToken, allowInactive, allowRestricted);		
	}
	
	private static boolean verifyAddress(String ip, String allowed) throws InternalServerException {
		String[] ranges = allowed.split(",");
		for (String range : ranges) {
			String[] r = range.trim().split("/");
			if (matches(r[0], ip, Integer.parseInt(r[1]))) return true;
		}
		return false;
	}
	
	public static boolean matches(String required, String address, int maskBits) throws InternalServerException {
		try {
			InetAddress requiredAddress = InetAddress.getByName(required);
			InetAddress remoteAddress = InetAddress.getByName(address);
			if (!requiredAddress.getClass().equals(remoteAddress.getClass())) {
				return false;
			}
			if (maskBits < 0) {
				return remoteAddress.equals(requiredAddress);
			}
			byte[] remAddr = remoteAddress.getAddress();
			byte[] reqAddr = requiredAddress.getAddress();
			int maskFullBytes = maskBits / 8;
			byte finalByte = (byte) (0xFF00 >> (maskBits & 0x07));
			for (int i = 0; i < maskFullBytes; i++) {
				if (remAddr[i] != reqAddr[i]) {
					return false;
				}
			}
			if (finalByte != 0) {
				return (remAddr[maskFullBytes] & finalByte) == (reqAddr[maskFullBytes] & finalByte);
			}
			return true;
		} catch (UnknownHostException e) {
			throw new InternalServerException("error.internal", "Unknown host");
		}
	}
		
	public static AccessContext checkMobileToken(Request request, MobileAppSessionToken authToken, boolean allowInactive, boolean allowRestricted) throws AppException {		
		if (authToken == null) OAuth2.invalidToken();				
		
		AccessLog.logBegin("begin check 'mobile' type session token");
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, MobileAppInstance.APPINSTANCE_ALL);
        if (appInstance == null) OAuth2.invalidToken(); 
        if (appInstance.status.equals(ConsentStatus.REJECTED) || appInstance.status.equals(ConsentStatus.EXPIRED) || appInstance.status.equals(ConsentStatus.FROZEN)) OAuth2.invalidToken();
        
        if (!allowInactive && !appInstance.status.equals(ConsentStatus.ACTIVE)) throw new BadRequestException("error.noconsent", "Consent needs to be confirmed before creating records!");
        
        Plugin plugin = Plugin.getById(appInstance.applicationId);
        if (plugin.allowedIPs != null) {
        	String reqIp = SpaceToken.remoteAddr(request);
        	if (!verifyAddress(reqIp, plugin.allowedIPs)) OAuth2.invalidToken(); 
        }
        
        KeyManager.instance.login(60000l, false);
        if (KeyManager.instance.unlock(appInstance._id, authToken.aeskey) == KeyManager.KEYPROTECTION_FAIL) {
        	OAuth2.invalidToken(); 
        }
               				       
		AccessContext tempContext = ContextManager.instance.createLoginOnlyContext(authToken.appInstanceId, authToken.role, appInstance);
		
		if (appInstance.sharingQuery == null) {
			appInstance.sharingQuery = RecordManager.instance.getMeta(tempContext, authToken.appInstanceId, "_query").toMap();
		}
				
		AccessContext session = ContextManager.instance.upgradeSessionForApp(tempContext, appInstance);
		
		if (authToken.restrictedResourceId != null) {
			if (allowRestricted) {
			  session = session.forSingleRecord(authToken.restrictedResourceId);
			} else OAuth2.invalidToken();
		}
		AccessLog.log("using as context:", session.toString());
		AccessLog.logEnd("end check 'mobile' type session token");
		
        return session;						
		
	}
}
