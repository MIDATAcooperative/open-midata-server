package utils.access;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.Circle;
import models.ContentInfo;
import models.Member;
import models.MemberKey;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.StudyParticipation;
import models.enums.APSSecurityLevel;
import models.enums.AggregationType;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.DateTimeUtils;
import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.FileStorage;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.search.Search;
import utils.search.SearchException;

import com.mongodb.BasicDBObject;

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
	public final static Set<String> COMPLETE_META = Sets.create("id", "owner",
			"app", "creator", "created", "name", "format", "content", "description", "isStream");
	public final static Set<String> COMPLETE_DATA = Sets.create("id", "owner",
			"app", "creator", "created", "name", "format", "content", "description", "isStream",
			"data", "group");
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
	protected APSCache getCache(ObjectId who) throws InternalServerException {
		if (apsCache.get() == null)
			apsCache.set(new APSCache(who));
		APSCache result = apsCache.get();
		if (!result.getOwner().equals(who)) throw new InternalServerException("error.internal", "Owner Change!");
		return result;
	}
	
	/**
	 * clears APS cache. Is automatically called after each request.
	 */
	public void clear() {
		if (apsCache.get() != null) apsCache.set(null);
	}

	/**
	 * create a new access permission where only the owner has access
	 * @param who ID of APS owner
	 * @param proposedId ID of APS to be created
	 * @return ID of APS
	 * @throws AppException
	 */
	public ObjectId createPrivateAPS(ObjectId who, ObjectId proposedId)
			throws AppException {
		
		EncryptedAPS eaps = new EncryptedAPS(proposedId, who, who, APSSecurityLevel.HIGH);
		EncryptionUtils.addKey(who, eaps);		
		eaps.create();

		return eaps.getId();
	}

	/**
	 * creates an access permission set where the owner and one other person have access
	 * @param owner ID of APS owner
	 * @param other ID of other person
	 * @param proposedId ID of APS to be created
	 * @return ID of APS
	 * @throws AppException
	 */
	public ObjectId createAnonymizedAPS(ObjectId owner, ObjectId other,
			ObjectId proposedId) throws AppException {

		EncryptedAPS eaps = new EncryptedAPS(proposedId, owner, owner, APSSecurityLevel.HIGH);
		EncryptionUtils.addKey(owner, eaps);
		EncryptionUtils.addKey(other, eaps);		
		eaps.create();

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
	public ObjectId createAPSForRecord(ObjectId executingPerson, ObjectId owner, ObjectId recordId,
			byte[] key, boolean direct) throws AppException {
		
		EncryptedAPS eaps = new EncryptedAPS(recordId, executingPerson, owner,
				direct ? APSSecurityLevel.MEDIUM : APSSecurityLevel.HIGH, key);
        EncryptionUtils.addKey(owner, eaps);
        if (!executingPerson.equals(owner)) EncryptionUtils.addKey(executingPerson, eaps);
		eaps.create();

		return eaps.getId();
	}

	/**
	 * share access permission set content with other users
	 * @param apsId ID of APS
	 * @param ownerId ID of owner of APS
	 * @param targetUsers IDs of user to share APS with
	 * @throws AppException
	 */
	public void shareAPS(ObjectId apsId, ObjectId ownerId,
			Set<ObjectId> targetUsers) throws AppException {
		getCache(ownerId).getAPS(apsId).addAccess(targetUsers);		
	}
	
	/**
	 * share access permission set content with another entity that has a public key
	 * @param apsId ID of APS
	 * @param ownerId ID of owner of APS
	 * @param targetId ID of target entity
	 * @param publickey public key of target entity
	 * @throws AppException
	 */
	public void shareAPS(ObjectId apsId, ObjectId ownerId,
			ObjectId targetId, byte[] publickey) throws AppException {
		getCache(ownerId).getAPS(apsId).addAccess(targetId, publickey);		
	}

	/**
	 * remove access permissions of given users from an APS
	 * @param apsId ID of APS
	 * @param ownerId ID of APS owner
	 * @param targetUsers set of IDs of target users
	 * @throws InternalServerException
	 */
	public void unshareAPS(ObjectId apsId, ObjectId ownerId,
			Set<ObjectId> targetUsers) throws InternalServerException {
		getCache(ownerId).getAPS(apsId).removeAccess(targetUsers);
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
	public void share(ObjectId who, ObjectId fromAPS, ObjectId toAPS,
			Set<ObjectId> records, boolean withOwnerInformation)
			throws AppException {
        AccessLog.debug("share: who="+who.toString()+" from="+fromAPS.toString()+" to="+toAPS.toString()+" count="+(records!=null ? records.size() : "?"));
		APS apswrapper = getCache(who).getAPS(toAPS);
		List<Record> recordEntries = QueryEngine.listInternal(getCache(who), fromAPS,
				records != null ? CMaps.map("_id", records) : RecordManager.FULLAPS_FLAT,
				Sets.create("_id", "key", "owner", "format", "content", "created", "name", "isStream"));
		
		List<Record> alreadyContained = QueryEngine.isContainedInAps(getCache(who), toAPS, recordEntries);
		AccessLog.debug("to-share: "+recordEntries.size()+" already="+alreadyContained.size());
        if (alreadyContained.size() == recordEntries.size()) return;
        if (alreadyContained.size() == 0) {		
		    apswrapper.addPermission(recordEntries, withOwnerInformation);
        } else {
        	Set<ObjectId> contained = new HashSet<ObjectId>();
        	for (Record rec : alreadyContained) contained.add(rec._id);
        	List<Record> filtered = new ArrayList<Record>(recordEntries.size());
        	for (Record rec : recordEntries) {
        		if (!contained.contains(rec._id)) filtered.add(rec);
        	}
        	apswrapper.addPermission(filtered, withOwnerInformation);
        }
				
	}

	public void shareByQuery(ObjectId who, ObjectId fromAPS, ObjectId toAPS,
			Map<String, Object> query) throws AppException {
        AccessLog.debug("shareByQuery who="+who.toString()+" from="+fromAPS.toString()+" to="+toAPS.toString());
		if (toAPS.equals(who)) throw new BadRequestException("error.internal", "Bad call to shareByQuery. target APS may not be user APS!");
        APS apswrapper = getCache(who).getAPS(toAPS);

        query.remove("aps");
        if (query.isEmpty()) {
           apswrapper.removeMeta("_query");
        } else {
		   query.put("aps", fromAPS.toString());
		   apswrapper.setMeta("_query", query);
        }
	}

	public void unshare(ObjectId who, ObjectId apsId, Set<ObjectId> records)
			throws AppException {

		APS apswrapper = getCache(who).getAPS(apsId);
		List<Record> recordEntries = QueryEngine.list(getCache(who), apsId,
				CMaps.map("_id", records), Sets.create("_id", "format", "content"));
		for (Record r : recordEntries) {
			AccessLog.debug("remove perm cnt="+r.content+" fmt="+r.format);
		}
		apswrapper.removePermission(recordEntries);
			
	}

	
	
	public BSONObject getMeta(ObjectId who, ObjectId apsId, String key) throws AppException {
		return getCache(who).getAPS(apsId).getMeta(key);
	}
	
	public void setMeta(ObjectId who, ObjectId apsId, String key, Map<String,Object> data) throws AppException {
		getCache(who).getAPS(apsId).setMeta(key, data);
	}

	public void addDocumentRecord(ObjectId owner, Record record,
			Collection<Record> parts) throws AppException {
		byte[] key = addRecordIntern(owner, record, false, null, false);
		if (key == null)
			throw new NullPointerException("no key");
		for (Record rec : parts) {
			rec.document = record._id;
			rec.key = key;
			addRecordIntern(owner, rec, true, null, false);
		}
	}

	public void addRecord(ObjectId executingPerson, Record record) throws AppException {
		addRecordIntern(executingPerson, record, false, null, false);
		record.key = null;
	}
	
	public void addRecord(ObjectId executingPerson, Record record, FileInputStream data, String fileName, String contentType) throws DatabaseException, AppException {
		byte[] kdata = addRecordIntern(executingPerson, record, false, null, false);
		SecretKey key = new SecretKeySpec(kdata, EncryptedAPS.KEY_ALGORITHM);
		FileStorage.store(EncryptionUtils.encryptStream(key, data), record._id, fileName, contentType);
		record.key = null;
	}
	
	public void addRecord(ObjectId executingPerson, Record record, ObjectId alternateAps) throws AppException {
		addRecordIntern(executingPerson, record, false, alternateAps, false);
		record.key = null;
	}
	
	/*public void addRecord(ObjectId executingPerson, Record record, ObjectId alternateAps) throws InternalServerException {
		addRecordIntern(executingPerson, record, false, alternateAps, false);
		record.key = null;
	}*/
	
	public void deleteRecord(ObjectId executingPerson, RecordToken tk) throws AppException {
		Record record = fetch(executingPerson, tk);
		
		if (!record.owner.equals(executingPerson)) throw new BadRequestException("error.internal", "Not owner of record!");
		APSCache cache = getCache(executingPerson);
		Set<Circle> circles = Circle.getAllByOwner(executingPerson);
		
		for (Circle c : circles) {
			cache.getAPS(c._id).removePermission(record);
		}
		
		Set<StudyParticipation> parts = StudyParticipation.getAllByMember(executingPerson, Sets.create("_id"));
		for (StudyParticipation part : parts) {
			cache.getAPS(part._id, executingPerson).removePermission(record);
		}
		
		Set<MemberKey> mkeys = MemberKey.getByOwner(executingPerson);
		for (MemberKey mk : mkeys) {
			cache.getAPS(mk._id, executingPerson).removePermission(record);
		}
		
		Set<Space> spaces = Space.getAllByOwner(executingPerson, Sets.create("_id"));
		for (Space s : spaces) {
			cache.getAPS(s._id, executingPerson).removePermission(record);
		}
		
		if (record.stream != null) {
			cache.getAPS(record.stream, executingPerson).removePermission(record);
		}
		
		cache.getAPS(executingPerson, executingPerson).removePermission(record);
		
		Record.delete(record.owner, record._id);
	}

	private byte[] addRecordIntern(ObjectId executingPerson, Record record, boolean documentPart, ObjectId alternateAps, boolean upsert) throws AppException {		
		
		if (!documentPart) Feature_Streams.placeNewRecordInStream(executingPerson, record, alternateAps);
		 		
		AccessLog.debug("Add Record execPerson="+executingPerson.toString()+" format="+record.format+" stream="+(record.stream != null ? record.stream.toString() : "null"));	
		byte[] usedKey = null;
		if (record.created == null) throw new InternalServerException("error.internal", "Missing creation date");
		
		record.time = Query.getTimeFromDate(record.created);				
		record = record.clone();
		if (record.owner.equals(record.creator)) record.creator = null;
																	
		if (!documentPart) {
			APS apswrapper = getCache(executingPerson).getAPS(record.stream, record.owner);	
			
			if (record.document != null) {
				List<Record> doc = QueryEngine.listInternal(getCache(executingPerson), record.owner, CMaps.map("_id", record.document.toString()), Sets.create("key"));
				if (doc.size() == 1) record.key = doc.get(0).key;
				else throw new InternalServerException("error.internal", "Document not identified");
				documentPart = true;
			} else apswrapper.provideRecordKey(record);
			
			usedKey = record.key;
    		
			if (!record.direct && !documentPart) apswrapper.addPermission(record, false);
			else apswrapper.touch();
			
			if (apswrapper.getSecurityLevel().equals(APSSecurityLevel.HIGH)) record.time = 0;
			
		} else {
			record.time = 0;
			usedKey = record.key;
		}
								
		RecordEncryption.encryptRecord(record, record.key != null ? APSSecurityLevel.HIGH : APSSecurityLevel.NONE);		
	    if (upsert) { Record.upsert(record); } else { Record.add(record); }	  
	    				
		return usedKey;	
	}
	
	public void applyQuery(ObjectId userId, Map<String, Object> query, ObjectId sourceaps, ObjectId targetaps, boolean ownerInformation) throws AppException {
		AccessLog.debug("BEGIN APPLY QUERY");
		
		
		List<Record> records = RecordManager.instance.list(userId, sourceaps, RecordManager.FULLAPS_FLAT_OWNER, RecordManager.COMPLETE_META);
		AccessLog.debug("SHARE CANDIDATES:"+records.size());
		records = QueryEngine.listFromMemory(query, records);
		AccessLog.debug("SHARE QUALIFIED:"+records.size());
		if (records.size() > 0) {
			Set<ObjectId> ids = new HashSet<ObjectId>();
			for (Record record : records) ids.add(record._id);
			RecordManager.instance.share(userId, sourceaps, targetaps, ids, ownerInformation);
		}
		
		List<Record> streams = RecordManager.instance.list(userId, targetaps, RecordManager.STREAMS_ONLY_OWNER, RecordManager.COMPLETE_META);
		AccessLog.debug("UNSHARE STREAMS CANDIDATES = "+streams.size());
		
		List<Record> stillOkay = QueryEngine.listFromMemory(query, streams);
		streams.removeAll(stillOkay);		
		Set<ObjectId> remove = new HashSet<ObjectId>();
		for (Record stream : streams) {
			remove.add(stream._id);
		}
		
		AccessLog.debug("UNSHARE STREAMS QUALIFIED = "+remove.size());
		RecordManager.instance.unshare(userId, targetaps, remove);
		AccessLog.debug("END APPLY RULES");
		
	}
	
	public void applyQueries(ObjectId executingPerson, ObjectId userId, Record record, ObjectId useAps) throws AppException {
		Member member = Member.getById(userId, Sets.create("queries"));
		if (member.queries!=null) {
			for (String key : member.queries.keySet()) {
				Map<String, Object> query = member.queries.get(key);
				if (QueryEngine.isInQuery(query, record)) {
					try {
					  RecordManager.instance.share(executingPerson, useAps, new ObjectId(key), Collections.singleton(record._id), true);
					} catch (APSNotExistingException e) {
						
					}
				}
			}
		}
	}
	

	public void deleteAPS(ObjectId apsId, ObjectId ownerId)
			throws InternalServerException {		
		AccessPermissionSet.delete(apsId);
	}

	public List<Record> list(ObjectId who, ObjectId apsId,
			Map<String, Object> properties, Set<String> fields)
			throws AppException {
		return QueryEngine.list(getCache(who), apsId, properties, fields);
	}
	
	public Collection<RecordsInfo> info(ObjectId who, ObjectId aps, Map<String, Object> properties, AggregationType aggrType) throws AppException {
		// Only allow specific properties as results are materialized
		Map<String, Object> nproperties = new HashMap<String, Object>();
		nproperties.put("streams", "true");
		nproperties.put("flat", "true");
		if (properties.containsKey("owner")) nproperties.put("owner", properties.get("owner"));
		if (properties.containsKey("study")) nproperties.put("study", properties.get("study"));
		if (properties.containsKey("format")) nproperties.put("format", properties.get("format"));
		if (properties.containsKey("content")) nproperties.put("content", properties.get("content"));
		if (properties.containsKey("group")) nproperties.put("group", properties.get("group"));
		return QueryEngine.info(getCache(who), aps, nproperties, aggrType);
	}

	public Record fetch(ObjectId who, RecordToken token) throws AppException {
		return fetch(who, token, RecordManager.COMPLETE_DATA);
	}
	
	public FileData fetchFile(ObjectId who, RecordToken token) throws AppException {		
		List<Record> result = QueryEngine.listInternal(getCache(who), new ObjectId(token.apsId), CMaps.map("_id", new ObjectId(token.recordId)), Sets.create("key"));
				
		if (result.size() != 1) throw new InternalServerException("error.internal.notfound", "Unknown Record");
		Record rec = result.get(0);
		
		if (rec.key == null) throw new InternalServerException("error.internal", "Missing key for record:"+rec._id.toString());
		FileData fileData = FileStorage.retrieve(new ObjectId(token.recordId));
				
		
		fileData.inputStream = EncryptionUtils.decryptStream(new SecretKeySpec(rec.key, EncryptedAPS.KEY_ALGORITHM), fileData.inputStream);
		
		return fileData;
	}

	public Record fetch(ObjectId who, RecordToken token, Set<String> fields)
			throws AppException {
		List<Record> result = list(who, new ObjectId(token.apsId),
				CMaps.map("_id", new ObjectId(token.recordId)), fields);
		if (result.isEmpty())
			return null;
		else
			return result.get(0);
	}

	public Record fetch(ObjectId who, ObjectId aps, ObjectId recordId)
			throws AppException {
		List<Record> result = list(who, aps, CMaps.map("_id", recordId),
				RecordManager.COMPLETE_DATA);
		if (result.isEmpty())
			return null;
		else
			return result.get(0);
	}

	public Set<String> listRecordIds(ObjectId who, ObjectId apsId)
			throws AppException {
		return listRecordIds(who, apsId, RecordManager.FULLAPS);
	}

	public Set<String> listRecordIds(ObjectId who, ObjectId apsId,
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
	public void resetInfo(ObjectId who) throws AppException {
		List<Record> result = list(who, who, RecordManager.STREAMS_ONLY_OWNER, Sets.create("_id", "owner"));
		for (Record stream : result) {
			AccessLog.debug("reset stream:"+stream._id.toString());
			getCache(who).getAPS(stream._id, stream.owner).removeMeta("_info");
		}
	}
	
	/*
	public void changeFormatName(ObjectId owner, String oldName, String newContent, String newFormat) throws AppException {
		List<Record> records = QueryEngine.listInternal(getCache(owner), owner, CMaps.map("format", oldName).map("owner", "self"), RecordManager.COMPLETE_DATA);
		List<Record> patchedRecords = new ArrayList<Record>();
		for (Record record : records) {
			Record patched = record.clone();
			patched.content = newContent;
			patched.format = newFormat;
			patched.stream = null;
			patched.key = null;
			patched.clearSecrets();
			patchedRecords.add(patched);
		}
		patch(owner, records, patchedRecords);		
	}
	
	public void patch(ObjectId owner, List<Record> old, List<Record> patched) throws AppException {
		Iterator<Record> oldIt = old.iterator();
		for (Record newRecord : patched) {
			Record oldRecord = oldIt.next();
			if (!newRecord._id.equals(oldRecord._id)) throw new InternalServerException("error.internal", "Patch error: Bad record _ids");
			if (!oldRecord.owner.equals(owner)) throw new InternalServerException("error.internal", "Can only patch own records.");
			ObjectId sourceAPS =  oldRecord.stream != null ? oldRecord.stream : owner;
			getCache(owner).getAPS(sourceAPS, owner).removePermission(oldRecord);
			addRecordIntern(owner, newRecord, false, null, true);			
		}
	}
	*/

}
