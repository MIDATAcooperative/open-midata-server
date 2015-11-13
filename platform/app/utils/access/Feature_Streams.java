package utils.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
			if (q.restrictedBy("writeable") && q.getProperties().get("writeable").equals("true")) {
				for (Record r : records) {
				  if (r.isStream && !r.isReadOnly) filtered.add(r);
				}
			} else {
				for (Record r : records) {
				  if (r.isStream) filtered.add(r);
				}
			}
			return filtered;
		} else
		if (q.deepQuery()) {
			List<Record> filtered = new ArrayList<Record>(records.size());
			for (Record r : records) {
				if (r.isStream) {
					try {
					  APS streamaps = q.getCache().getAPS(r._id, r.key, r.owner);
					  if (q.getMinTimestamp() <= streamaps.getLastChanged()) {						
					    for (Record r2 : streamaps.query(q)) {
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

	public static void placeNewRecordInStream(ObjectId executingPerson, Record record, ObjectId alternateAps) throws AppException {
		 if (record.stream == null) {
			  if (RecordManager.instance.getCache(executingPerson).getAPS(record.owner, record.owner).isAccessible()) {		 
			     record.stream = getStreamByFormatContent(executingPerson, record.owner, record.format, record.content, true);
			  } else if (alternateAps != null) {
				 record.stream = getStreamByFormatContent(executingPerson, alternateAps, record.format, record.content, true);
			  }
		  }
		  if (record.stream == null && record.format != null) {
			 ContentInfo content = ContentInfo.getByName(record.content);
			 if (RecordManager.instance.getCache(executingPerson).getAPS(record.owner, record.owner).isAccessible()) {
			    Record stream = createStream(executingPerson, record.owner, record.owner, record.content, record.format, content.security.equals(APSSecurityLevel.MEDIUM));			 			
			    record.stream = stream._id;
			 } else if (alternateAps != null) {
			    Record stream = createStream(executingPerson, record.owner, alternateAps, record.content, record.format, content.security.equals(APSSecurityLevel.MEDIUM));
				record.stream = stream._id;				
			 }
		  }
	}
	
	private static Record createStream(ObjectId executingPerson, ObjectId owner, ObjectId targetAPS, String content, String format,
			boolean direct) throws AppException {
		AccessLog.debug("Create Stream: who="+executingPerson.toString()+" content="+content+" format="+format+" direct="+direct+" into="+targetAPS);
		Record result = new Record();
		result._id = new ObjectId();
		result.name = content;
		result.owner = owner;
		result.direct = direct;
		result.format = format;
		result.content = content;
		result.isStream = true;
		result.created = DateTimeUtils.now();
		result.data = new BasicDBObject();
		result.time = 0;
		result.creator = null;
		
		APS apswrapper = null;
		
		if (targetAPS != null && targetAPS.equals(owner)) targetAPS = null; // That is what we expect anyway
		
		if (targetAPS != null) apswrapper = RecordManager.instance.getCache(executingPerson).getAPS(targetAPS);
		else apswrapper = RecordManager.instance.getCache(executingPerson).getAPS(result.owner, result.owner);

		apswrapper.provideRecordKey(result);
				
		boolean apsDirect = result.direct;
		result.stream = null;			
		result.direct = false;
		
		apswrapper.addPermission(result, targetAPS != null && !targetAPS.equals(result.owner));
								
		Record unecrypted = result.clone();
				
		RecordEncryption.encryptRecord(result, apswrapper.getSecurityLevel());		
	    Record.add(result);	  
	    				
		RecordManager.instance.createAPSForRecord(executingPerson, unecrypted.owner, unecrypted._id, unecrypted.key, apsDirect);
				
		if (targetAPS != null) {
			unecrypted.isReadOnly = true;
			RecordManager.instance.getCache(executingPerson).getAPS(result.owner, result.owner).addPermission(unecrypted, false);
		}
		
		RecordManager.instance.applyQueries(executingPerson, unecrypted.owner, unecrypted, targetAPS != null ? targetAPS : unecrypted.owner);
					
		return result;
	}

	private static ObjectId getStreamByFormatContent(ObjectId who, ObjectId apsId, String format, String content, boolean writeableOnly)
			throws AppException {
		List<Record> result = QueryEngine.listInternal(RecordManager.instance.getCache(who), apsId, 
				CMaps.map("format", format)
				     .map("content", content)
					 .map("streams", "only")
					 .map("writeable", writeableOnly ? "true" : "false"), RecordManager.INTERNALIDONLY);
		if (result.isEmpty())
			return null;
		Record streamRec = result.get(0);
		RecordManager.instance.getCache(who).getAPS(streamRec._id, streamRec.key, streamRec.owner);
		return streamRec._id;
	}	
    
}
