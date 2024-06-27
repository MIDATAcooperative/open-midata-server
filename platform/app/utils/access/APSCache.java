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

package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.AccessPermissionSet;
import models.Consent;
import models.MidataId;
import models.StudyRelated;
import models.UserGroupMember;
import models.enums.APSSecurityLevel;
import models.enums.EntityType;
import models.enums.Permission;
import utils.AccessLog;
import utils.access.index.ConsentToKeyIndexRoot;
import utils.access.index.StatsIndexRoot;
import utils.access.index.StreamIndexRoot;
import utils.auth.EncryptionNotSupportedException;
import utils.buffer.WatchesChangeBuffer;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.exceptions.RequestTooLargeException;

/**
 * caches access permission sets during a request. No inter-request caching. 
 *
 */
public class APSCache {

	private Map<String, APS> cache;
	private Map<String, APSCache> subcache;
	private MidataId accessorId;
	private MidataId accountOwner;
	
	private Map<MidataId, Consent> consentCache;
	private Map<MidataId, DBRecord> newRecordCache;
	private Map<MidataId, MidataId[]> ownerToConsent;
	
	private Set<MidataId> touchedConsents = null;
	private Set<MidataId> touchedAPS = null;
	private WatchesChangeBuffer changedPermissions = null;
	private StreamIndexRoot streamIndexRoot = null;
	private StatsIndexRoot statsIndexRoot = null;
	private StatsIndexRoot statsIndexRootPseudo = null;
	private ConsentToKeyIndexRoot consentKeysRoot = null;
	
	private long consentLimit;
	private Map<Permission, Set<UserGroupMember>> userGroupMember;
	
	private static final int CACHE_LIMIT = 5000;
	
	public APSCache(MidataId accessorId, MidataId accountApsId) {
		this.accessorId = accessorId;
		this.accountOwner = accountApsId;
		this.cache = new HashMap<String, APS>();
		this.consentCache = new HashMap<MidataId, Consent>();
		this.consentLimit = -1;
	}
	
	public void clear() {
		cache.clear();
		consentCache.clear();
		if (newRecordCache != null) newRecordCache.clear();
		if (ownerToConsent != null) ownerToConsent.clear();
		if (touchedConsents != null) touchedConsents.clear();
		if (touchedAPS != null) touchedAPS.clear();
		streamIndexRoot = null;
		consentKeysRoot = null;
		if (userGroupMember != null) userGroupMember.clear();
	}
	
	/**
	 * entity (user / group) that accesses APS in cache
	 * @return
	 */
	public MidataId getAccessor() {
		return accessorId;
	}
	
	/**
	 * entity that is the "owner" of the APS in cache 
	 * @return
	 */
	public MidataId getAccountOwner() {
		return accountOwner;
	}
	
	public void setAccountOwner(MidataId accountOwner) {
		this.accountOwner = accountOwner;
	}
	
	public boolean hasAPS(MidataId apsId) throws AppException {
		APS result = cache.get(apsId.toString());
		if (result == null) return false;
		return result.isReady();
	}
	
	public APS aps(EncryptedAPS eaps) {
		return new APSTreeImplementation(eaps);
	}
	
