package utils.access;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import controllers.Circles;
import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.AccountStats;
import models.Consent;
import models.ContentCode;
import models.ContentInfo;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.UserGroupMember;
import models.enums.APSSecurityLevel;
import models.enums.AggregationType;
import models.enums.ConsentStatus;
import models.enums.UserRole;
import play.mvc.Http;
import utils.AccessLog;
import utils.QueryTagTools;
import utils.RuntimeConstants;
import utils.auth.KeyManager;
import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.FileStorage;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.viruscheck.FileTypeScanner;
import utils.viruscheck.VirusScanner;

/**
 * Access to records. Manages authorizations using access permission sets.
 *
 */
public class RecordManager {

	public static RecordManager instance = new RecordManager();

	public final static Map<String, Object> FULLAPS_LIMITED_SIZE = Collections.unmodifiableMap(CMaps.map("limit-consents", 1000).map("limit", 10000));
	public final static Map<String, Object> FULLAPS_WITHSTREAMS = Collections.unmodifiableMap(CMaps.map("streams", "true"));
	public final static Map<String, Object> FULLAPS_FLAT = Collections.unmodifiableMap(CMaps.map("streams", "true").map("flat", "true"));
	public final static Map<String, Object> FULLAPS_FLAT_OWNER = Collections.unmodifiableMap(CMaps.map("streams", "true").map("flat", "true").map("owner", "self"));
	
	public final static Set<String> INTERNALIDONLY = Collections.unmodifiableSet(Sets.create("_id"));
	public final static Set<String> INTERNALID_AND_WACTHES = Collections.unmodifiableSet(Sets.create("_id","watches"));
	public final static Set<String> COMPLETE_META = Collections.unmodifiableSet(Sets.create("id", "owner",
			"app", "creator", "created", "name", "format",  "content", "code", "description", "isStream", "lastUpdated", "consentAps"));
	public final static Set<String> COMPLETE_DATA = Collections.unmodifiableSet(Sets.create("id", "owner", "ownerName",
			"app", "creator", "created", "name", "format", "content", "code", "description", "isStream", "lastUpdated",
			"data", "group"));
	public final static Set<String> COMPLETE_DATA_WITH_WATCHES = Collections.unmodifiableSet(Sets.create("id", "owner",
			"app", "creator", "created", "name", "format",  "content", "code", "description", "isStream", "lastUpdated",
			"data", "group", "watches", "stream"));
	public final static Set<String> SHARING_FIELDS = Collections.unmodifiableSet(Sets.create("_id", "key", "owner", "format", "content", "created", "name", "isStream", "stream", "app"));
	
	//public final static String STREAM_TYPE = "Stream";
	public final static Map<String, Object> STREAMS_ONLY = Collections.unmodifiableMap(CMaps.map("streams", "only").map("flat", "true"));
	public final static Map<String, Object> STREAMS_ONLY_OWNER = Collections.unmodifiableMap(CMaps.map("streams", "only").map("flat", "true").map("owner", "self"));	

	private static ThreadLocal<APSCache> apsCache = new ThreadLocal<APSCache>();

	/**
	 * returns intra-request cache
	 * @param who person who does the current request
	 * @return APSCache
	 * @throws InternalServerException
	 */
	protected APSCache getCache(MidataId who) throws InternalServerException {
		if (apsCache.get() == null)
			apsCache.set(new APSCache(who, who));
		APSCache result = apsCache.get();
		if (!result.getExecutor().equals(who)) throw new InternalServerException("error.internal", "Owner Change!");
		return result;
	}	
	
	public void setAccountOwner(MidataId executor, MidataId accountOwner) throws InternalServerException {
		getCache(executor).setAccountOwner(accountOwner);
	}
	
	/**
	 * clears APS cache. Is automatically called after each request.
	 */
	public void clear() {
		clearCache();
		KeyManager.instance.clear();
	}
	
	public void clearCache() {
		APSCache old = apsCache.get();
		if (old != null) {
			try {
			  old.finishTouch();
			} catch (AppException e) {
				AccessLog.logException("clearCache", e);
			}
			apsCache.set(null);
		}
	}

	/**
	 * create a new access permission where only the owner has access
	 * @param who ID of APS owner
	 * @param proposedId ID of APS to be created
	 * @return ID of APS
	 * @throws AppException
	 */
	public MidataId createPrivateAPS(MidataId who, MidataId proposedId)
			throws AppException {
		AccessLog.logBegin("begin createPrivateAPS who="+who.toString()+" id="+proposedId.toString());
		EncryptedAPS eaps = new EncryptedAPS(proposedId, who, who, APSSecurityLevel.HIGH, false);
		EncryptionUtils.addKey(who, eaps);		
		eaps.create();
		APSCache current = apsCache.get();
		if (current != null) current.addAPS(new APSImplementation(new EncryptedAPS(eaps.getId(), current.getExecutor(), eaps.getAPSKey(), eaps.getOwner())));
        AccessLog.logEnd("end createPrivateAPS");
		return eaps.getId();
	}

	/**
	 * creates an access permission set where the owner and one other person have access
	 * @param owner ID of APS owner
	 * @param other ID of other person
	 * @param proposedId ID of APS to be created
	 * @param consent APS is part of consent
	 * @return ID of APS
	 * @throws AppException
	 */
	public MidataId createAnonymizedAPS(MidataId owner, MidataId other,
			MidataId proposedId, boolean consent) throws AppException {
				return createAnonymizedAPS(owner, other, proposedId, consent, true, false);
	}

	public MidataId createAnonymizedAPS(MidataId owner, MidataId other,
			MidataId proposedId, boolean consent, boolean history, boolean otherNotOwner) throws AppException {
        AccessLog.logBegin("begin createAnonymizedAPS owner="+owner.toString()+" other="+other.toString()+" id="+proposedId.toString());
		EncryptedAPS eaps = new EncryptedAPS(proposedId, owner, owner, APSSecurityLevel.HIGH, consent);
		EncryptionUtils.addKey(owner, eaps);
		EncryptionUtils.addKey(other, eaps, otherNotOwner);	
		if (history) eaps.getPermissions().put("_history", new BasicBSONList()); // Init with history
		eaps.create();
		APSCache current = apsCache.get();
		if (current != null) current.addAPS(new APSImplementation(new EncryptedAPS(eaps.getId(), current.getExecutor(), eaps.getAPSKey(), eaps.getOwner())));
       
		AccessLog.logEnd("end createAnonymizedAPS");
		return eaps.getId();

	}

	

	/**
	 * create an APS for a record.
	 * @param executingPerson ID of person who does this request
	 * @param owner ID of owner of future APS
	 * @param recordId ID of record
	 * @param key key this record is encrypted with
	 * @param direct true for an access permission set which encrypts all records with the APS key
	 * @return ID of APS
	 * @throws InternalServerException
	 */
	public MidataId createAPSForRecord(MidataId executingPerson, MidataId owner, MidataId recordId,
			byte[] key, boolean direct) throws AppException {
		AccessLog.logBegin("begin createAPSForRecord exec="+executingPerson.toString()+" owner="+owner.toString()+" record="+recordId.toString());
		
		EncryptedAPS eaps = new EncryptedAPS(recordId, executingPerson, owner,
				direct ? APSSecurityLevel.MEDIUM : APSSecurityLevel.HIGH, key, false);
        EncryptionUtils.addKey(owner, eaps);
        if (!executingPerson.equals(owner)) EncryptionUtils.addKey(executingPerson, eaps);
		eaps.create();

		AccessLog.logEnd("end createAPSForRecord");
		return eaps.getId();
	}

	/**
	 * share access permission set content with other users
	 * @param apsId ID of APS
	 * @param executorId ID of executor having permission of APS
	 * @param targetUsers IDs of user to share APS with
	 * @throws AppException
	 */
	public void shareAPS(MidataId apsId, AccessContext context, MidataId executorId,
			Set<MidataId> targetUsers) throws AppException {		
		if (targetUsers==null || targetUsers.isEmpty()) return;
		
		AccessLog.logBegin("begin shareAPS aps="+apsId.toString()+" executor="+executorId.toString()+" #targetUsers="+targetUsers.size());
		APSCache cache = getCache(executorId);
		if (context != null) {
		  DBIterator<DBRecord> it = QueryEngine.listInternalIterator(cache, apsId, context, CMaps.map("flat","true").map("streams","only").map("owner",cache.getAccountOwner()).map("ignore-redirect","true"),SHARING_FIELDS);
		  shareRecursive(cache, it, targetUsers);
		}
		getCache(executorId).getAPS(apsId).addAccess(targetUsers);
		AccessLog.logEnd("end shareAPS");
	}
	
