package utils.access.index;

import java.util.HashMap;
import java.util.List;


import models.MidataId;
import utils.AccessLog;
import utils.access.DBRecord;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class StreamIndexRoot extends BaseIndexRoot<StreamIndexKey,DBRecord> {

	private IndexDefinition model;
	
	
	public StreamIndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		this.key = key;
		this.model = def;
		this.rootPage = new IndexPage(this.key, def, this, 1);		
		if (isnew) {
			locked = true;
			this.rootPage.initAsRootPage();
		}
		this.btree = new BTree(this, this.rootPage);
		this.loadedPages = new HashMap<MidataId, IndexPage<StreamIndexKey,DBRecord>>();
	}
	
	
	
	public IndexDefinition getModel() {
		return model;
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
	
	
	public List<String> getFormats() {		
		return model.formats;
	}
	
		

	public void addEntry(MidataId aps, DBRecord record) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();
				
		StreamIndexKey key = new StreamIndexKey(aps, record);
		btree.insert(key);						
	}
	
	public void removeEntry(MidataId aps, DBRecord record) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();
		if (record.data == null) throw new NullPointerException();
		
		StreamIndexKey key = new StreamIndexKey(aps, record);
		btree.delete(key);
	}
	


	@Override
	public StreamIndexKey createKey() {
		return new StreamIndexKey();
	}
	
	
	
}
