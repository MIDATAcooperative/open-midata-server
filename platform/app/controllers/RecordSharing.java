package controllers;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import utils.DateTimeUtils;
import utils.access.APSCache;
import utils.access.AccessLog;
import utils.access.ComplexQueryManager;
import utils.access.EncryptedAPS;
import utils.access.EncryptionUtils;
import utils.access.Query;
import utils.access.SingleAPSManager;
import utils.auth.EncryptionNotSupportedException;
import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.db.ObjectIdConversion;
import utils.search.Search;
import utils.search.SearchException;

import models.AccessPermissionSet;
import models.FormatInfo;
import models.ModelException;
import models.Record;
import models.enums.APSSecurityLevel;

public class RecordSharing {

	public static RecordSharing instance = new RecordSharing();

	public final static Map<String, Object> FULLAPS = new HashMap<String, Object>();
	public final static Map<String, Object> FULLAPS_WITHSTREAMS = CMaps.map("streams", "true");
	public final static Map<String, Object> FULLAPS_FLAT = CMaps.map("streams", "true").map("flat", "true");
	public final static Set<String> INTERNALIDONLY = Sets.create("_id");
	public final static Set<String> COMPLETE_META = Sets.create("id", "owner",
			"app", "creator", "created", "name", "format", "description");
	public final static Set<String> COMPLETE_DATA = Sets.create("id", "owner",
			"app", "creator", "created", "name", "format", "description",
			"data");
	public final static String STREAM_TYPE = "Stream";
	public final static String QUERY = "_query";

	public final static String KEY_ALGORITHM = "AES";
	public final static String CIPHER_ALGORITHM = "AES";

	public Random rand = new Random(System.currentTimeMillis());

	private static ThreadLocal<APSCache> apsCache = new ThreadLocal<APSCache>();

	public APSCache getCache(ObjectId who) throws ModelException {
		if (apsCache.get() == null)
			apsCache.set(new APSCache(who));
		APSCache result = apsCache.get();
		if (!result.getOwner().equals(who)) throw new ModelException("Owner Change!");
		return result;
	}
	
	public void clear() {
		if (apsCache.get() != null) apsCache.set(null);
	}

	public ObjectId createPrivateAPS(ObjectId who, ObjectId proposedId)
			throws ModelException {

		SecretKey encryptionKey = EncryptionUtils.generateKey(KEY_ALGORITHM);

		EncryptedAPS eaps = new EncryptedAPS(proposedId, who, who,
				APSSecurityLevel.HIGH, encryptionKey.getEncoded());

		try {
			eaps.setKey(
					"owner",
					KeyManager.instance.encryptKey(who,
							encryptionKey.getEncoded()));
			eaps.setSecurityLevel(APSSecurityLevel.HIGH);
		} catch (EncryptionNotSupportedException e) {
			eaps.setSecurityLevel(APSSecurityLevel.NONE);
			eaps.setKey("owner", who.toByteArray());
		}

		eaps.create();

		return eaps.getId();
	}

	public ObjectId createAnonymizedAPS(ObjectId owner, ObjectId other,
			ObjectId proposedId) throws ModelException {

		SecretKey encryptionKey = EncryptionUtils.generateKey(KEY_ALGORITHM);

		EncryptedAPS eaps = new EncryptedAPS(proposedId, owner, owner,
				APSSecurityLevel.HIGH, encryptionKey.getEncoded());

		try {
			eaps.setKey(
					"owner",
					KeyManager.instance.encryptKey(owner,
							encryptionKey.getEncoded()));
			try {
				eaps.setKey(
						other.toString(),
						KeyManager.instance.encryptKey(other,
								encryptionKey.getEncoded()));
			} catch (EncryptionNotSupportedException e2) {
				throw new ModelException("NOT POSSIBLE ENCRYPTION REQUIRED");
			}
		} catch (EncryptionNotSupportedException e) {
			eaps.setKey("owner", owner.toByteArray());
			eaps.setKey(other.toString(), null);
			eaps.setSecurityLevel(APSSecurityLevel.NONE);
		}

		eaps.create();

		return eaps.getId();

	}

