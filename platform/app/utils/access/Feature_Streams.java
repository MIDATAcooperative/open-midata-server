package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.BasicBSONObject;

import com.mongodb.BasicDBObject;

import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.ContentInfo;
import models.MidataId;
import models.enums.APSSecurityLevel;
import utils.AccessLog;
import utils.auth.EncryptionNotSupportedException;
import utils.collections.CMaps;
import utils.collections.NChainedMap;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * organizes records into "streams". these are access permission sets that contain only records of one type.
 *
 */
public class Feature_Streams extends Feature {

	
	
	public Feature_Streams() {
		
	}
		
	
	public final static Set<String> streamFields = Sets.create("content", "format", "app");
	
	private final static Set<String> streamQueryFields = Sets.create("_id", "key", "owner");
	
	/*
	@Override
	protected List<DBRecord> query(Query q) throws AppException {		
		APS next = q.getCache().getAPS(q.getApsId());
		List<DBRecord> records = Collections.emptyList();
		boolean restrictedByStream = q.restrictedBy("stream");
		
		if (restrictedByStream) {
			  
			 
			  // optimization for record lookup queries 
			  if (q.restrictedBy("quick")) {				  				  				  
			      records = next.query(q);			    
			      if (records.size() > 0) return records; 
			  }
			  
			  AccessLog.logBegin("begin single stream query");
			  
			  List<DBRecord> streams = next.query(new Query(CMaps.map(q.getProperties()).map("_id", q.getProperties().get("stream")).removeKey("quick"), streamQueryFields, q.getCache(), q.getApsId(), q.getContext() ));
				
			  for (DBRecord r : streams) {
				  if (r.isStream) {
					  
					  try {
						  APS myAps = q.getCache().getAPS(r._id, r.key, r.owner);						 
						  List<DBRecord> rs = myAps.query(q);
						  for (DBRecord r2 : rs) { r2.owner = r.owner;r2.stream = r._id; }
					      records = QueryEngine.combine(records, rs);
					      
					      if (myAps.getSecurityLevel().equals(APSSecurityLevel.MEDIUM)) {
					    	  for (DBRecord r2 : records) {
					    		fromStream(r, r2, true);						    	
					    	  }
					      }
					      
					  } catch (EncryptionNotSupportedException e) { throw new InternalServerException("error.internal", "Encryption not supported."); }
					  catch (APSNotExistingException e2) {
						  next.removePermission(r);
					  }
				  } 
			  }				
			  			  			  
			  AccessLog.logEnd("end single stream query");
			  return records;
		}
        		
		AccessLog.logBegin("start query on target APS");
		List<DBRecord> recs = next.query(q);
		
		AccessLog.logEnd("end query on target APS #res="+recs.size());
		if (recs.isEmpty()) return recs;
		boolean includeStreams = q.includeStreams();
		boolean streamsOnly = q.isStreamOnlyQuery();
		if (streamsOnly) {
			records = recs;
			List<DBRecord> filtered = new ArrayList<DBRecord>(records.size());
			if (q.restrictedBy("writeable") && q.getProperties().get("writeable").equals("true")) {
				for (DBRecord r : records) {
				  if (r.isStream && !r.isReadOnly) filtered.add(r);
				}
			} else {
				for (DBRecord r : records) {
				  if (r.isStream) filtered.add(r);
				}
			}
			return filtered;
		} else
		if (q.deepQuery()) {
			records = recs;
			List<DBRecord> filtered = new ArrayList<DBRecord>(records.size());
			List<DBRecord> streams = new ArrayList<DBRecord>();
			Map<MidataId, DBRecord> streamsToFetch = new HashMap<MidataId, DBRecord>();
			
			for (DBRecord r : records) {
				if (r.isStream) {
					if (!q.getCache().hasAPS(r._id)) streamsToFetch.put(r._id, r);
					else streams.add(r);
				} else filtered.add(r);
			}
			
			if (!streamsToFetch.isEmpty()) {
				NChainedMap<String, Object> restriction = CMaps.map("_id", streamsToFetch.keySet());
				if (q.getMinCreatedTimestamp() > 0) restriction = restriction.map("version", CMaps.map("$gt", q.getMinCreatedTimestamp()));
				if (q.getMinUpdatedTimestamp() > 0) restriction = restriction.map("version", CMaps.map("$gt", q.getMinUpdatedTimestamp()));
				
				Set<AccessPermissionSet> rsets = AccessPermissionSet.getAll(restriction, AccessPermissionSet.ALL_FIELDS);
				for (AccessPermissionSet set : rsets) {
					DBRecord r = streamsToFetch.get(set._id);
					streams.add(r);
					q.getCache().getAPS(r._id, r.key, r.owner, set, true);
				}
			}
				
			for (DBRecord r : streams) {										
				try {
				  APS streamaps = q.getCache().getAPS(r._id, r.key, r.owner);
				  boolean medium = streamaps.getSecurityLevel().equals(APSSecurityLevel.MEDIUM);
				  if (q.getMinUpdatedTimestamp() <= streamaps.getLastChanged() && q.getMinCreatedTimestamp() <= streamaps.getLastChanged()) {
					AccessLog.logBegin("start query on stream APS:"+streamaps.getId());
				    for (DBRecord r2 : streamaps.query(q)) {
				    	fromStream(r, r2, medium);				    	
				    	filtered.add(r2);
				    }
				    if (includeStreams) filtered.add(r);
				    AccessLog.logEnd("end query on stream APS");
				  }
				} catch (EncryptionNotSupportedException e) { throw new InternalServerException("error.internal", "Encryption not supported."); }					 	
			 				
			}
			records = filtered;
		} else if (!includeStreams) {
			records = recs;
			List<DBRecord> filtered = new ArrayList<DBRecord>(records.size());
			for (DBRecord record : records) {
				if (!record.isStream) filtered.add(record);
			}
			records = filtered;
		} else return recs;
							
		return records;
		
	}
	*/
	static class StreamCombineIterator extends Feature.MultiSource<DBRecord> {

