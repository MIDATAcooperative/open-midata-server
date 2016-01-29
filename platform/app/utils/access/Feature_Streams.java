package utils.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;

import utils.DateTimeUtils;
import utils.auth.EncryptionNotSupportedException;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import models.APSNotExistingException;
import models.ContentInfo;
import models.Record;
import models.enums.APSSecurityLevel;

/**
 * organizes records into "streams". these are access permission sets that contain only records of one type.
 *
 */
public class Feature_Streams extends Feature {

	
	
	public Feature_Streams() {
		
	}
	
	public final static Set<String> streamFields = Sets.create("content", "format");
	
	
	@Override
	protected List<DBRecord> lookup(List<DBRecord> records, Query q)
			throws AppException {
		APS next = q.getCache().getAPS(q.getApsId());
		
		List<DBRecord> result = (next != null) ? next.lookup(records, q) : null;
		
		boolean isStrict = q.restrictedBy("strict");
		
		for (DBRecord record : records) {			
			if (record.stream != null && record.key == null) {
				boolean lookup = true;
				if (isStrict) {
					DBRecord stream = new DBRecord();					
					stream._id = record.stream;
					if (!((APS) next).lookupSingle(stream, q)) lookup = false; 
					if (!lookup) AccessLog.debug("failed to find stream "+record.stream.toString()+" in "+q.getApsId().toString());
				}
				
				boolean found = lookup && q.getCache().getAPS(record.stream).lookupSingle(record, q);
				AccessLog.debug("looked for:"+record._id.toString()+" in "+record.stream.toString()+" found="+found+" key="+(record.key != null));                			
				if (found) {
					result.add(record);
					
					if (q.returns("owner") && record.owner == null) {
						List<DBRecord> stream = next.query(new Query(CMaps.map("_id", record.stream), Sets.create("_id", "key", "owner"), q.getCache(), q.getApsId(), true ));
						if (stream.size() > 0) record.owner = stream.get(0).owner;
					}                    					
				}
			}
		}
		
		return result;
	}
	
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {		
		APS next = q.getCache().getAPS(q.getApsId());
		List<DBRecord> records = new ArrayList<DBRecord>();
		boolean restrictedByStream = q.restrictedBy("stream");
		
		if (restrictedByStream) {
			  AccessLog.debug("single stream query");
			  //Set<String> streams1 = q.getRestriction("stream");
			  
			  List<DBRecord> streams = next.query(new Query(CMaps.map("_id", q.getProperties().get("stream")), Sets.create("_id", "key", "owner"), q.getCache(), q.getApsId(), true ));
				
			  for (DBRecord r : streams) {
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
			List<DBRecord> filtered = new ArrayList<DBRecord>(records.size());
			for (DBRecord r : records) {
				if (r.isStream) {
					try {
					  APS streamaps = q.getCache().getAPS(r._id, r.key, r.owner);
					  if (q.getMinTimestamp() <= streamaps.getLastChanged()) {						
					    for (DBRecord r2 : streamaps.query(q)) {
					    	r2.stream = r._id;
					    	filtered.add(r2);
					    }
					  }
					} catch (EncryptionNotSupportedException e) { throw new InternalServerException("error.internal", "Encryption not supported."); }
				    if (includeStreams) filtered.add(r);	
				} else filtered.add(r);
			}
			records = filtered;
		} else if (!includeStreams) {
			List<DBRecord> filtered = new ArrayList<DBRecord>(records.size());
			for (DBRecord record : records) {
				if (!record.isStream) filtered.add(record);
			}
			records = filtered;
		}  
							
		return records;
		
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q)
			throws InternalServerException {		
		return records;
	}

	public static void placeNewRecordInStream(ObjectId executingPerson, DBRecord record, ObjectId alternateAps) throws AppException {
		 Map<String, Object> props = new HashMap<String, Object>();
		 if (record.stream == null) {
			  for (String field : streamFields) props.put(field, record.meta.get(field));
			  if (RecordManager.instance.getCache(executingPerson).getAPS(record.owner, record.owner).isAccessible()) {		 
			     record.stream = getStreamByProperties(executingPerson, record.owner, props, true);
			  } else if (alternateAps != null) {
				 record.stream = getStreamByProperties(executingPerson, alternateAps, props, true);
			  }
		  }
		  if (record.stream == null) {
			 ContentInfo content = ContentInfo.getByName((String) record.meta.get("content"));
			 if (RecordManager.instance.getCache(executingPerson).getAPS(record.owner, record.owner).isAccessible()) {
			    DBRecord stream = createStream(executingPerson, record.owner, record.owner, props, content.security.equals(APSSecurityLevel.MEDIUM));			 			
			    record.stream = stream._id;
			 } else if (alternateAps != null) {
			    DBRecord stream = createStream(executingPerson, record.owner, alternateAps, props, content.security.equals(APSSecurityLevel.MEDIUM));
				record.stream = stream._id;				
			 }
		  }
	}
	
	private static DBRecord createStream(ObjectId executingPerson, ObjectId owner, ObjectId targetAPS, Map<String, Object> properties,
			boolean direct) throws AppException {
		AccessLog.debug("Create Stream: who="+executingPerson.toString()+" direct="+direct+" into="+targetAPS);
		DBRecord result = new DBRecord();
		result._id = new ObjectId();
		result.owner = owner;
		result.direct = direct;
		result.meta = new BasicBSONObject(properties);		
		result.isStream = true;
		result.meta.put("created", DateTimeUtils.now());
		result.data = new BasicDBObject();
		result.time = 0;		
		
		APS apswrapper = null;
		
		if (targetAPS != null && targetAPS.equals(owner)) targetAPS = null; // That is what we expect anyway
		
		if (targetAPS != null) apswrapper = RecordManager.instance.getCache(executingPerson).getAPS(targetAPS);
		else apswrapper = RecordManager.instance.getCache(executingPerson).getAPS(result.owner, result.owner);

		apswrapper.provideRecordKey(result);
				
		boolean apsDirect = result.direct;
		result.stream = null;			
		result.direct = false;
		
		apswrapper.addPermission(result, targetAPS != null && !targetAPS.equals(result.owner));
								
		DBRecord unecrypted = result.clone();
				
		RecordEncryption.encryptRecord(result, apswrapper.getSecurityLevel());		
	    DBRecord.add(result);	  
	    				
		RecordManager.instance.createAPSForRecord(executingPerson, unecrypted.owner, unecrypted._id, unecrypted.key, apsDirect);
				
		if (targetAPS != null) {
			unecrypted.isReadOnly = true;
			RecordManager.instance.getCache(executingPerson).getAPS(result.owner, result.owner).addPermission(unecrypted, false);
		}
		
		RecordManager.instance.applyQueries(executingPerson, unecrypted.owner, unecrypted, targetAPS != null ? targetAPS : unecrypted.owner);
					
		return result;
	}

	private static ObjectId getStreamByProperties(ObjectId who, ObjectId apsId, Map<String, Object> properties, boolean writeableOnly)
			throws AppException {
		List<DBRecord> result = QueryEngine.listInternal(RecordManager.instance.getCache(who), apsId, 
				CMaps.map(properties)				     
					 .map("streams", "only")
					 .map("writeable", writeableOnly ? "true" : "false"), RecordManager.INTERNALIDONLY);
		if (result.isEmpty())
			return null;
		DBRecord streamRec = result.get(0);
		RecordManager.instance.getCache(who).getAPS(streamRec._id, streamRec.key, streamRec.owner);
		return streamRec._id;
	}	
    
}