	public ObjectId createAPSForRecord(ObjectId executingPerson, ObjectId owner, ObjectId recordId,
			byte[] key, boolean direct) throws ModelException {

		byte[] encryptionKey = key != null ? key : EncryptionUtils.generateKey(
				KEY_ALGORITHM).getEncoded();

		EncryptedAPS eaps = new EncryptedAPS(recordId, executingPerson, owner,
				direct ? APSSecurityLevel.MEDIUM : APSSecurityLevel.HIGH,
				encryptionKey);

		if (key == null) {
			eaps.setKey("owner", owner.toByteArray());
			eaps.setSecurityLevel(APSSecurityLevel.NONE);
		} else {
			try {
				eaps.setKey("owner",
						KeyManager.instance.encryptKey(owner, encryptionKey));
			} catch (EncryptionNotSupportedException e) {
				eaps.setKey("owner", owner.toByteArray());
				eaps.setSecurityLevel(APSSecurityLevel.NONE);
			}
		}

		eaps.create();

		return eaps.getId();
	}

	public void shareAPS(ObjectId apsId, ObjectId ownerId,
			Set<ObjectId> targetUsers) throws ModelException {

		SingleAPSManager apswrapper = getCache(ownerId).getAPS(apsId);
		try {
			apswrapper.addAccess(targetUsers);
		} catch (EncryptionNotSupportedException e) {
			throw new ModelException("Encryption Problem");
		}

	}

	public void unshareAPS(ObjectId apsId, ObjectId ownerId,
			Set<ObjectId> targetUsers) throws ModelException {

		SingleAPSManager apswrapper = getCache(ownerId).getAPS(apsId);
		apswrapper.removeAccess(targetUsers);

	}

	public void share(ObjectId who, ObjectId fromAPS, ObjectId toAPS,
			Set<ObjectId> records, boolean withOwnerInformation)
			throws ModelException {
        AccessLog.debug("share: who="+who.toString()+" from="+fromAPS.toString()+" to="+toAPS.toString()+" count="+(records!=null ? records.size() : "?"));
		SingleAPSManager apswrapper = getCache(who).getAPS(toAPS);
		List<Record> recordEntries = ComplexQueryManager.listInternal(getCache(who), fromAPS,
				records != null ? CMaps.map("_id", records) : RecordSharing.FULLAPS_FLAT,
				Sets.create("_id", "key", "owner", "format", "name"));
		apswrapper.addPermission(recordEntries, withOwnerInformation);
	}

	public void shareByQuery(ObjectId who, ObjectId fromAPS, ObjectId toAPS,
			Map<String, Object> query) throws ModelException {
        AccessLog.debug("shareByQuery who="+who.toString()+" from="+fromAPS.toString()+" to="+toAPS.toString());
		SingleAPSManager apswrapper = getCache(who).getAPS(toAPS);

		query.put("aps", fromAPS.toString());
		apswrapper.setMeta("_query", query);
	}

	public void unshare(ObjectId who, ObjectId apsId, Set<ObjectId> records)
			throws ModelException {

		SingleAPSManager apswrapper = getCache(who).getAPS(apsId);
		List<Record> recordEntries = ComplexQueryManager.list(getCache(who), apsId,
				CMaps.map("_id", records), Sets.create("_id", "format"));
		apswrapper.removePermission(recordEntries);
	}

	public Record createStream(ObjectId executingPerson, ObjectId owner, ObjectId targetAPS, String name,
			boolean direct) throws ModelException {
		AccessLog.debug("Create Stream: who="+executingPerson.toString()+" name="+name+" direct="+direct+" into="+targetAPS);
		Record result = new Record();
		result._id = new ObjectId();
		result.name = name;
		result.owner = owner;
		result.direct = direct;
		result.format = STREAM_TYPE;
		result.created = DateTimeUtils.now();
		result.data = new BasicDBObject();

		addRecord(executingPerson, result, targetAPS);
		return result;
	}

