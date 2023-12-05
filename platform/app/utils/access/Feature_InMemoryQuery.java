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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.MidataId;
import models.enums.APSSecurityLevel;
import scala.NotImplementedError;
import utils.auth.EncryptionNotSupportedException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * query a preselected list of records in memory
 *
 */
public class Feature_InMemoryQuery extends APS {
	
	private List<DBRecord> contents;
	private MidataId me = new MidataId();
	private MidataId storedOwner;
	
	public Feature_InMemoryQuery(List<DBRecord> contents) {
		this.contents = contents;
	}
	
	public Feature_InMemoryQuery(MidataId storedOwner, List<DBRecord> contents) {
		this.storedOwner = storedOwner;
		this.contents = contents;
	}
	
	public void setContent(List<DBRecord> recs) {
		contents = recs;
	}
				
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		if (q.restrictedBy("_id")) {
		  Set<MidataId> ids = q.getMidataIdRestriction("_id");
		  List<DBRecord> result = new ArrayList<DBRecord>();
		  for (DBRecord record : contents) {
			  if (ids.contains(record._id)) result.add(record);
		  }
		  return result;
		} else return new ArrayList(contents);
	}
	

	@Override
	public MidataId getId() {
		return me;
	}

	@Override
	public boolean isReady() throws AppException {
		return true;
	}

	@Override
	public boolean isAccessible() throws InternalServerException {
		return true;
	}

	@Override
	public void touch() throws AppException {				
	}

	@Override
	public long getLastChanged() throws AppException {		
		return 0;
	}

	@Override
	public APSSecurityLevel getSecurityLevel() throws InternalServerException {		
		return APSSecurityLevel.HIGH;
	}

	@Override
	public void provideRecordKey(DBRecord record) throws AppException {
		throw new NotImplementedError();
	}

	@Override
	public void addAccess(Set<MidataId> targets) throws AppException, EncryptionNotSupportedException {
		throw new NotImplementedError();
	}

	@Override
	public void addAccess(MidataId target, byte[] publickey) throws AppException, EncryptionNotSupportedException {
		throw new NotImplementedError();
	}

	@Override
	public void removeAccess(Set<MidataId> targets) throws InternalServerException {
		throw new NotImplementedError();
	}

	@Override
	public void setMeta(String key, Map<String, Object> data) throws AppException {
		throw new NotImplementedError();
		
	}

	@Override
	public void removeMeta(String key) throws AppException {
		throw new NotImplementedError();
	}

	@Override
	public BasicBSONObject getMeta(String key) throws InternalServerException {		
		return null;
	}
	
	@Override
	public void addPermission(DBRecord record, boolean withOwner) throws AppException {
		//throw new NotImplementedError();
		
	}

	@Override
	public void addPermission(Collection<DBRecord> records, boolean withOwner) throws AppException {
		throw new NotImplementedError();
		
	}

	@Override
	public boolean removePermission(DBRecord record) throws AppException {
		throw new NotImplementedError();		
	}

	@Override
	public void removePermission(Collection<DBRecord> records) throws AppException {
		throw new NotImplementedError();		
	}

	@Override
	public List<DBRecord> historyQuery(long minUpd, boolean removes) throws AppException {
		throw new NotImplementedError();
	}

	@Override
	public void clearPermissions() throws AppException {
		throw new NotImplementedError();		
	}

	@Override
	public MidataId getStoredOwner() throws AppException {	
		if (this.storedOwner != null) return this.storedOwner;
		throw new NotImplementedError();	
	}

	@Override
	public boolean hasAccess(MidataId target) throws InternalServerException {		
		return false;
	}

	@Override
	public boolean hasNoDirectEntries() throws AppException {
		return contents.isEmpty();
	}

	@Override
	public void provideAPSKeyAndOwner(byte[] unlock, MidataId owner) {				
	}

	@Override
	public boolean isUsable() throws AppException {
		return true;
	}

	@Override
	public Set<MidataId> getAccess() throws AppException {		
		return null;
	}

	@Override
	public void reload() throws InternalServerException {
        // No operation				
	}
	
	

}
