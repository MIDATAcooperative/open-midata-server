package utils.access;

import java.util.ArrayList;
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

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.contrib.pattern.ClusterSingletonManager;
import akka.contrib.pattern.ClusterSingletonProxy;
import controllers.AutoRun.ImportManager;
import models.Consent;
import models.MidataId;
import play.libs.Akka;
import utils.AccessLog;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexKey;
import utils.access.index.IndexMatch;
import utils.access.index.IndexRemoveMsg;
import utils.access.index.IndexRoot;
import utils.access.index.IndexUpdateMsg;
import utils.access.op.Condition;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.stats.Stats;
import utils.sync.Instances;

/**
 * Manages indexes on encrypted data records. Allows creation of new indexes,
 * query for available indexes
 * 
 */
public class IndexManager {

	public static IndexManager instance = new IndexManager();
			
	private static long UPDATE_TIME = 1000 * 10;
	private static long UPDATE_UNUSED = 1000 * 60 * 60 * 24;
	
	private ActorRef indexSupervisor;
	private ActorRef indexSupervisorSingleton;
				
	
	public IndexManager() {		
		indexSupervisorSingleton = Instances.system().actorOf(ClusterSingletonManager.defaultProps(Props.create(IndexSupervisor.class), "indexSupervisor-instance",
			    null, null), "indexSupervisor-singleton");
		
		indexSupervisor = Instances.system().actorOf(ClusterSingletonProxy.defaultProps("user/indexSupervisor-singleton/indexSupervisor-instance", null), "indexSupervisor");			
	}

	public IndexPseudonym getIndexPseudonym(APSCache cache, MidataId user, MidataId targetAPS, boolean create) throws AppException {		

		APS aps = cache.getAPS(targetAPS);
		BSONObject obj = aps.getMeta("_pseudo");
		
		if (obj == null && !create && !user.equals(targetAPS)) return null;
				
		if (obj == null) { 
		obj = new BasicBSONObject();
		obj.put("name", UUID.randomUUID());
		aps.setMeta("_pseudo", obj.toMap());			
		}
		
		return new IndexPseudonym(obj.get("name").toString(), ((APSImplementation) aps).eaps.getAPSKey());
	}

	/**
	 * creates a new index
	 * 
	 * @param user
	 *            id of user of the new index
	 * @param format
	 *            format of records stored in the index
	 * @param fields
	 */
	public IndexDefinition createIndex(IndexPseudonym pseudo, Set<String> formats, List<String> fields) throws AppException {

		IndexDefinition indexDef = new IndexDefinition();

		indexDef._id = new MidataId();
		indexDef.fields = fields;
		indexDef.formats = new ArrayList<String>(formats);
		indexDef.owner = pseudo.getPseudonym();
		indexDef.lockTime = System.currentTimeMillis();
								
		IndexRoot root = new IndexRoot(pseudo.getKey(), indexDef, true);
		
		root.prepareToCreate();
		IndexDefinition.add(indexDef);
			
		indexDef.lockTime = 0;
		return indexDef;
	}
			
	protected void addRecords(IndexRoot index, MidataId aps, List<DBRecord> records) throws AppException, LostUpdateException {
		
		int max = records.size();
		int cur = 0;
		int CHUNK_SIZE = 100;
		while (cur < max) {
			List<DBRecord> part = records.subList(cur, cur+CHUNK_SIZE > max ? max : cur+CHUNK_SIZE);
			QueryEngine.loadData(part);
			for (DBRecord record : part) {
				
				if (record.data != null) index.addEntry(aps != null ? aps : record.consentAps, record);				
				// Remove from memory
				record.data = null;
				record.encryptedData = null;
				record.encrypted = null;
				record.meta = null;	
			}
			cur+=CHUNK_SIZE;
		}			
	}
	
	/*protected void scanStreamNewEntries(APSCache cache, IndexRoot index, MidataId executor, MidataId stream, Date updatedAfter) throws AppException {
		Map<String, Object> restrictions = new HashMap<String, Object>();
		if (updatedAfter != null) restrictions.put("updated-after", updatedAfter);
		Collection<DBRecord> recs = QueryEngine.listInternal(cache, stream, restrictions, Sets.create("_id"));
		addRecords(index, recs);
    }*/
	protected void indexRemove(APSCache cache, IndexRoot index, MidataId executor, List<DBRecord> records) throws AppException {
		AccessLog.logBegin("start remove entries from index");
		try {
			
			int max = records.size();
			int cur = 0;
			int CHUNK_SIZE = 100;
			while (cur < max) {
				List<DBRecord> part = records.subList(cur, cur+CHUNK_SIZE > max ? max : cur+CHUNK_SIZE);
				QueryEngine.loadData(part);
				for (DBRecord record : part) {
					try {
					   index.removeEntry(record);
					} catch (InternalServerException e) {
					  // We ignore error during index remove as this might be part of a delete operation
					  AccessLog.logException("Error during index entry remove", e);
					} catch (NullPointerException e2) {
					  AccessLog.logException("Error during index entry remove", e2);
					}
					// Remove from memory
					record.data = null;
					record.encryptedData = null;						
				}
				cur+=CHUNK_SIZE;
			}		
												
			index.flush();
		} catch (LostUpdateException e) {
			index.reload();
			indexRemove(cache, index, executor, records);
		}
		AccessLog.logEnd("end remove entries from index");
		
	}
	