		private APS next;
		private APS thisrecord;
		private MidataId owner;
		private int size;
		private long limit;
		private List<DBRecord> direct;
		
		StreamCombineIterator(APS next, Query query, DBIterator<DBRecord> streams, List<DBRecord> direct) throws AppException {
		  	this.next = next;		  			  
		  	this.query = query;		  
            this.limit = Math.max(query.getMinUpdatedTimestamp(), query.getMinCreatedTimestamp());
            this.limit = Math.max(this.limit, query.getMinSharedTimestamp());
		  	if (direct != null && !direct.isEmpty()) {		  		
		  		current = ProcessingTools.dbiterator("direct()", direct.iterator());
		  		chain = streams;
		  		thisrecord = next;
		  		owner = next.getStoredOwner();
		  		size = direct.size();
		  	}
		  	else init(streams);
		}
		
		@Override
		public DBIterator<DBRecord> advance(DBRecord r) throws AppException {
			
			if (r.isStream) {
				  
				
				try {
					  APS streamaps = query.getCache().getAPS(r._id, r.key, r.owner);
					  thisrecord = streamaps;
					  owner = thisrecord.getStoredOwner();
					  boolean medium = streamaps.getSecurityLevel().equals(APSSecurityLevel.MEDIUM);
					  if (limit <= streamaps.getLastChanged()) {
//						AccessLog.logBegin("start query on stream APS:"+streamaps.getId());
						 List<DBRecord> rs = streamaps.query(query);
						 
					    for (DBRecord r2 : rs) {
					    	 r2.owner = r.owner;
					    	fromStream(r, r2, medium);				    	
					    	//filtered.add(r2);
					    }
					    size = rs.size();					   

					    return ProcessingTools.dbiterator("",  rs.iterator());
					  }
					} catch (EncryptionNotSupportedException e) { throw new InternalServerException("error.internal", "Encryption not supported."); }					 	
				  catch (APSNotExistingException e2) {
					  next.removePermission(r);
				  }
				
				return ProcessingTools.empty();
			}
			return ProcessingTools.empty();			
		}

		@Override
		public String toString() {
			if (thisrecord == null) return "access-streams()";
			return "access-streams("+next.getId().toString()+", aps({ow:"+owner.toString()+", id:"+thisrecord.getId().toString()+", size:"+size+" }))";
		}
		
		
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		APS next = q.getCache().getAPS(q.getApsId());
		//Iterator<DBRecord> records = Collections.emptyIterator();
		boolean restrictedByStream = q.restrictedBy("stream");
		
