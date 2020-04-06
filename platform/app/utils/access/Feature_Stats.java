package utils.access;

import java.util.ArrayList;
import java.util.Collection;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import models.MidataId;
import models.RecordsInfo;
import utils.AccessLog;

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
		return r.content+"/"+r.app+"/"+r.format+"/"+r.owner;
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
		res.apps.add(key.app);
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
}
