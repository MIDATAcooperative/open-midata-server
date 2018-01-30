package utils.access;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import models.enums.APSSecurityLevel;
import utils.AccessLog;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

public class Feature_Versioning extends Feature {

	private Feature next;
	
	public Feature_Versioning(Feature next) {
		this.next = next;
	}
	
	
	
	
	@Override
	protected Iterator<DBRecord> iterator(Query q) throws AppException {
		Iterator<DBRecord> result = next.iterator(q);		
		if (q.restrictedBy("version")) {
			
			String version = q.getStringRestriction("version");
			
			return new VersionIterator(result, version, q);			
		} else if (q.restrictedBy("history")) {
			
			return new HistoryIterator(result, q);									
		}
		return result;
	}
	
	class HistoryIterator extends Feature.MultiSource<DBRecord> {

		private List<DBRecord> currentlist;
		
		HistoryIterator(Iterator<DBRecord> input, Query q) throws AppException {
			this.query = q;
			init(input);
		}
		
		@Override
		public Iterator<DBRecord> advance(DBRecord record) throws AppException {
			Set<VersionedDBRecord> recs = VersionedDBRecord.getAllById(record._id, query.getFieldsFromDB());
			for (VersionedDBRecord rec : recs) {				
				rec.merge(record);
				
				RecordEncryption.decryptRecord(rec);
				rec.meta.put("ownerName", record.meta.get("ownerName"));
				
                currentlist.add(rec);
			}
			
			currentlist.add(record);
			
			return currentlist.iterator();
		}

		@Override
		public String toString() {
			return "history(["+passed+"] "+chain.toString()+")";
		}
		
		
		
	}

    class VersionIterator implements Iterator<DBRecord> {
    	private Iterator<DBRecord> chain;
    	private String version;
    	private Query q;
    	private DBRecord next;
    	
    	VersionIterator(Iterator<DBRecord> chain, String version, Query q) {
    		this.chain = chain;
    		this.version = version;
    		this.q = q;
    		next();
    	}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public DBRecord next() {
			DBRecord result = next;
			next = null;
			try {
			while (next == null && chain.hasNext()) {
			
			DBRecord record = chain.next();
			VersionedDBRecord rec = VersionedDBRecord.getByIdAndVersion(record._id, version, q.getFieldsFromDB());
			if (rec != null) {				
				rec.merge(record);
				
				RecordEncryption.decryptRecord(rec);
				rec.meta.put("ownerName", record.meta.get("ownerName"));
				
                next = rec;
			} else {
				QueryEngine.fetchFromDB(q, record);
				RecordEncryption.decryptRecord(record);
				String vers = record.meta.getString("version", VersionedDBRecord.INITIAL_VERSION);
				if (vers.equals(version)) next = record;
			}
			}
			return result;
			
			} catch (AppException e) {
				throw new RuntimeException(e);
			}
		}
    	
		@Override
		public String toString() {
			return "version("+chain.toString()+")";
		}
    	
    }

/*
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
*/
/*
	public static List<DBRecord> historyDate(Query q, List<DBRecord> input) throws AppException {
		Date historyDate = q.getDateRestriction("history-date");
		List<DBRecord> result = new ArrayList<DBRecord>(input.size());
		for (DBRecord record : input) {
			Date lu = record.meta.getDate("lastUpdated");
			if (lu != null && lu.after(historyDate)) {
				Set<VersionedDBRecord> recs = VersionedDBRecord.getAllById(record._id, QueryEngine.META_AND_DATA);
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
	*/
	public static class HistoryDate implements Iterator<DBRecord> {

		private Iterator<DBRecord> chain;
		private DBRecord next;
		private Date historyDate;
		
		public HistoryDate(Iterator<DBRecord> chain, Query q) throws BadRequestException {
			this.chain = chain;
			historyDate = q.getDateRestriction("history-date");
			advance();
		}
		
		@Override
		public boolean hasNext() {
			return next != null;
		}

		private void advance() {
			if (!chain.hasNext()) {
				next = null;
				return;
			}
			next = chain.next();
			try {
				Date lu = next.meta.getDate("lastUpdated");
				if (lu != null && lu.after(historyDate)) {
					Set<VersionedDBRecord> recs = VersionedDBRecord.getAllById(next._id, QueryEngine.META_AND_DATA);
					VersionedDBRecord bestRecord = null;
					Date bestDate = null;
					for (VersionedDBRecord rec : recs) {	
	                    rec.merge(next);					
						RecordEncryption.decryptRecord(rec);
						Date vlastUpdate = rec.meta.getDate("lastUpdated");
						if (vlastUpdate == null) vlastUpdate = next._id.getCreationDate();
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
						bestRecord.meta.put("ownerName", next.meta.get("ownerName"));
						next = bestRecord;
					} else advance();
				} 
			} catch (AppException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public DBRecord next() {
            DBRecord result = next;
            advance();
			return result;
		}
		
		
	}
	
		
}
