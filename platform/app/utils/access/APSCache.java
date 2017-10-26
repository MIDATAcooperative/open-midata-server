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
import models.UserGroupMember;
import utils.AccessLog;
import utils.auth.EncryptionNotSupportedException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * caches access permission sets during a request. No inter-request caching. 
 *
 */
public class APSCache {

	private Map<String, APS> cache;
	private Map<String, APSCache> subcache;
	private MidataId executorId;
	private MidataId accountOwner;
	
	private Map<MidataId, Consent> consentCache;
	private Map<MidataId, MidataId[]> ownerToConsent;
	
	private Set<MidataId> touchedConsents = null;
	private Set<MidataId> touchedAPS = null;
	
	private long consentLimit;
	private Set<UserGroupMember> userGroupMember;
	
	public APSCache(MidataId executorId, MidataId accountApsId) {
		this.executorId = executorId;
		this.accountOwner = accountApsId;
		this.cache = new HashMap<String, APS>();
		this.consentCache = new HashMap<MidataId, Consent>();
		this.consentLimit = -1;
	}
	
	public MidataId getExecutor() {
		return executorId;
	}
	
	public MidataId getAccountOwner() {
		return accountOwner;
	}
	
	protected void setAccountOwner(MidataId accountOwner) {
		this.accountOwner = accountOwner;
	}
	
	public boolean hasAPS(MidataId apsId) throws AppException {
		APS result = cache.get(apsId.toString());
		if (result == null) return false;
		return result.isReady();
	}
	
	public APS getAPS(MidataId apsId) throws InternalServerException {
		APS result = cache.get(apsId.toString());
		if (result == null) {
			result = new APSImplementation(new EncryptedAPS(apsId, executorId));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public APS getAPS(MidataId apsId, MidataId owner) throws InternalServerException {
		APS result = cache.get(apsId.toString());
		if (result == null) {
			result = new APSImplementation(new EncryptedAPS(apsId, executorId, owner));
			cache.put(apsId.toString(), result);
		}	
		return result;
	}
	
	public APS getAPS(MidataId apsId, byte[] unlockKey, MidataId owner) throws AppException, EncryptionNotSupportedException {
		APS result = cache.get(apsId.toString());
		if (result == null) { 
			result = new APSImplementation(new EncryptedAPS(apsId, executorId, unlockKey, owner));
			if (!result.isAccessible()) {
				AccessLog.log("Adding missing access for "+executorId.toString()+" APS:"+apsId.toString());
				result.addAccess(Collections.<MidataId>singleton(executorId));
			}
			cache.put(apsId.toString(), result);
		} else if (unlockKey != null) {
			result.provideAPSKeyAndOwner(unlockKey, owner);
		}
		return result;
	}
	
	public APS getAPS(MidataId apsId, byte[] unlockKey, MidataId owner, AccessPermissionSet set) throws AppException, EncryptionNotSupportedException {
		APS result = cache.get(apsId.toString());
		if (result == null) { 
			result = new APSImplementation(new EncryptedAPS(apsId, executorId, unlockKey, owner, set));
			if (!result.isAccessible()) result.addAccess(Collections.<MidataId>singleton(executorId));
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
	
	public Consent getConsent(MidataId consentId) throws InternalServerException {
		if (consentId == null || consentId.equals(accountOwner)) return null;
		
		Consent result = consentCache.get(consentId);
		if (result != null) return result;
		
		result = Consent.getByIdAndAuthorized(consentId, executorId, Consent.ALL);
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
	
	public void prefetch(Collection<? extends Consent> consents) throws AppException {	
		if (consents.size() > 1) {			
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
			  getAPS(r._id, null, r.owner, set);
		  }	
			
		}
	}
	
	public Set<UserGroupMember> getAllActiveByMember() throws InternalServerException {
		if (userGroupMember != null) return userGroupMember;
		
		userGroupMember = UserGroupMember.getAllActiveByMember(getAccountOwner());
		return userGroupMember;
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
	
	public Set<Consent> getAllActiveByAuthorizedAndOwners(Set<MidataId> owners, long limit) throws InternalServerException {
		if (owners.size() == 1) {
			if (ownerToConsent == null) ownerToConsent = new HashMap<MidataId,MidataId[]>();
			MidataId owner = owners.iterator().next();
			MidataId[] consents = ownerToConsent.get(owner);
			if (consents != null) {
			  Set<Consent> result = new HashSet<Consent>(consents.length);
			  for (MidataId id : consents) result.add(getConsent(id));
			  return result;
			} else {
			  Set<Consent> result = Consent.getAllActiveByAuthorizedAndOwners(getAccountOwner(), owners);
			  cache(result);
			  MidataId[] ids = new MidataId[result.size()];
			  int idx = 0;
			  for (Consent c : result) { ids[idx] = c._id;idx++; }
			  ownerToConsent.put(owner, ids);
			  return result;
			}
		}
				
		Set<Consent> result = Consent.getAllActiveByAuthorizedAndOwners(getAccountOwner(), owners);
		cache(result);
		return result;
		
	}
	
	public void resetConsentCache() {
		consentLimit = -1;
		ownerToConsent = null;
		consentCache.clear();
	}
	
	public void touchConsent(MidataId consentId) {
		if (touchedConsents == null) touchedConsents = new HashSet<MidataId>();
		touchedConsents.add(consentId);
	}
	
	public void touchAPS(MidataId apsId) {
		if (touchedAPS == null) touchedAPS = new HashSet<MidataId>();
		touchedAPS.add(apsId);
	}
	
	public void finishTouch() throws AppException {
		if (touchedAPS != null) {
			for (MidataId id : touchedAPS) {
			   getAPS(id).touch();
			}
		}
		
		if (touchedConsents != null) {
			Consent.updateTimestamp(touchedConsents, System.currentTimeMillis(), System.currentTimeMillis() + 1000l * 60l * 60l);
		}
		if (subcache != null) {
			for (APSCache sub : subcache.values()) {
				sub.finishTouch();
			}
		}
		touchedConsents = null;
		touchedAPS = null;
	}
}