	public void reshareAPS(MidataId apsId, AccessContext context, MidataId executorId, MidataId groupWithAccessId,
			Set<MidataId> targetUsers) throws AppException {
		if (groupWithAccessId == null || groupWithAccessId.equals(executorId)) {
			shareAPS(apsId, context, executorId, targetUsers);
		} else {
			AccessLog.logBegin("begin reshareAPS aps="+apsId.toString()+" executor="+executorId.toString()+" #targetUsers="+targetUsers.size());
			APSCache cache = getCache(executorId);
			DBIterator<DBRecord> it = QueryEngine.listInternalIterator(cache, apsId, context, CMaps.map("flat","true").map("streams","only").map("owner",cache.getAccountOwner()), SHARING_FIELDS);
			shareRecursive(cache, it, targetUsers);
			Feature_UserGroups.findApsCacheToUse(cache, apsId).getAPS(apsId).addAccess(targetUsers);
			AccessLog.logEnd("end shareAPS");
		}
	}
	
	/**
	 * share access permission set content with another entity that has a public key
	 * @param apsId ID of APS
	 * @param executorId ID of executor having permission of APS
	 * @param targetId=apsID
	 * @param publickey public key of target entity
	 * @throws AppException
	 */
	public void shareAPS(MidataId apsId, MidataId executorId,
			byte[] publickey) throws AppException {
		AccessLog.logBegin("begin shareAPS aps="+apsId.toString()+" executor="+executorId);
		getCache(executorId).getAPS(apsId).addAccess(apsId, publickey);
		AccessLog.logEnd("end shareAPS");
	}

	/**
	 * remove access permissions of given users from an APS
	 * @param apsId ID of APS
	 * @param executorId ID of executing person
	 * @param targetUsers set of IDs of target users
	 * @throws InternalServerException
	 */
	public void unshareAPS(MidataId apsId, MidataId executorId,
			Set<MidataId> targetUsers) throws InternalServerException {
		AccessLog.logBegin("begin unshareAPS aps="+apsId.toString()+" executor="+executorId.toString()+" #targets="+targetUsers.size());
		getCache(executorId).getAPS(apsId).removeAccess(targetUsers);
		AccessLog.logEnd("end unshareAPS");
	}
	
	/**
	 * remove access permissions of given users from an APS and all streams contained in it.
	 * access to streams may be regained if accessable from another APS
	 * @param apsId ID of APS
	 * @param executorId ID of executing person
	 * @param targetUsers set of IDs of target users
	 * @throws AppException
	 */
	public void unshareAPSRecursive(MidataId apsId, MidataId executorId,
			Set<MidataId> targetUsers) throws AppException {
		AccessLog.logBegin("begin unshareAPSRecursive aps="+apsId.toString()+" executor="+executorId.toString()+" #targets="+targetUsers.size());
		if (getCache(executorId).getAPS(apsId).isAccessible()) {
			List<DBRecord> to_unshare = QueryEngine.listInternal(getCache(executorId), apsId, null, CMaps.map("streams", "only").map("ignore-redirect", true), Sets.create("_id"));
			for (DBRecord rec : to_unshare) unshareAPS(rec._id, executorId, targetUsers);
			getCache(executorId).getAPS(apsId).removeAccess(targetUsers);
		}
		AccessLog.logEnd("end unshareAPSRecursive");
	}
	
	
	private void shareRecursive(APSCache cache, DBIterator<DBRecord> recs, Set<MidataId> targetUsers) throws AppException {
		if (targetUsers.isEmpty() || !recs.hasNext()) return;		
		while (recs.hasNext()) {
			DBRecord rec = recs.next();
			if (rec.isStream != null) {
				APS stream = cache.getAPS(rec._id, rec.key, rec.owner);
				stream.addAccess(targetUsers);
			}
		}		
	}		
	
	public void share(MidataId who, MidataId fromAPS, MidataId toAPS, 
			Set<MidataId> records, boolean withOwnerInformation) throws AppException {
		share(who, fromAPS, toAPS, null, records, withOwnerInformation);
	}
	
	/**
	 * share records contained in an APS to another APS
	 * @param who ID of executing person
	 * @param fromAPS ID of source APS
	 * @param toAPS ID of target APS
	 * @param records set of record IDs or null for complete APS
	 * @param withOwnerInformation 
	 * @throws AppException
	 */
	public void share(MidataId who, MidataId fromAPS, MidataId toAPS, MidataId toAPSOwner,
			Set<MidataId> records, boolean withOwnerInformation)
			throws AppException {
		share(who, getCache(who), fromAPS, toAPS, toAPSOwner, records, withOwnerInformation);
	}
	
	protected void share(MidataId who, APSCache cache, MidataId fromAPS, MidataId toAPS, MidataId toAPSOwner,
			Set<MidataId> records, boolean withOwnerInformation)
			throws AppException {
		if (fromAPS.equals(toAPS)) return;
        AccessLog.logBegin("begin share: who="+who.toString()+" from="+fromAPS.toString()+" to="+toAPS.toString()+" count="+(records!=null ? records.size() : "?"));
		APS apswrapper = cache.getAPS(toAPS, toAPSOwner);
		
		List<DBRecord> recordEntries = null;
		
		if (records != null) {
			boolean failed = false;
			for (MidataId id : records) {
				DBRecord target = cache.lookupRecordInCache(id);
				if (target != null) {
					if (recordEntries == null) recordEntries = new ArrayList<DBRecord>();
					recordEntries.add(target);
				} else failed = true;
			}
			if (failed) recordEntries = null;
			
		}
		
		if (recordEntries == null) {
			recordEntries = QueryEngine.listInternal(cache, fromAPS, null,
					records != null ? CMaps.map("_id", records) : RecordManager.FULLAPS_FLAT,
					RecordManager.SHARING_FIELDS);
		}
		
		List<DBRecord> alreadyContained = QueryEngine.isContainedInAps(cache, toAPS, recordEntries);
		AccessLog.log("to-share: "+recordEntries.size()+" already="+alreadyContained.size());
		
		shareUnchecked(cache, recordEntries, alreadyContained, apswrapper, withOwnerInformation);
        
        AccessLog.logEnd("end share");
	}
	
	public int share(MidataId who, MidataId fromAPS, MidataId toAPS, MidataId toAPSOwner,
			Map<String, Object> query, boolean withOwnerInformation) throws AppException {
		
		APSCache cache = getCache(who);
		APS apswrapper = cache.getAPS(toAPS, toAPSOwner);
		List<DBRecord> recordEntries = QueryEngine.listInternal(cache, fromAPS, null,
				CMaps.map(query).map("owner", fromAPS),	RecordManager.SHARING_FIELDS);
		
		share(cache, toAPS, toAPSOwner, recordEntries, withOwnerInformation);
		
		return recordEntries.size();
	}
	
	public int copyAPS(MidataId who, MidataId fromAPS, MidataId toAPS, MidataId toAPSOwner) throws AppException {
		
		APSCache tocache = Feature_UserGroups.findApsCacheToUse(getCache(who), toAPS);
		APSCache fromcache = Feature_UserGroups.findApsCacheToUse(getCache(who), fromAPS);
		APS apswrapper = tocache.getAPS(toAPS, toAPSOwner);
		List<DBRecord> recordEntries = QueryEngine.listInternal(fromcache, fromAPS, null,
				CMaps.map("streams", "true").map("flat", "true"), RecordManager.SHARING_FIELDS);
		
		share(tocache, toAPS, toAPSOwner, recordEntries, false);
		
		return recordEntries.size();
	}
	
	
	protected void share(APSCache cache, MidataId toAPS, MidataId toAPSOwner,
			List<DBRecord> recordEntries, boolean withOwnerInformation)
			throws AppException {		
        
		APS apswrapper = cache.getAPS(toAPS, toAPSOwner);
		
		AccessLog.log("check if contained in target aps");
		List<DBRecord> alreadyContained = QueryEngine.isContainedInAps(cache, toAPS, recordEntries);		
		
		shareUnchecked(cache, recordEntries, alreadyContained, apswrapper, withOwnerInformation);
        
	}
	
