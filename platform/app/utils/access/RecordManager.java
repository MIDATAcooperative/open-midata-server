package utils.access;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import controllers.Circles;
import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.Consent;
import models.ContentCode;
import models.ContentInfo;
import models.Member;
import models.MidataId;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.enums.APSSecurityLevel;
import models.enums.AggregationType;
import models.enums.ConsentStatus;
import utils.AccessLog;
import utils.auth.KeyManager;
import utils.auth.KeySession;
import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.FileStorage;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

/**
 * Access to records. Manages authorizations using access permission sets.
 *
 */
public class RecordManager {

	public static RecordManager instance = new RecordManager();

	public final static Map<String, Object> FULLAPS = new HashMap<String, Object>();
	public final static Map<String, Object> FULLAPS_WITHSTREAMS = CMaps.map("streams", "true");
	public final static Map<String, Object> FULLAPS_FLAT = CMaps.map("streams", "true").map("flat", "true");
	public final static Map<String, Object> FULLAPS_FLAT_OWNER = CMaps.map("streams", "true").map("flat", "true").map("owner", "self");
	
	public final static Set<String> INTERNALIDONLY = Sets.create("_id");
	public final static Set<String> INTERNALID_AND_WACTHES = Sets.create("_id","watches");
	public final static Set<String> COMPLETE_META = Sets.create("id", "owner",
			"app", "creator", "created", "name", "format",  "content", "code", "description", "isStream", "lastUpdated");
	public final static Set<String> COMPLETE_DATA = Sets.create("id", "owner", "ownerName",
			"app", "creator", "created", "name", "format", "content", "code", "description", "isStream", "lastUpdated",
			"data", "group");
	public final static Set<String> COMPLETE_DATA_WITH_WATCHES = Sets.create("id", "owner",
			"app", "creator", "created", "name", "format",  "content", "code", "description", "isStream", "lastUpdated",
			"data", "group", "watches", "stream");
	//public final static String STREAM_TYPE = "Stream";
	public final static Map<String, Object> STREAMS_ONLY = CMaps.map("streams", "only").map("flat", "true");
	public final static Map<String, Object> STREAMS_ONLY_OWNER = CMaps.map("streams", "only").map("flat", "true").map("owner", "self");	

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
		if (apsCache.get() != null) apsCache.set(null);
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
        AccessLog.logBegin("begin createAnonymizedAPS owner="+owner.toString()+" other="+other.toString()+" id="+proposedId.toString());
		EncryptedAPS eaps = new EncryptedAPS(proposedId, owner, owner, APSSecurityLevel.HIGH, consent);
		EncryptionUtils.addKey(owner, eaps);
		EncryptionUtils.addKey(other, eaps);	
		eaps.getPermissions().put("_history", new BasicBSONList()); // Init with history
		eaps.create();
 
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
	public void shareAPS(MidataId apsId, MidataId executorId,
			Set<MidataId> targetUsers) throws AppException {
		AccessLog.logBegin("begin shareAPS aps="+apsId.toString()+" executor="+executorId.toString()+" #targetUsers="+targetUsers.size());
		getCache(executorId).getAPS(apsId).addAccess(targetUsers);
		AccessLog.logEnd("end shareAPS");
	}
	
	/**
	 * share access permission set content with another entity that has a public key
	 * @param apsId ID of APS
	 * @param executorId ID of executor having permission of APS
	 * @param targetId ID of target entity
	 * @param publickey public key of target entity
	 * @throws AppException
	 */
	public void shareAPS(MidataId apsId, MidataId executorId,
			MidataId targetId, byte[] publickey) throws AppException {
		AccessLog.logBegin("begin shareAPS aps="+apsId.toString()+" executor="+executorId+" target="+targetId.toString());
		getCache(executorId).getAPS(apsId).addAccess(targetId, publickey);
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
			List<DBRecord> to_unshare = QueryEngine.listInternal(getCache(executorId), apsId, CMaps.map("streams", "only"), Sets.create("_id"));
			for (DBRecord rec : to_unshare) unshareAPS(rec._id, executorId, targetUsers);
			getCache(executorId).getAPS(apsId).removeAccess(targetUsers);
		}
		AccessLog.logEnd("end unshareAPSRecursive");
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
        AccessLog.logBegin("begin share: who="+who.toString()+" from="+fromAPS.toString()+" to="+toAPS.toString()+" count="+(records!=null ? records.size() : "?"));
		APS apswrapper = getCache(who).getAPS(toAPS, toAPSOwner);
		List<DBRecord> recordEntries = QueryEngine.listInternal(getCache(who), fromAPS,
				records != null ? CMaps.map("_id", records) : RecordManager.FULLAPS_FLAT,
				Sets.create("_id", "key", "owner", "format", "content", "created", "name", "isStream", "stream"));
		
