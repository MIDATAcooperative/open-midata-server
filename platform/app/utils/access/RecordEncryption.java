package utils.access;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import models.Record;
import models.enums.APSSecurityLevel;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.DateTimeUtils;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * utility functions for encrypting and decryption records.
 *
 */
class RecordEncryption {

	public static void encryptRecord(Record record, APSSecurityLevel lvl) throws AppException {
		if (lvl.equals(APSSecurityLevel.NONE) || lvl.equals(APSSecurityLevel.LOW)) {
			record.clearSecrets();
			return;
		}
		
		if (record.key == null) throw new InternalServerException("error.internal", "Cannot encrypt");
		
		SecretKey encKey = new SecretKeySpec(record.key, EncryptedAPS.KEY_ALGORITHM);
		
		if (record.format == null) throw new InternalServerException("error.internal", "Missing format in record!");
		if (record.content == null) throw new InternalServerException("error.internal", "Missing content in record!");
		
		Map<String, Object> meta = new HashMap<String, Object>();
		meta.put("app", record.app);
		meta.put("creator", record.creator);
		meta.put("name", record.name);
		meta.put("created", record.created);
		meta.put("description", record.description);
		meta.put("tags", record.tags);
		meta.put("format", record.format);
		meta.put("content", record.content);
		
		record.encrypted = EncryptionUtils.encryptBSON(encKey, new BasicBSONObject(meta));
		record.encryptedData = EncryptionUtils.encryptBSON(encKey, record.data);
													
		record.clearEncryptedFields();
	}

	protected static void decryptRecord(Record record) throws AppException {
		//if (record.created != null) return;
				
		if (record.encrypted == null && record.encryptedData == null) return;
		if (record.key == null) AccessLog.decryptFailure(record._id);
		
		SecretKey encKey = new SecretKeySpec(record.key, EncryptedAPS.KEY_ALGORITHM);
		if (record.encrypted != null) {
			try {
		    BSONObject meta = EncryptionUtils.decryptBSON(encKey, record.encrypted);
		    
		    record.app = (ObjectId) meta.get("app");
			record.creator = (ObjectId) meta.get("creator");
			record.name = (String) meta.get("name");				
			record.created = (Date) meta.get("created");
			record.description = (String) meta.get("description");
			String format = (String) meta.get("format");
			if (format!=null) record.format = format;
			String content = (String) meta.get("content");				
			if (content!=null) record.content = content;
			//AccessLog.debug("decrypt cnt="+content+" fmt="+format);
			record.tags = (Set<String>) meta.get("tags");
			} catch (InternalServerException e) {
				AccessLog.debug("Error decrypting record: id="+record._id.toString());
				//throw e;
			}
		}
		
		if (record.encryptedData != null) {
			try {
			record.data = EncryptionUtils.decryptBSON(encKey, record.encryptedData);
			} catch (InternalServerException e) {
				AccessLog.debug("Error decrypting data of record: id="+record._id.toString());
				//throw e;
			}
		}
		
		//if (!record.encrypted.equals("enc"+record.key)) throw new InternalServerException("Cannot decrypt");
	}

}