		if (restrictedByStream) {
			  			 
			  // optimization for record lookup queries 
			  if (q.restrictedBy("quick")) {				  				  				  
			      DBIterator<DBRecord> records = next.iterator(q);			    
			      if (records.hasNext()) return records; 
			  }
			  
			  //AccessLog.logBegin("begin single stream query");
			  
			  DBIterator<DBRecord> streams = next.iterator(new Query(CMaps.map(q.getProperties()).map("_id", q.getProperties().get("stream")).removeKey("quick"), streamQueryFields, q.getCache(), q.getApsId(), q.getContext() ));				
			  return new StreamCombineIterator(next, q, streams, null);
			  
		}
        		
		//AccessLog.logBegin("start query on target APS");
		List<DBRecord> recs = next.query(q);
		
		//AccessLog.logEnd("end query on target APS #res="+recs.size());
		if (recs.isEmpty()) return ProcessingTools.empty();
		
		boolean includeStreams = q.includeStreams();
		boolean streamsOnly = q.isStreamOnlyQuery();
		if (streamsOnly) {
			
			List<DBRecord> filtered = new ArrayList<DBRecord>(recs.size());
			if (q.restrictedBy("writeable") && q.getProperties().get("writeable").equals("true")) {
				for (DBRecord r : recs) {
				  if (r.isStream && !r.isReadOnly) filtered.add(r);
				}
			} else {
				for (DBRecord r : recs) {
				  if (r.isStream) filtered.add(r);
				}
			}
			return ProcessingTools.dbiterator("", filtered.iterator());
		} else
		if (q.deepQuery()) {
			//records = recs;
			List<DBRecord> filtered = new ArrayList<DBRecord>(recs.size());
			List<DBRecord> streams = new ArrayList<DBRecord>();
			Map<MidataId, DBRecord> streamsToFetch = new HashMap<MidataId, DBRecord>();
			
			for (DBRecord r : recs) {
				if (r.isStream) {
					if (!q.getCache().hasAPS(r._id)) streamsToFetch.put(r._id, r);
					else streams.add(r);
				} else filtered.add(r);
			}
			
			if (!streamsToFetch.isEmpty()) {
				NChainedMap<String, Object> restriction = CMaps.map("_id", streamsToFetch.keySet());
				long min = q.getMinCreatedTimestamp() > 0 ? q.getMinCreatedTimestamp() : 0;
				min = q.getMinUpdatedTimestamp() > min ? q.getMinUpdatedTimestamp() : min;
				min = q.getMinSharedTimestamp() > min ? q.getMinSharedTimestamp() : min;
				if (min > 0) restriction = restriction.map("version", CMaps.map("$gt", min));
				
				Set<AccessPermissionSet> rsets = AccessPermissionSet.getAll(restriction, AccessPermissionSet.ALL_FIELDS);
				for (AccessPermissionSet set : rsets) {
					DBRecord r = streamsToFetch.get(set._id);
					streams.add(r);
					if (includeStreams) filtered.add(r);
					q.getCache().getAPS(r._id, r.key, r.owner, set, true);
				}
			}
			Collections.sort(streams);	
			
			
			return new StreamCombineIterator(next, q, ProcessingTools.dbiterator("", streams.iterator()), filtered);
			
		} else if (!includeStreams) {
			
			List<DBRecord> filtered = new ArrayList<DBRecord>(recs.size());
			for (DBRecord record : recs) {
				if (!record.isStream) filtered.add(record);
			}
			return ProcessingTools.dbiterator("", filtered.iterator());
		} else return ProcessingTools.dbiterator("", recs.iterator());
							
		
	}



	private static void fromStream(DBRecord stream, DBRecord record, boolean medium) {
		record.stream = stream._id;
    	if (medium) {
	    	for (String field : streamFields) {
	    		Object val = stream.meta.get(field);
	    		if (val!=null) record.meta.put(field, val);
	    	}
    	}
	}
	

