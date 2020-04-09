package utils.access.index;

import java.util.HashMap;

import models.MidataId;
import models.RecordsInfo;
import utils.AccessLog;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class StatsIndexRoot extends TsBaseIndexRoot<StatsIndexKey,StatsIndexKey> {

	private IndexDefinition model;
	
	
	public StatsIndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		super(key,def,isnew);
		this.key = key;
		this.model = def;
		this.rootPage = new IndexPage(this.key, def, this, 1);		
		if (isnew) {
			locked = true;
			this.rootPage.initAsRootPage();
		}
		this.btree = new BTree(this, this.rootPage);
		this.loadedPages = new HashMap<MidataId, IndexPage<StatsIndexKey,StatsIndexKey>>();
	}
	
	
	
	public IndexDefinition getModel() {
		return model;
	}
    		

	public void addEntry(StatsIndexKey key) throws InternalServerException, LostUpdateException {
		if (key.aps==null) throw new NullPointerException();
		modCount++;
		//if (modCount > 100) lockIndex();
				
		//StatsIndexKey key = new StatsIndexKey();
		btree.insert(key);						
	}
	
	public void removeEntry(StatsIndexKey key) throws InternalServerException, LostUpdateException {
		modCount++;
		
		
		//StatsIndexKey key = new StatsIndexKey();
		btree.delete(key);
	}
	


	@Override
	public StatsIndexKey createKey() {
		return new StatsIndexKey();
	}
	
	
}
