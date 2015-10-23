package utils.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.auth.EncryptionNotSupportedException;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import models.APSNotExistingException;
import models.Record;

public class Feature_Streams extends Feature {

	
	
	public Feature_Streams() {
		
	}
	
	
	@Override
	protected List<Record> lookup(List<Record> records, Query q)
			throws AppException {
		APS next = q.getCache().getAPS(q.getApsId());
		
		List<Record> result = (next != null) ? next.lookup(records, q) : null;
		
		boolean isStrict = q.restrictedBy("strict");
		
		for (Record record : records) {
			if (record.stream != null && record.key == null) {
				boolean lookup = true;
				if (isStrict) {
					Record stream = new Record();
					stream._id = record.stream;
					if (!((APS) next).lookupSingle(stream, q)) lookup = false; 
				}
				
				boolean found = lookup && q.getCache().getAPS(record.stream).lookupSingle(record, q);
				AccessLog.debug("looked for:"+record._id.toString()+" found="+found+" key="+(record.key != null));                			
				if (found) {
					result.add(record);
					
					if (q.returns("owner") && record.owner == null) {
						List<Record> stream = next.query(new Query(CMaps.map("_id", record.stream), Sets.create("_id", "key", "owner"), q.getCache(), q.getApsId(), true ));
						if (stream.size() > 0) record.owner = stream.get(0).owner;
					}                    					
				}
			}
		}
		
		return result;
	}
	
	
	@Override
	protected List<Record> query(Query q) throws AppException {		
		APS next = q.getCache().getAPS(q.getApsId());
		List<Record> records = new ArrayList<Record>();
		boolean restrictedByStream = q.restrictedBy("stream");
		
		if (restrictedByStream) {
			  AccessLog.debug("single stream query");
			  //Set<String> streams1 = q.getRestriction("stream");
			  
			  List<Record> streams = next.query(new Query(CMaps.map("_id", q.getProperties().get("stream")), Sets.create("_id", "key", "owner"), q.getCache(), q.getApsId(), true ));
				
			  for (Record r : streams) {
				  if (r.isStream) {
					  try {
					      records.addAll(q.getCache().getAPS(r._id, r.key, r.owner).query(q));
					  } catch (EncryptionNotSupportedException e) { throw new InternalServerException("error.internal", "Encryption not supported."); }
					  catch (APSNotExistingException e2) {
						  next.removePermission(r);
					  }
				  }
			  }				
			  			  			  
			  return records;
		}
        		
		
		records = next.query(q);
		boolean includeStreams = q.includeStreams();
		boolean streamsOnly = q.isStreamOnlyQuery();
		if (streamsOnly) {
			List<Record> filtered = new ArrayList<Record>(records.size());
			for (Record r : records) {
			  if (r.isStream) filtered.add(r);
			}
			return filtered;
		} else
		if (q.deepQuery()) {
			List<Record> filtered = new ArrayList<Record>(records.size());
			for (Record r : records) {
				if (r.isStream) {
					try {
					  filtered.addAll(q.getCache().getAPS(r._id, r.key, r.owner).query(q));
					} catch (EncryptionNotSupportedException e) { throw new InternalServerException("error.internal", "Encryption not supported."); }
				    if (includeStreams) filtered.add(r);	
				} else filtered.add(r);
			}
			records = filtered;
		} else if (!includeStreams) {
			List<Record> filtered = new ArrayList<Record>(records.size());
			for (Record record : records) {
				if (!record.isStream) filtered.add(record);
			}
			records = filtered;
		}
							
		return records;
		
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q)
			throws InternalServerException {		
		return records;
	}

	

    
}