	protected void shareUnchecked(APSCache cache, List<DBRecord> recordEntries, List<DBRecord> alreadyContained, APS apswrapper, boolean withOwnerInformation) throws AppException {
		
		// withOwnerInformation = false; // Preparing to remove this feature completely
		
		if (alreadyContained.size() == recordEntries.size()) {
        	
        	return;
        }
		AccessLog.log("to-share: count#="+recordEntries.size()+" already="+alreadyContained.size());
        if (alreadyContained.size() == 0) {	
        	shareRecursive(cache, ProcessingTools.dbiterator("", recordEntries.iterator()), apswrapper.getAccess());
		    apswrapper.addPermission(recordEntries, withOwnerInformation);
		    for (DBRecord rec : recordEntries) {		    	
		    	cache.changeWatches().addWatchingAps(rec, apswrapper.getId());
		    }
        } else {
        	Set<MidataId> contained = new HashSet<MidataId>();
        	for (DBRecord rec : alreadyContained) contained.add(rec._id);
        	List<DBRecord> filtered = new ArrayList<DBRecord>(recordEntries.size());
        	for (DBRecord rec : recordEntries) {
        		if (!contained.contains(rec._id)) filtered.add(rec);
        	}
        	shareRecursive(cache, ProcessingTools.dbiterator("", filtered.iterator()), apswrapper.getAccess());
        	apswrapper.addPermission(filtered, withOwnerInformation);
        	for (DBRecord rec : filtered) cache.changeWatches().addWatchingAps(rec, apswrapper.getId());
        }
	}

	/**
	 * set a query that dynamically shares records from one APS to another APS.
	 *  
	 * this operation will only work if all accessors of the target APS also have access to the source APS
	 * so this is only useful for APS of spaces
	 *  
	 * @param who id of user who must be owner and executing person 
	 * @param fromAPS id of APS to share records from
	 * @param toAPS id of APS to share records into
	 * @param query the query to be applied to the records
	 * @throws AppException
	 */
	public void shareByQuery(MidataId who, MidataId fromAPS, MidataId toAPS,
			Map<String, Object> query) throws AppException {
        AccessLog.log("shareByQuery who="+who.toString()+" from="+fromAPS.toString()+" to="+toAPS.toString()+ "query="+query.toString());
		//if (toAPS.equals(who)) throw new BadRequestException("error.internal", "Bad call to shareByQuery. target APS may not be user APS!");
        APS apswrapper = getCache(who).getAPS(toAPS);
        
        // Resolve "app" into IDs
        Query q = new Query("share-by-query",query, Sets.create("_id"), getCache(who), toAPS, new DummyAccessContext(getCache(who), fromAPS), null);
        query = q.getProperties();
        
        query.remove("aps");
        if (query.isEmpty()) {
           apswrapper.removeMeta("_query");
        } else {
		   query.put("aps", fromAPS.toString());
		   apswrapper.setMeta("_query", query);
        }
        
        if (query.containsKey("exclude-ids")) {
			Map<String, Object> ids = new HashMap<String,Object>();
			ids.put("ids", query.get("exclude-ids"));
			apswrapper.setMeta("_exclude", ids);
		} else {
			apswrapper.removeMeta("_exclude");
		}
        
        List<DBRecord> doubles = QueryEngine.listInternal(getCache(who), toAPS, null, CMaps.map(query).map("ignore-redirect", "true").map("flat", "true").map("streams", "true"), APSEntry.groupingFields);
        apswrapper.removePermission(doubles);        
	}
	
	/**
	 * Materialize the query results of an APS into the APS and remove the query
	 * @param who executing person
	 * @param targetAPS the APS with a query redirect
	 * @throws AppException
	 */
	public void materialize(MidataId executorAndOwnerId, MidataId targetAPS) throws AppException {
		APS apswrapper = getCache(executorAndOwnerId).getAPS(targetAPS);
		if (apswrapper.getMeta("_query") != null) {

			AccessLog.logBegin("start materialize query APS="+targetAPS.toString());
			Set<String> fields = Sets.create("owner");
			fields.addAll(APSEntry.groupingFields);
			List<DBRecord> content = QueryEngine.listInternal(getCache(executorAndOwnerId), targetAPS, null, CMaps.map("redirect-only", "true"), fields);
			Set<MidataId> ids = new HashSet<MidataId>();
			for (DBRecord rec : content) ids.add(rec._id);
			
			BasicBSONObject query = apswrapper.getMeta("_query");
			Circles.setQuery(executorAndOwnerId, executorAndOwnerId, targetAPS, query);
			apswrapper.removeMeta("_query");
       	    RecordManager.instance.applyQuery(new DummyAccessContext(getCache(executorAndOwnerId)), query, executorAndOwnerId, targetAPS, true);
			
       	    RecordManager.instance.share(executorAndOwnerId, executorAndOwnerId, targetAPS, ids, true);
             
			
			//Feature_Expiration.setup(apswrapper);
						
			
			AccessLog.logEnd("end materialize query");

		}
		if (apswrapper.getMeta("_filter") != null) {
			AccessLog.logBegin("start materialize consent APS="+targetAPS.toString());
			Set<String> fields = Sets.create("owner");
			fields.addAll(APSEntry.groupingFields);
			List<DBRecord> content = QueryEngine.listInternal(getCache(executorAndOwnerId), targetAPS, null, CMaps.map(), fields);
			apswrapper.clearPermissions();
			apswrapper.addPermission(content, true);
			
			Member member = Member.getById(executorAndOwnerId, Sets.create("queries", "rqueries"));
			
			if (member.queries != null) {			 
			  String key = targetAPS.toString();
		      if (member.queries.containsKey(key)) {
		    	  member.queries.remove(key);
		    	  Member.set(executorAndOwnerId, "queries", member.queries);
		      }
			}
			
			if (member.rqueries != null) {			 
				  String key = targetAPS.toString();
			      if (member.rqueries.containsKey(key)) {
			    	  member.rqueries.remove(key);
			    	  Member.set(executorAndOwnerId, "rqueries", member.rqueries);
			      }
			}
			
			apswrapper.removeMeta("_filter");
			AccessLog.logEnd("end materialize query");
		}
	}

	/**
	 * remove records from an APS
	 * @param who id of executing person
	 * @param apsId id of target APS
	 * @param records set of record ids to remove from APS
	 * @throws AppException
	 */
	public void unshare(MidataId who, MidataId apsId, Set<MidataId> records)
			throws AppException {
		if (records.size() == 0) return;
		
        AccessLog.logBegin("begin unshare who="+who.toString()+" aps="+apsId.toString()+" #recs="+records.size());
        APSCache cache = getCache(who);
		APS apswrapper = cache.getAPS(apsId);
		List<DBRecord> recordEntries = QueryEngine.listInternal(getCache(who), apsId, null,
				CMaps.map("_id", records), Sets.create("_id", "format", "content", "watches"));		
		apswrapper.removePermission(recordEntries);
		for (DBRecord rec : recordEntries) cache.changeWatches().removeWatchingAps(rec, apsId);
		AccessLog.logEnd("end unshare");
	}
	
	public void unshare(MidataId who, MidataId apsId, Collection<Record> records)
			throws AppException {
		if (records.size() == 0) return;
		
		Set<MidataId> ids = new HashSet<MidataId>();
		for (Record r : records) ids.add(r._id);
		
		unshare(who, apsId, ids);
	}

	/**
	 * read a meta data object from an APS
	 * @param who id of executing person
	 * @param apsId id of APS to read from
	 * @param key unique name of meta data object
	 * @return
	 * @throws AppException
	 */
	public BSONObject getMeta(MidataId who, MidataId apsId, String key) throws AppException {
		AccessLog.logBegin("begin getMeta who="+who.toString()+" aps="+apsId.toString()+" key="+key);
		BSONObject result = Feature_UserGroups.findApsCacheToUse(getCache(who), apsId).getAPS(apsId).getMeta(key);
		AccessLog.logEnd("end getMeta");
		return result;
	}
	
	/**
	 * store a meta data object into an APS
	 * @param who id of executing person
	 * @param apsId id of APS to save into
	 * @param key unique name of meta data object
	 * @param data key,value map containing the data to be stored
	 * @throws AppException
	 */
	public void setMeta(MidataId who, MidataId apsId, String key, Map<String,Object> data) throws AppException {
		AccessLog.logBegin("begin setMeta who="+who.toString()+" aps="+apsId.toString()+" key="+key);
		Feature_UserGroups.findApsCacheToUse(getCache(who), apsId).getAPS(apsId).setMeta(key, data);
		AccessLog.logEnd("end setMeta");
	}
	
	/**
	 * remove a meta data object from an APS
	 * @param who id of executing person
	 * @param apsId id of APS to save into
	 * @param key unique name of meta data object	
	 * @throws AppException
	 */
	public void removeMeta(MidataId who, MidataId apsId, String key) throws AppException {
		AccessLog.logBegin("begin removeMeta who="+who.toString()+" aps="+apsId.toString()+" key="+key);
		getCache(who).getAPS(apsId).removeMeta(key);
		AccessLog.logEnd("end removeMeta");
	}



