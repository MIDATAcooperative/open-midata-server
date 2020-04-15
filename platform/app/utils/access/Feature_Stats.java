package utils.access;

import java.util.ArrayList;
import java.util.Collection;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.Consent;
import models.MidataId;
import models.RecordGroup;
import models.RecordsInfo;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.access.Feature_AccountQuery.IdAndConsentFieldIterator;
import utils.access.index.StatsIndexKey;
import utils.access.index.StatsIndexRoot;
import utils.access.index.StatsLookup;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.RequestTooLargeException;

public class Feature_Stats extends Feature {

	private Feature next;

	public Feature_Stats(Feature next) {
		this.next = next;
	}

	public static  String getKey(StatsIndexKey r) {
		return r.content+"/"+r.app+"/"+r.format+"/"+r.owner+"/"+r.stream;
	}
	
	
	public static  String getKey(DBRecord r) throws AppException {
		return getKey(fromRecord(r));
	}
	
	public static StatsIndexKey fromRecord(DBRecord r) throws AppException {
		StatsIndexKey result = new StatsIndexKey();
		
		result.aps=r.consentAps;	
		result.format=r.meta.getString("format");
		result.content=r.meta.getString("content");
		result.group=r.group;
		result.app=MidataId.from(r.meta.getString("app"));	
		result.owner=r.context.getOwner();
		result.ownerName=r.context.getOwnerName();
		if (result.ownerName == null) result.ownerName = "?";
				
		result.newestRecord=r._id;	
		result.count=0;
		result.oldest=Long.MAX_VALUE;	
		result.newest=0;		
		result.calculated = System.currentTimeMillis();
		
		return result;
	}
	
	public static RecordsInfo toRecordInfo(StatsIndexKey key) {
        RecordsInfo res = new RecordsInfo();
        res.ownerNames = new HashSet<String>();
		res.formats.add(key.format);	
		res.contents.add(key.content);
		res.groups.add(key.group);
		if (key.app!=null) res.apps.add(key.app);
		res.owners.add(key.owner.toString());
		res.ownerNames.add(key.ownerName!=null ? key.ownerName : "?");
		res.newest = new Date(key.newest);
		res.oldest = new Date(key.oldest);
		res.count = key.count;
		res.newestRecord = key.newestRecord;	
		res.calculated = new Date(key.calculated);
		return res;
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
			
			AccessLog.logBeginPath("stats", null);
			long startTime = System.currentTimeMillis();
			
			//try {	
			Query qnew = q;
			       
			StatsIndexRoot index = q.getCache().getStatsIndexRoot();
			HashMap<String, StatsIndexKey> map = new HashMap<String, StatsIndexKey>();
			//IndexPseudonym pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), true);
			//IndexManager.instance.triggerUpdate(pseudo, q.getCache(), q.getCache().getExecutor(), index.getModel(), null);
			if (index != null) {
				long oldest = index.getAllVersion();								
				if (oldest > 0) qnew = new Query(q, "info-shared-after", CMaps.map("shared-after", oldest));
			}
			
			for (StatsIndexKey inf : countConsent(q, next, Feature_Indexes.getContextForAps(q, q.getApsId()))) {
				map.put(getKey(inf), inf);
			}
						
			if (q.getApsId().equals(q.getCache().getAccountOwner())) {				
				List<Consent> consents = Feature_AccountQuery.getConsentsForQuery(qnew, true, true);
				
				if (consents.size() > 100) {
					if (index==null) {
						index = IndexManager.instance.getStatsIndex(q.getCache(), q.getCache().getAccountOwner(), true);
					}
					IndexPseudonym pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), true);
					IndexManager.instance.triggerUpdate(pseudo, q.getCache(), q.getCache().getExecutor(), index.getModel(), null);
				}
				
				if (consents.size() >= Feature_AccountQuery.MAX_CONSENTS_IN_QUERY) {
					throw new RequestTooLargeException("error.toomany.consents", "Too many consents in query #="+consents.size());
				}
				
