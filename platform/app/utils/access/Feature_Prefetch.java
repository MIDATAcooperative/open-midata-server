package utils.access;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import models.MidataId;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.AppException;

public class Feature_Prefetch extends Feature {

    private Feature next;
    private boolean setUserGroup;
	
	public Feature_Prefetch(boolean setUserGroup, Feature next) throws AppException {
		this.setUserGroup = setUserGroup;
		this.next = next;		
	}
		
	/*
	@Override
	protected List<DBRecord> query(Query q) throws AppException {						
		if (q.restrictedBy("_id")) {
			List<DBRecord> prefetched = QueryEngine.lookupRecordsById(q);
			
			return lookup(q, prefetched, next);									
		} else return next.query(q);
	}
	*/
	
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("_id")) {									
			if (setUserGroup && q.restrictedBy("usergroup")) {
			  return new Feature_UserGroups(new Feature_Prefetch(false, next)).iterator(q);
			} else {
			  List<DBRecord> prefetched = QueryEngine.lookupRecordsById(q);
			  return new LookupIterator(lookup(q, prefetched, next, !q.restrictedBy("usergroup")));
			}
		} else return next.iterator(q);
	}
		

	protected static List<DBRecord> lookup(Query q, List<DBRecord> prefetched, Feature next, boolean withUserGroup) throws AppException {
		AccessLog.logBegin("start lookup #recs="+prefetched.size()+" withGroups="+withUserGroup);
		long time = System.currentTimeMillis();
		List<DBRecord> results = null;
		Feature nextWithUserGroup = null;
		for (DBRecord record : prefetched) {
		  List<DBRecord> partResult = null;	
		
		  if (record.stream != null) {
		    APS stream = q.getCache().getAPS(record.stream);		    
		    if (stream.isAccessible()) {	
		    	AccessLog.log("direct");		
		    	MidataId owner = stream.getStoredOwner();
		    	partResult = QueryEngine.combine(q, CMaps.map("_id", record._id).map("flat", "true").map("stream", record.stream).map("owner", owner).map("quick",  record), next);
		    } else {
		    	if (withUserGroup) {
		    	  APSCache c2 = Feature_UserGroups.findApsCacheToUse(q.getCache(), record.stream);		    	
		    	  if (c2 != null && !c2.equals(q.getCache())) {
		    		AccessLog.log("with usergroup");		
		    		APS streamUG = c2.getAPS(record.stream);		    				    		
		    		MidataId owner = streamUG.getStoredOwner();
		    		if (nextWithUserGroup == null) nextWithUserGroup = new Feature_UserGroups(next);
	    		    partResult = QueryEngine.combine(q, CMaps.map("_id", record._id).map("flat", "true").map("usergroup", c2.getAccountOwner()).map("owner", owner).map("stream", record.stream).map("quick",  record), nextWithUserGroup);
	    		    if (partResult.isEmpty()) partResult = null;	    		   
		    	  }
		    	}
		    	
		    	// This "study" section is just a hack to fetch researcher shared data faster until a general solution is found
		    	if (partResult == null && q.restrictedBy("study") && !q.restrictedBy("owner"))	{
		    		AccessLog.log("study related variant");
		    		partResult = QueryEngine.combine(q, CMaps.map("_id", record._id).map("flat", "true").map("stream", record.stream).map("study-related", true).map("quick",  record), next);
		    		if (partResult.isEmpty()) partResult = null;
		    	}
		    	
		    	if (partResult == null) {	
		    		AccessLog.log("last");		    		
		    		//if (true) throw new NullPointerException();
		    		partResult = QueryEngine.combine(q, CMaps.map("_id", record._id).map("flat", "true").map("stream", record.stream).map("quick",  record), next);
		    	}
		    	
		    }		 
		  } else {	
			  AccessLog.log("no stream given");	
			  partResult = QueryEngine.combine(q, CMaps.map("_id", record._id).map("flat", "true").map("streams", "true"), next);
		  }
		  
		  
		  
		  results = QueryEngine.combine(results,  partResult);
		}
		if (results==null) results = Collections.emptyList();
									
		//Feature_AccountQuery.setOwnerField(q, results);			
						
		AccessLog.logEnd("end lookup #found="+results.size()+" time="+(System.currentTimeMillis() - time));
		return results;
	}
		

	static class LookupIterator implements DBIterator<DBRecord> {
		private Iterator<DBRecord> data;
		private int size;
		
		
		LookupIterator(List<DBRecord> data) {
			data = QueryEngine.modifyable(data);
			Collections.sort(data);
			this.size = data.size();
			this.data = data.iterator();			
		}

		@Override
		public boolean hasNext() {
			return data.hasNext();
		}

		@Override
		public DBRecord next() {
			return data.next();
		}

		@Override
		public String toString() {
			return "lookup({ size: "+size+"})";
		}
		
		
		
	}

}