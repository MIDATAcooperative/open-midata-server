/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.MidataId;
import models.Record;
import models.enums.APSSecurityLevel;
import utils.AccessLog;
import utils.auth.EncryptionNotSupportedException;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class MediumStreamAPS extends APS {
	
	private MidataId apsId;	
	private MidataId owner;
	private byte[] encryptionKey;		
	private List<DBRecord> cachedRecords;
	private final static Map<String, Object> NOTNULL = Collections.unmodifiableMap(Collections.singletonMap("$ne", null));	

	public MediumStreamAPS(MidataId apsId, MidataId owner, byte[] encryptionKey) {
		if (owner == null) throw new NullPointerException();
		this.apsId = apsId;
		this.owner = owner;
		this.encryptionKey = encryptionKey;
	}

	public MidataId getId() {
		return apsId;
	}
	
	public boolean isAccessible() throws AppException {
		return true;
	}
	
	public boolean isUsable() throws AppException {
		return true;
	}
	
	public APSSecurityLevel getSecurityLevel() throws InternalServerException {
		return APSSecurityLevel.MEDIUM;
	}
	
	public void provideRecordKey(DBRecord record) throws AppException {
		record.direct = true;
		record.security = APSSecurityLevel.MEDIUM;
		record.key = encryptionKey;				
	}
	
	public void provideAPSKeyAndOwner(byte[] unlock, MidataId owner) {
		this.owner = owner;
	}
	
	public void touch() throws AppException {
		
	}
	
	public long getLastChanged() throws AppException {
		return 0;
	}

	public void addAccess(Set<MidataId> targets) throws AppException, EncryptionNotSupportedException {
	}

	public void addAccess(MidataId target, byte[] publickey) throws AppException, EncryptionNotSupportedException {
	}		

	public void removeAccess(Set<MidataId> targets) throws InternalServerException {	
	}
	
	public boolean hasAccess(MidataId target) throws InternalServerException {		
		return true;
	}

	public void setMeta(String key, Map<String, Object> data) throws AppException {
		throw new NullPointerException();
	}

	public void removeMeta(String key) throws AppException {
		throw new NullPointerException();
	}

	public BasicBSONObject getMeta(String key) throws AppException {
		throw new NullPointerException();
	}
	
	public MidataId getStoredOwner() throws AppException {
		return owner;		
	}

	@Override
	public List<DBRecord> query(Query q) throws AppException {	
		return ProcessingTools.collect(queryInternal(q));
	}
	
	private DBIterator<DBRecord> queryInternal(Query q) throws AppException {		
		
		List<DBRecord> result = null;
		boolean withOwner = q.returns("owner");
		
		if (q.isStreamOnlyQuery()) return ProcessingTools.empty();
			
		if (q.restrictedBy("quick")) {					
			DBRecord record = (DBRecord) q.getProperties().get("quick");															
			record.key = encryptionKey;
			record.security = APSSecurityLevel.MEDIUM;
			record.context = q.getContext();
			if (withOwner) record.owner = owner; 
							
			return ProcessingTools.singleton(record);
		}
			
		Map<String, Object> query = new HashMap<String, Object>();
		query.put("stream", apsId);
		if (!q.restrictedBy("deleted")) {
			query.put("encryptedData", NOTNULL);
		}
		//boolean useCache = true;
		if (q.restrictedBy("_id")) {
				                				
			Set<MidataId> idRestriction = q.getMidataIdRestriction("_id");
							query.put("_id", idRestriction);
			//useCache = false;
		}
			
		q.addMongoTimeRestriction(query, false);
		List<DBRecord> directResult;
			
		//if (useCache && cachedRecords != null) {
		//		return Collections.unmodifiableList(cachedRecords);
		//}
			
		DBIterator<DBRecord> fromDB = DBRecord.getAllCursor(query, q.getFieldsFromDB());		
		AccessLog.log("direct query stream=", apsId.toString());
			
		// Disabled: Produces wrong results first call to Observation/$lastn after first record inserted
		// if (useCache && withOwner) cachedRecords = directResult;
			
		return new MediumStreamIterator(fromDB, apsId, q.getContext(), encryptionKey, withOwner ? owner : null);				
	}
	
	static class MediumStreamIterator implements DBIterator<DBRecord> {
		private DBIterator<DBRecord> input;
		private MidataId owner;
		private MidataId apsId;
		private byte[] encryptionKey;
		private int count;
		private AccessContext context;
		
		public MediumStreamIterator(DBIterator input, MidataId apsId, AccessContext context, byte[] encryptionKey, MidataId owner) {
			this.input = input;
			this.encryptionKey = encryptionKey;
			this.owner = owner;
			this.apsId = apsId;
			this.context = context;
			count = 0;
		}

		@Override
		public DBRecord next() throws AppException {
			DBRecord record = input.next();
			record.key = encryptionKey;
			record.security = APSSecurityLevel.MEDIUM;
			record.context = context;
			if (owner != null) record.owner = owner;
			count++;
			return record;
		}

		@Override
		public boolean hasNext() throws AppException {
			return input.hasNext();
		}
		
		@Override
		public void close() {
			input.close();			
		}
		
		@Override
		public String toString() {
			return "medium-stream({ aps:"+apsId.toString()+", read:"+count+" })";
		}
		
	}
	
		
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		return queryInternal(q);		
	}
	
	protected boolean satisfies(BasicBSONObject entry, Query q) {
		if (q.getMinDateCreated() != null) {
			Date created = entry.getDate("created");
			if (created != null && created.before(q.getMinDateCreated()))
				return false;
		}
		if (q.getMaxDateCreated() != null) {
			Date created = entry.getDate("created");
			if (created != null && created.after(q.getMaxDateCreated()))
				return false;
		}
		return true;
	}
	

	public void addPermission(DBRecord record, boolean withOwner) throws AppException {
		
	}

	public void addPermission(Collection<DBRecord> records, boolean withOwner) throws AppException {
		
	}

		
	public boolean removePermission(DBRecord record) throws AppException {
		return false;
	}

	public void removePermission(Collection<DBRecord> records) throws AppException {
		
	}
		
	public void clearPermissions() throws AppException {		
		
	}
			

	@Override
	public boolean isReady() throws AppException {
		return true;
	}
	    
	public List<DBRecord> historyQuery(long minUpd, boolean removes) throws AppException {
		return null;
	}
	
	public boolean hasNoDirectEntries() throws AppException {
	   return true;
	}

	@Override
	public Set<MidataId> getAccess() throws AppException {
		throw new NullPointerException();
	}


}