		List<DBRecord> alreadyContained = QueryEngine.isContainedInAps(getCache(who), toAPS, recordEntries);
		AccessLog.log("to-share: "+recordEntries.size()+" already="+alreadyContained.size());
        if (alreadyContained.size() == recordEntries.size()) {
        	AccessLog.logEnd("end share");
        	return;
        }
        if (alreadyContained.size() == 0) {		
		    apswrapper.addPermission(recordEntries, withOwnerInformation);
		    for (DBRecord rec : recordEntries) {		    	
		    	RecordLifecycle.addWatchingAps(rec, toAPS);
		    }
        } else {
        	Set<MidataId> contained = new HashSet<MidataId>();
        	for (DBRecord rec : alreadyContained) contained.add(rec._id);
        	List<DBRecord> filtered = new ArrayList<DBRecord>(recordEntries.size());
        	for (DBRecord rec : recordEntries) {
        		if (!contained.contains(rec._id)) filtered.add(rec);
        	}
        	apswrapper.addPermission(filtered, withOwnerInformation);
        	for (DBRecord rec : filtered) RecordLifecycle.addWatchingAps(rec, toAPS);
        }
        AccessLog.logEnd("end share");
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
        Query q = new Query(query, Sets.create("_id"), getCache(who), toAPS);
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
        
