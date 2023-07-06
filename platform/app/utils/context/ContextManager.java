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

package utils.context;

import java.util.List;
import java.util.Map;

import models.Consent;
import models.MidataId;
import models.MobileAppInstance;
import models.UserGroupMember;
import models.enums.Permission;
import models.enums.UserRole;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.access.APSCache;
import utils.access.Feature_UserGroups;
import utils.access.RecordManager;
import utils.auth.ActionToken;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;


/**
 * Create AccessContext as primary context information source for current request or action 
 * @author alexander
 *
 */
public class ContextManager {
	
	public static ContextManager instance = new ContextManager();
	
	private static ThreadLocal<AccessContext> threadContext = new ThreadLocal<AccessContext>();

	/**
	 * returns intra-request cache
	 * @param who person who does the current request
	 * @return APSCache
	 * @throws InternalServerException
	 */
	protected APSCache getCache(MidataId who) throws InternalServerException {
		AccessContext current = threadContext.get(); 
		if (current == null) return new APSCache(who, who);
		APSCache result = current.getCache();
		if (!result.getAccessor().equals(who)) throw new InternalServerException("error.internal", "Owner Change!");
		return result;
	}
	
	/**
	 * Use this context for current request
	 * @param context
	 * @return
	 * @throws InternalServerException
	 */
	protected AccessContext use(AccessContext context) throws InternalServerException {
		threadContext.set(context);
		return context;
	}
	
	/**
	 * only to be used by RecordManager.
	 * @return
	 */
	public APSCache currentCacheUsed() {
		AccessContext current = threadContext.get(); 
		return current == null ? null : current.getCache();
	}
	
	public void setAccountOwner(MidataId executor, MidataId accountOwner) throws InternalServerException {
		getCache(executor).setAccountOwner(accountOwner);
	}
	
	/**
	 * Clear Cache and Keystore after request
	 */
	public void clear() {
		clearCache();
		KeyManager.instance.clear();
	}
	
	/**
	 * Clear Cache
	 */
	public void clearCache() {
		AccessContext old = threadContext.get();
		if (old != null) {
			old.cleanup();			
			threadContext.set(null);
		}
	}
	
	/**
	 * Create a new AccessContext for a request
	 * @param accessorId
	 * @param accessorRole
	 * @param pluginId
	 * @param legacyOwner
	 * @param overrideBaseUrl
	 * @return
	 * @throws InternalServerException
	 */
	public AccessContext createSession(MidataId accessorId, UserRole accessorRole, MidataId pluginId, MidataId legacyOwner, String overrideBaseUrl) throws InternalServerException {
		AccessLog.log("[session] new context for ", accessorId.toString());
		return use(new SessionAccessContext(getCache(accessorId), accessorRole, pluginId, overrideBaseUrl, legacyOwner));
	}
	
	/**
	 * Create a AccessContext from portal session
	 * @param token
	 * @return
	 * @throws InternalServerException
	 */
	public AccessContext createSession(PortalSessionToken token) throws InternalServerException {
		AccessLog.log("[session] portal context for ", token.getOwnerId().toString());
		return use(new SessionAccessContext(getCache(token.getOwnerId()), token.getRole(), null, null, token.getOwnerId()));
	}
	
	/**
	 * Create AccessContext for an request that creates a new user
	 * @param accessorId
	 * @param accessorRole
	 * @param pluginId
	 * @return
	 * @throws InternalServerException
	 */
	public AccessContext createInitialSession(MidataId accessorId, UserRole accessorRole, MidataId pluginId) throws InternalServerException {
		AccessLog.log("[session] Initial context for ", accessorId.toString());
		return use(new SessionAccessContext(getCache(accessorId), accessorRole, pluginId, null, accessorId));
	}
	
	/**
	 * Create AccessContext for an request that only uses an action token
	 * @param token
	 * @return
	 * @throws InternalServerException
	 */
	public ActionTokenAccessContext createActionTokenSession(ActionToken token) throws InternalServerException {
		AccessLog.log("[session] Action-token context for ", token.action.toString());
		return (ActionTokenAccessContext) use(new ActionTokenAccessContext(token));
	}
	
	/**
	 * Create AccessContext for an already running action that is done by multiple Threads
	 * @param executorId
	 * @param role
	 * @return
	 * @throws InternalServerException
	 */
	public AccessContext createSessionForDownloadStream(MidataId executorId, UserRole role) throws InternalServerException {
		return use(new SessionAccessContext(getCache(executorId), role, null, null, executorId));		
	}
						
