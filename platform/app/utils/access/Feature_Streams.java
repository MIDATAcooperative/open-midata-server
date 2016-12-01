package utils.access;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	public final static Set<String> streamFields = Sets.create("content", "format", "subformat");					
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {		
		APS next = q.getCache().getAPS(q.getApsId());
		List<DBRecord> records = new ArrayList<DBRecord>();
		boolean restrictedByStream = q.restrictedBy("stream");
		
		if (restrictedByStream) {
			  AccessLog.logBegin("begin single stream query");
			  //Set<String> streams1 = q.getRestriction("stream");
			  
			  List<DBRecord> streams = next.query(new Query(CMaps.map(q.getProperties()).map("_id", q.getProperties().get("stream")), Sets.create("_id", "key", "owner"), q.getCache(), q.getApsId() ));
				
			  for (DBRecord r : streams) {
				  if (r.isStream) {
					  try {
						  APS myAps = q.getCache().getAPS(r._id, r.key, r.owner);
						  List<DBRecord> rs = myAps.query(q);
						  for (DBRecord r2 : rs) { r2.owner = r.owner;r2.stream = r._id; }
					      records.addAll(rs);
					      
					      if (myAps.getSecurityLevel().equals(APSSecurityLevel.MEDIUM)) {
					    	  for (DBRecord r2 : records) {
						    	for (String field : streamFields) {
						    		Object val = r.meta.get(field);
						    		if (val!=null) r2.meta.put(field, val);
						    	}
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
		records = next.query(q);
		AccessLog.logEnd("end query on target APS #res="+records.size());
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
				if (q.getMinUpdatedTimestamp() > 0) restriction = restriction.map("version", CMaps.map("$gt", q.getMinUpdatedTimestamp()));
				Set<AccessPermissionSet> rsets = AccessPermissionSet.getAll(restriction, AccessPermissionSet.ALL_FIELDS);
				for (AccessPermissionSet set : rsets) {
					DBRecord r = streamsToFetch.get(set._id);
					streams.add(r);
					q.getCache().getAPS(r._id, r.key, r.owner, set);
				}
			}
				
			for (DBRecord r : streams) {										
				try {
				  APS streamaps = q.getCache().getAPS(r._id, r.key, r.owner);
				  boolean medium = streamaps.getSecurityLevel().equals(APSSecurityLevel.MEDIUM);
				  if (q.getMinUpdatedTimestamp() <= streamaps.getLastChanged() && q.getMinCreatedTimestamp() <= streamaps.getLastChanged()) {
					AccessLog.logBegin("start query on stream APS:"+streamaps.getId());
				    for (DBRecord r2 : streamaps.query(q)) {
				    	r2.stream = r._id;
				    	if (medium) {
					    	for (String field : streamFields) {
					    		Object val = r.meta.get(field);
					    		if (val!=null) r2.meta.put(field, val);
					    	}
				    	}
				    	filtered.add(r2);
				    }
				    if (includeStreams) filtered.add(r);
				    AccessLog.logEnd("end query on stream APS");
				  }
				} catch (EncryptionNotSupportedException e) { throw new InternalServerException("error.internal", "Encryption not supported."); }					 	
			 				
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
	

	public static void placeNewRecordInStream(MidataId executingPerson, DBRecord record, MidataId alternateAps) throws AppException {
		 Map<String, Object> props = new HashMap<String, Object>();
		 if (record.stream == null) {
			  for (String field : streamFields) props.put(field, record.meta.get(field));
			  if (RecordManager.instance.getCache(executingPerson).getAPS(record.owner, record.owner).isAccessible()) {		 
			     record.stream = getStreamByProperties(executingPerson, record.owner, props, true, true);
			  } else if (alternateAps != null) {
				 record.stream = getStreamByProperties(executingPerson, alternateAps, props, true, true);
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
	
	private static DBRecord createStream(MidataId executingPerson, MidataId owner, MidataId targetAPS, Map<String, Object> properties,
			boolean direct) throws AppException {
		AccessLog.logBegin("begin create Stream: who="+executingPerson.toString()+" direct="+direct+" into="+targetAPS);
		DBRecord result = new DBRecord();
		result._id = new MidataId();
		result.owner = owner;
		result.direct = direct;
		result.meta = new BasicBSONObject(properties);		
		result.isStream = true;
		result.meta.put("created", new Date());
		result.data = new BasicDBObject();
		result.time = 0;		
		
		APS apswrapper = null;
		
		if (targetAPS != null && targetAPS.equals(owner)) targetAPS = null; // That is what we expect anyway
		
		if (targetAPS != null) apswrapper = RecordManager.instance.getCache(executingPerson).getAPS(targetAPS);
		else apswrapper = RecordManager.instance.getCache(executingPerson).getAPS(result.owner, result.owner);

		boolean apsDirect = direct;
		
		AccessLog.log("provide key by "+apswrapper.getId().toString());
		
		apswrapper.provideRecordKey(result);
						
		result.stream = null;			
		result.direct = false;
		
		AccessLog.log("adding permission");
		
		apswrapper.addPermission(result, targetAPS != null && !targetAPS.equals(result.owner));
		if (targetAPS != null) RecordLifecycle.addWatchingAps(result, targetAPS);
								
		DBRecord unecrypted = result.clone();
				
		RecordEncryption.encryptRecord(result);		
	    DBRecord.add(result);	  

	    AccessLog.log("create aps for stream");
	    
		RecordManager.instance.createAPSForRecord(executingPerson, unecrypted.owner, unecrypted._id, unecrypted.key, apsDirect);
				
		if (targetAPS != null) {
			AccessLog.log("add permission for owner");
			unecrypted.isReadOnly = true;
			RecordManager.instance.getCache(executingPerson).getAPS(result.owner, result.owner).addPermission(unecrypted, false);
		}
				
		RecordManager.instance.applyQueries(executingPerson, unecrypted.owner, unecrypted, targetAPS != null ? targetAPS : unecrypted.owner);
				
		AccessLog.logEnd("end create stream");
		return result;
	}

	private static MidataId getStreamByProperties(MidataId who, MidataId apsId, Map<String, Object> properties, boolean writeableOnly, boolean doNotify)
			throws AppException {
		List<DBRecord> result = QueryEngine.listInternal(RecordManager.instance.getCache(who), apsId, 
				CMaps.map(properties)				     
					 .map("streams", "only")
					 .map("owner", "self")
					 .map("writeable", writeableOnly ? "true" : "false"), RecordManager.INTERNALIDONLY);
		if (result.isEmpty())
			return null;
		DBRecord streamRec = result.get(0);
		if (doNotify) RecordLifecycle.notifyOfChange(streamRec, RecordManager.instance.getCache(who));
		RecordManager.instance.getCache(who).getAPS(streamRec._id, streamRec.key, streamRec.owner);
		return streamRec._id;
	}	
    
}