				for (Consent consent : consents) {
					for (StatsIndexKey inf : countConsent(q, next, Feature_Indexes.getContextForAps(q, consent._id))) {
					  map.put(getKey(inf), inf);
					}
				}
			}
			
			if (index != null) {
				
				StatsLookup lookup = new StatsLookup();			
				if (q.restrictedBy("app")) lookup.setApp(q.getRestriction("app"));
				if (q.restrictedBy("owner")) lookup.setOwner(q.getRestriction("owner")); 
				if (q.restrictedBy("content")) lookup.setContent(q.getRestriction("content"));
				if (q.restrictedBy("format")) lookup.setFormat(q.getRestriction("format"));
				if (q.restrictedBy("study-group")) lookup.setStudyGroup(q.getRestriction("study-group"));	
				if (!q.getApsId().equals(q.getCache().getAccountOwner())) lookup.setAps(q.getApsId());
				Collection<StatsIndexKey> matches = index.lookup(lookup);										
				
				for (StatsIndexKey inf : matches) {									
					map.putIfAbsent(getKey(inf), inf);
				}
								
			}
				
			AccessLog.logEndPath("# matches="+map.size());
			return new StatsIterator(map.values().iterator());
	
	}
	
	public static class StatsIterator implements DBIterator<DBRecord> {

		private Iterator<StatsIndexKey> iterator;
		
		public StatsIterator(Iterator<StatsIndexKey> iterator) {
			this.iterator = iterator;			
		}
		
		@Override
		public DBRecord next() throws AppException {
			StatsIndexKey inf = iterator.next();
			DBRecord r = new DBRecord();
			r._id = inf.newestRecord;
			r.attached = inf;
			return r;
		}

		@Override
		public boolean hasNext() throws AppException {
			return iterator.hasNext();
		}

		@Override
		public String toString() {
			return "stats-iterator";
		}						
	}
	
	public static StatsIndexKey countStream(Query q, MidataId stream, MidataId owner, Feature qm, StatsIndexKey inf, boolean cached) throws AppException {
		
		String groupSystem = q.getStringRestriction("group-system");
		APS myaps = q.getCache().getAPS(stream, owner);
		if (!myaps.isAccessible()) return null;
		BasicBSONObject obj = myaps.getMeta("_info");		
		if (cached && obj != null && obj.containsField("apps")) { // Check for apps for compatibility with old versions 						
			inf.count = obj.getInt("count");				
			inf.newest = obj.getDate("newest").getTime();
			inf.oldest = obj.getDate("oldest").getTime();
			inf.newestRecord = new MidataId(obj.getString("newestRecord"));								
			inf.format = obj.getString("formats");
			inf.content = obj.getString("contents");
			inf.app = MidataId.from(obj.getString("apps"));
			inf.group = RecordGroup.getGroupForSystemAndContent(groupSystem, inf.content);
			if (owner != null) inf.owner = owner;
			inf.calculated = obj.getDate("calculated").getTime();
			Date from = (inf.calculated - 1000 > inf.newest + 1) ? new Date(inf.calculated - 1000) : new Date(inf.newest + 1);
			q = new Query(q, "info-stream-after", CMaps.map("stream", stream).map("created-after", from));			
			long diff = myaps.getLastChanged() - from.getTime();					
			if (diff < 1200) return inf;				
		} else {
			q = new Query(q, "info-stream", CMaps.map("stream", stream));
		}
				
		//Feature qm = new Feature_Prefetch(false, new Feature_BlackList(myaps, new Feature_QueryRedirect(new Feature_FormatGroups(new Feature_ProcessFilters(new Feature_Pseudonymization(new Feature_PublicData(new Feature_UserGroups(new Feature_AccountQuery(new Feature_ConsentRestrictions(new Feature_Streams()))))))))));						 
		List<DBRecord> recs = ProcessingTools.collect(ProcessingTools.noDuplicates(qm.iterator(q)));
				
		if (inf.app==null && recs.size()>0) inf.app = MidataId.from(recs.get(0).meta.getString("app"));
		for (DBRecord record : recs) {
			inf.count++;
			long created = record._id.getCreationDate().getTime();
			if (created > inf.newest) {
				inf.newest = created;
				inf.newestRecord = record._id;
			}
			if (inf.oldest==0 || created < inf.oldest) {
				inf.oldest = created;
			}			 													
		}		
		
		if (cached && recs.size()>0 && inf.app!=null) {			
			BasicBSONObject r = new BasicBSONObject();			
			r.put("formats", inf.format);
			r.put("contents", inf.content);
			r.put("apps", inf.app.toString());
			r.put("count", inf.count);
			r.put("newest", new Date(inf.newest));
			r.put("oldest", new Date(inf.oldest));
			r.put("newestRecord", inf.newestRecord.toString());
			r.put("calculated", new Date());
			myaps.setMeta("_info", r);			
		}
		return inf;
	}
	
	public static Collection<StatsIndexKey> countConsent(Query q, Feature qm, AccessContext context) throws AppException {
		q = new Query(q, "info-consent", CMaps.map(), context.getTargetAps(), context);
	
		boolean hasFilter = Feature_ConsentRestrictions.hasFilter(q);
		
		Query q2 = hasFilter 
				   ? new Query(q, "info-streams", CMaps.map("owner","self"))
				   : new Query(q, "info-streams", CMaps.map("flat",true).map("streams","true").map("owner","self"));
		
		HashMap<String, StatsIndexKey> map = new HashMap<String, StatsIndexKey>();		
		List<DBRecord> toupdate = qm.query(q2);
		
		q.getCache().prefetch(toupdate);
		for (DBRecord r : toupdate) {		   
		   boolean isnew = false;
		   StatsIndexKey inf;
		   if (r.isStream != null) {
			   inf = fromRecord(r);
			   inf.stream = r._id;			   			   			   
			   StatsIndexKey streamKey = countStream(q, r._id, r.owner, qm, inf, true);
			   map.put(getKey(inf), inf);			   
		   } else {
			   inf = map.get(getKey(r));
			   if (inf == null) { inf = fromRecord(r);isnew = true; }
			   inf.count++;
			   long created = r._id.getCreationDate().getTime();
			   if (created > inf.newest) {
				   inf.newest = created;
				   inf.newestRecord = r._id;
			   }
			   if (created < inf.oldest) {
				   inf.oldest = created;
			   }
		   }
		   
		   if (isnew) {
			   map.put(getKey(inf), inf);			  
		   }			   				  		   
		}		
		return map.values();
	}

}
