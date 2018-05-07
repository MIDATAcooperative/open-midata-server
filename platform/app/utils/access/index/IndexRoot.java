package utils.access.index;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import utils.stats.Stats;

/**
 * Manages one index
 *
 */
public class IndexRoot {

	private IndexDefinition model;
	private IndexPage rootPage;
	private BTree btree;
	private byte[] key;
	private boolean locked;
	private int modCount = 0;
	protected Map<MidataId, IndexPage> loadedPages;
	
	public final static int MIN_DEGREE          =   100;
	public final static int UPPER_BOUND_KEYNUM  =   (MIN_DEGREE * 2) - 1;
	public final static int LOWER_BOUND_KEYNUM  =   MIN_DEGREE - 1;	
	
	public IndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		this.key = key;
		this.model = def;
		this.rootPage = new IndexPage(this.key, def, this);		
		if (isnew) {
			locked = true;
			this.rootPage.initAsRootPage();
		}
		this.btree = new BTree(this, this.rootPage);
		this.loadedPages = new HashMap<MidataId, IndexPage>();
	}
	
	
	
	public IndexDefinition getModel() {
		return model;
	}

    
	

	public int getModCount() {
		return modCount;
	}



	public long getVersion() {
		return rootPage.getVersion();
	}
	
	public long getVersion(MidataId aps) {		
		Long result = rootPage.ts.get(aps.toString());
		return result == null ? 0 : result;
	}
	
	public void setVersion(MidataId aps, long now) {
		rootPage.changed = true;
		rootPage.ts.put(aps.toString(), now);
		
	}
	
	public long getAllVersion() {
		Long result = rootPage.ts.get("all");
		return result == null ? 0 : result;
	}
	
	public void setAllVersion(long now) {
		AccessLog.log("setAllVersion="+now);
		rootPage.changed = true;
		rootPage.ts.put("all", now);			
	}
	
	public void flush() throws InternalServerException, LostUpdateException {
		
		int modified = 0;		
		for (IndexPage page : loadedPages.values()) if (page.changed) modified++;
		if (rootPage.changed) modified += 1;
		
		AccessLog.log("Flushing index root, modifiedPages="+modified+" modCount="+modCount);
		
		if (modified > 1) lockIndex();
		
		if (locked) {
			for (IndexPage page : loadedPages.values()) {
				if (page.changed) page.model.updateLock();
			}
		}
		
		for (IndexPage page : loadedPages.values()) {
            if (locked) page.model.lockTime = System.currentTimeMillis();			
			page.flush();		
		}
		rootPage.model.lockTime = 0;
		if (!rootPage.flush() && locked) {
			rootPage.model.updateLock();
		}
				
		locked = false;
		modCount = 0;
	}
	
	public void prepareToCreate() throws InternalServerException {
		rootPage.encrypt();
		rootPage.changed = false;
	}
	
	public void lockIndex() throws InternalServerException, LostUpdateException {
		if (!locked) {
			AccessLog.log("lock index");
			rootPage.model.lockTime = System.currentTimeMillis();
			rootPage.model.updateLock();
			locked = true;
		}
	}
	
	public void checkLock() throws InternalServerException {
		if (locked) return;
		while (rootPage.model.lockTime > System.currentTimeMillis() - 1000l * 60l) {
			AccessLog.log("waiting for lock release");
			try {
			  Stats.reportConflict();
			  Thread.sleep(50);
			} catch (InterruptedException e) {}
			rootPage.reload();
		}
	}
			
	public void reload() throws InternalServerException {
		loadedPages.clear();
		rootPage.reload();
		
		locked = false;
		modCount = 0;
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
		//if (modCount > 100) lockIndex();
		if (record.data == null) throw new NullPointerException();
		EntryInfo inf = new EntryInfo();
		inf.aps = aps;
		inf.record = record;
		inf.key = new Comparable[model.fields.size()];		
		extract(0, inf, null,null, null, 0, false);				
		
	}
	
	public void removeEntry(DBRecord record) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();
		if (record.data == null) throw new NullPointerException();
		EntryInfo inf = new EntryInfo();		
		inf.record = record;
		inf.aps = record.consentAps;
		inf.key = new Comparable[model.fields.size()];		
		extract(0, inf, null,null, null, 0, true);						
	}
	
	


	private void extract(int keyIdx, EntryInfo inf, BSONObject data, String path, String[] allpath, int pathIdx, boolean remove) throws InternalServerException, LostUpdateException {
		if (data == null) {
			if (allpath != null) {
				if (pathIdx < allpath.length) {			
				  path = allpath[pathIdx];
				  pathIdx++;
				  data = inf.record.data;
				  if (path.equals("null")) {
					  inf.key[keyIdx] = (Comparable) null;
					  extract(keyIdx+1, inf, null, null, null, 0, remove);  
				  }
				} else return;
			} else {
			
				if (keyIdx >= model.fields.size()) {
					if (remove) {
					  btree.delete(new IndexKey(inf.key, inf.record._id, inf.aps));
					  
					} else {
					  btree.insert(new IndexKey(inf.key, inf.record._id, inf.aps));					  
					}
					return;
				}			
				data = inf.record.data;
				allpath = model.getFieldsSplit().get(keyIdx);
				path = allpath[0];
				pathIdx = 1;
			}
			
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
						extract(keyIdx, inf, (BSONObject) obj, remain, allpath, pathIdx, remove);
					}
				}				
			} else if (access instanceof BasicBSONObject) {
			   extract(keyIdx, inf, (BasicBSONObject) access, remain, allpath, pathIdx, remove);
			} else extract(keyIdx, inf, null, null, allpath, pathIdx, remove);;
		} else {
			Object res = data.get(path);
			if (res instanceof BasicBSONList) {
				BasicBSONList lst = (BasicBSONList) res;
				if (lst.size() == 0) return;
				for (Object obj : lst) {
					if (obj instanceof Comparable) {
					  if (obj instanceof String) inf.key[keyIdx] = (Comparable) ((String) obj).toUpperCase();
					  else inf.key[keyIdx] = (Comparable) obj;
					  extract(keyIdx+1, inf, null, null, null, 0, remove);
					} else {
					  AccessLog.log("Cannot extract path:"+path);
					}					
				}				
			} else if (res != null) {
			  if (res instanceof String) inf.key[keyIdx] = (Comparable) ((String) res).toUpperCase();
			  else if (res instanceof Comparable) inf.key[keyIdx] = (Comparable) res;
			  else inf.key[keyIdx] = null;
			  extract(keyIdx+1, inf, null, null, null, 0, remove);
			} else {
			  extract(keyIdx, inf, null, null, allpath, pathIdx, remove);
			}
		}
	}
	
	public Collection<IndexMatch> lookup(Condition[] key) throws InternalServerException {
		try {
		  return rootPage.lookup(key);
		} catch (LostUpdateException e) {
			try {
			   Thread.sleep(20);
			} catch (InterruptedException e2) {}
			rootPage.reload();
			return lookup(key);
		}
	}
	
	public Collection<IndexMatch> lookup(Condition[] key, MidataId targetAps) throws InternalServerException {
		try {
		  return rootPage.lookup(key, targetAps);
		} catch (LostUpdateException e) {
			try {
			   Thread.sleep(20);
			} catch (InterruptedException e2) {}
			rootPage.reload();
			return lookup(key);
		}
	}

	protected static IndexPage getRightSiblingAtIndex(IndexPage parentNode, int keyIdx) throws InternalServerException, LostUpdateException  {
	    return getChildNodeAtIndex(parentNode, keyIdx, 1);
	}

	protected static IndexPage getLeftSiblingAtIndex(IndexPage parentNode, int keyIdx) throws InternalServerException, LostUpdateException  {
	    return getChildNodeAtIndex(parentNode, keyIdx, -1);
	}

	protected static IndexPage getRightChildAtIndex(IndexPage btNode, int keyIdx) throws InternalServerException, LostUpdateException  {
	    return getChildNodeAtIndex(btNode, keyIdx, 1);
	}

	protected static IndexPage getLeftChildAtIndex(IndexPage btNode, int keyIdx) throws InternalServerException, LostUpdateException  {
	    return getChildNodeAtIndex(btNode, keyIdx, 0);
	}

	protected static IndexPage getChildNodeAtIndex(IndexPage btNode, int keyIdx, int nDirection) throws InternalServerException, LostUpdateException  {
	    if (btNode.mIsLeaf) {
	        return null;
	    }
	
	    keyIdx += nDirection;
	    if ((keyIdx < 0) || (keyIdx > btNode.mCurrentKeyNum)) {
	        return null;
	    }
	
	    return btNode.getChild(keyIdx);
	}



	public void removeOutdated(List<IndexMatch> records, Condition[] cond) throws InternalServerException, LostUpdateException {
        Collection<IndexKey> matches = rootPage.findEntries(cond);
		
        for (IndexMatch rec : records) {
			for (IndexKey match : matches) {
				if (match.id.equals(rec.recordId) && match.value.equals(rec.apsId)) {
					btree.delete(match);
					modCount++;
				}
			}
        }
								
	}

	/*
	public void removeRecords(Condition[] key, Set<IndexMatch> ids) throws InternalServerException {
		for (IndexMatch m : ids) {
			btree.delete(new IndexKey(key, m.recordId, m.apsId))
		}
		Collection<IndexKey> entries = rootPage.findEntries(key);
		for (IndexKey k : entries) {
			if (ids.contains(new IndexMatch(MidataId.from(k.getId()), MidataId.from(k.getValue())) {
				
			}
		}			
	}
*/
	
}
