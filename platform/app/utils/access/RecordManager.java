

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
import org.bson.types.BasicBSONList;

import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.AccountStats;
import models.Consent;
import models.ContentCode;
import models.ContentInfo;
import models.Member;
import models.MidataId;
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
import utils.ConsentQueryTools;
import utils.QueryTagTools;
import utils.RuntimeConstants;
import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ConsentAccessContext;
import utils.context.ContextManager;
import utils.context.DummyAccessContext;
import utils.db.FileStorage;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.messaging.SubscriptionManager;
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
			"app", "creator", "modifiedBy", "created", "name", "format", "content", "code", "description", "isStream", "lastUpdated",
			"data", "group"));
	public final static Set<String> COMPLETE_DATA_WITH_WATCHES = Collections.unmodifiableSet(Sets.create("id", "owner",
			"app", "creator", "modifiedBy", "created", "name", "format",  "content", "code", "description", "isStream", "lastUpdated",
			"data", "group", "watches", "stream"));
	public final static Set<String> SHARING_FIELDS = Collections.unmodifiableSet(Sets.create("_id", "key", "owner", "format", "content", "created", "name", "isStream", "stream", "app"));
	
	//public final static String STREAM_TYPE = "Stream";
	public final static Map<String, Object> STREAMS_ONLY = Collections.unmodifiableMap(CMaps.map("streams", "only").map("flat", "true"));
	public final static Map<String, Object> STREAMS_ONLY_OWNER = Collections.unmodifiableMap(CMaps.map("streams", "only").map("flat", "true").map("owner", "self"));	

		
	/**
	 * create a new access permission where only the owner has access
	 * @param who ID of APS owner
	 * @param proposedId ID of APS to be created
	 * @return ID of APS
	 * @throws AppException
	 */
	public MidataId createPrivateAPS(APSCache cache, MidataId who, MidataId proposedId)
			throws AppException {
		AccessLog.logBegin("begin createPrivateAPS who=",who.toString()," id=",proposedId.toString());
		EncryptedAPS eaps = new EncryptedAPS(proposedId, who, who, APSSecurityLevel.HIGH, false);
		EncryptionUtils.addKey(who, eaps);		
		eaps.create();
		APSCache current = cache != null ? cache : ContextManager.instance.currentCacheUsed();
		if (current != null) current.addAPS(new APSTreeImplementation(new EncryptedAPS(eaps.getId(), current.getAccessor(), eaps.getAPSKey(), eaps.getOwner())));
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
        AccessLog.logBegin("begin createAnonymizedAPS owner=",owner.toString()," other=",other.toString()," id=",proposedId.toString());
		EncryptedAPS eaps = new EncryptedAPS(proposedId, owner, owner, APSSecurityLevel.HIGH, consent);
		EncryptionUtils.addKey(owner, eaps);
		EncryptionUtils.addKey(other, eaps, otherNotOwner);	
		if (history) eaps.getPermissions().put("_history", new BasicBSONList()); // Init with history
		eaps.create();
		APSCache current = ContextManager.instance.currentCacheUsed();
		if (current != null) current.addAPS(new APSTreeImplementation(new EncryptedAPS(eaps.getId(), current.getAccessor(), eaps.getAPSKey(), eaps.getOwner())));
       
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
		AccessLog.logBegin("begin createAPSForRecord exec=",executingPerson.toString()," owner=",owner.toString()," record=",recordId.toString());
		
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
	public void shareAPS(AccessContext context, Set<MidataId> targetUsers) throws AppException {		
		if (targetUsers==null || targetUsers.isEmpty()) return;
		
		AccessLog.logBegin("begin shareAPS aps=",context.getTargetAps().toString()," executor=",context.getAccessor().toString()," #targetUsers=",Integer.toString(targetUsers.size()));
		APSCache cache = context.getCache();
		//if (context != null) {
		  try (DBIterator<DBRecord> it = QueryEngine.listInternalIterator(cache, context.getTargetAps(), context, CMaps.map("flat","true").map("streams","only").map("owner",cache.getAccountOwner()).map("ignore-redirect","true"),SHARING_FIELDS)) {
			  shareRecursive(cache, it, targetUsers);
		  }
		//}
		cache.getAPS(context.getTargetAps()).addAccess(targetUsers);
		AccessLog.logEnd("end shareAPS");
	}
	
	public void reshareAPS(AccessContext context, MidataId groupWithAccessId,
			Set<MidataId> targetUsers) throws AppException {
		if (groupWithAccessId == null || groupWithAccessId.equals(context.getAccessor())) {
			shareAPS(context, targetUsers);
		} else {
			AccessLog.logBegin("begin reshareAPS aps=",context.getTargetAps().toString()," executor=",context.getAccessor().toString()," #targetUsers=",Integer.toString(targetUsers.size()));
			APSCache cache = context.getCache();
			try (DBIterator<DBRecord> it = QueryEngine.listInternalIterator(cache, context.getTargetAps(), context, CMaps.map("flat","true").map("streams","only").map("owner",cache.getAccountOwner()), SHARING_FIELDS)) {
			  shareRecursive(cache, it, targetUsers);
			}
			Feature_UserGroups.findApsCacheToUse(cache, context.getTargetAps()).getAPS(context.getTargetAps()).addAccess(targetUsers);
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
	public void shareAPS(AccessContext context,
			byte[] publickey) throws AppException {
		AccessLog.logBegin("begin shareAPS aps=",context.getTargetAps().toString()," executor=",context.getAccessor().toString());
		context.getCache().getAPS(context.getTargetAps()).addAccess(context.getTargetAps(), publickey);
		AccessLog.logEnd("end shareAPS");
	}

	/**
	 * remove access permissions of given users from an APS
	 * @param apsId ID of APS
	 * @param executorId ID of executing person
	 * @param targetUsers set of IDs of target users
	 * @throws InternalServerException
	 */
	public void unshareAPS(AccessContext context,MidataId apsId,
			Set<MidataId> targetUsers) throws InternalServerException {
		AccessLog.logBegin("begin unshareAPS aps=",apsId.toString()," executor=",context.getAccessor().toString()," #targets=", Integer.toString(targetUsers.size()));
		context.getCache().getAPS(apsId).removeAccess(targetUsers);
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
	public void unshareAPSRecursive(AccessContext context, MidataId apsId, 
			Set<MidataId> targetUsers) throws AppException {
		AccessLog.logBegin("begin unshareAPSRecursive aps=",apsId.toString()," executor=",context.getAccessor().toString()," #targets=",Integer.toString(targetUsers.size()));
		if (context.getCache().getAPS(apsId).isAccessible()) {
			List<DBRecord> to_unshare = QueryEngine.listInternal(context.getCache(), apsId, context.internal(), CMaps.map("streams", "only").map("ignore-redirect", true), Sets.create("_id"));
			for (DBRecord rec : to_unshare) unshareAPS(context, apsId, targetUsers);
			context.getCache().getAPS(apsId).removeAccess(targetUsers);
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
	
	public void share(AccessContext context, MidataId fromAPS, MidataId toAPS, 
			Set<MidataId> records, boolean withOwnerInformation) throws AppException {
		share(context, fromAPS, toAPS, null, records, withOwnerInformation);
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
	public void share(AccessContext context, MidataId fromAPS, MidataId toAPS, MidataId toAPSOwner,
			Set<MidataId> records, boolean withOwnerInformation)
			throws AppException {
		share(context.getAccessor(), context.getCache(), fromAPS, toAPS, toAPSOwner, records, withOwnerInformation);
	}
	
	protected void share(MidataId who, APSCache cache, MidataId fromAPS, MidataId toAPS, MidataId toAPSOwner,
			Set<MidataId> records, boolean withOwnerInformation)
			throws AppException {
		if (fromAPS.equals(toAPS)) return;
        AccessLog.logBegin("begin share: who=",who.toString()," from=",fromAPS.toString()," to=",toAPS.toString()," count=",(records!=null ? Integer.toString(records.size()) : "?"));
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
			recordEntries = QueryEngine.listInternal(cache, fromAPS, new DummyAccessContext(cache),
					records != null ? CMaps.map("_id", records) : RecordManager.FULLAPS_FLAT,
					RecordManager.SHARING_FIELDS);
		}
		
		List<DBRecord> alreadyContained = QueryEngine.isContainedInAps(cache, toAPS, recordEntries);
		AccessLog.log("to-share: ", Integer.toString(recordEntries.size()), " already=", Integer.toString(alreadyContained.size()));
		
		shareUnchecked(cache, recordEntries, alreadyContained, apswrapper, withOwnerInformation);
        
        AccessLog.logEnd("end share");
	}
	
	public int share(AccessContext fromContext, MidataId toAPS, MidataId toAPSOwner,
			Map<String, Object> query, boolean withOwnerInformation) throws AppException {
		
		APSCache cache = fromContext.getCache();
		
		/*if (!who.equals(fromAPS)) {
			UserGroupMember ugm = UserGroupMember.getByGroupAndActiveMember(fromAPS, who);
			if (ugm!=null) {
				cache = Feature_UserGroups.findApsCacheToUse(cache, ugm);
			}
		}*/
		
		APS apswrapper = cache.getAPS(toAPS, toAPSOwner);
		List<DBRecord> recordEntries = QueryEngine.listInternal(cache, fromContext.getTargetAps(), fromContext,
				CMaps.map(query).map("owner", fromContext.getOwner()),	RecordManager.SHARING_FIELDS);
		
		share(cache, toAPS, toAPSOwner, recordEntries, withOwnerInformation);
		
		return recordEntries.size();
	}
	
	public int copyAPS(AccessContext context, MidataId fromAPS, MidataId toAPS, MidataId toAPSOwner) throws AppException {
		
		APSCache tocache = Feature_UserGroups.findApsCacheToUse(context.getCache(), toAPS);
		APSCache fromcache = Feature_UserGroups.findApsCacheToUse(context.getCache(), fromAPS);
		APS apswrapper = tocache.getAPS(toAPS, toAPSOwner);
		List<DBRecord> recordEntries = QueryEngine.listInternal(fromcache, fromAPS, context.internal(),
				CMaps.map("streams", "true").map("flat", "true"), RecordManager.SHARING_FIELDS);
		
		share(tocache, toAPS, toAPSOwner, recordEntries, false);
		
		return recordEntries.size();
	}
	
	
	public void share(APSCache cache, MidataId toAPS, MidataId toAPSOwner,
			List<DBRecord> recordEntries, boolean withOwnerInformation)
			throws AppException {		
        if (recordEntries.isEmpty()) return;
        
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
		AccessLog.log("to-share: count#=", Integer.toString(recordEntries.size()), " already=", Integer.toString(alreadyContained.size()));
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
	public void shareByQuery(AccessContext fromContext, MidataId toAPS,
			Map<String, Object> query) throws AppException {
        AccessLog.log("shareByQuery who=", fromContext.getAccessor().toString(), " from=", fromContext.getTargetAps().toString(), " to=", toAPS.toString(), "query=", query.toString());
		//if (toAPS.equals(who)) throw new BadRequestException("error.internal", "Bad call to shareByQuery. target APS may not be user APS!");
        APS apswrapper = fromContext.getCache().getAPS(toAPS);
        
        // Resolve "app" into IDs
        Query q = new Query("share-by-query",query, Sets.create("_id"), fromContext.getCache(), toAPS, fromContext, null);
        query = q.getProperties();
        
        query.remove("aps");
        if (query.isEmpty()) {
           apswrapper.removeMeta("_query");
        } else {
		   query.put("aps", fromContext.getTargetAps().toString());
		   apswrapper.setMeta("_query", query);
        }
        
        if (query.containsKey("exclude-ids")) {
			Map<String, Object> ids = new HashMap<String,Object>();
			ids.put("ids", query.get("exclude-ids"));
			apswrapper.setMeta("_exclude", ids);
		} else {
			apswrapper.removeMeta("_exclude");
		}
        
        List<DBRecord> doubles = QueryEngine.listInternal(fromContext.getCache(), toAPS, fromContext, CMaps.map(query).map("ignore-redirect", "true").map("flat", "true").map("streams", "true"), APSEntry.groupingFields);
        apswrapper.removePermission(doubles);        
	}
		

	/**
	 * remove records from an APS
	 * @param who id of executing person
	 * @param apsId id of target APS
	 * @param records set of record ids to remove from APS
	 * @throws AppException
	 */
	/*
	public void unshare(MidataId who, MidataId apsId, Set<MidataId> records)
			throws AppException {
		if (records.size() == 0) return;
		
        AccessLog.logBegin("begin unshare who="+who.toString()+" aps="+apsId.toString()+" #recs="+records.size());
        APSCache cache = Feature_UserGroups.findApsCacheToUse(getCache(who), apsId);
		APS apswrapper = cache.getAPS(apsId);
		List<DBRecord> recordEntries = QueryEngine.listInternal(getCache(who), apsId, null,
				CMaps.map("_id", records), Sets.create("_id", "format", "content", "watches"));		
		apswrapper.removePermission(recordEntries);
		for (DBRecord rec : recordEntries) cache.changeWatches().removeWatchingAps(rec, apsId);
		AccessLog.logEnd("end unshare");
	}*/
	
	public void unshare(AccessContext context, Set<MidataId> records)
			throws AppException {
		if (records.size() == 0) return;
		
        AccessLog.logBegin("begin unshare who=",context.getCache().getAccessor().toString()," aps=",context.getTargetAps().toString()," #recs=", Integer.toString(records.size()));
        APSCache cache = context.getCache();
		APS apswrapper = cache.getAPS(context.getTargetAps());
		List<DBRecord> recordEntries = QueryEngine.listInternal(cache, context.getTargetAps(), context.internal(),
				CMaps.map("_id", records), Sets.create("_id", "format", "content", "watches"));		
		apswrapper.removePermission(recordEntries);
		for (DBRecord rec : recordEntries) cache.changeWatches().removeWatchingAps(rec, context.getTargetAps());
		AccessLog.logEnd("end unshare");
	}
	
	public void unshare(AccessContext context, Collection<Record> records)
			throws AppException {
		if (records.size() == 0) return;
		
		Set<MidataId> ids = new HashSet<MidataId>();
		for (Record r : records) ids.add(r._id);
		
		unshare(context, ids);
	}

	/**
	 * read a meta data object from an APS
	 * @param who id of executing person
	 * @param apsId id of APS to read from
	 * @param key unique name of meta data object
	 * @return
	 * @throws AppException
	 */
	public BSONObject getMeta(AccessContext context, MidataId apsId, String key) throws AppException {
		AccessLog.logBegin("begin getMeta accessor=",context.getCache().getAccessor().toString()," aps=",apsId.toString()," key=",key);
		try {
		  BSONObject result = Feature_UserGroups.findApsCacheToUse(context.getCache(), apsId).getAPS(apsId).getMeta(key);
		  return result;
		} finally {
		  AccessLog.logEnd("end getMeta");
		}		
	}
	
	/**
	 * store a meta data object into an APS
	 * @param who id of executing person
	 * @param apsId id of APS to save into
	 * @param key unique name of meta data object
	 * @param data key,value map containing the data to be stored
	 * @throws AppException
	 */
	public void setMeta(AccessContext context, MidataId apsId, String key, Map<String,Object> data) throws AppException {
		AccessLog.logBegin("begin setMeta who=",context.getCache().getAccessor().toString()," aps=",apsId.toString()," key=",key);
		Feature_UserGroups.findApsCacheToUse(context.getCache(), apsId).getAPS(apsId).setMeta(key, data);
		AccessLog.logEnd("end setMeta");
	}
	
	/**
	 * remove a meta data object from an APS
	 * @param who id of executing person
	 * @param apsId id of APS to save into
	 * @param key unique name of meta data object	
	 * @throws AppException
	 */
	public void removeMeta(AccessContext context, MidataId apsId, String key) throws AppException {
		AccessLog.logBegin("begin removeMeta who=",context.getCache().getAccessor().toString()," aps=",apsId.toString()," key=",key);
		context.getCache().getAPS(apsId).removeMeta(key);
		AccessLog.logEnd("end removeMeta");
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
	public void addRecord(AccessContext context, Record record, MidataId alternateAps, List<EncryptedFileHandle> allData) throws AppException {
		
		for (EncryptedFileHandle data : allData) {
		  String virus = checkVirusFree(data);
	      if (virus != null) throw new BadRequestException("error.virus", "A virus has been detected: "+virus);
		}
	
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
		int idx = 0;
		for (EncryptedFileHandle data : allData) {
			dbrecord.meta.append(getFileMetaName(idx), data.getId().toObjectId());
			dbrecord.meta.append(getFileMetaName(idx)+"-key", data.getKey());
			idx++;
		}
		
		byte[] kdata = addRecordIntern(context, dbrecord, false, alternateAps, false);				
	}
	
	protected String getFileMetaName(int idx) {
		if (idx==0) return "file";
		return "file-"+idx;
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
		addRecord(context, record, alternateAps, Collections.singletonList(data));	
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
		  try {
		    FileStorage.delete(id.toObjectId());
		  } catch (Exception e2) { 
			// We do not handle error during Error
		  }
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
		
		AccessLog.logBegin("start virus scan");
		InputStream inputStream = EncryptionUtils.decryptStream(handle.getKey(), fileData.inputStream);
		
		VirusScanner vscan = new VirusScanner();
		String result = vscan.scan(inputStream);
		AccessLog.logEnd("end virus scan");
		return result;
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
	public String updateRecord(MidataId executingPerson, MidataId pluginId, AccessContext context, Record record, List<UpdateFileHandleSupport> allData) throws AppException {
		AccessLog.logBegin("begin updateRecord executor=",executingPerson.toString()," aps=",context.getTargetAps().toString()," record=",record._id.toString());
		try {
			List<DBRecord> result = QueryEngine.listInternal(context.getCache(), context.getTargetAps(),context, CMaps.map("_id", record._id).mapNotEmpty("format", record.format).map("updatable", true), RecordManager.COMPLETE_DATA_WITH_WATCHES);	
			if (result.size() != 1) {
				List<DBRecord> resultx = QueryEngine.listInternal(context.getCache(), context.getTargetAps(),context, CMaps.map("_id", record._id), RecordManager.INTERNALIDONLY);
				if (resultx.isEmpty()) {
				  throw new InternalServerException("error.internal.notfound", "Unknown Record");
				} else {
				  throw new PluginException(pluginId, "error.plugin", record.getErrorInfo()+" may not be updated!\n\nRecord is only available read-only");	
				}
			}
			if (record.data == null) throw new BadRequestException("error.internal", "Missing data");		
			
			DBRecord rec = result.get(0);
			if (!rec.context.mayUpdateRecord(rec, record)) throw new PluginException(pluginId, "error.plugin", record.getErrorInfo()+" may not be updated!\n\nUpdate permission chain:\n========================\n"+rec.context.getMayUpdateReport(rec, record));
			if (rec.context.mustPseudonymize())  throw new PluginException(pluginId, "error.plugin", "Pseudonymized record may not be updated!\n\nUpdate permission chain:\n========================\n"+rec.context.getMayUpdateReport(rec, record));
			
			String storedVersion = rec.meta.getString("version");
			if (storedVersion == null) storedVersion = VersionedDBRecord.INITIAL_VERSION;
			String providedVersion = record.version != null ? record.version : VersionedDBRecord.INITIAL_VERSION; 
			if (!providedVersion.equals(storedVersion)) throw new BadRequestException("error.concurrent.update", "Concurrent update", Http.Status.CONFLICT);
			
			if (record.format != null && !rec.meta.getString("format").equals(record.format)) throw new PluginException(pluginId, "error.invalid.request", "Tried to change record format during update.");
			if (record.content != null && !rec.meta.getString("content").equals(record.content)) throw new PluginException(pluginId, "error.invalid.request", "Tried to change record content type during update.");
			if (record.owner != null && !rec.owner.equals(record.owner)) throw new PluginException(pluginId, "error.invalid.request", "Tried to change record owner during update! new="+record.owner.toString()+" old="+rec.owner.toString());
						
			QueryTagTools.checkTagsForUpdate(context, record, rec);
						
			List<EncryptedFileHandle> allData2 = new ArrayList<EncryptedFileHandle>(allData.size());
			for (UpdateFileHandleSupport data : allData) {
				if (data != null) {
				   EncryptedFileHandle handle = data.toEncryptedFileHandle(rec);
				   String virus = checkVirusFree(handle);
				   if (virus != null) throw new BadRequestException("error.virus", "A virus has been detected: "+virus);
				   allData2.add(handle);
				}
			}
						
			VersionedDBRecord vrec = null;
			
			if (context.produceHistory()) {
			  vrec = new VersionedDBRecord(rec);		
			  RecordEncryption.encryptRecord(vrec);
			}
			
			int idx = 0;
			for (EncryptedFileHandle data : allData2) {
				if (data != null) {
				rec.meta.append(getFileMetaName(idx), data.getId().toObjectId());
				rec.meta.append(getFileMetaName(idx)+"-key", data.getKey());
				}
				idx++;
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
		    
		    if (record.modifiedBy.equals(rec.owner)) {
		      // use "O" for owner. No entry is same as creator for backwards compatibility
		      rec.meta.put("modifiedBy", "O");
		    } else {
		      rec.meta.put("modifiedBy", record.modifiedBy.toDb());
		    }
		    
			
		    DBRecord clone = rec.clone();
		    
			RecordEncryption.encryptRecord(rec);	
			
			if (vrec!=null) {
				try {
 				    VersionedDBRecord.add(vrec);
				} catch (InternalServerException e) {
					throw new PluginException(pluginId, "error.concurrent.update", "Please check your application so that it does not try concurrent updates on the same resource. Record has id '"+rec._id.toString()+" with record format '"+clone.getFormatOrNull()+"'.");
				}
			}
		    DBRecord.upsert(rec); 	  	
		    
		    RecordLifecycle.notifyOfChange(clone, context.getCache());
		    
		    SubscriptionManager.resourceChange(record);
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
	public void wipe(AccessContext context, Map<String, Object> query) throws AppException {
		AccessLog.logBegin("begin deletingRecords executor=",context.getAccessor().toString());
		
		Set<String> fields = new HashSet<String>();
		fields.add("owner");
		fields.add("stream");
		fields.add("isStream");
		fields.add("consentAps");
		fields.addAll(APSEntry.groupingFields);
		APSCache cache = context.getCache();
		query.put("owner", "self");
		List<DBRecord> recs = QueryEngine.listInternal(cache, context.getAccessor(), context.internal(), query, fields);
		
		wipe(context, recs);		
		//fixAccount(executingPerson);
		
		AccessLog.logEnd("end deleteRecord");
	}

	
	
	protected void wipe(AccessContext context, List<DBRecord> recs) throws AppException {	
		APSCache cache = context.getCache();
		MidataId executingPerson = context.getAccessor();
		if (recs.size() == 0) return;
		
		AccessLog.logBegin("begin wipe #records=", Integer.toString(recs.size()));
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
		VersionedDBRecord.deleteMany(ids);
		DBRecord.deleteMany(ids);
		
		for (MidataId streamId : streams) {
			try {
				cache.getAPS(streamId).removeMeta("_info");
										
				List<DBRecord> testRec = QueryEngine.listInternal(cache, streamId, context, CMaps.map("limit", 1), Sets.create("_id"));
				if (testRec.size() == 0) {
					wipe(context, CMaps.map("_id", streamId).map("streams", "only"));
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
	public void delete(AccessContext context, Map<String, Object> query) throws AppException {
		MidataId executingPerson = context.getAccessor();
		AccessLog.logBegin("begin deletingRecords executor=",executingPerson.toString());
			
		APSCache cache = context.getCache();
		query.put("owner", "self");
		List<DBRecord> recs = QueryEngine.listInternal(cache, executingPerson, context.internal(), query, COMPLETE_META);
		
		delete(cache, executingPerson, recs);				
		
		AccessLog.logEnd("end deleteRecord");
	}
	
	public void deleteFromPublic(AccessContext context, Map<String, Object> query) throws AppException {
		MidataId executingPerson = context.getAccessor();
		AccessLog.logBegin("begin deleteFromPublic executor=",executingPerson.toString());
					
		query.put("public", "only");
		query.put("public-strict", true);
		
		List<DBRecord> recs = QueryEngine.listInternal(context.getCache(), executingPerson, context.internal(), query, COMPLETE_META);
		
		APSCache cache = Feature_PublicData.getPublicAPSCache(context.getCache());
		delete(cache, RuntimeConstants.instance.publicUser, recs);				
		
		AccessLog.logEnd("end deleteFromPublic");
	}
	
	public void wipeFromPublic(AccessContext context, Map<String, Object> query) throws AppException {
		AccessLog.logBegin("begin wipeFromPublic executor=",context.getAccessor().toString());
					
		query.put("public", "only");
		query.put("public-strict", true);
		query.put("deleted", true);
		
		List<DBRecord> recs = QueryEngine.listInternal(context.getCache(), context.getAccessor(), context, query, COMPLETE_META);
		
		//APSCache cache = Feature_PublicData.getPublicAPSCache(getCache(executingPerson));
		wipe(context.forPublic(), recs);				
		
		AccessLog.logEnd("end deleteFromPublic");
	}
	
	private void delete(APSCache cache, MidataId executingPerson, List<DBRecord> recs) throws AppException {
		
		if (recs.size() == 0) return;
		
		AccessLog.logBegin("begin delete #records=",Integer.toString(recs.size()));
		Set<MidataId> streams = new HashSet<MidataId>();
		
		Iterator<DBRecord> it = recs.iterator();
		while (it.hasNext()) {
	   	   DBRecord record = it.next();			
	       if (record.meta.getString("content").equals("Patient")) it.remove();
	       Set<String> tags = RecordConversion.instance.getTags(record);
	       if (tags.contains(QueryTagTools.SECURITY_NODELETE)) it.remove();
	       
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
		    
		    Record rec = RecordConversion.instance.currentVersionFromDB(record);
			RecordEncryption.encryptRecord(record);	
						
			VersionedDBRecord.add(vrec);
		    DBRecord.upsert(record); 	  			    
		    RecordLifecycle.notifyOfChange(clone, cache);	
		    
		    SubscriptionManager.resourceChange(rec);
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
		 		
		AccessLog.logBegin("Begin Add Record execPerson=",context.getCache().getAccountOwner().toString()," format=",String.valueOf(record.meta.get("format"))," stream=",(record.stream != null ? record.stream.toString() : "null"));	
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
	
		
	/**
	 * Share (stream) records from one APS to another by applying a query once.
	 * @param userId id of executing user
	 * @param query to query to be applied
	 * @param sourceaps the source APS id
	 * @param targetaps the target APS id
	 * @param ownerInformation include owner information?
	 * @throws AppException
	 */
	public void applyQuery(AccessContext context, Map<String, Object> query, MidataId sourceaps, Consent target, boolean ownerInformation) throws AppException {
		
		MidataId targetaps = target._id;
		Pair<Map<String, Object>, Map<String, Object>> pair = Feature_Streams.convertToQueryPair(query);
				
		AccessLog.logBegin("BEGIN APPLY QUERY");
		MidataId userId = context.getCache().getAccessor();
		AccessContext targetContext = new ConsentAccessContext(target, context);
		boolean targetIsEmpty = targetContext.getCache().getAPS(targetaps).hasNoDirectEntries();
		if (pair.getRight() != null) {
			AccessLog.logBegin("SINGLE RECORDS");
			List<DBRecord> recs = QueryEngine.listInternal(context.getCache(), sourceaps, context, CMaps.map(pair.getRight()).map("owner", context.getSelf()), RecordManager.SHARING_FIELDS);			
			RecordManager.instance.share(context.getCache(), targetaps, null, recs, ownerInformation);
		}
		if (pair.getLeft() != null) {
			if (!targetIsEmpty) {
				List<DBRecord> recs = QueryEngine.listInternal(context.getCache(), targetaps, targetContext, CMaps.map(pair.getLeft()).map("flat", "true").map("owner", context.getSelf()), Sets.create("_id"));
				Set<MidataId> remove = new HashSet<MidataId>();
				for (DBRecord r : recs) remove.add(r._id);
				AccessLog.log("REMOVE DUPLICATES:", Integer.toString(remove.size()));
				RecordManager.instance.unshare(targetContext, remove);		
			}
			
			Map<String, Object> selectionQuery = CMaps.map(pair.getLeft()).map("streams", "true").map("flat", "true").map("owner", context.getSelf());		
			List<DBRecord> records = QueryEngine.listInternal(context.getCache(), sourceaps, context, selectionQuery, RecordManager.SHARING_FIELDS);
			
			AccessLog.log("SHARE QUALIFIED STREAMS:", Integer.toString(records.size()));
			if (records.size() > 0) {				
				RecordManager.instance.share(context.getCache(), targetaps, null, records, ownerInformation);
			}
			
			if (!targetIsEmpty) {
				List<DBRecord> streams = QueryEngine.listInternal(context.getCache(), targetaps, targetContext, RecordManager.STREAMS_ONLY_OWNER, RecordManager.COMPLETE_META);
				AccessLog.log("UNSHARE STREAMS CANDIDATES = ", Integer.toString(streams.size()));
				
				List<DBRecord> stillOkay = QueryEngine.listFromMemory(context, pair.getLeft(), streams);
				streams.removeAll(stillOkay);		
				Set<MidataId> remove = new HashSet<MidataId>();
				for (DBRecord stream : streams) {
					remove.add(stream._id);
				}
				
				AccessLog.log("UNSHARE STREAMS QUALIFIED = ", Integer.toString(remove.size()));
				RecordManager.instance.unshare(targetContext, remove);
			}
			AccessLog.logEnd("END APPLY RULES");
		}
		
	}
	
	protected void applyQueries(AccessContext context, MidataId userId, DBRecord record, MidataId useAps) throws AppException {
		AccessLog.logBegin("start applying queries for targetUser=",userId.toString());
		if (record.isStream!=null) {
		
			Member member = Member.getById(userId, Sets.create("queries"));
			if (member != null && member.queries!=null) {
				for (String key : member.queries.keySet()) {
					try {
					Map<String, Object> query = member.queries.get(key);
					if (QueryEngine.isInQuery(context, query, record)) {
						try {
						  MidataId targetAps = new MidataId(key);
						  Map<String, Object> original = ConsentQueryTools.getVerifiedSharingQuery(targetAps);
						  if (QueryEngine.isInQuery(context, original, record)) {
							  APS apswrapper = context.getCache().getAPS(targetAps, userId);
							  RecordManager.instance.shareUnchecked(context.getCache(),Collections.singletonList(record), Collections.<DBRecord>emptyList(), apswrapper, true);
						  }
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
						  Map<String, Object> original = ConsentQueryTools.getVerifiedSharingQuery(targetAps);
						  if (QueryEngine.isInQuery(context, original, record)) {							  
							  APS apswrapper = context.getCache().getAPS(targetAps, userId);
							  RecordManager.instance.shareUnchecked(context.getCache(),Collections.singletonList(record), Collections.<DBRecord>emptyList(), apswrapper, true);
						  }
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
	
	/*
	public Set<MidataId> findAllSharingAps(MidataId executorId, Record record1) throws AppException {
		 
		    DBRecord record = RecordConversion.instance.toDB(record1);
		    MidataId recordOwner = record1.owner;
		    AccessContext context = new DummyAccessContext(getCache(executorId), record1.owner);
		    Set<MidataId> result = new HashSet<MidataId>();
		
			Member member = Member.getById(recordOwner, Sets.create("rqueries", "queries"));
			if (member.queries!=null) {
				for (String key : member.queries.keySet()) {
					
					Map<String, Object> query = member.queries.get(key);
					if (QueryEngine.isInQuery(context, query, record)) {
						result.add(new MidataId(key));						
					}
					
				}
			}
									
			if (member.rqueries!=null) {
				for (String key : member.rqueries.keySet()) {
				
					Map<String, Object> query = member.rqueries.get(key);
					if (QueryEngine.isInQuery(context, query, record)) {
						result.add(new MidataId(key));						
					}
					
				}
			}
		
			return result;		
	}
	*/
	
    /**
     * Delete an access permission set
     * @param apsId id of APS
     * @param ownerId id of owner of APS
     * @throws InternalServerException
     */
	public void deleteAPS(AccessContext context, MidataId apsId) throws AppException {
		MidataId executorId = context.getAccessor();
		AccessLog.logBegin("begin deleteAPS aps=",apsId.toString()," executor=",executorId.toString());
		
		APSCache cache = context.getCache();
		APS apswrapper = cache.getAPS(apsId);
		try {
		List<DBRecord> recordEntries = QueryEngine.listInternal(context.getCache(), apsId, context.internal(), CMaps.map("ignore-redirect", true),
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
	
	public List<Record> list(UserRole role, AccessContext context,
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
	public Collection<RecordsInfo> info(UserRole role, MidataId aps, AccessContext context, Map<String, Object> properties, AggregationType aggrType) throws AppException {
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
		    Collection<RecordsInfo> result = QueryEngine.info(aps, context, nproperties, aggrType);
		    
		    if (properties.containsKey("include-records")) {		    	
			    for (RecordsInfo inf : result) {
			    	if (inf.newestRecord != null) {
			    		inf.newestRecordContent = fetch(role, context, inf.newestRecord, null);
			    	}
			    }
		    }
		    
		    return result;
		} catch (APSNotExistingException e) {
			checkRecordsInAPS(context, aps, false, "", new ArrayList<String>());
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
	public Record fetch(AccessContext context, UserRole role, RecordToken token) throws AppException {
		return fetch(context, role, token, RecordManager.COMPLETE_DATA);
	}
	
	public Pair<String, Integer> parseFileId(String fileId) {
		int p = fileId.indexOf("_");
		if (p >= 0) {
			return Pair.of(fileId.substring(0, p), Integer.parseInt(fileId.substring(p+1)));
		}
		return Pair.of(fileId, 0);
	}
	/**
	 * Lookup a single record by providing a RecordToken and return the attachment of the record
	 * @param who id of executing person
	 * @param token token with the id of the record
	 * @return the attachment content
	 * @throws AppException
	 */
	public FileData fetchFile(AccessContext context, RecordToken token, int idx) throws AppException {		
		List<DBRecord> result = QueryEngine.listInternal(context.getCache(), new MidataId(token.apsId), context.forAps(MidataId.from(token.apsId)), CMaps.map("_id", new MidataId(token.recordId)), Sets.create("key", "data"));
				
		if (result.size() != 1) throw new InternalServerException("error.internal.notfound", "Unknown Record");
		DBRecord rec = result.get(0);
		
		if (rec.security == null) throw new InternalServerException("error.internal", "Missing key for record:"+rec._id.toString());
		
		MidataId fileId;
		byte[] key;
		if (rec.meta.containsField("file") || idx>0) {
			fileId = MidataId.from(rec.meta.get(getFileMetaName(idx)));
			key = (byte[]) rec.meta.get(getFileMetaName(idx)+"-key");
		} else {
			fileId = rec._id;
			key = rec.key;
		}
		
		FileData fileData = FileStorage.retrieve(fileId, 0);
		if (fileData == null) throw new InternalServerException("error.internal", "Record "+rec._id.toString()+" has no binary data attached.");		
		
		if (rec.security.equals(APSSecurityLevel.NONE) || rec.security.equals(APSSecurityLevel.LOW)) {
		  // fileData.inputStream = fileData.inputStream;			
		} else {
		  fileData.inputStream = EncryptionUtils.decryptStream(key, fileData.inputStream);
		}
		
		return fileData;
	}
	
	public EncryptedFileHandle reUseAttachment(AccessContext context, MidataId record, int idx) throws AppException {
		List<DBRecord> result = QueryEngine.listInternal(context.getCache(), context.getOwner() , context, CMaps.map("_id", record), Sets.create("key", "meta", "data"));
		if (result.size() != 1) throw new InternalServerException("error.internal.notfound", "Unknown Record");
		DBRecord rec = result.get(0);
		if (rec.meta.containsField("file") || idx>0) {
			MidataId fileId = MidataId.from(rec.meta.get(getFileMetaName(idx)));
			byte[] key = (byte[]) rec.meta.get(getFileMetaName(idx)+"-key");
			return new EncryptedFileHandle(fileId, key, 0);
		} else {
			MidataId fileId = rec._id;
			byte[] key = rec.key;
			return new EncryptedFileHandle(fileId, key, 0);
		}
	}

	/**
	 * Lookup a single record by providing a RecordToken and return only requested fields
	 * @param who id of executing person
	 * @param token token with the id of the record
	 * @param fields set of field names that are required
	 * @return the record to be returned
	 * @throws AppException
	 */
	public Record fetch(AccessContext context, UserRole role, RecordToken token, Set<String> fields)
			throws AppException {
		List<Record> result = list(role, context.forAps(MidataId.from(token.apsId)),
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
	public Record fetch(UserRole role, AccessContext context, MidataId recordId, String format)
			throws AppException {
		List<Record> result = list(role, context, CMaps.map("_id", recordId).mapNotEmpty("format", format),
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
	public Set<String> listRecordIds(UserRole role, AccessContext context)
			throws AppException {
		return listRecordIds(role, context, RecordManager.FULLAPS_LIMITED_SIZE);
	}

	/**
	 * Return a set with all record ids of records matching some criteria accessible from an APS
	 * @param who id of executing person
	 * @param apsId id of APS to search
	 * @param properties key,value map containing query
	 * @return set with record ids as strings
	 * @throws AppException
	 */
	public Set<String> listRecordIds(UserRole role, AccessContext context,
			Map<String, Object> properties) throws AppException {
		List<Record> result = list(role, context, properties,
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
	private int resetInfo(AccessContext context) throws AppException {
		MidataId who = context.getAccessor();
		AccessLog.logBegin("start reset info user=",who.toString());
		int count = 0;
		List<Record> result = list(UserRole.ANY, context, CMaps.map("streams", "only").map("flat", "true"), Sets.create("_id", "owner"));
		for (Record stream : result) {
			try {
			  AccessLog.log("reset stream:", stream._id.toString());
			  Feature_UserGroups.findApsCacheToUse(context.getCache(), stream._id).getAPS(stream._id, stream.owner).removeMeta("_info");
			  count++;
			} catch (APSNotExistingException e) {}
			catch (InternalServerException e2) {
				AccessLog.log("Stream access error: "+stream._id+" ow="+stream.owner.toString());
			}
		}
		AccessLog.logEnd("end reset info user="+who.toString());
		return count;
	}

	/**
	 * removes info objects and all indexes for the current account
	 * @param userId id of user
	 * @throws AppException
	 */
	public List<String> fixAccount(AccessContext context) throws AppException {
		MidataId userId = context.getAccessor();
		List<String> msgs = new ArrayList<String>();
		msgs.add(IndexManager.instance.clearIndexes(context.getCache(), context.getAccessor()));
		
		APSCache cache = context.getCache();
				
		if (context.getOwner().equals(RuntimeConstants.instance.publicUser)) {					
			RecordManager.instance.shareAPS(context, Collections.singleton(RuntimeConstants.publicGroup));	
		}
		
		AccessLog.logBegin("start search for missing records");
		checkRecordsInAPS(context, userId, true, "account:", msgs);		
		AccessLog.logEnd("end search for missing records");
		
		AccessLog.logBegin("start search for broken user groups");
		Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByMember(userId);
		for (UserGroupMember ugm : ugms) {
			try {
			  Feature_UserGroups.loadKey(context, ugm);
			  APSCache sub = Feature_UserGroups.findApsCacheToUse(cache, ugm);
			  msgs.add("ug: "+IndexManager.instance.clearIndexes(sub, ugm.userGroup));
			} catch (Exception e) {
				msgs.add("disabled usergroup "+ugm.userGroup.toString());
				ugm.status = ConsentStatus.EXPIRED;
				UserGroupMember.set(ugm._id, "status", ugm.status);
				context.clearCache();				
			}
		}
		AccessLog.logEnd("end search for broken user groups");
		
		AccessLog.logBegin("start searching for missing records in consents");
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map(), Sets.create("_id"), Integer.MAX_VALUE);
		for (Consent consent : consents) {
			try {
				cache.getAPS(consent._id, userId).getStoredOwner();					
			} catch (Exception e) {
				Consent.delete(userId, consent._id);
				continue;
			}
			checkRecordsInAPS(context, consent._id, true, "owned consent "+consent._id.toString()+": ",msgs);
		}
		AccessLog.logEnd("end searching for missing records in consents");
		
		AccessLog.logBegin("start searching for missing records in authorized consents");		
		consents = Consent.getAllActiveByAuthorized(userId);
		for (Consent consent : consents) {
			checkRecordsInAPS(context, consent._id, false, "consent "+consent._id.toString()+": ",msgs);
		}
		AccessLog.logEnd("end searching for missing records in authorized consents");
						
		AccessLog.logBegin("start searching for missing records in spaces");
		Set<Space> spaces = Space.getAllByOwner(userId, Sets.create("_id"));
		for (Space space : spaces) {			
			checkRecordsInAPS(context, space._id, true, "space "+space._id.toString()+": ", msgs);
		}
		AccessLog.logEnd("end searching for missing records in spaces");
		
		AccessLog.logBegin("start searching for empty streams");
		Set<String> fields = new HashSet<String>();
		fields.add("owner");
		fields.addAll(APSEntry.groupingFields);
		fields.add("consentAps");
		List<DBRecord> streams = QueryEngine.listInternal(cache, userId, context, CMaps.map("owner", "self").map("streams", "only").map("flat", "true"), fields);
		
		List<DBRecord> emptyStreams = new ArrayList<DBRecord>();
		for (DBRecord str : streams) {
			List<DBRecord> testRec = QueryEngine.listInternal(cache, str._id, context, CMaps.map("limit", 1), Sets.create("_id"));
			if (testRec.size() == 0) {
				emptyStreams.add(str);
			}
		}
		if (emptyStreams.size() > 0) {
			wipe(context, emptyStreams);
			msgs.add("Wiped "+emptyStreams.size()+" empty streams");
		}
		
		AccessLog.logEnd("end searching for empty streams");
		
		int statsCleared = resetInfo(context);
		msgs.add("Cleared statistics from "+statsCleared+" streams.");
		
		Feature_Streams.streamJoin(context, msgs);
		
		for (String msg : msgs) AccessLog.log(msg);
		
		return msgs;
	}
	
	public void checkRecordsInAPS(AccessContext context, MidataId apsId, boolean instreams, String prefix, List<String> results) throws AppException {		
		APSCache cache = context.getCache();
		AccessLog.logBegin("check records in APS:",apsId.toString());
		List<DBRecord> recs = QueryEngine.listInternal(cache, apsId, context, CMaps.map("owner", "self").map("streams", "only").map("flat", "true"), Sets.create("_id"));
		Set<String> idOnly = Sets.create("_id");
		for (DBRecord rec : recs) {
			if (DBRecord.getById(rec._id, idOnly) == null) {				
				cache.getAPS(apsId).removePermission(rec);
				results.add(prefix+"removed permission for non existing stream "+rec._id.toString()+" from "+apsId.toString());
			} else {
				try {
				  cache.getAPS(rec._id, rec.owner).getStoredOwner();
				} catch (Exception e) {
				  cache.getAPS(apsId).removePermission(rec);
				  results.add(prefix+"removed permission for unacessable stream "+rec._id.toString()+" from "+apsId.toString());
				}
			}			
		}
		
		recs = QueryEngine.listInternal(cache, apsId, context, CMaps.map("owner", "self"), Sets.create("_id"));		
		for (DBRecord rec : recs) {
			if (DBRecord.getById(rec._id, idOnly) == null) {
				if (instreams && rec.stream != null) cache.getAPS(rec.stream, rec.owner).removePermission(rec);
				cache.getAPS(apsId).removePermission(rec);
				results.add(prefix+"removed permission for non existing record "+rec._id.toString()+" from "+apsId.toString());
			} 			
		}
		AccessLog.logEnd("end check records in APS:"+apsId.toString());
	}

	public void patch20160407(AccessContext context) throws AppException {
		MidataId who = context.getAccessor();
		List<DBRecord> all = QueryEngine.listInternal(context.getCache(), who, context.internal(), CMaps.map("owner", "self"), RecordManager.COMPLETE_META);
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
		wipe(context, toWipe);
		
	}
	
	public AccountStats getStats(AccessContext context) throws AppException {
		MidataId userId = context.getAccessor();
		AccountStats result = new AccountStats();
		result.numConsentsOwner = Consent.count(userId);
		Set<MidataId> auth = new HashSet<MidataId>();
		auth.add(userId);
		for (UserGroupMember ugm : UserGroupMember.getAllActiveByMember(userId)) {
			auth.add(ugm.userGroup);
			result.numUserGroups++;
		}
		result.numConsentsAuth = Consent.countAuth(auth);
		try (DBIterator<DBRecord> it = QueryEngine.listInternalIterator(context.getCache(), userId, context.forAccountReshare(), CMaps.map("streams","only").map("owner","self").map("flat",true), Sets.create("_id"))) {
		  while (it.hasNext()) { it.next();result.numOwnStreams++;result.numOtherStreams--; }
		}
		try (DBIterator<DBRecord> it = QueryEngine.listInternalIterator(context.getCache(), userId, context.forAccountReshare(), CMaps.map("streams","only").map("flat",true), Sets.create("_id"))) {
		  while (it.hasNext()) { it.next();result.numOtherStreams++; }
		}
		
		return result;
	}
	
	public void clearIndexes(AccessContext context, MidataId userId) throws AppException {
		IndexManager.instance.clearIndexes(context.getCache(), context.getAccessor());		
	}
	

	

}
