package utils.access;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;

import controllers.RecordSharing;

import models.ModelException;
import models.Record;

public class FormatHandling extends QueryManager {

	private QueryManager next;
	
	public FormatHandling(QueryManager next) {
		this.next = next;
	}
	
	@Override
	protected boolean lookupSingle(Record record, Query q) throws ModelException {
		return next.lookupSingle(record, q);
	}

	@Override
	protected List<Record> query(Query q) throws ModelException {
		
		 if (q.isStreamOnlyQuery()) return next.query(q);
		 
		 if (q.restrictedBy("format") && !q.restrictedBy("stream")) {
			 			 
			 Set<String> formats = q.getRestriction("format");
			 			 
			 //if (results.size() == 0) return Collections.<Record>emptyList();
			 
			 if (q.deepQuery()) {
				 List<Record> results = next.query(new Query(CMaps.map("format", Query.STREAM_TYPE).map("name", formats), Sets.create("_id"), q.getCache(), q.getApsId()));
				 Set<String> streams = new HashSet<String>();
				 streams.add(q.getApsId().toString());
				 for (Record r : results) streams.add(r._id.toString());
				 q = new Query(q, CMaps.map("stream", streams));
				 
				 return next.query(q);
			 } else {
				 List<Record> results = next.query(new Query(CMaps.map("format", Query.STREAM_TYPE).map("name", formats), q.getFields(), q.getCache(), q.getApsId()));
				 results.addAll(next.query(q));
				 return results;
			 }
		 }
		
		return next.query(q);
	}
	
	protected static List<Record> findStreams(Query q) throws ModelException {
		 Set<String> formats = q.getRestriction("format");
		 
		 return ComplexQueryManager.fullQuery(new Query(CMaps.map("format", Query.STREAM_TYPE).map("name", formats), Sets.create("_id"), q.getCache(), q.getApsId()), q.getApsId());	
	}

}
