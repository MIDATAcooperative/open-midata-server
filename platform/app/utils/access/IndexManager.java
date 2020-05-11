package utils.access;

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
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import models.Consent;
import models.MidataId;
import models.StudyParticipation;
import models.enums.ConsentStatus;
import utils.AccessLog;
import utils.access.index.BaseIndexRoot;
import utils.access.index.ConsentToKeyIndexRoot;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexMatch;
import utils.access.index.IndexRemoveMsg;
import utils.access.index.IndexRoot;
import utils.access.index.IndexUpdateMsg;
import utils.access.index.Lookup;
import utils.access.index.StatsIndexKey;
import utils.access.index.StatsIndexRoot;
import utils.access.index.StatsLookup;
import utils.access.index.StreamIndexRoot;
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
		final ClusterSingletonManagerSettings settings =
				  ClusterSingletonManagerSettings.create(Instances.system());
	
		
		indexSupervisorSingleton = Instances.system().actorOf(ClusterSingletonManager.props(Props.create(IndexSupervisor.class), PoisonPill.getInstance(),
				settings), "indexSupervisor");
		
		final ClusterSingletonProxySettings proxySettings =
			    ClusterSingletonProxySettings.create(Instances.system());
		
		indexSupervisor = Instances.system().actorOf(ClusterSingletonProxy.props("/user/indexSupervisor", proxySettings), "indexSupervisor-Consumer");			
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
	
	public StreamIndexRoot getStreamIndex(APSCache cache, MidataId user) throws AppException {
		IndexPseudonym pseudo = getIndexPseudonym(cache, user, user, true);
		APS aps = cache.getAPS(user);
		BSONObject obj = aps.getMeta("_streamindex");
		
		MidataId id = null;
							
		if (obj == null) {
			id = new MidataId();
		    obj = new BasicBSONObject();
		    obj.put("index", id.toString());
		    aps.setMeta("_streamindex", obj.toMap());			
		} else {
			id = MidataId.from(obj.get("index"));
		}
		
		IndexDefinition def = IndexDefinition.getById(id);
		if (def==null) {
			def = new IndexDefinition();
			def._id = id;			
			def.owner = pseudo.getPseudonym();
			def.formats = Collections.singletonList("_streamIndex");
			def.lockTime = System.currentTimeMillis();
			def.rev = 1;
			def.creation = currentCreationTime();
									
			StreamIndexRoot root = new StreamIndexRoot(pseudo.getKey(), def, true);
			
			root.prepareToCreate();
			try {
			  IndexDefinition.add(def);
			} catch (Exception e) {
			  def = IndexDefinition.getById(id);
			  if (def==null) throw new NullPointerException();
			}
			def.lockTime = 0;
			triggerUpdate(pseudo, cache, user, def, null);
			return root;
		} else {
			return new StreamIndexRoot(pseudo.getKey(), def,false);
		}
	}
	
	public ConsentToKeyIndexRoot getConsentToKey(APSCache cache, MidataId user, MidataId id) throws AppException {
		IndexPseudonym pseudo = getIndexPseudonym(cache, user, user, true);
		IndexDefinition def = IndexDefinition.getById(id);
		if (def==null) {
			def = new IndexDefinition();
			def._id = id;			
			def.owner = pseudo.getPseudonym();
			def.formats = Collections.singletonList("_consentKey");
			def.lockTime = System.currentTimeMillis();
			def.rev = 1;
			def.creation = currentCreationTime();	
			
			ConsentToKeyIndexRoot root = new ConsentToKeyIndexRoot(pseudo.getKey(), def, true);
			
			root.prepareToCreate();
			try {
			  IndexDefinition.add(def);
			} catch (Exception e) {
			  def = IndexDefinition.getById(id);
			  if (def==null) throw new NullPointerException();
			}
			def.lockTime = 0;
			//triggerUpdate(pseudo, cache, user, def, null);
			return root;
		} else {
			return new ConsentToKeyIndexRoot(pseudo.getKey(), def,false);
		}
	}
	
	public StatsIndexRoot getStatsIndex(APSCache cache, MidataId user, boolean create) throws AppException {
		IndexPseudonym pseudo = getIndexPseudonym(cache, user, user, true);
		APS aps = cache.getAPS(user);
		BSONObject obj = aps.getMeta("_statsindex");
		
		MidataId id = null;
							
		if (obj == null) {
			if (!create) return null;
			id = new MidataId();
		    obj = new BasicBSONObject();
		    obj.put("index", id.toString());
		    aps.setMeta("_statsindex", obj.toMap());			
		} else {
			id = MidataId.from(obj.get("index"));
		}
		
		IndexDefinition def = IndexDefinition.getById(id);
		if (def==null) {
			if (!create) return null;
			def = new IndexDefinition();
			def._id = id;			
			def.owner = pseudo.getPseudonym();
			def.formats = Collections.singletonList("_statsIndex");
			def.lockTime = System.currentTimeMillis();
			def.rev = 1;
			def.creation = currentCreationTime();	
			
			StatsIndexRoot root = new StatsIndexRoot(pseudo.getKey(), def, true);
			
			root.prepareToCreate();
			try {
			  IndexDefinition.add(def);
			} catch (Exception e) {
			  def = IndexDefinition.getById(id);
			  if (def==null) throw new NullPointerException();
			}
			def.lockTime = 0;
			//triggerUpdate(pseudo, cache, user, def, null);
			return root;
		} else {
			return new StatsIndexRoot(pseudo.getKey(), def,false);
		}
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
		indexDef.rev = 1;
		indexDef.creation = currentCreationTime();
								
		IndexRoot root = new IndexRoot(pseudo.getKey(), indexDef, true);
		
		root.prepareToCreate();
		IndexDefinition.add(indexDef);
			
		indexDef.lockTime = 0;
		return indexDef;
	}
	
	protected long currentCreationTime() {
		// Creation time with about month precision
		return System.currentTimeMillis() / 1000l / 60l / 60l / 24l / 30l;
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

	
	public void indexUpdate(APSCache cache, BaseIndexRoot index, MidataId executor, Set<MidataId> targetAps) throws AppException {
		if (index instanceof IndexRoot) {
			indexUpdate(cache, (IndexRoot) index, executor, targetAps);
		} else if (index instanceof StreamIndexRoot) {
			indexUpdate(cache, (StreamIndexRoot) index, executor);
		} else if (index instanceof StatsIndexRoot) {
			indexUpdate(cache, (StatsIndexRoot) index, executor);
		}
	}
	
	public void indexUpdate(APSCache cache, IndexRoot index, MidataId executor, Set<MidataId> targetAps) throws AppException {
						
		AccessLog.logBegin("start index update");
		long startUpdate = System.currentTimeMillis();
		try {
			index.checkLock();
			
			long updateAllTs = 0;
			int modCount = 0;
		    if (targetAps == null) {
		    	updateAllTs = System.currentTimeMillis() - 2000;
		    	long limit = index.getAllVersion();
		    	indexUpdatePart(index, executor, executor, cache);
		    	Set<Consent> consents = Consent.getAllActiveByAuthorized(executor, limit);	
		    	
		    	DBIterator<Consent> consentIt = new Feature_AccountQuery.BlockwiseConsentPrefetch(cache, consents.iterator(), 200);
		    	while (consentIt.hasNext()) {
		    		indexUpdatePart(index, executor, consentIt.next()._id, cache);
		    		modCount += index.getModCount();
		    	}		    			    				
		    } else {		    
			    AccessLog.log("number of aps to update = "+targetAps.size());				
				for (MidataId aps : targetAps) {				
					indexUpdatePart(index,executor,aps,cache);				
					modCount += index.getModCount();								
				}
		    }
			
			AccessLog.log("updateAllTs="+updateAllTs+" modCount="+modCount+" ts="+(targetAps!=null?targetAps.size():"all"));
			if (updateAllTs != 0 && (modCount>0 || targetAps==null || targetAps.size() > 3)) index.setAllVersion(updateAllTs);
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
	
	private void indexUpdatePart(IndexRoot index, MidataId executor, MidataId aps, APSCache cache) throws AppException, LostUpdateException {
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
		 
		if (limit != null) restrictions.put("shared-after", limit);								
		List<DBRecord> recs = QueryEngine.listInternal(cache, aps, null, restrictions, Sets.create("_id"));
		addRecords(index, aps, recs);
		boolean updateTs = recs.size() > 0 || limit == null || (now-v) > UPDATE_UNUSED;
		// Records that have been freshly shared				
		
		if (updateTs) index.setVersion(aps, now);
		AccessLog.log("Add index: from updated="+recs.size());
		
	}
	
	public void indexUpdate(APSCache cache, StreamIndexRoot index, MidataId executor) throws AppException {
		
		AccessLog.logBegin("start index update");
		long startUpdate = System.currentTimeMillis();
		try {
			index.checkLock();
						    
	    	long updateAllTs = System.currentTimeMillis() - 2000;	    	
	    	Set<Consent> consents = Consent.getAllActiveByAuthorized(executor, index.getAllVersion());	
	    	cache.prefetch(consents, null);
	    	Set<MidataId> targetAps = new HashSet<MidataId>();
			targetAps.add(executor);
			for (Consent consent : consents) targetAps.add(consent._id);				
	    
		    
		    AccessLog.log("number of aps to update = "+targetAps.size());
			int modCount = 0;
			for (MidataId aps : targetAps) {
				if (index.getModCount() > 5000) index.flush();
				
				Map<String, Object> restrictions = new HashMap<String, Object>();
				restrictions.put("streams", "true");
				restrictions.put("flat", "true");
				if (aps.equals(executor)) restrictions.put("owner", "self");
				
			    AccessLog.log("Checking aps:"+aps.toString());
				// Records that have been updated or created
			    long v = index.getVersion(aps);
			    //AccessLog.log("v="+v);
				Date limit = v>0 ? new Date(v - UPDATE_TIME) : null;
				long now = System.currentTimeMillis();
				 
				if (limit != null) restrictions.put("shared-after", limit);								
				List<DBRecord> recs = QueryEngine.listInternal(cache, aps, null, restrictions, Sets.create("_id","format","content","app","owner"));
				for (DBRecord r : recs) {
					index.addEntry(aps, r);
				}
				boolean updateTs = recs.size() > 0 || limit == null || (now-v) > UPDATE_UNUSED;
				// Records that have been freshly shared				
				
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
			indexUpdate(cache, index, executor);
		}
		AccessLog.logEnd("end index update time= "+(System.currentTimeMillis() - startUpdate)+" ms");
	}
	
public void indexUpdate(APSCache cache, StatsIndexRoot index, MidataId executor) throws AppException {
		
		AccessLog.logBegin("start index update");
		long startUpdate = System.currentTimeMillis();
		try {
			index.checkLock();
						    
	    	long updateAllTs = System.currentTimeMillis() - 2000;	    
	    	
	    	Feature nextWithProcessing = new Feature_ProcessFilters(new Feature_FormatGroups(new Feature_AccountQuery(new Feature_ConsentRestrictions(new Feature_Consents(new Feature_Streams())))));
	    	
	    	long limit = index.getAllVersion();
	    	int modCount = 0;
	    	List<Consent> consents = new ArrayList<Consent>(StudyParticipation.getAllAuthorizedWithGroup(executor, limit));	
	    	
	    	DBIterator<Consent> consentIt = new Feature_AccountQuery.BlockwiseConsentPrefetch(cache, consents.iterator(), 200);
	    	while (consentIt.hasNext()) {
	    			    		
                if (index.getModCount() > 5000) index.flush();
				StudyParticipation consent = (StudyParticipation) consentIt.next();
				MidataId aps = consent._id;
				StatsLookup lookup = new StatsLookup();
				lookup.setAps(aps);
				
				Collection<StatsIndexKey> old = index.lookup(lookup);
				for (StatsIndexKey k : old) index.removeEntry(k);
				
				if (consent.status == ConsentStatus.ACTIVE || consent.status == ConsentStatus.FROZEN) {
					Map<String, Object> restrictions = new HashMap<String, Object>();				
					restrictions.put("no-postfilter-steams", "true");
					restrictions.put("group-system", "v1");
					//if (aps.equals(executor)) restrictions.put("owner", "self");
									
					Query q = new Query(restrictions, Sets.create("app","content","format","owner","ownerName","stream","group"), cache, executor, new DummyAccessContext(cache), false);
					
					Collection<StatsIndexKey> keys = Feature_Stats.countConsent(q, nextWithProcessing, Feature_Indexes.getContextForAps(q, aps));
					for (StatsIndexKey k : keys) {
						k.studyGroup = consent.group;
						index.addEntry(k);
					}
				}
	    			    			    			    		
	    		modCount += index.getModCount();
	    			    			    		
	    	}	
	    		    		    		    			    	
            if (index.getModCount() > 5000) index.flush();
			
			MidataId aps = executor;
			StatsLookup lookup = new StatsLookup();
			lookup.setAps(aps);
			
			Collection<StatsIndexKey> old = index.lookup(lookup);
			for (StatsIndexKey k : old) index.removeEntry(k);
			
			
			Map<String, Object> restrictions = new HashMap<String, Object>();				
			restrictions.put("no-postfilter-steams", "true");
			restrictions.put("group-system", "v1");
			//if (aps.equals(executor)) restrictions.put("owner", "self");
								
			Query q = new Query(restrictions, Sets.create("app","content","format","owner","ownerName","stream","group"), cache, executor, new DummyAccessContext(cache), false);
				
			Collection<StatsIndexKey> keys = Feature_Stats.countConsent(q, nextWithProcessing, Feature_Indexes.getContextForAps(q, aps));
			for (StatsIndexKey k : keys) {
				k.studyGroup = null;
				index.addEntry(k);					
			}
    			    			    			    		
    		modCount += index.getModCount();
    			    			    		    		    		    			
			if (modCount>0) index.setAllVersion(updateAllTs);
			index.flush();
		} catch (LostUpdateException e) {
			try {
			  Stats.reportConflict();
			  Thread.sleep(50);
			} catch (InterruptedException e2) {}
			index.reload(); //XXXX
			indexUpdate(cache, index, executor);
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
		Collection<IndexMatch> matches = root.lookup(new Lookup(values));
		if (matches == null) matches = new ArrayList<IndexMatch>();
		return matches;		
	}
	
	public Collection<IndexMatch> queryIndex(IndexRoot root, Condition[] values, MidataId targetAps) throws AppException {						
		Collection<IndexMatch> matches = root.lookup(new Lookup(values), targetAps);
		if (matches == null) matches = new ArrayList<IndexMatch>();
		return matches;		
	}
	
	public void triggerUpdate(IndexPseudonym pseudo, APSCache cache, MidataId user, IndexDefinition idx, Set<MidataId> targetAps) throws AppException {
		if (targetAps != null && targetAps.size() > 10) targetAps = null;
		AccessLog.log("TRIGGER UPDATE "+user+" aps="+(targetAps!=null?targetAps.toString():"null"));		
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
				
		cache.getAPS(user).removeMeta("_statsindex");
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
	public void removeRecords(APSCache cache, MidataId user, List<IndexMatch> records, MidataId indexId, Condition[] cond, IndexPseudonym pseudo) throws AppException {		
		AccessLog.logBegin("start removing outdated entries from indexes #recs="+records.size());
	
		IndexDefinition def = IndexDefinition.getById(indexId);		
		IndexRoot root = new IndexRoot(pseudo.getKey(), def, false);
		
		boolean again = true;
		
		while (again) {
			again = false;
			try {				
				root.removeOutdated(records, new Lookup(cond));
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