	public APS getAPS(MidataId apsId) throws InternalServerException {
		APS result = cache.get(apsId.toString());
		if (result == null) {
			result = aps(new EncryptedAPS(apsId, accessorId));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public APS getAPS(MidataId apsId, MidataId owner) throws InternalServerException {
		APS result = cache.get(apsId.toString());
		if (result == null) {
			result = aps(new EncryptedAPS(apsId, accessorId, owner));
			cache.put(apsId.toString(), result);
		}	
		return result;
	}
	
	public APS getAPS(MidataId apsId, byte[] unlockKey, MidataId owner) throws AppException, EncryptionNotSupportedException {
		APS result = cache.get(apsId.toString());
		if (result == null) { 
			result = aps(new EncryptedAPS(apsId, accessorId, unlockKey, owner));
			if (!result.isAccessible()) {
				AccessLog.log("Adding missing access for ",accessorId.toString()," APS:",apsId.toString());
				result.addAccess(Collections.<MidataId>singleton(accessorId), true);
			}
			cache.put(apsId.toString(), result);
		} else if (unlockKey != null) {
			result.provideAPSKeyAndOwner(unlockKey, owner);
		}
		return result;
	}
	
	public APS getAPS(MidataId apsId, byte[] unlockKey, MidataId owner, AccessPermissionSet set, boolean addIfMissing) throws AppException, EncryptionNotSupportedException {
		APS result = cache.get(apsId.toString());
		if (result == null) { 
			result = aps(new EncryptedAPS(apsId, accessorId, unlockKey, owner, set));
			if (!result.isAccessible() && addIfMissing) result.addAccess(Collections.<MidataId>singleton(accessorId), true);
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public void addAPS(APS aps) {		
		cache.put(aps.getId().toString(), aps);
	}

	public APSCache getSubCache(MidataId group) {
		if (subcache == null) {
			subcache = new HashMap<String, APSCache>();
		}
		APSCache result = subcache.get(group.toString());
		if (result == null) {
			result = new APSCache(group, group);
			subcache.put(group.toString(), result);
		}
		return result;
	}
	
	public boolean hasSubCache(MidataId group) {
		if (subcache == null) return false;			
		return  subcache.get(group.toString()) != null;		
	}
	
	public Consent getConsent(MidataId consentId) throws InternalServerException {
		if (consentId == null || consentId.equals(accountOwner)) return null;
		
		Consent result = consentCache.get(consentId);
		if (result != null) return result;
		
		result = Consent.getByIdAndAuthorized(consentId, accessorId, Consent.ALL);
		consentCache.put(consentId, result);
		
		return result;
	}
	
	public Consent cache(Consent consent) throws InternalServerException {
		if (consent != null) {
			consentCache.put(consent._id, consent);
		}
		
		return consent;
	}
	
	public boolean cache(Collection<? extends Consent> consents) throws InternalServerException {		
		boolean hasnew = false;
		for (Consent consent : consents) hasnew = consentCache.put(consent._id, consent) == null || hasnew;
		return hasnew;
	}
	
	public void prefetch(Collection<? extends Consent> consents, Map<MidataId, byte[]> keys) throws AppException {	
		if (consents.size() > 1) {
			if (cache.size() > CACHE_LIMIT) {
			   AccessLog.log("APS Cache clear (cache size)");
			   cache.clear();
			}
			
			int end = 0;
			Map<MidataId, Consent> ids = new HashMap<MidataId, Consent>(consents.size());
			for (Consent consent : consents) {			  			
			  if (!cache.containsKey(consent._id.toString())) {
				  ids.put(consent._id, consent);
			  }	else {
				  end++;
				  if (end > 3) return; 
			  }
			}
   		  if (ids.isEmpty()) return;
		  Map<String, Set<MidataId>> properties = Collections.singletonMap("_id", ids.keySet());
		  Set<AccessPermissionSet> rsets = AccessPermissionSet.getAll(properties, AccessPermissionSet.ALL_FIELDS);
		  for (AccessPermissionSet set : rsets) {
			  Consent r = ids.get(set._id);				
			  getAPS(r._id, keys == null ? null : keys.get(r._id), r.owner, set, false);
		  }	
			
		}
	}
	
	public void prefetch(Collection<? extends DBRecord> streams) throws AppException {						
			int end = 0;
			Map<MidataId, DBRecord> ids = new HashMap<MidataId, DBRecord>(streams.size());
			for (DBRecord rec : streams) {	
			  if (rec.isStream == APSSecurityLevel.HIGH) {
				  if (!cache.containsKey(rec._id.toString())) {
					  ids.put(rec._id, rec);
				  }	else {
					  end++;
					  if (end > 3) return; 
				  }
			  }
			}
   		    if (ids.isEmpty()) return;
   		    if (ids.size()>1000) throw new RequestTooLargeException("error.toolarge", "Too large");
		    Map<String, Set<MidataId>> properties = Collections.singletonMap("_id", ids.keySet());
		    Set<AccessPermissionSet> rsets = AccessPermissionSet.getAll(properties, AccessPermissionSet.ALL_FIELDS);
		    for (AccessPermissionSet set : rsets) {
		    	DBRecord r = ids.get(set._id);				
			    getAPS(r._id, r.key, r.owner, set, true);
		    }  						
	}
	
	public Set<UserGroupMember> getAllActiveByMember(Permission permission) throws InternalServerException {
		if (userGroupMember == null) userGroupMember = new HashMap<Permission, Set<UserGroupMember>>(); 		
		if (userGroupMember.containsKey(permission)) return userGroupMember.get(permission);		
		Set<UserGroupMember> ugms = getAllActiveByMember(new HashSet<MidataId>(), Collections.singleton(getAccountOwner()), permission);						
		userGroupMember.put(permission, ugms);
		return ugms;
	}
	
	private Set<UserGroupMember> getAllActiveByMember(Set<MidataId> alreadyFound, Set<MidataId> members, Permission permission) throws InternalServerException {
		Set<UserGroupMember> results = UserGroupMember.getAllActiveByMember(members);
		Set<MidataId> recursion = new HashSet<MidataId>();
		for (UserGroupMember ugm : results) {
			if (!ugm.role.may(permission)) continue;
			if (!alreadyFound.contains(ugm.userGroup)) {
				recursion.add(ugm.userGroup);
				alreadyFound.add(ugm.userGroup);
			}
		}
		if (!recursion.isEmpty()) {
			Set<UserGroupMember> inner = getAllActiveByMember(alreadyFound, recursion, permission);
			results.addAll(inner);
		}
		return results;
	}
	
	public List<UserGroupMember> getByGroupAndActiveMember(UserGroupMember ugm, MidataId member, Permission permission) throws InternalServerException {
		if (ugm.member.equals(member) && ugm.getConfirmedRole().may(permission)) return Collections.singletonList(ugm);
		return getByGroupAndActiveMember(ugm.userGroup, member, permission);
	}
	
	public List<UserGroupMember> getByGroupAndActiveMember(MidataId userGroup, MidataId member, Permission permission) throws InternalServerException {
		if (userGroupMember == null && member.equals(getAccountOwner())) {
			UserGroupMember isMemberOfGroup = UserGroupMember.getByGroupAndActiveMember(userGroup, member);
			if (isMemberOfGroup != null && isMemberOfGroup.getConfirmedRole().may(permission)) return Collections.singletonList(isMemberOfGroup);
		}
		if (!member.equals(getAccountOwner())) {
			UserGroupMember isMemberOfGroup = UserGroupMember.getByGroupAndActiveMember(userGroup, member);
			if (isMemberOfGroup != null && isMemberOfGroup.getConfirmedRole().may(permission)) return Collections.singletonList(isMemberOfGroup);
		}
		
		List<UserGroupMember> result = new ArrayList<UserGroupMember>();
		Set<MidataId> tested = new HashSet<MidataId>();
		if (getByGroupAndActiveMember(tested, result, userGroup, member, permission)) {
			AccessLog.log("getByGroupAndActiveMember grp=",userGroup.toString()," permission=",permission.toString()," tested=",tested.toString()," r=true");
			return result;
		} else {
			AccessLog.log("getByGroupAndActiveMember grp=",userGroup.toString()," permission=",permission.toString()," tested=",tested.toString()," r=false");
			return null;
		}

	}
	
	private boolean getByGroupAndActiveMember(Set<MidataId> tested, List<UserGroupMember> result, MidataId userGroup, MidataId member, Permission permission) throws InternalServerException  {		
	    Set<UserGroupMember> all = getAllActiveByMember(permission);
	    for (UserGroupMember ugm : all) {
	    	if (tested.contains(ugm._id)) continue;	    	
	    	
	    	if (ugm.userGroup.equals(userGroup)) {
	    		tested.add(ugm._id);
	    		
	    		if (ugm.member.equals(member) && ugm.getConfirmedRole().may(permission)) {
	    			result.add(ugm);
	    			return true;
	    		} else if (ugm.entityType == EntityType.USERGROUP || ugm.entityType == EntityType.ORGANIZATION || ugm.entityType == EntityType.PROJECT) {
		    	   if (ugm.getConfirmedRole().may(permission) && getByGroupAndActiveMember(tested, result, ugm.member, member, permission)) {
		    		   result.add(ugm);		    		   
		    		   return true;
		    	   }
		    	}
	    	} 
	    		    	
	    }	
	    
	    return false;
	}
	
	
	
	public Collection<Consent> getAllActiveConsentsByAuthorized(long limit) throws InternalServerException {
		if (consentLimit != -1 && limit >= consentLimit) {
			return consentCache.values();
		}
		
		Set<Consent> consents;
		if (limit == 0) {
		  consents = Consent.getAllActiveByAuthorized(getAccountOwner());
		} else {
		  consents = Consent.getAllActiveByAuthorized(getAccountOwner(), limit);
		}
		cache(consents);
		consentLimit = limit;
		return consents;
	}
	
	public List<Consent> getAllActiveByAuthorizedAndOwners(Set<MidataId> owners, long limit) throws InternalServerException {
		if (owners.size() == 1) {
			if (ownerToConsent == null) ownerToConsent = new HashMap<MidataId,MidataId[]>();
			MidataId owner = owners.iterator().next();
			MidataId[] consents = ownerToConsent.get(owner);
			if (consents != null) {
			  List<Consent> result = new ArrayList<Consent>(consents.length);
			  for (MidataId id : consents) result.add(getConsent(id));
			  return result;
			} else {
			  List<Consent> result = new ArrayList<Consent>(Consent.getAllActiveByAuthorizedAndOwners(getAccountOwner(), owners));
			  if (result.isEmpty()) result.addAll(StudyRelated.getActiveByAuthorizedAndIds(getAccountOwner(), owners));
			  cache(result);
			  MidataId[] ids = new MidataId[result.size()];
			  int idx = 0;
			  for (Consent c : result) { ids[idx] = c._id;idx++; }
			  ownerToConsent.put(owner, ids);
			  return result;
			}
		}
				
		List<Consent> result = new ArrayList<Consent>(Consent.getAllActiveByAuthorizedAndOwners(getAccountOwner(), owners));
		result.addAll(StudyRelated.getActiveByAuthorizedAndIds(getAccountOwner(), owners));
		cache(result);
		return result;
		
	}
	
	public void resetConsentCache() {
		AccessLog.log("resetConsentCache");
		consentLimit = -1;
		ownerToConsent = null;
		consentCache.clear();
	}
	
	/**
	 * touch a consent (update last data update time)
	 * @param consentId
	 */
	public void touchConsent(MidataId consentId) {
		if (touchedConsents == null) touchedConsents = new HashSet<MidataId>();
		touchedConsents.add(consentId);
	}
	
	/**
	 * touch an APS (update last update time)
	 * @param apsId
	 */
	public void touchAPS(MidataId apsId) {
		if (touchedAPS == null) touchedAPS = new HashSet<MidataId>();
		touchedAPS.add(apsId);
	}
	
	/**
	 * create or retrieve cache for watching APS changes
	 * @return
	 */
	public WatchesChangeBuffer changeWatches() {
		if (changedPermissions == null) changedPermissions = new WatchesChangeBuffer();
		return changedPermissions;
	}
	
	/**
	 * flush buffered touches and changes to DB
	 * @throws AppException
	 */
	public void finishTouch() throws AppException {
		if (changedPermissions != null) {
			changedPermissions.save();
			changedPermissions = null;
		}
		
		if (touchedAPS != null) {
			AccessLog.log("touching aps #aps=", Integer.toString(touchedAPS.size()));
			for (MidataId id : touchedAPS) {
			   getAPS(id).touch();
			}
		}
		
		if (touchedConsents != null) {
			AccessLog.log("touching consents #consents=", Integer.toString(touchedConsents.size()));
			Consent.updateTimestamp(touchedConsents, System.currentTimeMillis(), System.currentTimeMillis() + 1000l * 60l);
		}
		if (subcache != null) {
			AccessLog.log("flushing subcaches #caches=", Integer.toString(subcache.size()));
			for (APSCache sub : subcache.values()) {
				sub.finishTouch();
			}
		}
		touchedConsents = null;
		touchedAPS = null;
	}
	
	/**
	 * buffer new record in new record cache
	 * @param record
	 */
	public void addNewRecord(DBRecord record) {
		if (newRecordCache == null) newRecordCache = new HashMap<MidataId, DBRecord>();
		newRecordCache.put(record._id, record);
	}
	
	/**
	 * lookup a new record from new record cache
	 * @param id
	 * @return
	 */
	public DBRecord lookupRecordInCache(MidataId id) {
		if (newRecordCache == null) return null;
		return newRecordCache.get(id);
	}
	
	/*public StreamIndexRoot getStreamIndexRoot() throws AppException {
		if (streamIndexRoot != null) return streamIndexRoot;
		streamIndexRoot = IndexManager.instance.getStreamIndex(this, getAccountOwner());
		return streamIndexRoot;
	}*/
	
	public StatsIndexRoot getStatsIndexRoot(boolean pseudonymized) throws AppException {
		if (pseudonymized) {
			if (statsIndexRootPseudo != null) return statsIndexRootPseudo;
			statsIndexRootPseudo = IndexManager.instance.getStatsIndex(this, getAccountOwner(), true, false);
			return statsIndexRootPseudo;
		} else {
			if (statsIndexRoot != null) return statsIndexRoot;
			statsIndexRoot = IndexManager.instance.getStatsIndex(this, getAccountOwner(), false, false);
			return statsIndexRoot;
		}
	}
	
	public ConsentToKeyIndexRoot getConsentKeyIndexRoot(MidataId id) throws AppException {
		if (consentKeysRoot != null) return consentKeysRoot;
		consentKeysRoot = IndexManager.instance.getConsentToKey(this, getAccountOwner(), id);
		return consentKeysRoot;
	}
}