        List<DBRecord> doubles = QueryEngine.listInternal(getCache(who), toAPS, CMaps.map(query).map("ignore-redirect", "true").map("flat", "true").map("streams", "true"), APSEntry.groupingFields);
        apswrapper.removePermission(doubles);        
	}
	
	/**
	 * Materialize the query results of an APS into the APS and remove the query
	 * @param who executing person
	 * @param targetAPS the APS with a query redirect
	 * @throws AppException
	 */
	public void materialize(MidataId who, MidataId targetAPS) throws AppException {
		APS apswrapper = getCache(who).getAPS(targetAPS);
		if (apswrapper.getMeta("_query") != null) {

			AccessLog.logBegin("start materialize query APS="+targetAPS.toString());
			Set<String> fields = Sets.create("owner");
			fields.addAll(APSEntry.groupingFields);
			List<DBRecord> content = QueryEngine.listInternal(getCache(who), targetAPS, CMaps.map("redirect-only", "true"), fields);
			Set<MidataId> ids = new HashSet<MidataId>();
			for (DBRecord rec : content) ids.add(rec._id);
			
			BasicBSONObject query = apswrapper.getMeta("_query");
			Circles.setQuery(who, who, targetAPS, query);
			apswrapper.removeMeta("_query");
       	    RecordManager.instance.applyQuery(who, query, who, targetAPS, true);
			
       	    RecordManager.instance.share(who, who, targetAPS, ids, true);
             
			
			//Feature_Expiration.setup(apswrapper);
						
			
			AccessLog.logEnd("end materialize query");

		}
		if (apswrapper.getMeta("_filter") != null) {
			AccessLog.logBegin("start materialize consent APS="+targetAPS.toString());
			Set<String> fields = Sets.create("owner");
			fields.addAll(APSEntry.groupingFields);
			List<DBRecord> content = QueryEngine.listInternal(getCache(who), targetAPS, CMaps.map(), fields);
			apswrapper.clearPermissions();
			apswrapper.addPermission(content, true);
			
			Member member = Member.getById(who, Sets.create("queries"));
			
			if (member.queries != null) {			 
			  String key = targetAPS.toString();
		      if (member.queries.containsKey(key)) {
		    	  member.queries.remove(key);
		    	  Member.set(who, "queries", member.queries);
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
		APS apswrapper = getCache(who).getAPS(apsId);
		List<DBRecord> recordEntries = QueryEngine.listInternal(getCache(who), apsId,
				CMaps.map("_id", records), Sets.create("_id", "format", "content", "watches"));		
		apswrapper.removePermission(recordEntries);
		for (DBRecord rec : recordEntries) RecordLifecycle.removeWatchingAps(rec, apsId);
		AccessLog.logEnd("end unshare");
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
		getCache(who).getAPS(apsId).setMeta(key, data);
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

	public void addDocumentRecord(MidataId owner, Record record,
			Collection<Record> parts) throws AppException {
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
		byte[] key = addRecordIntern(owner, dbrecord, false, null, false);
		if (key == null)
			throw new NullPointerException("no key");
		for (Record rec : parts) {
			DBRecord dbrec = RecordConversion.instance.toDB(rec);
			dbrec.document = record._id;
			dbrec.key = key;
			addRecordIntern(owner, dbrec, true, null, false);
		}
	}

	/**
	 * add a new record to the database 
	 * @param executingPerson id of executing person
	 * @param record the record to be saved
	 * @throws AppException
	 */
	public void addRecord(MidataId executingPerson, Record record) throws AppException {
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
		byte[] enckey = addRecordIntern(executingPerson, dbrecord, false, null, false);	
		createAndShareDependend(executingPerson, dbrecord, record.dependencies, enckey);
		
	}
	
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
	public void addRecord(MidataId executingPerson, Record record, MidataId alternateAps, InputStream data, String fileName, String contentType) throws AppException {
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
		byte[] kdata = addRecordIntern(executingPerson, dbrecord, false, alternateAps, false);	
		try {
		FileStorage.store(EncryptionUtils.encryptStream(kdata, data), record._id, fileName, contentType);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal", e);
		}
		createAndShareDependend(executingPerson, dbrecord, record.dependencies, kdata);
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
	public void addRecord(MidataId executingPerson, Record record, MidataId alternateAps) throws AppException {
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
		byte[] kdata = addRecordIntern(executingPerson, dbrecord, false, alternateAps, false);	
		createAndShareDependend(executingPerson, dbrecord, record.dependencies, kdata);
	}
	
	/**
	 * update a record in the database 
	 * @param executingPerson id of executing person
	 * @param record the record to be updated
	 * @throws AppException
	 * @return the new version string of the record
	 */
	public String updateRecord(MidataId executingPerson, MidataId apsId, Record record) throws AppException {
		AccessLog.logBegin("begin updateRecord executor="+executingPerson.toString()+" aps="+apsId.toString()+" record="+record._id.toString());
		try {
			List<DBRecord> result = QueryEngine.listInternal(getCache(executingPerson), apsId, CMaps.map("_id", record._id), RecordManager.COMPLETE_DATA_WITH_WATCHES);	
			if (result.size() != 1) throw new InternalServerException("error.internal.notfound", "Unknown Record");
			if (record.data == null) throw new BadRequestException("error.internal", "Missing data");
			
			DBRecord rec = result.get(0);
			String storedVersion = rec.meta.getString("version");
			if (storedVersion == null) storedVersion = VersionedDBRecord.INITIAL_VERSION;
			String providedVersion = record.version != null ? record.version : VersionedDBRecord.INITIAL_VERSION; 
			if (!providedVersion.equals(storedVersion)) throw new BadRequestException("error.concurrent.update", "Concurrent update", HttpStatus.SC_CONFLICT);
			
			VersionedDBRecord vrec = new VersionedDBRecord(rec);		
			RecordEncryption.encryptRecord(vrec);			
					
			record.lastUpdated = new Date(); 
			
		    rec.data = record.data;
		    rec.meta.put("lastUpdated", record.lastUpdated);
		    rec.time = Query.getTimeFromDate(record.lastUpdated);		
		    
		    String version = Long.toString(System.currentTimeMillis());
		    rec.meta.put("version", version);
			
		    DBRecord clone = rec.clone();
		    
			RecordEncryption.encryptRecord(rec);	
			
			VersionedDBRecord.add(vrec);
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
		fields.addAll(APSEntry.groupingFields);
		APSCache cache = getCache(executingPerson);
		query.put("owner", "self");
		List<DBRecord> recs = QueryEngine.listInternal(cache, executingPerson, query, fields);
		
		wipe(executingPerson, recs);		
		//fixAccount(executingPerson);
		
		AccessLog.logEnd("end deleteRecord");
	}
	
	private void wipe(MidataId executingPerson, List<DBRecord> recs) throws AppException {
		APSCache cache = getCache(executingPerson);
		if (recs.size() == 0) return;
		
		AccessLog.logBegin("begin wipe #records="+recs.size());
		Set<MidataId> streams = new HashSet<MidataId>();
		
		Iterator<DBRecord> it = recs.iterator();
		while (it.hasNext()) {
	   	   DBRecord record = it.next();			
	       if (record.meta.getString("content").equals("Patient")) it.remove();
		   if (record.owner == null) throw new InternalServerException("error.internal", "Owner of record is null.");
		   if (!record.owner.equals(executingPerson)) throw new BadRequestException("error.internal", "Not owner of record!");
		}
		
		IndexManager.instance.removeRecords(cache, executingPerson, recs);
		
		Set<Consent> consents = Consent.getAllByOwner(executingPerson, new HashMap<String, Object>(), Sets.create("_id"));
		
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
			if (record.isStream) {
				AccessPermissionSet.delete(record._id);
			}
		}
		
		for (DBRecord record : recs) { 
		  DBRecord.delete(record.owner, record._id);
		}
		
		for (MidataId streamId : streams) {
			getCache(executingPerson).getAPS(streamId).removeMeta("_info");
									
			List<DBRecord> testRec = QueryEngine.listInternal(cache, streamId, CMaps.map("limit", 1), Sets.create("_id"));
			if (testRec.size() == 0) {
				wipe(executingPerson, CMaps.map("_id", streamId).map("streams", "only"));
			}			
		}
				
		AccessLog.logEnd("end wipe #records="+recs.size());				
	}

	private byte[] addRecordIntern(MidataId executingPerson, DBRecord record, boolean documentPart, MidataId alternateAps, boolean upsert) throws AppException {		
		
		if (!documentPart) Feature_Streams.placeNewRecordInStream(executingPerson, record, alternateAps);
		 		
		AccessLog.logBegin("Begin Add Record execPerson="+executingPerson.toString()+" format="+record.meta.get("format")+" stream="+(record.stream != null ? record.stream.toString() : "null"));	
		byte[] usedKey = null;
		if (record.meta.get("created") == null) throw new InternalServerException("error.internal", "Missing creation date");
		
		record.time = Query.getTimeFromDate((Date) record.meta.get("created"));				
		record = record.clone();
		if (record.owner.equals(record.meta.get("creator"))) record.meta.removeField("creator");
																	
		if (!documentPart) {
			APS apswrapper = getCache(executingPerson).getAPS(record.stream, record.owner);	
			
			if (record.document != null) {
				List<DBRecord> doc = QueryEngine.listInternal(getCache(executingPerson), record.owner, CMaps.map("_id", record.document.toString()), Sets.create("key"));
				if (doc.size() == 1) {
					record.key = doc.get(0).key;
					record.security = doc.get(0).security;
				}
				else throw new InternalServerException("error.internal", "Document not identified");
				documentPart = true;
			} else apswrapper.provideRecordKey(record);
			
			usedKey = record.key;
    								
			if (apswrapper.getSecurityLevel().equals(APSSecurityLevel.HIGH)) record.time = 0;
			
			DBRecord unencrypted = record.clone();
			
			RecordEncryption.encryptRecord(record);		
		    if (upsert) { DBRecord.upsert(record); } else { DBRecord.add(record); }	  
		    
		    if (!unencrypted.direct && !documentPart) apswrapper.addPermission(unencrypted, false);
			else apswrapper.touch();
		    
		    //Feature_Expiration.check(getCache(executingPerson), apswrapper);
			
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
	 * Share stream records from one APS to another by applying a query once.
	 * @param userId id of executing user
	 * @param query to query to be applied
	 * @param sourceaps the source APS id
	 * @param targetaps the target APS id
	 * @param ownerInformation include owner information?
	 * @throws AppException
	 */
	public void applyQuery(MidataId userId, Map<String, Object> query, MidataId sourceaps, MidataId targetaps, boolean ownerInformation) throws AppException {
		AccessLog.logBegin("BEGIN APPLY QUERY");
		
		
		//List<DBRecord> records = QueryEngine.listInternal(getCache(userId), sourceaps, RecordManager.FULLAPS_FLAT_OWNER, RecordManager.COMPLETE_META);
		//AccessLog.debug("SHARE CANDIDATES:"+records.size());
		//records = QueryEngine.listFromMemory(query, records);
		
		Map<String, Object> selectionQuery = CMaps.map(query).map("streams", "true").map("flat", "true").map("owner", "self");		
		List<DBRecord> records = QueryEngine.listInternal(getCache(userId), sourceaps, selectionQuery, RecordManager.COMPLETE_META);
		
		AccessLog.log("SHARE QUALIFIED:"+records.size());
		if (records.size() > 0) {
			Set<MidataId> ids = new HashSet<MidataId>();
			for (DBRecord record : records) ids.add(record._id);
			RecordManager.instance.share(userId, sourceaps, targetaps, ids, ownerInformation);
		}
		
		List<DBRecord> streams = QueryEngine.listInternal(getCache(userId), targetaps, RecordManager.STREAMS_ONLY_OWNER, RecordManager.COMPLETE_META);
		AccessLog.log("UNSHARE STREAMS CANDIDATES = "+streams.size());
		
		List<DBRecord> stillOkay = QueryEngine.listFromMemory(getCache(userId), query, streams);
		streams.removeAll(stillOkay);		
		Set<MidataId> remove = new HashSet<MidataId>();
		for (DBRecord stream : streams) {
			remove.add(stream._id);
		}
		
		AccessLog.log("UNSHARE STREAMS QUALIFIED = "+remove.size());
		RecordManager.instance.unshare(userId, targetaps, remove);
		AccessLog.logEnd("END APPLY RULES");
		
	}
	
	protected void applyQueries(MidataId executingPerson, MidataId userId, DBRecord record, MidataId useAps) throws AppException {
		AccessLog.logBegin("start applying queries");
		Member member = Member.getById(userId, Sets.create("queries"));
		if (member.queries!=null) {
			for (String key : member.queries.keySet()) {
				try {
				Map<String, Object> query = member.queries.get(key);
				if (QueryEngine.isInQuery(getCache(executingPerson), query, record)) {
					try {
					  MidataId targetAps = new MidataId(key);
					  getCache(executingPerson).getAPS(targetAps, userId);
					  RecordManager.instance.share(executingPerson, useAps, targetAps, Collections.singleton(record._id), true);
					} catch (APSNotExistingException e) {
						
					}
				}
				} catch (BadRequestException e) {
					AccessLog.logException("error while sharing", e);
				}  
			}
		}
		AccessLog.logEnd("end applying queries");
	}
	
    /**
     * Delete an access permission set
     * @param apsId id of APS
     * @param ownerId id of owner of APS
     * @throws InternalServerException
     */
	public void deleteAPS(MidataId apsId, MidataId executorId) throws AppException {
		AccessLog.logBegin("begin deleteAPS aps="+apsId.toString()+" executor="+executorId.toString());
		
		APS apswrapper = getCache(executorId).getAPS(apsId);
		try {
		List<DBRecord> recordEntries = QueryEngine.listInternal(getCache(executorId), apsId, new HashMap<String, Object>(),
				Sets.create("_id", "watches"));		
		
			for (DBRecord rec : recordEntries) {
				try {
				  RecordLifecycle.removeWatchingAps(rec, apsId);
				} catch (AppException e) {
				  AccessLog.logException("error while deleting APS during remove watch", e);
				} catch (NullPointerException e2) {
				  AccessLog.logException("error while deleting APS during remove watch", e2);	
				}
			}
		} catch (AppException e) {
			AccessLog.logException("error while deleting APS", e);
		}
		
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
	public List<Record> list(MidataId who, MidataId apsId,
			Map<String, Object> properties, Set<String> fields)
			throws AppException {
		return QueryEngine.list(getCache(who), apsId, properties, fields);
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
	public Collection<RecordsInfo> info(MidataId who, MidataId aps, Map<String, Object> properties, AggregationType aggrType) throws AppException {
		// Only allow specific properties as results are materialized
		Map<String, Object> nproperties = new HashMap<String, Object>();
		nproperties.put("streams", "true");
		nproperties.put("flat", "true");
		nproperties.put("group-system", "v1");
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
		if (properties.containsKey("group")) nproperties.put("group", properties.get("group"));
		
		try {
		    return QueryEngine.info(getCache(who), aps, nproperties, aggrType);
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
	public Record fetch(MidataId who, RecordToken token) throws AppException {
		return fetch(who, token, RecordManager.COMPLETE_DATA);
	}
	
	/**
	 * Lookup a single record by providing a RecordToken and return the attachment of the record
	 * @param who id of executing person
	 * @param token token with the id of the record
	 * @return the attachment content
	 * @throws AppException
	 */
	public FileData fetchFile(MidataId who, RecordToken token) throws AppException {		
		List<DBRecord> result = QueryEngine.listInternal(getCache(who), new MidataId(token.apsId), CMaps.map("_id", new MidataId(token.recordId)), Sets.create("key"));
				
		if (result.size() != 1) throw new InternalServerException("error.internal.notfound", "Unknown Record");
		DBRecord rec = result.get(0);
		
		if (rec.security == null) throw new InternalServerException("error.internal", "Missing key for record:"+rec._id.toString());
		FileData fileData = FileStorage.retrieve(new MidataId(token.recordId));
		if (fileData == null) throw new InternalServerException("error.internal", "Record "+rec._id.toString()+" has no binary data attached.");		
		
		if (rec.security.equals(APSSecurityLevel.NONE) || rec.security.equals(APSSecurityLevel.LOW)) {
		  fileData.inputStream = fileData.inputStream;			
		} else {
		  fileData.inputStream = EncryptionUtils.decryptStream(rec.key, fileData.inputStream);
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
	public Record fetch(MidataId who, RecordToken token, Set<String> fields)
			throws AppException {
		List<Record> result = list(who, new MidataId(token.apsId),
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
	public Record fetch(MidataId who, MidataId aps, MidataId recordId)
			throws AppException {
		List<Record> result = list(who, aps, CMaps.map("_id", recordId),
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
	public Set<String> listRecordIds(MidataId who, MidataId apsId)
			throws AppException {
		return listRecordIds(who, apsId, RecordManager.FULLAPS);
	}

	/**
	 * Return a set with all record ids of records matching some criteria accessible from an APS
	 * @param who id of executing person
	 * @param apsId id of APS to search
	 * @param properties key,value map containing query
	 * @return set with record ids as strings
	 * @throws AppException
	 */
	public Set<String> listRecordIds(MidataId who, MidataId apsId,
			Map<String, Object> properties) throws AppException {
		List<Record> result = list(who, apsId, properties,
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
		List<Record> result = list(who, who, RecordManager.STREAMS_ONLY_OWNER, Sets.create("_id", "owner"));
		for (Record stream : result) {
			try {
			  AccessLog.log("reset stream:"+stream._id.toString());
			  getCache(who).getAPS(stream._id, stream.owner).removeMeta("_info");
			} catch (APSNotExistingException e) {}
		}
		AccessLog.logEnd("end reset info user="+who.toString());
	}

	/**
	 * removes info objects and all indexes for the current account
	 * @param userId id of user
	 * @throws AppException
	 */
	public void fixAccount(MidataId userId) throws AppException {
		
		resetInfo(userId);		
		IndexManager.instance.clearIndexes(getCache(userId), userId);
		
		APSCache cache = getCache(userId);
				
		
		AccessLog.logBegin("start search for missing records");
		checkRecordsInAPS(userId, userId, true);		
		AccessLog.logEnd("end search for missing records");
		
		AccessLog.logBegin("start searching for missing records in consents");
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map(), Sets.create("_id"));
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
		List<DBRecord> streams = QueryEngine.listInternal(cache, userId, CMaps.map("owner", "self").map("streams", "only").map("flat", "true"), fields);
		List<DBRecord> emptyStreams = new ArrayList<DBRecord>();
		for (DBRecord str : streams) {
			List<DBRecord> testRec = QueryEngine.listInternal(cache, str._id, CMaps.map("limit", 1), Sets.create("_id"));
			if (testRec.size() == 0) {
				emptyStreams.add(str);
			}
		}
		if (emptyStreams.size() > 0) {
			wipe(userId, emptyStreams);
		}
		
		AccessLog.logEnd("end searching for empty streams");
	}
	
	public void checkRecordsInAPS(MidataId userId, MidataId apsId, boolean instreams) throws AppException {
		APSCache cache = getCache(userId);
		AccessLog.logBegin("check records in APS:"+apsId.toString());
		List<DBRecord> recs = QueryEngine.listInternal(cache, apsId, CMaps.map("owner", "self").map("streams", "only").map("flat", "true"), Sets.create("_id"));
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
		
		recs = QueryEngine.listInternal(cache, apsId, CMaps.map("owner", "self"), Sets.create("_id"));		
		for (DBRecord rec : recs) {
			if (DBRecord.getById(rec._id, idOnly) == null) {
				if (instreams && rec.stream != null) cache.getAPS(rec.stream, userId).removePermission(rec);
				cache.getAPS(apsId).removePermission(rec);
			} 			
		}
		AccessLog.logEnd("end check records in APS:"+apsId.toString());
	}

	public void patch20160407(MidataId who) throws AppException {
		List<DBRecord> all = QueryEngine.listInternal(getCache(who), who, CMaps.map("owner", "self"), RecordManager.COMPLETE_META);
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
	

}