	/**
	 * add a new record to the database 
	 * @param executingPerson id of executing person
	 * @param record the record to be saved
	 * @throws AppException
	 */
	public void addRecord(MidataId executingPerson, Record record) throws AppException {
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
		byte[] enckey = addRecordIntern(createContextFromAccount(executingPerson), dbrecord, false, null, false);	
		//createAndShareDependend(executingPerson, dbrecord, record.dependencies, enckey);
		
	}
	
	/*
	protected void createAndShareDependend(MidataId executingPerson, DBRecord record, Set<MidataId> dependencies, byte[] enckey) throws AppException {
		if (dependencies != null && !dependencies.isEmpty()) {
			createAPSForRecord(executingPerson, record.owner, record._id, enckey, false);
			share(executingPerson, executingPerson, record._id, dependencies, false);
			
			AccessLog.logBegin("start applying queries");
			Member member = Member.getById(record.owner, Sets.create("queries"));
			if (member.queries!=null) {
				for (String key : member.queries.keySet()) {
					Map<String, Object> query = member.queries.get(key);
					if (QueryEngine.isInQuery(getCache(executingPerson), query, record)) {
						try {
						  MidataId targetAps = new MidataId(key);
						  getCache(executingPerson).getAPS(targetAps, record.owner);
						  RecordManager.instance.share(executingPerson, record.owner, targetAps, Collections.singleton(record._id), true);
						} catch (APSNotExistingException e) {
							
						}
					}
				}
			}
			AccessLog.logEnd("end applying queries");			
		}
	}*/
	
	/**
	 * Add a new record containing an attachment to the database
	 * @param executingPerson id of executing person
	 * @param record the record to be saved
	 * @param alternateAps alternative target aps
	 * @param data a file input stream containing the file to be stored as attachment
	 * @param fileName the name of the attached file
	 * @param contentType the mime type of the attached file	
	 * @throws AppException
	 */
	public void addRecord(AccessContext context, Record record, MidataId alternateAps, EncryptedFileHandle data, String fileName, String contentType) throws AppException {
		
		String virus = checkVirusFree(data);
	    if (virus != null) throw new BadRequestException("error.virus", "A virus has been detected: "+virus);
	
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
		dbrecord.meta.append("file", data.getId().toObjectId());
		dbrecord.meta.append("file-key", data.getKey());
		byte[] kdata = addRecordIntern(context, dbrecord, false, alternateAps, false);	
		/*
		try {
		FileStorage.store(EncryptionUtils.encryptStream(kdata, data), record._id, 0, fileName, contentType);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal", e);
		}*/		
	}
	
	/**
	 * Add a new record containing an attachment to the database
	 * @param executingPerson id of executing person
	 * @param record the record to be saved
	 * @param alternateAps alternative target aps
	 * @param data a file input stream containing the file to be stored as attachment
	 * @param fileName the name of the attached file
	 * @param contentType the mime type of the attached file	
	 * @throws AppException
	 */
	public void addRecord(AccessContext context, Record record, MidataId alternateAps, InputStream input, String fileName, String contentType) throws AppException {
		EncryptedFileHandle data = addFile(input, fileName, contentType);
		addRecord(context, record, alternateAps, data, fileName, contentType);	
	}
	
	public EncryptedFileHandle addFile(InputStream data, String fileName, String contentType) throws AppException {
		MidataId id = new MidataId();
		byte[] kdata = EncryptionUtils.generateKey();
		CountingInputStream countInput = new CountingInputStream(data);
		System.out.println("START UPLOAD");
		try {
		  FileStorage.store(EncryptionUtils.encryptStream(kdata, countInput), id, 0, fileName, contentType);
		} catch (Exception e) {
		  System.out.println("FAIL UPLOAD");
		  FileStorage.delete(id.toObjectId());
		  throw new InternalServerException("error.internal", e);
		}
		System.out.println("EXIT UPLOAD");
		return new EncryptedFileHandle(id, kdata, countInput.getByteCount());
		//createAndShareDependend(executingPerson, dbrecord, record.dependencies, kdata);
	}
	
	public String checkVirusFree(EncryptedFileHandle handle) throws AppException {

		FileData fileData = FileStorage.retrieve(handle.getId(), 0);
		if (fileData == null) throw new InternalServerException("error.internal", "Internal file not found");		
		
		// Do not check files larger than 100MB
		if (handle.getLength() >= 1024l * 1024l * 100l) return null;
		
		FileTypeScanner.getInstance().isValidFile(fileData.filename, fileData.contentType);
		
		InputStream inputStream = EncryptionUtils.decryptStream(handle.getKey(), fileData.inputStream);
		
		VirusScanner vscan = new VirusScanner();
		return vscan.scan(inputStream);
	}
	
	/**
	 * Add a new record to the database and give access not only to the owner APS but to a second APS
	 * 
	 * This function is useful if a record needs to be added by another person that does not have 
	 * access to the record owners APS. By adding the record to a second APS that the adding person has access
	 * to it is guaranteed that the executing person does not loose access to the new record.
	 * 
	 * @param executingPerson id of executing person
	 * @param record the record to be added
	 * @param alternateAps an APS where the executing person has access to. (a consent APS for example)
	 * @throws AppException
	 */
	public void addRecord(AccessContext context, Record record, MidataId alternateAps) throws AppException {
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
		byte[] kdata = addRecordIntern(context, dbrecord, false, alternateAps, false);	
		//createAndShareDependend(executingPerson, dbrecord, record.dependencies, kdata);
	}
	
	
	
	/**
	 * update a record in the database 
	 * @param executingPerson id of executing person
	 * @param record the record to be updated
	 * @throws AppException
	 * @return the new version string of the record
	 */
	public String updateRecord(MidataId executingPerson, AccessContext context, Record record) throws AppException {
		AccessLog.logBegin("begin updateRecord executor="+executingPerson.toString()+" aps="+context.getTargetAps().toString()+" record="+record._id.toString());
		try {
			List<DBRecord> result = QueryEngine.listInternal(getCache(executingPerson), context.getTargetAps(),context, CMaps.map("_id", record._id).map("updatable", true), RecordManager.COMPLETE_DATA_WITH_WATCHES);	
			if (result.size() != 1) {
				List<DBRecord> resultx = QueryEngine.listInternal(getCache(executingPerson), context.getTargetAps(),context, CMaps.map("_id", record._id), RecordManager.INTERNALIDONLY);
				if (resultx.isEmpty()) {
				  throw new InternalServerException("error.internal.notfound", "Unknown Record");
				} else {
				  throw new InternalServerException("error.internal", "Record may not be updated!");	
				}
			}
			if (record.data == null) throw new BadRequestException("error.internal", "Missing data");		
			
			DBRecord rec = result.get(0);
			if (!rec.context.mayUpdateRecord(rec, record)) throw new InternalServerException("error.internal", "Record may not be updated!");
			
			String storedVersion = rec.meta.getString("version");
			if (storedVersion == null) storedVersion = VersionedDBRecord.INITIAL_VERSION;
			String providedVersion = record.version != null ? record.version : VersionedDBRecord.INITIAL_VERSION; 
			if (!providedVersion.equals(storedVersion)) throw new BadRequestException("error.concurrent.update", "Concurrent update", Http.Status.CONFLICT);
			
			if (record.format != null && !rec.meta.getString("format").equals(record.format)) throw new InternalServerException("error.invalid.request", "Tried to change record format during update.");
			if (record.content != null && !rec.meta.getString("content").equals(record.content)) throw new InternalServerException("error.invalid.request", "Tried to change record content type during update.");
			if (record.owner != null && !rec.owner.equals(record.owner)) throw new InternalServerException("error.invalid.request", "Tried to change record owner during update! new="+record.owner.toString()+" old="+rec.owner.toString());
			
			VersionedDBRecord vrec = null;
			
			if (context.produceHistory()) {
			  vrec = new VersionedDBRecord(rec);		
			  RecordEncryption.encryptRecord(vrec);
			}
					
			record.lastUpdated = new Date(); 
			
		    rec.data = record.data;
		    rec.meta.put("lastUpdated", record.lastUpdated);
		    rec.time = Query.getTimeFromDate(record.lastUpdated);
		    if (vrec==null) {
		    	rec.meta.put("previousVersion", storedVersion);
		    	rec.meta.put("previousLastUpdated", rec.meta.get("lastUpdated"));
		    }
		    
		    String version = Long.toString(System.currentTimeMillis());
		    rec.meta.put("version", version);
			
		    DBRecord clone = rec.clone();
		    
			RecordEncryption.encryptRecord(rec);	
			
			if (vrec!=null) VersionedDBRecord.add(vrec);
		    DBRecord.upsert(rec); 	  	
		    
		    RecordLifecycle.notifyOfChange(clone, getCache(executingPerson));
		    return version;
		} finally {
	        AccessLog.logEnd("end updateRecord");
		}
	    
	}
	