	public ObjectId getStreamByName(ObjectId who, ObjectId apsId, String name)
			throws ModelException {
		List<Record> result = list(who, apsId,
				CMaps.map("format", RecordSharing.STREAM_TYPE)
						.map("name", name), RecordSharing.INTERNALIDONLY);
		if (result.isEmpty())
			return null;
		return result.get(0)._id;
	}

	public Set<String> getStreamsByName(ObjectId who, ObjectId apsId,
			Collection<String> name) throws ModelException {
		return listRecordIds(who, apsId,
				CMaps.map("format", RecordSharing.STREAM_TYPE)
						.map("name", name));
	}
	
	public BSONObject getMeta(ObjectId who, ObjectId apsId, String key) throws ModelException {
		return getCache(who).getAPS(apsId).getMeta(key);
	}
	
	public void setMeta(ObjectId who, ObjectId apsId, String key, Map<String,Object> data) throws ModelException {
		getCache(who).getAPS(apsId).setMeta(key, data);
	}

	/*
	 * private int getTimeFromDate(Date dt) { return (int) (dt.getTime() / 1000
	 * / 60 / 60 / 24 / 7); }
	 */

	public void addDocumentRecord(ObjectId owner, Record record,
			Collection<Record> parts) throws ModelException {
		byte[] key = addRecordIntern(owner, record, false, null);
		if (key == null)
			throw new NullPointerException("no key");
		for (Record rec : parts) {
			rec.document = record._id;
			rec.key = key;
			addRecordIntern(owner, rec, true, null);
		}
	}

	public void addRecord(ObjectId executingPerson, Record record) throws ModelException {
		addRecordIntern(executingPerson, record, false, null);
		record.key = null;
	}
	
	public void addRecord(ObjectId executingPerson, Record record, ObjectId alternateAps) throws ModelException {
		addRecordIntern(executingPerson, record, false, alternateAps);
		record.key = null;
	}