	public void indexUpdate(APSCache cache, IndexRoot index, MidataId executor, Set<MidataId> targetAps) throws AppException {
						
		AccessLog.logBegin("start index update");
		long startUpdate = System.currentTimeMillis();
		try {
			index.checkLock();
			
			long updateAllTs = 0;
		    if (targetAps == null) {
		    	updateAllTs = System.currentTimeMillis() - 2000;
		    	long limit = index.getAllVersion();
		    	Set<Consent> consents = Consent.getAllActiveByAuthorized(executor, limit);	
		    	cache.prefetch(consents, null);
				targetAps = new HashSet<MidataId>();
				targetAps.add(executor);
				for (Consent consent : consents) targetAps.add(consent._id);				
		    }
		    
		    AccessLog.log("number of aps to update = "+targetAps.size());
			int modCount = 0;
			for (MidataId aps : targetAps) {
				if (index.getModCount() > 5000) index.flush();
				
				Map<String, Object> restrictions = new HashMap<String, Object>();
				restrictions.put("format", index.getFormats());				
				if (aps.equals(executor)) restrictions.put("owner", "self");
				
			    AccessLog.log("Checking aps:"+aps.toString());
				// Records that have been updated or created
			    long v = index.getVersion(aps);
			    //AccessLog.log("v="+v);
				Date limit = v>0 ? new Date(v - UPDATE_TIME) : null;
				long now = System.currentTimeMillis();
				 
				if (limit != null) restrictions.put("updated-after", limit);								
				List<DBRecord> recs = QueryEngine.listInternal(cache, aps, null, restrictions, Sets.create("_id"));
				addRecords(index, aps, recs);
				boolean updateTs = recs.size() > 0 || limit == null || (now-v) > UPDATE_UNUSED;
				// Records that have been freshly shared
				if (limit != null) {
					restrictions.remove("updated-after");
					restrictions.put("shared-after", limit);
					List<DBRecord> recs2 = QueryEngine.listInternal(cache, aps, null, restrictions, Sets.create("_id", "key"));
					addRecords(index, aps, recs2);
					if (recs2.size()>0) updateTs = true;
					AccessLog.log("Add index from sharing="+recs2.size());
				}
				
				if (updateTs) index.setVersion(aps, now);
				AccessLog.log("Add index: from updated="+recs.size());
				
				modCount += index.getModCount();
				
				
			}
			
			AccessLog.log("updateAllTs="+updateAllTs+" modCount="+modCount+" ts="+targetAps.size());
			if (updateAllTs != 0 && (modCount>0 || targetAps.size() > 3)) index.setAllVersion(updateAllTs);
			index.flush();
		} catch (LostUpdateException e) {
			try {
			  Stats.reportConflict();
			  Thread.sleep(50);
			} catch (InterruptedException e2) {}
			index.reload(); //XXXX
			indexUpdate(cache, index, executor, targetAps);
		}
		AccessLog.logEnd("end index update time= "+(System.currentTimeMillis() - startUpdate)+" ms");
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
	
	public Collection<IndexMatch> queryIndex(IndexRoot root, Condition[] values, MidataId targetAps) throws AppException {						
		Collection<IndexMatch> matches = root.lookup(values, targetAps);
		if (matches == null) matches = new ArrayList<IndexMatch>();
		return matches;		
	}
	
	public void triggerUpdate(IndexPseudonym pseudo, APSCache cache, MidataId user, IndexDefinition idx, Set<MidataId> targetAps) throws AppException {			
		indexSupervisor.tell(new IndexUpdateMsg(idx._id, user, pseudo, KeyManager.instance.currentHandle(user), targetAps), null);		
	}
	
	public IndexRoot getIndexRoot(IndexPseudonym pseudo, IndexDefinition idx) throws AppException {
		return new IndexRoot(pseudo.getKey(), idx, false);							
	}
	
	public IndexDefinition findIndex(IndexPseudonym pseudo, Set<String> format, List<String> pathes) throws AppException {		
		Set<IndexDefinition> res = IndexDefinition.getAll(CMaps.map("owner", pseudo.getPseudonym()).map("formats", CMaps.map("$all", format)).map("fields", CMaps.map("$all", pathes)), IndexDefinition.ALL);
		if (res.size() >= 1) return res.iterator().next();
		return null;
	}
	
	public IndexDefinition findIndex(IndexPseudonym pseudo, MidataId id) throws AppException {		
		Set<IndexDefinition> res = IndexDefinition.getAll(CMaps.map("owner", pseudo.getPseudonym()).map("_id", id), IndexDefinition.ALL);
		if (res.size() >= 1) return res.iterator().next();
		return null;
	}
	
	public Collection<IndexDefinition> findIndexes(IndexPseudonym pseudo, String format) throws AppException {		
		Set<IndexDefinition> res = IndexDefinition.getAll(CMaps.map("owner", pseudo.getPseudonym()).map("formats", format), IndexDefinition.ALL);       
		return res;
	}
	
	public void clearIndexes(APSCache cache, MidataId user) throws AppException {
		AccessLog.logBegin("start clear indexes");
		IndexPseudonym pseudo = getIndexPseudonym(cache, user, user, false);
		
		Set<IndexDefinition> defs = IndexDefinition.getAll(CMaps.map("owner", pseudo.getPseudonym()), Sets.create("_id"));
		
		for (IndexDefinition def : defs) {
			IndexDefinition.delete(def._id);
		}
				
		cache.getAPS(user).removeMeta("_pseudo");
		AccessLog.logEnd("end clear indexes");
	}

	public void revalidate(List<DBRecord> validatedResult, MidataId executor,  IndexPseudonym pseudo, IndexRoot root, Object indexQuery, Condition[] cond) throws AppException {		
		if (validatedResult.size() == 0) return;
							
		List<DBRecord> stillValid;
		List<DBRecord> notValid = new ArrayList<DBRecord>();
		stillValid = QueryEngine.filterByDataQuery(validatedResult, indexQuery, notValid);
				
		AccessLog.log("Index found records:"+validatedResult.size()+" still valid:"+stillValid.size());
		if (validatedResult.size() > stillValid.size()) {
			AccessLog.log("Removing "+notValid.size()+" records from index.");
			// You must remove the record IDS from the match not using the records data!!
			List<IndexMatch> matches = new ArrayList<IndexMatch>(notValid.size());
			for (DBRecord r : notValid) matches.add(new IndexMatch(r._id, r.consentAps));
			
			indexSupervisor.tell(new IndexRemoveMsg(root.getModel()._id, executor, pseudo, KeyManager.instance.currentHandle(executor), matches, cond), null);			
		}				 
	}
	
	/*
	private void removeRecords(IndexRoot root, Condition[] cond, Set<IndexMatch> ids) throws InternalServerException {
		try {
		  root.removeRecords(cond, ids);
		  root.flush();
		} catch (LostUpdateException e) {
			root.reload();
			removeRecords(root, cond, ids);
		}
	}*/
	public void removeRecords(APSCache cache, MidataId user, List<IndexMatch> records, MidataId indexId, Condition[] cond) throws AppException {
		IndexPseudonym pseudo = getIndexPseudonym(cache, user, user, false);
		if (pseudo == null) return;
		AccessLog.logBegin("start removing outdated entries from indexes #recs="+records.size());
	
		IndexDefinition def = IndexDefinition.getById(indexId);		
		IndexRoot root = new IndexRoot(pseudo.getKey(), def, false);
		
		boolean again = true;
		
		while (again) {
			again = false;
			try {				
				root.removeOutdated(records, cond);
				root.flush();		
			} catch (LostUpdateException e) {
				root.reload();
				again = true;
		    }
		}
		
		AccessLog.logEnd("end removing outdated entries from indexes");
	}
	
	public void removeRecords(APSCache cache, MidataId user, List<DBRecord> records) throws AppException {
		IndexPseudonym pseudo = getIndexPseudonym(cache, user, user, false);
		if (pseudo == null) return;
		AccessLog.logBegin("start removing records from indexes #recs="+records.size());
		Map<String, List<DBRecord>> hashMap = new HashMap<String, List<DBRecord>>();
		for (DBRecord rec : records) {			
		   String format = rec.meta.getString("format");
		   if (!hashMap.containsKey(format)) {
		       List<DBRecord> list = new ArrayList<DBRecord>();
		       list.add(rec);

		       hashMap.put(format, list);
		   } else {
		       hashMap.get(format).add(rec);
		   }
		}
		for (Map.Entry<String, List<DBRecord>> entry : hashMap.entrySet()) {
			for (IndexDefinition def : findIndexes(pseudo, entry.getKey())) {
				IndexRoot root = new IndexRoot(pseudo.getKey(), def, false);
				indexRemove(cache, root, user, entry.getValue());				
			}
		}
		AccessLog.logEnd("end removing records from indexes");
	}
			
	
}