	/**
	 * Wipe records from the system. Currently only used for debugging purposes.
	 * @param executingPerson id of executing person
	 * @param tk a token for the record to be deleted.
	 * @throws AppException
	 */	
	public void wipe(MidataId executingPerson, Map<String, Object> query) throws AppException {
		AccessLog.logBegin("begin deletingRecords executor="+executingPerson.toString());
		
		Set<String> fields = new HashSet<String>();
		fields.add("owner");
		fields.add("stream");
		fields.add("isStream");
		fields.add("consentAps");
		fields.addAll(APSEntry.groupingFields);
		APSCache cache = getCache(executingPerson);
		query.put("owner", "self");
		List<DBRecord> recs = QueryEngine.listInternal(cache, executingPerson, null, query, fields);
		
		wipe(executingPerson, recs);		
		//fixAccount(executingPerson);
		
		AccessLog.logEnd("end deleteRecord");
	}
	
	protected void wipe(MidataId executingPerson, List<DBRecord> recs) throws AppException {
		APSCache cache = getCache(executingPerson);
		if (recs.size() == 0) return;
		
		AccessLog.logBegin("begin wipe #records="+recs.size());
		cache.finishTouch();
		
		Set<MidataId> streams = new HashSet<MidataId>();
		
		Iterator<DBRecord> it = recs.iterator();
		while (it.hasNext()) {
	   	   DBRecord record = it.next();			
	       if (record.meta.getString("content").equals("Patient")) it.remove();
		   if (record.owner == null) throw new InternalServerException("error.internal", "Owner of record is null.");
		   if (!record.owner.equals(executingPerson)) throw new BadRequestException("error.internal", "Not owner of record!");
		}
		
		IndexManager.instance.removeRecords(cache, executingPerson, recs);
		
		Set<Consent> consents = Consent.getAllByOwner(executingPerson, new HashMap<String, Object>(), Sets.create("_id"), Integer.MAX_VALUE);
		
		for (Consent c : consents) {
			cache.getAPS(c._id).removePermission(recs);		
		}
						
		Set<Space> spaces = Space.getAllByOwner(executingPerson, Sets.create("_id"));
		for (Space s : spaces) {
			cache.getAPS(s._id, executingPerson).removePermission(recs);			
		}
		
		for (DBRecord record : recs) {
			if (record.stream != null) {
				cache.getAPS(record.stream, executingPerson).removePermission(record);
				streams.add(record.stream);
			}
		}
		
		cache.getAPS(executingPerson, executingPerson).removePermission(recs);
		
		for (DBRecord record : recs) {
			if (record.isStream!=null) {
				AccessPermissionSet.delete(record._id);
			}
		}
		
		Set<MidataId> ids = new HashSet<MidataId>(recs.size());				
		for (DBRecord record : recs) { 
			ids.add(record._id);		  
		}
		DBRecord.deleteMany(ids);
		
		for (MidataId streamId : streams) {
			try {
				getCache(executingPerson).getAPS(streamId).removeMeta("_info");
										
				List<DBRecord> testRec = QueryEngine.listInternal(cache, streamId, null, CMaps.map("limit", 1), Sets.create("_id"));
				if (testRec.size() == 0) {
					wipe(executingPerson, CMaps.map("_id", streamId).map("streams", "only"));
				}	
			} catch (APSNotExistingException e) {}
		}
				
		AccessLog.logEnd("end wipe #records="+recs.size());				
	}
	
	/**
	 * "Delete" records from the system. This actually only hides records.
	 * @param executingPerson id of executing person
	 * @param query
	 * @throws AppException
	 */	
	public void delete(MidataId executingPerson, Map<String, Object> query) throws AppException {
		AccessLog.logBegin("begin deletingRecords executor="+executingPerson.toString());
			
		APSCache cache = getCache(executingPerson);
		query.put("owner", "self");
		List<DBRecord> recs = QueryEngine.listInternal(cache, executingPerson, null, query, COMPLETE_META);
		
		delete(cache, executingPerson, recs);				
		
		AccessLog.logEnd("end deleteRecord");
	}
	
	public void deleteFromPublic(MidataId executingPerson, Map<String, Object> query) throws AppException {
		AccessLog.logBegin("begin deleteFromPublic executor="+executingPerson.toString());
					
		query.put("public", "only");
		query.put("public-strict", true);
		
		List<DBRecord> recs = QueryEngine.listInternal(getCache(executingPerson), executingPerson, null, query, COMPLETE_META);
		
		APSCache cache = Feature_PublicData.getPublicAPSCache(getCache(executingPerson));
		delete(cache, RuntimeConstants.instance.publicUser, recs);				
		
		AccessLog.logEnd("end deleteFromPublic");
	}
	
	private void delete(APSCache cache, MidataId executingPerson, List<DBRecord> recs) throws AppException {
		
		if (recs.size() == 0) return;
		
		AccessLog.logBegin("begin delete #records="+recs.size());
		Set<MidataId> streams = new HashSet<MidataId>();
		
		Iterator<DBRecord> it = recs.iterator();
		while (it.hasNext()) {
	   	   DBRecord record = it.next();			
	       if (record.meta.getString("content").equals("Patient")) it.remove();
		   if (record.owner == null) throw new InternalServerException("error.internal", "Owner of record is null.");
		   if (!record.owner.equals(executingPerson)) throw new BadRequestException("error.internal", "Not owner of record!");
		}
		
		IndexManager.instance.removeRecords(cache, executingPerson, recs);
		
		QueryEngine.loadDataAndWatches(recs);
		
		for (DBRecord record : recs) {
			if (record.stream != null) {		
				streams.add(record.stream);
			}
		}
		
		Date now = new Date();		
		for (DBRecord record : recs) { 			
			if (record.data == null) throw new NullPointerException();
			VersionedDBRecord vrec = new VersionedDBRecord(record);
			
			RecordEncryption.encryptRecord(vrec);			
						
			record.data = null;
			record.meta.put("name", "deleted");
			record.meta.put("deleted", true);
		    record.meta.put("lastUpdated", now);
		    record.time = Query.getTimeFromDate(now);		
		    
		    String version = Long.toString(System.currentTimeMillis());
		    record.meta.put("version", version);
			
		    DBRecord clone = record.clone();
		    
			RecordEncryption.encryptRecord(record);	
						
			VersionedDBRecord.add(vrec);
		    DBRecord.upsert(record); 	  			    
		    RecordLifecycle.notifyOfChange(clone, cache);									
		}
				
		for (MidataId streamId : streams) {
			try {
				cache.getAPS(streamId).removeMeta("_info");
														
			} catch (APSNotExistingException e) {}
		}
				
		AccessLog.logEnd("end delete #records="+recs.size());				
	}

	private byte[] addRecordIntern(AccessContext context, DBRecord record, boolean documentPart, MidataId alternateAps, boolean upsert) throws AppException {		
		
		if (!documentPart) Feature_Streams.placeNewRecordInStream(context, record, alternateAps);
		 		
		AccessLog.logBegin("Begin Add Record execPerson="+context.getCache().getAccountOwner().toString()+" format="+record.meta.get("format")+" stream="+(record.stream != null ? record.stream.toString() : "null"));	
		byte[] usedKey = null;
		if (record.meta.get("created") == null) throw new InternalServerException("error.internal", "Missing creation date");
		
		record.time = Query.getTimeFromDate((Date) record.meta.get("created"));				
		record = record.clone();
		if (record.owner.equals(record.meta.get("creator"))) record.meta.removeField("creator");
																	
		if (!documentPart) {
			APS apswrapper = context.getCache().getAPS(record.stream, record.owner);	
			
			apswrapper.provideRecordKey(record);
			
			usedKey = record.key;
    								
			if (apswrapper.getSecurityLevel().equals(APSSecurityLevel.HIGH)) record.time = 0;
			
			DBRecord unencrypted = record.clone();
			
			RecordEncryption.encryptRecord(record);		
		    if (upsert) { DBRecord.upsert(record); } else { DBRecord.add(record); }	  
		    
		    if (!unencrypted.direct && !documentPart) apswrapper.addPermission(unencrypted, false);
			else context.getCache().touchAPS(apswrapper.getId());
		    
		    //Feature_Expiration.check(getCache(executingPerson), apswrapper);
		    context.getCache().addNewRecord(unencrypted);
		    
		    RecordManager.instance.applyQueries(context, unencrypted.owner, unencrypted, alternateAps != null ? alternateAps : unencrypted.owner);
			
		    
			
		} else {
			record.time = 0;
			usedKey = record.key;
			
			RecordEncryption.encryptRecord(record);		
		    if (upsert) { DBRecord.upsert(record); } else { DBRecord.add(record); }	  
		}
								
		
	    
	    //RecordLifecycle.notifyOfCreation(record, getCache(executingPerson));
	    AccessLog.logEnd("End Add Record");				
		return usedKey;	
	}
	
