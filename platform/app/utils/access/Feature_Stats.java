package utils.access;

import java.util.ArrayList;
import java.util.Collection;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
	
	private RecordsInfo toRecordInfo(StatsIndexKey key) {
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
			List<DBRecord> result = new ArrayList<DBRecord>();
			//try {	
			
			/*	       
			StatsIndexRoot index = IndexManager.instance.getStatsIndex(q.getCache(), q.getCache().getAccountOwner()) ;
			
			IndexPseudonym pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), true);
			IndexManager.instance.triggerUpdate(pseudo, q.getCache(), q.getCache().getExecutor(), index.getModel(), null);
			
			
			StatsLookup lookup = new StatsLookup();			
			if (q.restrictedBy("app")) lookup.setApp(q.getRestriction("app"));
			if (q.restrictedBy("owner")) lookup.setOwner(q.getRestriction("owner")); 
			if (q.restrictedBy("content")) lookup.setContent(q.getRestriction("content"));
			if (q.restrictedBy("format")) lookup.setFormat(q.getRestriction("format"));
			
			Collection<StatsIndexKey> matches = index.lookup(lookup);
			HashMap<String, StatsIndexKey> map = new HashMap<String, StatsIndexKey>();			
			
			long oldest = index.getAllVersion();
			for (StatsIndexKey inf : matches) {				
				map.put(getKey(inf), inf);
			}
			*/
			
			List<StatsIndexKey> matches = new ArrayList<StatsIndexKey>();
			
			matches.addAll(countConsent(q, next, Feature_Indexes.getContextForAps(q, q.getApsId())));	
			
			if (q.getApsId().equals(q.getCache().getAccountOwner())) {				
				List<Consent> consents = Feature_AccountQuery.getConsentsForQuery(q, true, true);
				for (Consent consent : consents) {
					matches.addAll(countConsent(q, next, Feature_Indexes.getContextForAps(q, consent._id)));
				}
			}
			
			
			/*
			Feature nextWithProcessing = new Feature_ProcessFilters(next);
			
			List<DBRecord> toupdate = next.query(new Query(q,CMaps.map("shared-after", oldest)));
			lookup = new StatsLookup();
			for (DBRecord r : toupdate) {
			   boolean isnew = false;
			   StatsIndexKey inf;
			   if (r.isStream != null) {
				   String key = getKey(r);				   
				   inf = map.get(key);
				   AccessLog.log("key:"+key+" exists:"+(inf!=null));
				   List<DBRecord> newRecs;
				   if (inf != null) {
				      newRecs = nextWithProcessing.query(new Query(CMaps.map("owner",r.owner).map("stream",r._id).map("created-after", inf.calculated), Sets.create("_id"), q.getCache(), q.getApsId(), q.getContext()));
				   } else {
					  newRecs = nextWithProcessing.query(new Query(CMaps.map("owner",r.owner).map("stream",r._id), Sets.create("_id"), q.getCache(), q.getApsId(), q.getContext()));
				   }
				   
				   if (!newRecs.isEmpty()) {
				       if (inf == null) { inf = fromRecord(r);isnew = true; }				       
					   inf.count += newRecs.size();
					   for (DBRecord rec : newRecs) {
						   long created = rec._id.getCreationDate().getTime();
						   if (created > inf.newest) {
							   inf.newest = created;
							   inf.newestRecord = rec._id;
						   }
						   if (created < inf.oldest) {
							   inf.oldest = created;
						   }
					   }
				   }
				   
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
				   index.addEntry(inf);
				   matches.add(inf);
			   }			   				  
			   
			}
			*/
	
			for (StatsIndexKey inf : matches) {
				DBRecord r = new DBRecord();
				r._id = inf.newestRecord;
				r.attached = toRecordInfo(inf);
				result.add(r);
			}
			
			//index.flush();
			/*} catch (LostUpdateException e) {
				
			}*/
			AccessLog.logEndPath("# matches="+matches.size());
			return ProcessingTools.dbiterator("stats", result.iterator());
	
	}
	
	public static StatsIndexKey countStream(Query q, MidataId stream, MidataId owner, Feature qm, StatsIndexKey inf, boolean cached) throws AppException {
		
		String groupSystem = q.getStringRestriction("group-system");
		APS myaps = q.getCache().getAPS(stream);
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
		
		Query q2 = new Query(q, "info-streams", CMaps.map("flat",true).map("streams","also").map("owner","self"));
		//List<DBRecord> recs = ProcessingTools.collect(ProcessingTools.noDuplicates(new IdAndConsentFieldIterator(qm.iterator(q2), q.getContext(), q.getApsId(), q.returns("id"))));
		
		//AccessLog.log("COUNT CONSENT :"+context.toString()+" RECS="+recs.size());
		
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
			   //index.addEntry(inf);
			   //matches.add(inf);
		   }			   				  		   
		}		
		return map.values();
	}
	/*
	public static 
	long limit = index.getAllVersion();
	Set<Consent> consents = Consent.getAllActiveByAuthorized(executor, limit);	
	
	DBIterator<Consent> consentIt = new Feature_AccountQuery.BlockwiseConsentPrefetch(cache, consents.iterator(), 200);
	while (consentIt.hasNext()) {
		indexUpdatePart(index, executor, consentIt.next()._id, cache);
		modCount += index.getModCount();
	}		
	*/
}
