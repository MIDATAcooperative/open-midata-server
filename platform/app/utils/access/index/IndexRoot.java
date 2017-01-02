package utils.access.index;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import utils.AccessLog;
import utils.access.DBRecord;
import utils.access.op.Condition;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

/**
 * Manages one index
 *
 */
public class IndexRoot {

	private IndexDefinition model;
	private IndexPage rootPage;
	private byte[] key;
	private boolean locked;
	private int modCount = 0;
	
	public IndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		this.key = key;
		this.model = def;
		this.rootPage = new IndexPage(this.key, def);		
		if (isnew) {
			locked = true;
			this.rootPage.init();
		}
		if (this.rootPage.isNonLeaf()) this.rootPage = new IndexNonLeafPage(key, def);
	}
	
	public long getVersion() {
		return rootPage.getVersion();
	}
	
	public long getVersion(MidataId aps) {
		return rootPage.getTimestamp(aps.toString());
	}
	
	public void setVersion(MidataId aps, long now) {
		rootPage.setTimestamp(aps.toString(), now);	
	}
	
	public long getAllVersion() {
		return rootPage.getTimestamp("all");
	}
	
	public void setAllVersion(long now) {
		rootPage.setTimestamp("all", now);	
	}
	
	public void flush() throws InternalServerException, LostUpdateException {
		rootPage.model.lockTime = 0;
		rootPage.flush();
		locked = false;
		modCount = 0;
	}
	
	public void prepareToCreate() throws InternalServerException {
		rootPage.encrypt();
		rootPage.changed = false;
	}
	
	public void lockIndex() throws InternalServerException, LostUpdateException {
		if (!locked) {
			rootPage.model.lockTime = System.currentTimeMillis();
			rootPage.model.updateLock();
			locked = true;
		}
	}
	
	public void checkLock() throws InternalServerException {
		if (locked) return;
		while (rootPage.model.lockTime > System.currentTimeMillis() - 1000l * 60l) {
			try {
			  Thread.sleep(500);
			} catch (InterruptedException e) {}
			rootPage.reload();
		}
	}
			
	public void reload() throws InternalServerException {
		rootPage.reload();
		locked = false;
		modCount = 0;
	}
	
	public boolean isChanged() {
		return rootPage.changed;
	}

	public List<String> getFormats() {		
		return model.formats;
	}
	
	
	class EntryInfo {
		MidataId aps;
		DBRecord record;
		Comparable<Object>[] key;
	}

	public void addEntry(MidataId aps, DBRecord record) throws InternalServerException, LostUpdateException {
		modCount++;
		if (modCount > 100) lockIndex();
		
		EntryInfo inf = new EntryInfo();
		inf.aps = aps;
		inf.record = record;
		inf.key = new Comparable[model.fields.size()];		
		extract(0, inf, null,null);				
		if (rootPage.needsSplit()) {
			flush();
			lockIndex();
			rootPage = IndexNonLeafPage.split(this.key, rootPage);				
		}
	}


	private void extract(int keyIdx, EntryInfo inf, BSONObject data, String path) throws InternalServerException {
		if (data == null) {
			if (keyIdx >= model.fields.size()) {
				rootPage.addEntry(inf.key, inf.aps, inf.record._id);
				return;
			}			
			data = inf.record.data;
			path = model.fields.get(keyIdx);
		}
		
		int i = path.indexOf('.');
		if (i > 0) {
			String prefix = path.substring(0, i);
			String remain = path.substring(i+1);
			Object access = data.get(prefix);
			if (access instanceof BasicBSONList) {
				BasicBSONList lst = (BasicBSONList) access;
				if (lst.size() == 0) return;
				for (Object obj : lst) {
					if (obj != null && obj instanceof BSONObject) {
						extract(keyIdx, inf, (BSONObject) obj, remain);
					}
				}				
			} else if (access instanceof BasicBSONObject) {
			   extract(keyIdx, inf, (BasicBSONObject) access, remain);
			} else return;
		} else {
			Object res = data.get(path);
			if (res instanceof BasicBSONList) {
				BasicBSONList lst = (BasicBSONList) res;
				if (lst.size() == 0) return;
				for (Object obj : lst) {
					if (obj instanceof Comparable) {					
					  inf.key[keyIdx] = (Comparable) obj;
					  extract(keyIdx+1, inf, null, null);
					} else {
					  AccessLog.log("Cannot extract path:"+path);
					}					
				}				
			} else {
			  inf.key[keyIdx] = (Comparable) res;
			  extract(keyIdx+1, inf, null, null);
			}
		}
	}
	
	public Collection<IndexMatch> lookup(Condition[] key) throws InternalServerException {
		return rootPage.lookup(key);
	}

	public void removeRecords(Condition[] key, Set<String> ids) throws InternalServerException {
		rootPage.removeFromEntries(key, ids);				
	}

	
}