	public void applyQuery(MidataId executor, MidataId ownerId, Map<String, Object> query, MidataId sourceaps, MidataId targetaps, boolean ownerInformation) throws AppException {
		applyQuery(new DummyAccessContext(getCache(executor), ownerId), query, sourceaps, targetaps, ownerInformation);
	}
	
	/**
	 * Share (stream) records from one APS to another by applying a query once.
	 * @param userId id of executing user
	 * @param query to query to be applied
	 * @param sourceaps the source APS id
	 * @param targetaps the target APS id
	 * @param ownerInformation include owner information?
	 * @throws AppException
	 */
	public void applyQuery(AccessContext context, Map<String, Object> query, MidataId sourceaps, MidataId targetaps, boolean ownerInformation) throws AppException {
		
		Pair<Map<String, Object>, Map<String, Object>> pair = Feature_Streams.convertToQueryPair(query);
				
		AccessLog.logBegin("BEGIN APPLY QUERY");
		MidataId userId = context.getCache().getExecutor();
		if (pair.getRight() != null) {
			AccessLog.logBegin("SINGLE RECORDS");
			List<DBRecord> recs = QueryEngine.listInternal(getCache(userId), sourceaps, context, CMaps.map(pair.getRight()).map("owner", context.getSelf()), RecordManager.SHARING_FIELDS);			
			RecordManager.instance.share(getCache(userId), targetaps, null, recs, ownerInformation);
		}
		if (pair.getLeft() != null) {
			List<Record> recs = RecordManager.instance.list(userId, UserRole.ANY, targetaps, CMaps.map(pair.getLeft()).map("flat", "true").map("owner", context.getSelf()), Sets.create("_id"));
			Set<MidataId> remove = new HashSet<MidataId>();
			for (Record r : recs) remove.add(r._id);
			AccessLog.log("REMOVE DUPLICATES:"+remove.size());
			RecordManager.instance.unshare(userId, targetaps, remove);		
			
			
			Map<String, Object> selectionQuery = CMaps.map(pair.getLeft()).map("streams", "true").map("flat", "true").map("owner", context.getSelf());		
			List<DBRecord> records = QueryEngine.listInternal(getCache(userId), sourceaps, null, selectionQuery, RecordManager.SHARING_FIELDS);
			
			AccessLog.log("SHARE QUALIFIED STREAMS:"+records.size());
			if (records.size() > 0) {				
				RecordManager.instance.share(getCache(userId), targetaps, null, records, ownerInformation);
			}
			
			List<DBRecord> streams = QueryEngine.listInternal(getCache(userId), targetaps, null, RecordManager.STREAMS_ONLY_OWNER, RecordManager.COMPLETE_META);
			AccessLog.log("UNSHARE STREAMS CANDIDATES = "+streams.size());
			
			List<DBRecord> stillOkay = QueryEngine.listFromMemory(context, pair.getLeft(), streams);
			streams.removeAll(stillOkay);		
			remove = new HashSet<MidataId>();
			for (DBRecord stream : streams) {
				remove.add(stream._id);
			}
			
			AccessLog.log("UNSHARE STREAMS QUALIFIED = "+remove.size());
			RecordManager.instance.unshare(userId, targetaps, remove);
			AccessLog.logEnd("END APPLY RULES");
		}
		
	}
	
	protected void applyQueries(AccessContext context, MidataId userId, DBRecord record, MidataId useAps) throws AppException {
		AccessLog.logBegin("start applying queries for targetUser="+userId.toString());
		if (record.isStream!=null) {
		
			Member member = Member.getById(userId, Sets.create("queries"));
			if (member != null && member.queries!=null) {
				for (String key : member.queries.keySet()) {
					try {
					Map<String, Object> query = member.queries.get(key);
					if (QueryEngine.isInQuery(context, query, record)) {
						try {
						  MidataId targetAps = new MidataId(key);
						  APS apswrapper = context.getCache().getAPS(targetAps, userId);
						  RecordManager.instance.shareUnchecked(context.getCache(),Collections.singletonList(record), Collections.<DBRecord>emptyList(), apswrapper, true);
						} catch (APSNotExistingException e) {
							
						}
					}
					} catch (BadRequestException e) {
						AccessLog.logException("error while sharing", e);
					}  
				}
			}
		
		} else {
			
			Member member = Member.getById(userId, Sets.create("rqueries"));
			if (member != null && member.rqueries!=null) {
				for (String key : member.rqueries.keySet()) {
					try {
					Map<String, Object> query = member.rqueries.get(key);
					if (QueryEngine.isInQuery(context, query, record)) {
						try {
						  MidataId targetAps = new MidataId(key);
						  APS apswrapper = context.getCache().getAPS(targetAps, userId);
						  RecordManager.instance.shareUnchecked(context.getCache(),Collections.singletonList(record), Collections.<DBRecord>emptyList(), apswrapper, true);
						} catch (APSNotExistingException e) {
							
						}
					}
					} catch (BadRequestException e) {
						AccessLog.logException("error while sharing", e);
					}  
				}
			}
		
			
		}
		AccessLog.logEnd("end applying queries");
	}
	
	public Set<MidataId> findAllSharingAps(MidataId executorId, Record record1) throws AppException {
		 
		    DBRecord record = RecordConversion.instance.toDB(record1);
		    MidataId recordOwner = record1.owner;
		    AccessContext context = new DummyAccessContext(getCache(executorId), record1.owner);
		    Set<MidataId> result = new HashSet<MidataId>();
		
			Member member = Member.getById(recordOwner, Sets.create("rqueries", "queries"));
			if (member.queries!=null) {
				for (String key : member.queries.keySet()) {
					//try {
					Map<String, Object> query = member.queries.get(key);
					if (QueryEngine.isInQuery(context, query, record)) {
						result.add(new MidataId(key));						
					}
					//} catch (BadRequestException e) {
				//		AccessLog.logException("error while sharing", e);
				//	}  
				}
			}
									
			if (member.rqueries!=null) {
				for (String key : member.rqueries.keySet()) {
					//try {
					Map<String, Object> query = member.rqueries.get(key);
					if (QueryEngine.isInQuery(context, query, record)) {
						result.add(new MidataId(key));						
					}
					//} catch (BadRequestException e) {
					//	AccessLog.logException("error while sharing", e);
					//}  
				}
			}
		
			return result;		
	}
	
    /**
     * Delete an access permission set
     * @param apsId id of APS
     * @param ownerId id of owner of APS
     * @throws InternalServerException
     */
	public void deleteAPS(MidataId apsId, MidataId executorId) throws AppException {
		AccessLog.logBegin("begin deleteAPS aps="+apsId.toString()+" executor="+executorId.toString());
		
		APSCache cache = getCache(executorId);
		APS apswrapper = cache.getAPS(apsId);
		try {
		List<DBRecord> recordEntries = QueryEngine.listInternal(getCache(executorId), apsId, null, CMaps.map("ignore-redirect", true),
				Sets.create("_id", "watches"));		
		
			for (DBRecord rec : recordEntries) {		
				cache.changeWatches().removeWatchingAps(rec, apsId);				
			}
		} catch (AppException e) {
			AccessLog.logException("error while deleting APS", e);
		}
		cache.changeWatches().save();
		AccessPermissionSet.delete(apsId);
		AccessLog.logEnd("end deleteAPS");
	}

	/**
	 * Query for records matching some criteria.
	 * In the result list only those fields of the record need to be filled out that have been requested.
	 * Additional fields may be set if they were needed to processing the query.
	 * 
	 * @param who id of executing person
	 * @param apsId id of APS to access
	 * @param properties key,value map containing the restrictions
	 * @param fields set of fields to return from the query
	 * @return list of records matching properties
	 * @throws AppException
	 */
	public List<Record> list(MidataId who, UserRole role, MidataId apsId,
			Map<String, Object> properties, Set<String> fields)
			throws AppException {
		AccessContext context = null;
		if (who.equals(apsId)) context = createContextFromAccount(who);
		else {
          Consent consent = Consent.getByIdUnchecked(apsId, Consent.ALL);
          if (consent != null) {
        	  if (consent.status != ConsentStatus.ACTIVE && consent.status != ConsentStatus.FROZEN && !consent.owner.equals(who)) return Collections.emptyList();
        	  context =  createContextFromConsent(who, consent);
          } else return Collections.emptyList(); 
		}
		AccessLog.log("context="+context);
		QueryTagTools.handleSecurityTags(role, properties, fields);
		return QueryEngine.list(context.getCache(), apsId, context, properties, fields);
	}
		
	
	public DBIterator<Record> listIterator(MidataId who, UserRole role, AccessContext context,
			Map<String, Object> properties, Set<String> fields)
			throws AppException {	
		QueryTagTools.handleSecurityTags(role, properties, fields);
		return QueryEngine.listIterator(context.getCache(), context.getTargetAps(), context, properties, fields);
	}
	
