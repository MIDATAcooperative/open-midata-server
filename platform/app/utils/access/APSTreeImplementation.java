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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.BasicBSONList;

import models.MidataId;
import models.enums.APSSecurityLevel;
import utils.AccessLog;
import utils.access.index.HistoryIndexKey;
import utils.access.index.HistoryIndexRoot;
import utils.access.index.HistoryLookup;
import utils.access.index.StreamIndexLookup;
import utils.access.index.StreamIndexRoot;
import utils.db.DBLayer;
import utils.db.DBSession;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class APSTreeImplementation extends APSImplementation {

	private StreamIndexRoot streamIndexRoot;
	private HistoryIndexRoot historyIndexRoot;
	private boolean oldStyle;
	private boolean noupdateToNewVersion;
	
	public APSTreeImplementation(EncryptedAPS eaps) {
		super(eaps);		
	}
	
	public APSTreeImplementation(EncryptedAPS eaps, boolean noupdate) {
		super(eaps);		
		this.noupdateToNewVersion = noupdate;
	}
	
	private void ready() throws AppException {
		if (streamIndexRoot==null) {
			if (eaps.isDirect()) oldStyle = true;
			else if (!eaps.getPermissions().containsKey("p")) {
			   streamIndexRoot = new StreamIndexRoot(eaps.getLocalAPSKey(), eaps);
			   oldStyle = false;
			   AccessLog.log("NEW FORMAT");
			} else {
				oldStyle = true;
				AccessLog.log("OLD FORMAT");
				if (!noupdateToNewVersion) {
					noupdateToNewVersion = true;
					upgrade();
				}
			}
		}
	}
	
	private void readyHistory() throws AppException {
		if (historyIndexRoot==null) {
			if (eaps.isDirect()) oldStyle = true;
			else if (!eaps.getPermissions().containsKey("p")) {
				historyIndexRoot = new HistoryIndexRoot(eaps.getLocalAPSKey(), eaps);				
			   AccessLog.log("NEW FORMAT");
			} else {
				oldStyle = true;
				AccessLog.log("OLD FORMAT");
			}
		}
	}
	
	@Override
	protected DBIterator<DBRecord> queryInternal(Query q) throws AppException {
		if (eaps.isDirect()) return super.queryInternal(q);		
		ready();
		if (oldStyle) return super.queryInternal(q);
		merge();
		StreamIndexLookup lookup = new StreamIndexLookup();
		if (q.restrictedBy("app")) lookup.setApp(q.getMidataIdRestriction("app"));
		lookup.setContent(q.getRestrictionOrNull("content"));				
		lookup.setOnlyStreams(q.isStreamOnlyQuery());
		lookup.setMinCreated(q.getMinCreatedTimestamp());
		lookup.setMaxCreated(q.getMaxCreatedTimestamp());
		Collection<DBRecord> result = null;
		Set<String> format = null;
		if (q.restrictedBy("format")) {
			format = q.getRestriction("format");
			if (format.size()==1) {
				lookup.setFormat(format.iterator().next());
			}
		}
		if (q.restrictedBy("_id")) {
			for (MidataId id : q.getMidataIdRestriction("_id")) {
				lookup.setId(id);
				Collection<DBRecord> r = streamIndexRoot.lookup(lookup);
				if (result==null) result = r;
				else result.addAll(r);
			}
		} else if (format != null && format.size()>1) {
			for (String f : format) {
				lookup.setFormat(f);
				Collection<DBRecord> r = streamIndexRoot.lookup(lookup);
				if (result==null) result = r;
				else result.addAll(r);
			}
		} else result = streamIndexRoot.lookup(lookup);	
		for (DBRecord r : result) r.context = q.getContext();
		return new APSIterator(result.iterator(), result.size(), getId());	
	}
	
	
	
	public void addPermission(DBRecord record, boolean withOwner) throws AppException {				
		try {
			forceAccessibleSubset();
			ready();				
			try (DBSession session = DBLayer.startTransaction("aps")) {
				addPermissionInternal(record, withOwner);
				if (streamIndexRoot!=null) streamIndexRoot.flush();
				if (historyIndexRoot !=null) historyIndexRoot.flush();
				eaps.savePermissions();
				session.commit();
			}
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			addPermission(record, withOwner);
		}
	}

	public void addPermission(Collection<DBRecord> records, boolean withOwner) throws AppException {
		try  {
			forceAccessibleSubset();
			ready();			
			try (DBSession session = DBLayer.startTransaction("aps")) {
				for (DBRecord record : records)
					addPermissionInternal(record, withOwner);
				if (streamIndexRoot!=null)  streamIndexRoot.flush();
				if (historyIndexRoot !=null) historyIndexRoot.flush();
				eaps.savePermissions();
				session.commit();
			}
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			addPermission(records, withOwner);
		}
	}
	
	protected void addPermissionInternal(DBRecord record, boolean withOwner) throws AppException, LostUpdateException {
		if (oldStyle) super.addPermissionInternal(record, withOwner);
		else {
			streamIndexRoot.addEntry(record);		
			addHistory(record._id, record.isStream, false);
		}
	}	
	
	protected boolean removePermissionInternal(DBRecord record) throws AppException, LostUpdateException {
        if (oldStyle) return super.removePermissionInternal(record);
        
		if (streamIndexRoot.removeEntry(record)) {
			addHistory(record._id, record.isStream, true);
			return true;
		}
		return false;
        
	}
	
	public boolean removePermission(DBRecord record) throws AppException {
		try {
			ready();
			try (DBSession session = DBLayer.startTransaction("aps")) {
				boolean success = removePermissionInternal(record);
	
				// Store
				if (success) {
					if (streamIndexRoot!=null) streamIndexRoot.flush();
					if (historyIndexRoot !=null) historyIndexRoot.flush();
					eaps.savePermissions();
				}
				session.commit();
				return success;
			}
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			return removePermission(record);
		}
	}

	public void removePermission(Collection<DBRecord> records) throws AppException {
		if (records.isEmpty()) return;
		try {
			ready();
			try (DBSession session = DBLayer.startTransaction("aps")) {
				boolean updated = false;
				for (DBRecord record : records)
					updated = removePermissionInternal(record) || updated;
	
				// Store
				if (updated) {
					if (streamIndexRoot!=null)  streamIndexRoot.flush();
					if (historyIndexRoot !=null) historyIndexRoot.flush();
					eaps.savePermissions();
				}
				
				session.commit();
			}
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			removePermission(records);
		}
	}
		
	public void clearPermissions() throws AppException {
		throw new InternalServerException("error.internal", "Not implemented");		
	}
		
	protected void merge() throws AppException {
		try {
		if (eaps.needsMerge()) {
			ready();
			try (DBSession session = DBLayer.startTransaction("aps")) {
			
				if (oldStyle) {
					super.merge();
					return;
				}
				AccessLog.logBegin("begin merge:",eaps.getId().toString());
				
				for (EncryptedAPS encsubaps : eaps.getAllUnmerged()) {	
					
					APSTreeImplementation temp = new APSTreeImplementation(encsubaps, true);
					
					Collection<DBRecord> recs = temp.query(new Query());
					for (DBRecord record : recs) streamIndexRoot.addEntry(record);	
					
					Collection<DBRecord> histRecs = temp.historyQuery(0, false);
					for (DBRecord histRec : histRecs) historyIndexRoot.addEntry(new HistoryIndexKey(histRec.sharedAt.getTime(), false, histRec.isStream, histRec._id));

				}
				if (streamIndexRoot!=null)  streamIndexRoot.flush();
				if (historyIndexRoot != null) historyIndexRoot.flush();
				eaps.clearUnmerged();
				eaps.savePermissions();				
				session.commit();
			}
			AccessLog.logEnd("end merge");
		}
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			merge();		
		}		
	}
	
	protected void recoverFromLostUpdate() throws InternalServerException {
		super.recoverFromLostUpdate();
		streamIndexRoot = null;		
	}
	
	public boolean hasNoDirectEntries() throws AppException {
		   boolean r = super.hasNoDirectEntries();
		   return r || !eaps.getPermissions().containsKey("idxEnc");		  
	}
	
	protected void addHistory(MidataId recordId, APSSecurityLevel isStream, boolean isRemove) throws AppException, LostUpdateException {
		BasicBSONList history = (BasicBSONList) eaps.getPermissions().get("_history");
		if (history == null) return;
		
		readyHistory();
		if (oldStyle) super.addHistory(recordId, isStream, isRemove);		
		else {		   			
		   HistoryIndexKey hi = new HistoryIndexKey(System.currentTimeMillis(), isRemove, isStream, recordId);
		   historyIndexRoot.addEntry(hi);						
		}
	}
		
    
	public List<DBRecord> historyQuery(long minUpd, boolean removes) throws AppException {
		readyHistory();
		if (oldStyle) return super.historyQuery(minUpd, removes);
		
		HistoryLookup hl = new HistoryLookup(minUpd, removes);
		Collection<HistoryIndexKey> result = historyIndexRoot.lookup(hl);
		List<DBRecord> result2 = new ArrayList<DBRecord>(result.size());
		for (HistoryIndexKey k : result) {
			DBRecord r = new DBRecord();
			r._id = k.getRecordId();
			r.isStream = k.getIsstream();	
			r.sharedAt = new Date(k.getTs());				
			result2.add(r);			
		}
		return result2;
	}
	
	public void upgrade() throws AppException {
		AccessLog.logBegin("upgrade APS to new version");
		try {	
			if (!eaps.getPermissions().containsKey("p")) {
				ready();
				return;
			}
			try (DBSession session = DBLayer.startTransaction("aps")) {
				
				streamIndexRoot = new StreamIndexRoot(eaps.getLocalAPSKey(), eaps);				
				if (eaps.getPermissions().containsKey("_history")) historyIndexRoot = new HistoryIndexRoot(eaps.getLocalAPSKey(), eaps);
				
				Collection<DBRecord> recs = super.query(new Query());
				for (DBRecord record : recs) streamIndexRoot.addEntry(record);	
				
				Collection<DBRecord> histRecs = super.historyQuery(0, false);
				for (DBRecord histRec : histRecs) historyIndexRoot.addEntry(new HistoryIndexKey(histRec.sharedAt.getTime(), false, histRec.isStream, histRec._id));
			
			    streamIndexRoot.flush();
			    if (historyIndexRoot != null) historyIndexRoot.flush();
			
			    Map<String, Object> p = eaps.getPermissions();
			    p.remove("p");
			    
			    if (historyIndexRoot != null) p.put("_history", new BasicBSONList());
			    
			    eaps.savePermissions();				
			    session.commit();
			    oldStyle = false;
			}
		} catch (LostUpdateException e) {
		   recoverFromLostUpdate();
		   upgrade();
		} finally {
		  AccessLog.logEnd("end upgrade APS to new version");
		}
	}

}
