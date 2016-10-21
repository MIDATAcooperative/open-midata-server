package utils.access;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.enums.APSSecurityLevel;
import utils.AccessLog;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * utility functions for encrypting and decryption records.
 *
 */
class RecordEncryption {

	/**
	 * encrypt a record using the key in record.key
	 * @param record the record to encrypt
	 * @param lvl the security level of the APS where the record will be added
	 * @throws AppException
	 */
	public static void encryptRecord(DBRecord record) throws AppException {
		if (record.security == null) throw new InternalServerException("error.internal", "Missing encryption level");
		
		if (record.security.equals(APSSecurityLevel.NONE) || record.security.equals(APSSecurityLevel.LOW)) {
			record.clearSecrets();
			return;
		}
		
		if (record.key == null) throw new InternalServerException("error.internal", "Cannot encrypt");
						
		if (record.meta != null) {
			if (!record.meta.containsField("format")) throw new InternalServerException("error.internal", "Missing format in record!");
		    if (!record.meta.containsField("content")) throw new InternalServerException("error.internal", "Missing content in record!");			
		    record.encrypted = EncryptionUtils.encryptBSON(record.key, record.meta);
		} else record.encrypted = null;
		record.encryptedData = record.data != null ? EncryptionUtils.encryptBSON(record.key, record.data): null;
		record.encWatches = record.watches != null ? EncryptionUtils.encryptBSON(record.key, record.watches) : null;
													
		record.clearEncryptedFields();
	}

	/**
	 * decrypt a record using the key in record.key
	 * @param record the record to decrypt
	 * @throws AppException
	 */
	protected static void decryptRecord(DBRecord record) throws AppException {
		//if (record.created != null) return;
				
		if (record.encrypted == null && record.encryptedData == null && record.encWatches == null) return;
		if (record.security != null && record.security.equals(APSSecurityLevel.NONE)) return;
		if (record.key == null) AccessLog.decryptFailure(record._id);
				
		if (record.encrypted != null && !record.meta.containsField("name")) {
			try {
		       record.meta = (BasicBSONObject) EncryptionUtils.decryptBSON(record.key, record.encrypted);		    		    
			} catch (InternalServerException e) {
				AccessLog.log("Error decrypting record: id="+record._id.toString());
				//throw e;
			}
		}
		
		if (record.encryptedData != null) {
			try {
			record.data = EncryptionUtils.decryptBSON(record.key, record.encryptedData);
			} catch (InternalServerException e) {
				AccessLog.log("Error decrypting data of record: id="+record._id.toString());
				//throw e;
			}
		}
		
		if (record.encWatches != null) {
			try {
			   BSONObject r = EncryptionUtils.decryptBSON(record.key, record.encWatches);
			   record.watches = new BasicBSONList();
			   record.watches.putAll(r);
			} catch (InternalServerException e) {
				AccessLog.log("Error decrypting watches of record: id="+record._id.toString());
					//throw e;
			}
		}
		
		
	}

}