	/*
	public List<Record> list(MidataId who, AccessContext context,
			Map<String, Object> properties, Set<String> fields)
			throws AppException {
		return QueryEngine.list(getCache(who), context.getTargetAps(), context, properties, fields);
	}
	*/
	
	public List<Record> list(MidataId who, UserRole role, AccessContext context,
			Map<String, Object> properties, Set<String> fields)
			throws AppException {
		QueryTagTools.handleSecurityTags(role, properties, fields);
		return QueryEngine.list(context.getCache(), context.getTargetAps(), context, properties, fields);
	}
	
	/**
	 * computes a summary of records
	 * @param who id of executing person
	 * @param aps id of APS to access
	 * @param properties key,value map containing restrictions
	 * @param aggrType level of aggreagtion
	 * @return list of RecordsInfo objects containing a summary of the records
	 * @throws AppException
	 */
	public Collection<RecordsInfo> info(MidataId who, UserRole role, MidataId aps, AccessContext context, Map<String, Object> properties, AggregationType aggrType) throws AppException {
		// Only allow specific properties as results are materialized
		Map<String, Object> nproperties = new HashMap<String, Object>();
		nproperties.put("streams", "true");
		nproperties.put("flat", "true");
		nproperties.put("group-system", "v1");
		nproperties.put("consent-limit", 1000);
		nproperties.put("no-postfilter-streams", true); // For old streams without "app" field
		if (properties.containsKey("group-system")) nproperties.put("group-system", properties.get("group-system"));
		if (properties.containsKey("owner")) nproperties.put("owner", properties.get("owner"));
		if (properties.containsKey("study")) nproperties.put("study", properties.get("study"));
		if (properties.containsKey("study-group")) nproperties.put("study-group", properties.get("study-group"));
		if (properties.containsKey("format")) nproperties.put("format", properties.get("format"));
		if (properties.containsKey("format/*")) nproperties.put("format/*", properties.get("format/*"));
		if (properties.containsKey("content")) nproperties.put("content", properties.get("content"));
		if (properties.containsKey("content/*")) nproperties.put("content/*", properties.get("content/*"));
		if (properties.containsKey("app")) nproperties.put("app", properties.get("app"));
		if (properties.containsKey("public")) nproperties.put("public", properties.get("public"));
		if (properties.containsKey("group")) nproperties.put("group", properties.get("group"));
		if (properties.containsKey("code")) {
			Set<String> codes = Query.getRestriction(properties.get("code"), "code");
			Set<String> contents = new HashSet<String>();
			for (String code : codes) {
				 String content = ContentCode.getContentForSystemCode(code);
				 if (content == null) throw new BadRequestException("error.unknown.code", "Unknown code '"+code+"' in restriction.");
				 contents.add(content);
			}
			nproperties.put("content", contents);
		}
		
		try {
		    Collection<RecordsInfo> result = QueryEngine.info(context.getCache(), aps, context, nproperties, aggrType);
		    
		    if (properties.containsKey("include-records")) {		    	
			    for (RecordsInfo inf : result) {
			    	if (inf.newestRecord != null) {
			    		inf.newestRecordContent = fetch(who, role, context, inf.newestRecord, null);
			    	}
			    }
		    }
		    
		    return result;
		} catch (APSNotExistingException e) {
			checkRecordsInAPS(who, aps, false);
			//fixAccount(who);
			throw e;
		}
	}

	/**
	 * Lookup a single record by providing a RecordToken 
	 * @param who id of executing person
	 * @param token token with id of record to return 
	 * @return the complete record (no attachment)
	 * @throws AppException
	 */
	public Record fetch(MidataId who, UserRole role, RecordToken token) throws AppException {
		return fetch(who, role, token, RecordManager.COMPLETE_DATA);
	}
	
	/**
	 * Lookup a single record by providing a RecordToken and return the attachment of the record
	 * @param who id of executing person
	 * @param token token with the id of the record
	 * @return the attachment content
	 * @throws AppException
	 */
	public FileData fetchFile(MidataId who, RecordToken token) throws AppException {		
		List<DBRecord> result = QueryEngine.listInternal(getCache(who), new MidataId(token.apsId), createContext(who, MidataId.from(token.apsId)), CMaps.map("_id", new MidataId(token.recordId)), Sets.create("key", "data"));
				
		if (result.size() != 1) throw new InternalServerException("error.internal.notfound", "Unknown Record");
		DBRecord rec = result.get(0);
		
		if (rec.security == null) throw new InternalServerException("error.internal", "Missing key for record:"+rec._id.toString());
		
		MidataId fileId;
		byte[] key;
		if (rec.meta.containsField("file")) {
			fileId = MidataId.from(rec.meta.get("file"));
			key = (byte[]) rec.meta.get("file-key");
		} else {
			fileId = rec._id;
			key = rec.key;
		}
		
		FileData fileData = FileStorage.retrieve(fileId, 0);
		if (fileData == null) throw new InternalServerException("error.internal", "Record "+rec._id.toString()+" has no binary data attached.");		
		
		if (rec.security.equals(APSSecurityLevel.NONE) || rec.security.equals(APSSecurityLevel.LOW)) {
		  fileData.inputStream = fileData.inputStream;			
		} else {
		  fileData.inputStream = EncryptionUtils.decryptStream(key, fileData.inputStream);
		}
		
		return fileData;
	}

	/**
	 * Lookup a single record by providing a RecordToken and return only requested fields
	 * @param who id of executing person
	 * @param token token with the id of the record
	 * @param fields set of field names that are required
	 * @return the record to be returned
	 * @throws AppException
	 */
	public Record fetch(MidataId who, UserRole role, RecordToken token, Set<String> fields)
			throws AppException {
		List<Record> result = list(who, role, createContext(who, MidataId.from(token.apsId)),
				CMaps.map("_id", new MidataId(token.recordId)), fields);
		if (result.isEmpty())
			return null;
		else
			return result.get(0);
	}

	/**
	 * Lookup a single record by providing its id and the APS from where it is accessible
	 * @param who id of executing person
	 * @param aps id of APS 
	 * @param recordId id of record
	 * @return the record to be returned
	 * @throws AppException
	 */
	public Record fetch(MidataId who, UserRole role, AccessContext context, MidataId recordId, String format)
			throws AppException {
		List<Record> result = list(who, role, context, CMaps.map("_id", recordId).mapNotEmpty("format", format),
				RecordManager.COMPLETE_DATA);
		if (result.isEmpty())
			return null;
		else
			return result.get(0);
	}

	/**
	 * Return a set with all record ids accessible from an APS
	 * @param who id of executing person
	 * @param apsId id of APS to search
	 * @return set with record ids as strings
	 * @throws AppException
	 */
	public Set<String> listRecordIds(MidataId who, UserRole role, AccessContext context)
			throws AppException {
		return listRecordIds(who, role, context, RecordManager.FULLAPS_LIMITED_SIZE);
	}

	/**
	 * Return a set with all record ids of records matching some criteria accessible from an APS
	 * @param who id of executing person
	 * @param apsId id of APS to search
	 * @param properties key,value map containing query
	 * @return set with record ids as strings
	 * @throws AppException
	 */
	public Set<String> listRecordIds(MidataId who, UserRole role, AccessContext context,
			Map<String, Object> properties) throws AppException {
		List<Record> result = list(who, role, context, properties,
				RecordManager.INTERNALIDONLY);
		Set<String> ids = new HashSet<String>();
		for (Record record : result)
			ids.add(record._id.toString());
		return ids;
	}
	
	/**
	 * for debugging only: remove all "info" objects from all APSs so that they are recomputed next time.
	 * @param who
	 * @throws AppException
	 */
	private void resetInfo(MidataId who) throws AppException {
		AccessLog.logBegin("start reset info user="+who.toString());
		List<Record> result = list(who, UserRole.ANY, RecordManager.instance.createContextFromAccount(who), CMaps.map("streams", "only").map("flat", "true"), Sets.create("_id", "owner"));
		for (Record stream : result) {
			try {
			  AccessLog.log("reset stream:"+stream._id.toString());
			  Feature_UserGroups.findApsCacheToUse(getCache(who), stream._id).getAPS(stream._id, stream.owner).removeMeta("_info");			  
			} catch (APSNotExistingException e) {}
			catch (InternalServerException e2) {
				AccessLog.log("Stream access error: "+stream._id+" ow="+stream.owner.toString());
			}
		}
		AccessLog.logEnd("end reset info user="+who.toString());
	}

