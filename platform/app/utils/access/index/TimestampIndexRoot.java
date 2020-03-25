package utils.access.index;

import java.util.HashMap;

import models.MidataId;
import utils.AccessLog;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class TimestampIndexRoot extends BaseIndexRoot<TimestampIndexKey,TimestampIndexKey> {

	private IndexDefinition model;
	
	
	public TimestampIndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		this.key = key;
		this.model = def;
		this.rootPage = new IndexPage(this.key, new TimestampIndexPageModel(def), this, 1);		
		if (isnew) {
			locked = true;
			this.rootPage.initAsRootPage();
		}
		this.btree = new BTree(this, this.rootPage);
		this.loadedPages = new HashMap<MidataId, IndexPage<TimestampIndexKey,TimestampIndexKey>>();
	}
	
	
	
	public IndexDefinition getModel() {
		return model;
	}
    		

	public void addEntry(TimestampIndexKey key) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();
				
		//StatsIndexKey key = new StatsIndexKey();
		btree.insert(key);						
	}
	
	public void removeEntry(TimestampIndexKey key) throws InternalServerException, LostUpdateException {
		modCount++;
		
		
		//StatsIndexKey key = new StatsIndexKey();
		btree.delete(key);
	}
	
	public long getValue(String what) throws InternalServerException, LostUpdateException {		
		TimestampIndexKey result = btree.search(new TimestampIndexKey(what, 0));		
		return result != null ? result.value : 0;
	}
	
	public void setValue(String what, long value) throws InternalServerException, LostUpdateException {
		addEntry(new TimestampIndexKey(what, value));
	}

	@Override
	public TimestampIndexKey createKey() {
		return new TimestampIndexKey();
	}		
	
	
}