	/**
	 * Create an AccessContext used during login	
	 * @param executorId
	 * @param role
	 * @return
	 * @throws InternalServerException
	 */
	public AccessContext createLoginOnlyContext(MidataId executorId, MidataId usedApp, UserRole role) throws InternalServerException {
		AccessLog.log("[session] Login-context for ", executorId.toString());
		return use(new PreLoginAccessContext(getCache(executorId), role, usedApp).forAccount());
	}
	
	/**
	 * Create an AccessContext used during login
	 * @param executorId
	 * @param role
	 * @param appInstance
	 * @return
	 * @throws AppException
	 */
	public AccessContext createLoginOnlyContext(MidataId executorId, UserRole role, MobileAppInstance appInstance) throws AppException {
		AccessLog.log("[session] Login-context for ", executorId.toString());
		return use(new PreLoginAccessContext(getCache(executorId), role, appInstance.applicationId).forApp(appInstance));
	}
			
	
	public AccessContext createSharingContext(AccessContext context1, MidataId aps) throws AppException {
		AccessLog.log("create sharing context user=", context1.getAccessor().toString(), " source aps=", aps.toString());
		//if (!context1.canCreateActiveConsents() && context1.getOwner().equals(aps)) return context1;
		
		APSCache cache = context1.getCache();
		AccessContext context = new AccountAccessContext(cache, null);
		if (context1.getAccessor().equals(aps)) return context;
		
		List<UserGroupMember> ugms = cache.getByGroupAndActiveMember(aps, context1.getAccessor(), Permission.READ_DATA);
		if (ugms!=null) {
			return context.forUserGroup(ugms);			
		}
	
		
		if (context.getCache().hasSubCache(aps)) return new RepresentativeAccessContext(context.getCache().getSubCache(aps), context);
		
		Consent consent = Consent.getRepresentativeActiveByAuthorizedAndOwner(context1.getAccessor(), aps);
		
		if (consent != null) {				
			Map<String, Object> meta = RecordManager.instance.getMeta(context, consent._id, "_representative").toMap();
			if (meta.containsKey("aliaskey") && meta.containsKey("alias")) {
				AccessLog.log("Act as representative: unlock ", aps.toString());
				MidataId alias = new MidataId(meta.get("alias").toString());
				byte[] key = (byte[]) meta.get("aliaskey");
				KeyManager.instance.unlock(aps, alias, key);			
				//ContextManager.instance.clearCache();
				
				return new RepresentativeAccessContext(context.getCache().getSubCache(aps), context);
			}
		}
		
		throw new InternalServerException("error.internal", "Context for data sharing cannot be created");
	}
		
	
	public AccessContext createRootPublicUserContext() throws InternalServerException {
		AccessLog.log("[session] Start PUBLIC Session");
		return use(new SessionAccessContext(getCache(RuntimeConstants.instance.publicUser), UserRole.ANY, null, null, RuntimeConstants.instance.publicUser).forAccount());
	}
	
	public AccessContext createAdminRootPublicGroupContext() throws AppException {
		AccessLog.log("[session] Start PUBLIC Group Session");
		return use(new SessionAccessContext(getCache(RuntimeConstants.instance.publicUser), UserRole.ADMIN, null, null, RuntimeConstants.instance.publicUser).forPublic());
	}

	public AccessContext upgradeSessionForApp(AccessContext tempContext, MobileAppInstance appInstance) throws AppException {
	    return upgradeSessionForApp(tempContext, appInstance, null);
	}
	
	public AccessContext upgradeSessionForApp(AccessContext tempContext, MobileAppInstance appInstance, String baseUrl) throws AppException {
		Map<String, Object> appobj = RecordManager.instance.getMeta(tempContext, appInstance._id, "_app").toMap();
		MidataId executorId = tempContext.getAccessor();
		if (appobj.containsKey("aliaskey") && appobj.containsKey("alias")) {
			MidataId alias = new MidataId(appobj.get("alias").toString());
			byte[] key = (byte[]) appobj.get("aliaskey");
			if (appobj.containsKey("targetAccount")) {
				executorId = MidataId.from(appobj.get("targetAccount").toString());
			} else executorId = appInstance.owner;
			KeyManager.instance.unlock(executorId, alias, key);		
			clearCache();
			return createSession(executorId, tempContext.getAccessorRole(), appInstance.applicationId, appInstance.owner, null).forApp(appInstance);
		} else {
			setAccountOwner(appInstance._id, appInstance.owner);
		}
		return createSession(appInstance._id, tempContext.getAccessorRole(), appInstance.applicationId, appInstance.owner, null).forApp(appInstance);
	}
	
	
	
	
}