	/**
	 * removes info objects and all indexes for the current account
	 * @param userId id of user
	 * @throws AppException
	 */
	public void fixAccount(MidataId userId) throws AppException {
				
		IndexManager.instance.clearIndexes(getCache(userId), userId);
		
		APSCache cache = getCache(userId);
				
		
		AccessLog.logBegin("start search for missing records");
		checkRecordsInAPS(userId, userId, true);		
		AccessLog.logEnd("end search for missing records");
		
		AccessLog.logBegin("start searching for missing records in consents");
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map(), Sets.create("_id"), Integer.MAX_VALUE);
		for (Consent consent : consents) {
			try {
				cache.getAPS(consent._id, userId).getStoredOwner();					
			} catch (Exception e) {
				Consent.delete(userId, consent._id);
				continue;
			}
			checkRecordsInAPS(userId, consent._id, true);
		}
		AccessLog.logEnd("end searching for missing records in consents");
		
		AccessLog.logBegin("start searching for missing records in authorized consents");		
		consents = Consent.getAllActiveByAuthorized(userId);
		for (Consent consent : consents) {
			checkRecordsInAPS(userId, consent._id, false);
		}
		AccessLog.logEnd("end searching for missing records in authorized consents");
						
		AccessLog.logBegin("start searching for missing records in spaces");
		Set<Space> spaces = Space.getAllByOwner(userId, Sets.create("_id"));
		for (Space space : spaces) {			
			checkRecordsInAPS(userId,space._id, true);
		}
		AccessLog.logEnd("end searching for missing records in spaces");
		
		AccessLog.logBegin("start searching for empty streams");
		Set<String> fields = new HashSet<String>();
		fields.add("owner");
		fields.addAll(APSEntry.groupingFields);
		fields.add("consentAps");
		List<DBRecord> streams = QueryEngine.listInternal(cache, userId, null, CMaps.map("owner", "self").map("streams", "only").map("flat", "true"), fields);
		
		List<DBRecord> emptyStreams = new ArrayList<DBRecord>();
		for (DBRecord str : streams) {
			List<DBRecord> testRec = QueryEngine.listInternal(cache, str._id, null, CMaps.map("limit", 1), Sets.create("_id"));
			if (testRec.size() == 0) {
				emptyStreams.add(str);
			}
		}
		if (emptyStreams.size() > 0) {
			wipe(userId, emptyStreams);
		}
		
		AccessLog.logEnd("end searching for empty streams");
		
		resetInfo(userId);
		
		Feature_Streams.streamJoin(createContextFromAccount(userId));
	}
	
	public void checkRecordsInAPS(MidataId userId, MidataId apsId, boolean instreams) throws AppException {
		APSCache cache = getCache(userId);
		AccessLog.logBegin("check records in APS:"+apsId.toString());
		List<DBRecord> recs = QueryEngine.listInternal(cache, apsId, null, CMaps.map("owner", "self").map("streams", "only").map("flat", "true"), Sets.create("_id"));
		Set<String> idOnly = Sets.create("_id");
		for (DBRecord rec : recs) {
			if (DBRecord.getById(rec._id, idOnly) == null) {				
				cache.getAPS(apsId).removePermission(rec);
			} else {
				try {
				  cache.getAPS(rec._id, rec.owner).getStoredOwner();
				} catch (Exception e) {
				  cache.getAPS(apsId).removePermission(rec);
				}
			}			
		}
		
		recs = QueryEngine.listInternal(cache, apsId, null, CMaps.map("owner", "self"), Sets.create("_id"));		
		for (DBRecord rec : recs) {
			if (DBRecord.getById(rec._id, idOnly) == null) {
				if (instreams && rec.stream != null) cache.getAPS(rec.stream, userId).removePermission(rec);
				cache.getAPS(apsId).removePermission(rec);
			} 			
		}
		AccessLog.logEnd("end check records in APS:"+apsId.toString());
	}

	public void patch20160407(MidataId who) throws AppException {
		List<DBRecord> all = QueryEngine.listInternal(getCache(who), who, null, CMaps.map("owner", "self"), RecordManager.COMPLETE_META);
		List<DBRecord> toWipe = new ArrayList<DBRecord>();
		for (DBRecord r : all) {
			if (!r.meta.containsField("code")) { 
				String content = r.meta.getString("content");				
				if (content == null) {
					toWipe.add(r);					
					continue;
				}
												
				if (ContentInfo.isCoding(content)) {
				   r.meta.put("code", content);
				   String content2 = ContentCode.getContentForSystemCode(content);
				   if (content2 == null) {
					   toWipe.add(r);					   
					   continue;
				   }
				   r.meta.put("content", content2);
				} else {
				   try {
				      r.meta.put("code", ContentInfo.getByName(r.meta.getString("content")).defaultCode);
				   } catch (BadRequestException e) {
					   toWipe.add(r);					  
					  continue;
				   }
				}
			    RecordEncryption.encryptRecord(r);
			    DBRecord.set(r._id, "encrypted", r.encrypted);
			}
		}
		wipe(who, toWipe);
		
	}
	
	public AccountStats getStats(MidataId userId) throws AppException {
		AccountStats result = new AccountStats();
		result.numConsentsOwner = Consent.count(userId);
		Set<MidataId> auth = new HashSet<MidataId>();
		auth.add(userId);
		for (UserGroupMember ugm : UserGroupMember.getAllActiveByMember(userId)) {
			auth.add(ugm.userGroup);
			result.numUserGroups++;
		}
		result.numConsentsAuth = Consent.countAuth(auth);
		DBIterator<DBRecord> it = QueryEngine.listInternalIterator(getCache(userId), userId, new AccountAccessContext(getCache(userId), null), CMaps.map("streams","only").map("owner","self").map("flat",true), Sets.create("_id"));
		while (it.hasNext()) { it.next();result.numOwnStreams++;result.numOtherStreams--; }
		it = QueryEngine.listInternalIterator(getCache(userId), userId, new AccountAccessContext(getCache(userId), null), CMaps.map("streams","only").map("flat",true), Sets.create("_id"));
		while (it.hasNext()) { it.next();result.numOtherStreams++; }
		
		return result;
	}
	
	public void clearIndexes(MidataId userId) throws AppException {
		IndexManager.instance.clearIndexes(RecordManager.instance.getCache(userId), userId);		
	}
	
	public SpaceAccessContext createContextFromSpace(MidataId executorId, Space space, MidataId self) throws InternalServerException {
		return new SpaceAccessContext(space, getCache(executorId), null, self);
	}	
	
	public ConsentAccessContext createContextFromConsent(MidataId executorId, Consent consent) throws AppException {
		return new ConsentAccessContext(consent, getCache(executorId), null);
	}
	
	public AccountAccessContext createContextFromAccount(MidataId executorId) throws InternalServerException {
		return new AccountAccessContext(getCache(executorId), null);
	}
	
	public PublicAccessContext createPublicContext(MidataId executorId, AccessContext parent) throws InternalServerException {
		return new PublicAccessContext(Feature_PublicData.getPublicAPSCache(getCache(executorId)), parent);
	}
	
	public AccessContext createContext(MidataId executor, MidataId aps) throws AppException {
		if (executor.equals(aps)) return createContextFromAccount(executor);
		else if (aps.equals(RuntimeConstants.instance.publicUser)) return createPublicContext(executor, createContextFromAccount(executor));
		else {
          Consent consent = Consent.getByIdUnchecked(aps, Consent.ALL);
          if (consent != null) {
        	  if (consent.status != ConsentStatus.ACTIVE && consent.status != ConsentStatus.FROZEN && !consent.owner.equals(executor)) throw new InternalServerException("error.internal",  "Consent-Context creation not possible");
        	  return  createContextFromConsent(executor, consent);
          }
          
          Space space = Space.getByIdAndOwner(aps, executor, Space.ALL);
          if (space != null) return createContextFromSpace(executor, space, space.owner);
		}
		
		throw new InternalServerException("error.internal",  "Consent creation not possible");
	}
	
	public AppAccessContext createContextFromApp(MidataId executorId, MobileAppInstance app) throws InternalServerException {
		Plugin plugin = Plugin.getById(app.applicationId);
		return new AppAccessContext(app, plugin, getCache(executorId), null);
	}
	
	public UserGroupAccessContext createContextForUserGroup(UserGroupMember ugm, AccessContext parent) throws AppException {
		return new UserGroupAccessContext(ugm, Feature_UserGroups.findApsCacheToUse(parent.getCache(), ugm), parent);
	}

}