	private byte[] addRecordIntern(ObjectId executingPerson, Record record, boolean documentPart, ObjectId alternateAps) throws ModelException {		
		if (!record.format.equals(RecordSharing.STREAM_TYPE)) {
		  if (record.stream == null) {
			  if (getCache(executingPerson).getAPS(record.owner, record.owner).eaps.isAccessable()) {		 
			     record.stream = RecordSharing.instance.getStreamByName(executingPerson, record.owner, record.format);
			  } else if (alternateAps != null) {
				 record.stream = RecordSharing.instance.getStreamByName(executingPerson, alternateAps, record.format);
			  }
		  }
		  if (record.stream == null && record.format != null) {
			 FormatInfo format = FormatInfo.getByName(record.format);
			 if (getCache(executingPerson).getAPS(record.owner, record.owner).eaps.isAccessable()) {
			    Record stream = RecordSharing.instance.createStream(executingPerson, record.owner, record.owner, record.format, format.security.equals(APSSecurityLevel.MEDIUM));			 			
			    record.stream = stream._id;
			 } else if (alternateAps != null) {
 			    Record stream = RecordSharing.instance.createStream(executingPerson, record.owner, alternateAps, record.format, format.security.equals(APSSecurityLevel.MEDIUM));
				record.stream = stream._id;
				//getCache(executingPerson).getAPS(record.owner, record.owner).addPermission(stream, true);				
			 }
		  }
		}
		AccessLog.debug("Add Record execPerson="+executingPerson.toString()+" format="+record.format+" stream="+(record.stream != null ? record.stream.toString() : "null"));	
		byte[] usedKey = null;
		record.time = Query.getTimeFromDate(record.created); //System.currentTimeMillis() / 1000 / 60 / 60 / 24 / 7;
		
		Record orig = record;
		record = record.clone();
		
		SingleAPSManager apswrapper = null;
		
		boolean apsDirect = false;
		
		if (record.owner.equals(record.creator)) record.creator = null;
		
		if (record.stream != null) {
		  apswrapper = getCache(executingPerson).getAPS(record.stream, record.owner);
		  record.direct = apswrapper.eaps.isDirect();
		  
		} else {		
			if (alternateAps != null) apswrapper = getCache(executingPerson).getAPS(alternateAps);
			else apswrapper = getCache(executingPerson).getAPS(record.owner, record.owner);
		}
	
		if (record.format.equals(STREAM_TYPE)) {
			apsDirect = record.direct;
			record.stream = null;			
			record.direct = false;
		}
		
		if (apswrapper.eaps.getSecurityLevel().equals(APSSecurityLevel.NONE) || apswrapper.eaps.getSecurityLevel().equals(APSSecurityLevel.LOW)) {
			record.key = null;
			if (record.document != null) documentPart = true;
		} else if (record.direct) {
			record.key = apswrapper.eaps.getAPSKey().getEncoded();		
		} else if (!documentPart) {
			if (record.document != null) {
				List<Record> doc = ComplexQueryManager.listInternal(getCache(executingPerson), record.owner, CMaps.map("_id", record.document.toString()), Sets.create("key"));
				if (doc.size() == 1) record.key = doc.get(0).key;
				else throw new ModelException("Document not identified");
				documentPart = true;
			} else  record.key = EncryptionUtils.generateKey(KEY_ALGORITHM).getEncoded();
		}
	    usedKey = record.key;
	    
		try {
		    if (!documentPart) Search.add(record.owner, "record", record._id, record.name, record.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
		
		if (!record.direct && !documentPart) apswrapper.addPermission(record, false);
		
		Record unecrypted = record.clone();
				
		SingleAPSManager.encryptRecord(record, apswrapper.eaps.getSecurityLevel());		
	    Record.add(record);	  
	    		
		if (unecrypted.format.equals(STREAM_TYPE)) {
			RecordSharing.instance.createAPSForRecord(executingPerson, unecrypted.owner, unecrypted._id, unecrypted.key, apsDirect);
		}
		
		if (alternateAps != null && record.stream == null) {
			getCache(executingPerson).getAPS(record.owner, record.owner).addPermission(unecrypted, false);
		}
		
		if (unecrypted.stream == null) { RuleApplication.instance.applyRules(executingPerson, unecrypted.owner, unecrypted, alternateAps != null ? alternateAps : unecrypted.owner); }
		
		return usedKey;	
	}

	public void deleteAPS(ObjectId apsId, ObjectId ownerId)
			throws ModelException {
		// AccessPermissionSet aps = AccessPermissionSet.getById(apsId);
		AccessPermissionSet.delete(apsId);
	}

	public List<Record> list(ObjectId who, ObjectId apsId,
			Map<String, Object> properties, Set<String> fields)
			throws ModelException {
		return ComplexQueryManager.list(getCache(who), apsId, properties, fields);
	}

	public Record fetch(ObjectId who, RecordToken token) throws ModelException {
		return fetch(who, token, RecordSharing.COMPLETE_DATA);
	}

	public Record fetch(ObjectId who, RecordToken token, Set<String> fields)
			throws ModelException {
		List<Record> result = list(who, new ObjectId(token.apsId),
				CMaps.map("_id", new ObjectId(token.recordId)), fields);
		if (result.isEmpty())
			return null;
		else
			return result.get(0);
	}

	public Record fetch(ObjectId who, ObjectId aps, ObjectId recordId)
			throws ModelException {
		List<Record> result = list(who, aps, CMaps.map("_id", recordId),
				RecordSharing.COMPLETE_DATA);
		if (result.isEmpty())
			return null;
		else
			return result.get(0);
	}

	public Set<String> listRecordIds(ObjectId who, ObjectId apsId)
			throws ModelException {
		return listRecordIds(who, apsId, RecordSharing.FULLAPS);
	}

	public Set<String> listRecordIds(ObjectId who, ObjectId apsId,
			Map<String, Object> properties) throws ModelException {
		List<Record> result = list(who, apsId, properties,
				RecordSharing.INTERNALIDONLY);
		Set<String> ids = new HashSet<String>();
		for (Record record : result)
			ids.add(record._id.toString());
		return ids;
	}

}
