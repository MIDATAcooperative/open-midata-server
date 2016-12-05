package utils.access;

import java.util.HashSet;
import java.util.Set;

import com.mongodb.BasicDBList;

import models.APSNotExistingException;
import models.Consent;
import models.MidataId;
import utils.AccessLog;
import utils.collections.Sets;
import utils.exceptions.AppException;


/**
 * Functions supporting the record lifecycle
 *
 */
public class RecordLifecycle {
	
	/**
	 * Adds an APS to the watch list of a record
	 * @param rec record where watch should be added
	 * @param aps id of aps that wants to watch for changes
	 * @throws AppException
	 */
	public static void addWatchingAps(DBRecord rec, MidataId aps) throws AppException {
		DBRecord record = DBRecord.getById(rec._id, Sets.create("encWatches"));
		record.key = rec.key;
		record.security = rec.security;
		record.meta = null;
		RecordEncryption.decryptRecord(record);
		if (record.watches == null) record.watches = new BasicDBList();
		if (!record.watches.contains(aps.toString())) {
			record.watches.add(aps.toString());
			RecordEncryption.encryptRecord(record);
			DBRecord.set(record._id, "encWatches", record.encWatches);
		}
	}
	
	/**
	 * Removes an APS from the watch list of a record
	 * @param rec record where watch should be removed
	 * @param aps id of aps that no longer wants to watch for changes
	 * @throws AppException
	 */
	public static void removeWatchingAps(DBRecord rec, MidataId aps) throws AppException  {
		DBRecord record = DBRecord.getById(rec._id, Sets.create("encWatches"));
		record.key = rec.key;
		record.security = rec.security;
		record.meta = null;
		if (record.encWatches == null) return;
		RecordEncryption.decryptRecord(record);
		if (record.watches.contains(aps.toString())) {
			record.watches.remove(aps.toString());
			RecordEncryption.encryptRecord(record);
			DBRecord.set(record._id, "encWatches", record.encWatches);
		}
	}
	
	/**
	 * Touch all watching APS of a record
	 * @param rec record that has been changed
	 * @param cache APS cache for request
	 * @throws AppException
	 */
	public static void notifyOfChange(DBRecord rec, APSCache cache) throws AppException {
		if (rec.stream != null) cache.getAPS(rec.stream).touch();
		if (rec.watches == null) {
			AccessLog.log("no watches registered for notify of change");
			return;	
		}
		AccessLog.logBegin("start notify of change");
		Set<MidataId> ids = new HashSet<MidataId>();
		for (Object watch : rec.watches) ids.add(MidataId.from(watch));
		Consent.updateTimestamp(ids, System.currentTimeMillis(), System.currentTimeMillis() + 1000l * 60l * 60l);
		AccessLog.logEnd("end notify of change");
	}
		
		

}
