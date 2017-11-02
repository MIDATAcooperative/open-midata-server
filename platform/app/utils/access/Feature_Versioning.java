package utils.access;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import models.enums.APSSecurityLevel;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;

public class Feature_Versioning extends Feature {

	private Feature next;
	
	public Feature_Versioning(Feature next) {
		this.next = next;
	}
	
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		List<DBRecord> result = next.query(q);		
		if (q.restrictedBy("version")) {
			List<DBRecord> finalResult = new ArrayList<DBRecord>();
			String version = q.getStringRestriction("version");
			for (DBRecord record : result) {
								
				VersionedDBRecord rec = VersionedDBRecord.getByIdAndVersion(record._id, version, q.getFieldsFromDB());
				if (rec != null) {				
					rec.merge(record);
					
					RecordEncryption.decryptRecord(rec);
					rec.meta.put("ownerName", record.meta.get("ownerName"));
					
	                finalResult.add(rec);
				} else {
					QueryEngine.fetchFromDB(q, record);
					RecordEncryption.decryptRecord(record);
					String vers = record.meta.getString("version", VersionedDBRecord.INITIAL_VERSION);
					if (vers.equals(version)) finalResult.add(record);
				}
								
			}
			return finalResult;
		} else if (q.restrictedBy("history")) {
			List<DBRecord> finalResult = new ArrayList<DBRecord>();			
			for (DBRecord record : result) {
								
				Set<VersionedDBRecord> recs = VersionedDBRecord.getAllById(record._id, q.getFieldsFromDB());
				for (VersionedDBRecord rec : recs) {				
					rec.merge(record);
					
					RecordEncryption.decryptRecord(rec);
					rec.meta.put("ownerName", record.meta.get("ownerName"));
					
	                finalResult.add(rec);
				}
				
				finalResult.add(record);				
								
			}
			return finalResult;
						
		}
		return result;
		
	}


	public static List<DBRecord> historyDate(Query q, List<DBRecord> input) throws AppException {
		Date historyDate = q.getDateRestriction("history-date");
		List<DBRecord> result = new ArrayList<DBRecord>(input.size());
		for (DBRecord record : input) {
			Date lu = record.meta.getDate("lastUpdated");
			if (lu != null && lu.after(historyDate)) {
				Set<VersionedDBRecord> recs = VersionedDBRecord.getAllById(record._id, q.getFieldsFromDB());
				VersionedDBRecord bestRecord = null;
				Date bestDate = null;
				for (VersionedDBRecord rec : recs) {	
                    rec.merge(record);					
					RecordEncryption.decryptRecord(rec);
					Date vlastUpdate = rec.meta.getDate("lastUpdated");
					if (vlastUpdate == null) vlastUpdate = record._id.getCreationDate();
					if (!vlastUpdate.after(historyDate)) {
						if (bestRecord == null) {
							bestRecord = rec;
							bestDate = vlastUpdate;
						} else if (vlastUpdate.after(bestDate)) {
							bestRecord = rec;
							bestDate = vlastUpdate;
						}
				    }
				}
				if (bestRecord != null) {
					bestRecord.meta.put("ownerName", record.meta.get("ownerName"));
					result.add(bestRecord);
				}
			} else result.add(record);
		}
		return result;
	}
	
		
}