	public static void placeNewRecordInStream(AccessContext context, DBRecord record, MidataId alternateAps) throws AppException {
		 Map<String, Object> props = new HashMap<String, Object>();		
		 if (record.stream == null) {
			  for (String field : streamFields) props.put(field, record.meta.get(field));
			  if (context.getCache().getAPS(record.owner, record.owner).isAccessible()) {		 
			     record.stream = getStreamByProperties(context, record.owner, props, true, true);
			  } else if (alternateAps != null) {
				 record.stream = getStreamByProperties(context, alternateAps, props, true, true);
			  }
		  }
		  if (record.stream == null) {
			 ContentInfo content = ContentInfo.getByName((String) record.meta.get("content"));
			 if (context.getCache().getAPS(record.owner, record.owner).isAccessible()) {
			    DBRecord stream = createStream(context, record.owner, record.owner, props, content.security.equals(APSSecurityLevel.MEDIUM));			 			
			    record.stream = stream._id;
			 } else if (alternateAps != null) {
			    DBRecord stream = createStream(context, record.owner, alternateAps, props, content.security.equals(APSSecurityLevel.MEDIUM));
				record.stream = stream._id;				
			 }
		  }
	}
	
	private static DBRecord createStream(AccessContext context, MidataId owner, MidataId targetAPS, Map<String, Object> properties,
			boolean direct) throws AppException {
		AccessLog.logBegin("begin create Stream: who="+context.getCache().getExecutor().toString()+" direct="+direct+" into="+targetAPS);
		DBRecord result = new DBRecord();
		result._id = new MidataId();
		result.owner = owner;
		result.direct = direct;
		result.meta = new BasicBSONObject(properties);	
		
		Object app = result.meta.get("app");
		if (app != null) result.meta.put("app", MidataId.from(app).toDb());
		
		result.isStream = true;
		result.meta.put("created", new Date());
		result.data = new BasicDBObject();
		result.time = 0;		
		
		APS apswrapper = null;
		
		if (targetAPS != null && targetAPS.equals(owner)) targetAPS = null; // That is what we expect anyway
		
		if (targetAPS != null) apswrapper = context.getCache().getAPS(targetAPS);
		else apswrapper = context.getCache().getAPS(result.owner, result.owner);

		boolean apsDirect = direct;
		
		AccessLog.log("provide key by "+apswrapper.getId().toString());
		
		apswrapper.provideRecordKey(result);
						
		result.stream = null;			
		result.direct = false;
		
		AccessLog.log("adding permission");
														
		DBRecord unecrypted = result.clone();
				
		RecordEncryption.encryptRecord(result);		
	    DBRecord.add(result);
	    
	    if (targetAPS != null) RecordLifecycle.addWatchingAps(result, targetAPS);

	    AccessLog.log("create aps for stream");
	    
		RecordManager.instance.createAPSForRecord(context.getCache().getExecutor(), unecrypted.owner, unecrypted._id, unecrypted.key, apsDirect);
		
		apswrapper.addPermission(unecrypted, targetAPS != null && !targetAPS.equals(unecrypted.owner));
		
		if (targetAPS != null) {
			AccessLog.log("add permission for owner");
			if (!context.isIncluded(unecrypted)) unecrypted.isReadOnly = true;			
			context.getCache().getAPS(result.owner, result.owner).addPermission(unecrypted, false);
		}
				
		RecordManager.instance.applyQueries(context, unecrypted.owner, unecrypted, targetAPS != null ? targetAPS : unecrypted.owner);
				
		AccessLog.logEnd("end create stream");
		return result;
	}

	private static MidataId getStreamByProperties(AccessContext context, MidataId apsId, Map<String, Object> properties, boolean writeableOnly, boolean doNotify)
			throws AppException {
		List<DBRecord> result = QueryEngine.listInternal(context.getCache(), apsId, null,
				CMaps.map(properties)				     
					 .map("streams", "only")
					 .map("owner", "self")
					 .map("writeable", writeableOnly ? "true" : "false"), RecordManager.INTERNALID_AND_WACTHES);
		if (result.isEmpty())
			return null;
		DBRecord streamRec = result.get(0);		
		if (doNotify) RecordLifecycle.notifyOfChange(streamRec, context.getCache());
		context.getCache().getAPS(streamRec._id, streamRec.key, streamRec.owner);
		return streamRec._id;
	}
	
