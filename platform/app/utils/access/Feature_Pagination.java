package utils.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import models.MidataId;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.AppException;

public class Feature_Pagination extends Feature {

	private Feature next;
	
	public Feature_Pagination(Feature next) {
		this.next = next;
	}

	@Override
	protected Iterator<DBRecord> iterator(Query q) throws AppException {
		
		if (q.restrictedBy("from")) {
			MidataId from = q.getFrom();
			
			Map<String, Object> props = new HashMap<String, Object>(q.getProperties());			
			props.put("limit", null);
			
			List<DBRecord> findFrom = next.query(new Query(q, CMaps.map("_id", from)));
			DBRecord fromRecord = null;
			if (findFrom.size() == 1) {
				fromRecord = findFrom.get(0);
			}
			
			Iterator<DBRecord> result = next.iterator(new Query(q, props).setFromRecord(fromRecord));
			boolean foundFrom = false;
			while (!foundFrom && result.hasNext()) { DBRecord rec = result.next();if (rec._id.equals(from)) foundFrom = true; }
			
			AccessLog.log("foundFrom="+foundFrom);
			if (!result.hasNext()) return Collections.emptyIterator();
			
			return ProcessingTools.limit(q.getProperties(), result);
		}
		
		if (q.restrictedBy("skip")) {
			int skip = (Integer) q.getProperties().get("skip");
			
			Map<String, Object> props = new HashMap<String, Object>(q.getProperties());
			props.put("skip", null);
			
			
			
			Iterator<DBRecord> result = next.iterator(new Query(q, props));
			int current = 0;
			while ( current < skip && result.hasNext()) { result.next();current++; }
			
			if (!result.hasNext()) return Collections.emptyIterator();
			
			return ProcessingTools.limit(q.getProperties(), result);
		}				
		
		return ProcessingTools.limit(q.getProperties(), next.iterator(q));
	}
	
	
}
