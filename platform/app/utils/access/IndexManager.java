package utils.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.AccessLog;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexMatch;
import utils.access.index.IndexRoot;
import utils.access.op.Condition;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * Manages indexes on encrypted data records. Allows creation of new indexes,
 * query for available indexes
 * 
 */
public class IndexManager {

	public static IndexManager instance = new IndexManager();
	
	private static long UPDATE_TIME = 1000 * 10;

	public String getIndexPseudonym(ObjectId user) throws AppException {
		BSONObject obj = RecordManager.instance.getMeta(user, user, "_pseudo");

		if (obj == null) {
			obj = new BasicBSONObject();
			obj.put("name", UUID.randomUUID());
			RecordManager.instance.setMeta(user, user, "_pseudo", obj.toMap());
		}
		return obj.get("name").toString();
	}

	/**
	 * creates a new index
	 * 
	 * @param user
	 *            id of user of the new index
	 * @param format
	 *            format of records stored in the index
	 * @param selfOnly
	 * @param fields
	 */
	public IndexDefinition createIndex(APSCache cache, ObjectId user, Set<String> formats, boolean selfOnly, List<String> fields) throws AppException {

		IndexDefinition indexDef = new IndexDefinition();

		indexDef._id = new ObjectId();
		indexDef.fields = fields;
		indexDef.formats = new ArrayList<String>(formats);
		indexDef.selfOnly = selfOnly;
		indexDef.owner = getIndexPseudonym(user);
		
		IndexDefinition.add(indexDef);
					
		IndexRoot root = new IndexRoot(getIndexKey(cache, user), indexDef, true);
		
		indexAll(cache, root, user);
		
		return indexDef;
	}
	
	private byte[] getIndexKey(APSCache cache, ObjectId user) throws AppException {
		APS aps = cache.getAPS(user, user);
		return ((APSImplementation) aps).eaps.getAPSKey();
	}
	
	protected void addRecords(IndexRoot index, ObjectId aps, Collection<DBRecord> records) throws AppException {
		for (DBRecord record : records) {
			QueryEngine.loadData(record);
			index.addEntry(aps != null ? aps : record.consentAps, record);
		}
	}
	
	/*protected void scanStreamNewEntries(APSCache cache, IndexRoot index, ObjectId executor, ObjectId stream, Date updatedAfter) throws AppException {
		Map<String, Object> restrictions = new HashMap<String, Object>();
		if (updatedAfter != null) restrictions.put("updated-after", updatedAfter);
		Collection<DBRecord> recs = QueryEngine.listInternal(cache, stream, restrictions, Sets.create("_id"));
		addRecords(index, recs);
    }*/
	
	
	protected void indexAll(APSCache cache, IndexRoot index, ObjectId executor) throws AppException {
		try {
			/*Map<String, Object> restrictions = new HashMap<String, Object>();
			restrictions.put("format", index.getFormats());
			if (index.restrictedToSelf()) restrictions.put("owner", "self");
			Collection<DBRecord> recs = QueryEngine.listInternal(cache, executor, restrictions, Sets.create("_id", "consentAps"));
			addRecords(index, recs);*/
			index.flush();
		} catch (LostUpdateException e) {
			index.reload();
			indexAll(cache, index, executor);
		}
	}
	
	protected void indexUpdate(APSCache cache, IndexRoot index, ObjectId executor, Set<ObjectId> targetAps) throws AppException {
		AccessLog.logBegin("start index update");
		try {
			
			
			for (ObjectId aps : targetAps) {
				Map<String, Object> restrictions = new HashMap<String, Object>();
				restrictions.put("format", index.getFormats());
				if (aps.equals(executor)) restrictions.put("owner", "self");
				
			    AccessLog.log("Checking aps:"+aps.toString());
				// Records that have been updated or created
				Date limit = new Date(index.getVersion(aps) - UPDATE_TIME);
				long now = System.currentTimeMillis();
				 
				restrictions.put("updated-after", limit);								
				Collection<DBRecord> recs = QueryEngine.listInternal(cache, aps, restrictions, Sets.create("_id"));
				addRecords(index, aps, recs);
				boolean updateTs = recs.size() > 0;
				// Records that have been freshly shared
				if (limit.getTime() > 0) {
					restrictions.remove("updated-after");
					restrictions.put("shared-after", limit);
					Collection<DBRecord> recs2 = QueryEngine.listInternal(cache, aps, restrictions, Sets.create("_id", "key"));
					addRecords(index, aps, recs2);
					if (recs2.size()>0) updateTs = true;
					AccessLog.log("Add index from sharing="+recs2.size());
				}
				
				if (updateTs) index.setVersion(aps, now);
				AccessLog.log("Add index: from updated="+recs.size());
				
			}
			
			index.flush();
		} catch (LostUpdateException e) {
			index.reload();
			indexUpdate(cache, index, executor, targetAps);
		}
		AccessLog.logEnd("end index update");
	}