	public static Pair<Map<String, Object>, Map<String, Object>> convertToQueryPair(Map<String, Object> inputQuery) {
		if (inputQuery.containsKey("$or")) {
		   Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) inputQuery.get("$or");
	       List<Map<String, Object>> resultStream = new ArrayList<Map<String, Object>>();
	       List<Map<String, Object>> resultRecord = new ArrayList<Map<String, Object>>();
	       for (Map<String, Object> prop : col) {
	      	  Pair<Map<String, Object>, Map<String, Object>> part = convertToQueryPair(prop);
	      	  if (part.getLeft() != null) resultStream.add(part.getLeft());
	      	  else resultRecord.add(part.getRight());
	       }
	       Map<String, Object> streamQuery = null;
	       Map<String, Object> recordQuery = null;
	       
	       if (resultStream.size()==1) streamQuery = resultStream.iterator().next();
	       else if (resultStream.size() > 1) streamQuery = CMaps.map("$or", resultStream);
	       
	       if (resultRecord.size()==1) recordQuery = resultRecord.iterator().next();
	       else if (resultRecord.size() > 1) recordQuery = CMaps.map("$or", resultRecord);
	       
	       return Pair.of(streamQuery, recordQuery);
		} else {
			if (inputQuery.containsKey("data")) return Pair.of(null, inputQuery);		
			return Pair.of(inputQuery, null);
		}
	}
	
	public static void streamJoin(AccessContext context) throws AppException {
		AccessLog.logBegin("start streams optimization");
		List<DBRecord> streams = QueryEngine.listInternal(context.getCache(), context.getTargetAps(), context, CMaps.map("owner", "self").map("streams","only"), Sets.create(streamFields));
		Map<String, List<DBRecord>> ordered = new HashMap<String, List<DBRecord>>();
		AccessLog.log("found streams: "+streams.size());
		// Sort streams
		for (DBRecord stream : streams) {
			String key = "";
			boolean skip = false;
			for (String field : streamFields) {
				Object o = stream.meta.get(field); 
				if (o==null) skip = true;
				key+=(o!=null?o.toString():"null")+"//";
			}
			if (skip) continue;
			List<DBRecord> recs = ordered.get(key);
			if (recs == null) {
				recs = new ArrayList<DBRecord>();
				ordered.put(key, recs);
			}
			recs.add(stream);
		}
		
		// Check streams
		for (String key : ordered.keySet()) {
			AccessLog.log(key);
			List<DBRecord> streamrecs = ordered.get(key);
			if (streamrecs.size() > 2) {
				AccessLog.logBegin("start optimize streams :"+key);
				Map<String, Object> props = CMaps.map("owner","self");
				for (String field : streamFields) props.put(field, streamrecs.get(0).meta.get(field));
				List<DBRecord> recs = QueryEngine.listInternal(context.getCache(), context.getTargetAps(), context, props, Sets.create(streamFields,"_id","owner"));
				AccessLog.log("Number of streams in group="+streamrecs.size());
				AccessLog.log("Number of records="+recs.size());
				// Do not optimize for full streams
				if (recs.size() > 1000) {
					AccessLog.logEnd("end optimize streams :"+key+" (too many entries)");
					continue;
				}
				
				DBRecord newstream = createStream(context, context.getOwner(), context.getTargetAps(), props, false);
				APS newstreamaps = context.getCache().getAPS(newstream._id, context.getOwner());
				newstreamaps.addPermission(recs, false);
				AccessLog.log("Change records and remove from old stream");
				
				for (DBRecord record : recs) {																				
					if (record.direct) {
						record.direct = false;
						DBRecord.set(record._id, "direct", false);
					} else {
						APS oldstreamaps = context.getCache().getAPS(record.stream, record.owner);
						oldstreamaps.removePermission(record);
					}
					record.stream = newstream._id;
					DBRecord.set(record._id, "stream", record.stream);
				}
				
				RecordManager.instance.wipe(context.getOwner(), streamrecs);
				AccessLog.logEnd("end optimize streams :"+key);
			}
		}
		
		AccessLog.logEnd("end streams optimization");
	}
    
}
