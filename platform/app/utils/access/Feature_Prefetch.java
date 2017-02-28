package utils.access;

import java.util.ArrayList;
import java.util.List;

import models.MidataId;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.AppException;

public class Feature_Prefetch extends Feature {

    private Feature next;  
	
	public Feature_Prefetch(Feature next) throws AppException {
		this.next = next;		
	}
		
	@Override
	protected List<DBRecord> query(Query q) throws AppException {						
		if (q.restrictedBy("_id")) {
			List<DBRecord> prefetched = QueryEngine.lookupRecordsById(q);
			
			return lookup(q, prefetched, next);									
		} else return next.query(q);
	}
	
	protected static List<DBRecord> lookup(Query q, List<DBRecord> prefetched, Feature next) throws AppException {
		AccessLog.logBegin("start lookup #recs="+prefetched.size());
		List<DBRecord> results = null;
		for (DBRecord record : prefetched) {
		  List<DBRecord> partResult = null;	
		
		  if (record.stream != null) {
		    APS stream = q.getCache().getAPS(record.stream);
		    if (stream.isAccessible()) {
		    	MidataId owner = stream.getStoredOwner();
		    	partResult = QueryEngine.combine(q, CMaps.map("_id", record._id).map("flat", "true").map("stream", record.stream).map("owner", owner).map("quick",  record), next);
		    } else {
	    		partResult = QueryEngine.combine(q, CMaps.map("_id", record._id).map("flat", "true").map("stream", record.stream).map("quick",  record), next);
		    }		 
		  } else partResult = QueryEngine.combine(q, CMaps.map("_id", record._id).map("flat", "true").map("streams", "true"), next);
		  
		  
		  
		  if (results == null) results = partResult; else results.addAll(partResult);
		}
		if (results==null) results = new ArrayList<DBRecord>();
		AccessLog.logEnd("end lookup #found="+results.size());
		return results;
	}
		


}