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

import models.Consent;
import models.MidataId;
import models.RecordsInfo;
import utils.AccessLog;
import utils.access.Feature_AccountQuery.IdAndConsentFieldIterator;
import utils.access.Feature_StreamIndex.StreamIndexIterator;
import utils.access.index.StatsIndexKey;
import utils.access.index.StatsIndexRoot;
import utils.access.index.StatsLookup;
import utils.access.index.StreamIndexLookup;
import utils.access.index.StreamIndexRoot;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;

public class Feature_Stats extends Feature {

	private Feature next;

	public Feature_Stats(Feature next) {
		this.next = next;
	}

	private String getKey(StatsIndexKey r) {
		return "";
	}
	
	private String getKey(DBRecord r) {
		return "";
	}
	
	private StatsIndexKey fromRecord(DBRecord r) {
		StatsIndexKey result = new StatsIndexKey();
		
		return result;
	}
	
	private RecordsInfo toRecordInfo(StatsIndexKey key) {
        RecordsInfo res = new RecordsInfo();		
		res.formats.add(key.format);	
		res.contents.add(key.content);
		res.groups.add(key.group);
		res.apps.add(key.app);
		res.owners.add(key.owner.toString());
		res.ownerNames.add(key.ownerName);
		res.newest = new Date(key.newest);
		res.oldest = new Date(key.oldest);
		res.count = key.count;
		res.newestRecord = key.newestRecord;	
		res.calculated = new Date(key.calculated);
		return res;
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
			
			AccessLog.logBeginPath("stats");
			long startTime = System.currentTimeMillis();						
			try {	
			List<DBRecord> result = new ArrayList<DBRecord>();
				       
			StatsIndexRoot index = IndexManager.instance.getStatsIndex(q.getCache(), q.getCache().getAccountOwner()) ;
					
			StatsLookup lookup = new StatsLookup();			
			if (q.restrictedBy("app")) lookup.setApp(q.getRestriction("app"));
			if (q.restrictedBy("owner")) lookup.setOwner(q.getRestriction("owner")); 
			if (q.restrictedBy("content")) lookup.setContent(q.getRestriction("content"));
			if (q.restrictedBy("format")) lookup.setFormat(q.getRestriction("format"));
			
			Collection<StatsIndexKey> matches = index.lookup(lookup);
			HashMap<String, StatsIndexKey> map = new HashMap<String, StatsIndexKey>();			
			
			long oldest = System.currentTimeMillis();
			for (StatsIndexKey inf : matches) {
				if (inf.calculated < oldest) oldest = inf.calculated;
				map.put(getKey(inf), inf);
			}
			
			Feature nextWithProcessing = new Feature_ProcessFilters(next);
			
			List<DBRecord> toupdate = next.query(new Query(q,CMaps.map("shared-after", oldest)));
			lookup = new StatsLookup();
			for (DBRecord r : toupdate) {
			   boolean isnew = false;
			   StatsIndexKey inf;
			   if (r.isStream != null) {
				   inf = map.get(getKey(r));
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
			
	
			for (StatsIndexKey inf : matches) {
				DBRecord r = new DBRecord();
				r.attached = toRecordInfo(inf);
				result.add(r);
			}
			
			index.flush();
			} catch (LostUpdateException e) {
				
			}
			
			return ProcessingTools.dbiterator("stats", result.iterator());
	
	}
}