	/**
	 * update index: search all aps(consents) if version newer than index root
	 * for each match: consent not included in index yet -> add consent to index
	 * [add stream and records to index] search for streams created after index
	 * root -> add stream to index standard query for records changed after
	 * index root -> [update index]
	 * 
	 * search all included streams if version is newer than index root for each
	 * match: query for records created after index root -> add query for
	 * records updated/deleted after index root -> update/delete
	 * 
	 * update index root version
	 */

	/**
	 * lazy deletion : records are not removed from the index unless they are
	 * looked up and no key can be provided
	 */

	/**
	 * 
	 * @param idx
	 * @param values
	 * @return
	 */
	public Collection<IndexMatch> queryIndex(IndexRoot root, Condition[] values) throws AppException {						
		Collection<IndexMatch> matches = root.lookup(values);
		if (matches == null) matches = new ArrayList<IndexMatch>();
		return matches;		
	}
	
	public IndexRoot getIndexRootAndUpdate(APSCache cache, ObjectId user, IndexDefinition idx, Set<ObjectId> targetAps) throws AppException {
		IndexRoot root = new IndexRoot(getIndexKey(cache, user), idx, false);		
		indexUpdate(cache, root, user, targetAps);		
		return root;
	}
	
	public IndexDefinition findIndex(APSCache cache, ObjectId user, Set<String> format, List<String> pathes) throws AppException {
		String owner = getIndexPseudonym(user);
		Set<IndexDefinition> res = IndexDefinition.getAll(CMaps.map("owner", owner).map("formats", CMaps.map("$all", format)).map("fields", CMaps.map("$all", pathes)), IndexDefinition.ALL);
		if (res.size() == 1) return res.iterator().next();
		return null;
	}
	
	public void clearIndexes(APSCache cache, ObjectId user) throws AppException {
		AccessLog.logBegin("start clear indexes");
		String pseudo = getIndexPseudonym(user);
		
		Set<IndexDefinition> defs = IndexDefinition.getAll(CMaps.map("owner", pseudo), Sets.create("_id"));
		
		for (IndexDefinition def : defs) {
			IndexDefinition.delete(def._id);
		}
				
		cache.getAPS(user).removeMeta("_pseudo");
		AccessLog.logEnd("end clear indexes");
	}

	public void revalidate(List<DBRecord> validatedResult, IndexRoot root, Map<String, Object> indexQuery, Condition[] cond) throws AppException {		
		if (validatedResult.size() == 0) return;
					
		for (DBRecord r : validatedResult) QueryEngine.loadData(r);
		List<DBRecord> stillValid;
		if (validatedResult.size()> 0) stillValid = QueryEngine.filterByDataQuery(validatedResult, indexQuery);
		else stillValid = validatedResult;
		
		AccessLog.log("Index found records:"+validatedResult.size()+" still valid:"+stillValid.size());
		if (validatedResult.size() > stillValid.size()) {			
			Set<String> ids = new HashSet<String>();
			for (DBRecord rec : validatedResult) ids.add(rec._id.toString());
			for (DBRecord rec : stillValid) ids.remove(rec._id.toString());
			AccessLog.log("Removing "+ids.size()+" records from index.");
			removeRecords(root, cond, ids);			
		}				 
	}
	
	private void removeRecords(IndexRoot root, Condition[] cond, Set<String> ids) throws InternalServerException {
		try {
		  root.removeRecords(cond, ids);
		  root.flush();
		} catch (LostUpdateException e) {
			root.reload();
			removeRecords(root, cond, ids);
		}
	}
			
	
}
