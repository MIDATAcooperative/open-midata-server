package utils.access.index;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import models.MidataId;
import utils.AccessLog;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;
import utils.stats.Stats;

public abstract class BaseIndexRoot<A extends BaseIndexKey<A,B>,B> {

	protected IndexPage<A,B> rootPage;
	protected BTree<A,B> btree;
	protected byte[] key;
	protected boolean locked;
	protected int modCount = 0;
	protected int maxDepth = 0;
	protected Map<MidataId, IndexPage<A,B>> loadedPages;
	
	public final static int MIN_DEGREE          =   100;
	public final static int UPPER_BOUND_KEYNUM  =   (MIN_DEGREE * 2) - 1;
	public final static int LOWER_BOUND_KEYNUM  =   MIN_DEGREE - 1;	
	
	public int getModCount() {
		return modCount;
	}

    public int getMaxDepth() {
    	return maxDepth;
    }

	public long getVersion() {
		return rootPage.getVersion();
	}
	
    public void flush() throws InternalServerException, LostUpdateException {
		
		int modified = 0;		
		for (IndexPage<A,B> page : loadedPages.values()) if (page.changed) modified++;
		if (rootPage.changed) modified += 1;
		
		AccessLog.log("Flushing index root, modifiedPages="+modified+" modCount="+modCount);
		
		if (modified > 1) lockIndex();
		
		if (locked) {
			for (IndexPage<A,B> page : loadedPages.values()) {
				if (page.changed) page.model.updateLock();
			}
		}
		
		for (IndexPage<A,B> page : loadedPages.values()) {
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
	
	public int getEstimatedIndexCoverage(BaseLookup<A> key) {
		return rootPage.getEstimatedIndexCoverage(key);
	}
	
	public void removeOutdated(List<B> records, BaseLookup<A> cond) throws InternalServerException, LostUpdateException {
        Collection<A> matches = rootPage.findEntries(cond);
		
        for (B rec : records) {
			for (A match : matches) {
				if (match.matches(rec)) {
					btree.delete(match);
					modCount++;
				}
			}
        }
								
	}
	
	public Collection<B> lookup(BaseLookup<A> key) throws InternalServerException {
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
	
	public Collection<B> lookup(BaseLookup<A> key, MidataId targetAps) throws InternalServerException {
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
	
	protected static <A extends BaseIndexKey<A,B>,B> IndexPage<A,B> getRightSiblingAtIndex(IndexPage<A,B> parentNode, int keyIdx) throws InternalServerException, LostUpdateException  {
	    return getChildNodeAtIndex(parentNode, keyIdx, 1);
	}

	protected static <A extends BaseIndexKey<A,B>,B> IndexPage<A,B> getLeftSiblingAtIndex(IndexPage<A,B> parentNode, int keyIdx) throws InternalServerException, LostUpdateException  {
	    return getChildNodeAtIndex(parentNode, keyIdx, -1);
	}

	protected static <A extends BaseIndexKey<A,B>,B> IndexPage<A,B> getRightChildAtIndex(IndexPage<A,B> btNode, int keyIdx) throws InternalServerException, LostUpdateException  {
	    return getChildNodeAtIndex(btNode, keyIdx, 1);
	}

	protected static <A extends BaseIndexKey<A,B>,B> IndexPage<A,B> getLeftChildAtIndex(IndexPage<A,B> btNode, int keyIdx) throws InternalServerException, LostUpdateException  {
	    return getChildNodeAtIndex(btNode, keyIdx, 0);
	}

	protected static <A extends BaseIndexKey<A,B>,B> IndexPage<A,B> getChildNodeAtIndex(IndexPage<A,B> btNode, int keyIdx, int nDirection) throws InternalServerException, LostUpdateException  {
	    if (btNode.mIsLeaf) {
	        return null;
	    }
	
	    keyIdx += nDirection;
	    if ((keyIdx < 0) || (keyIdx > btNode.mCurrentKeyNum)) {
	        return null;
	    }
	
	    return btNode.getChild(keyIdx);
	}
	
	public void prepareToCreate() throws InternalServerException {
		rootPage.encrypt();
		rootPage.changed = false;
	}
	
	
			
	public void reload() throws InternalServerException {
		loadedPages.clear();
		rootPage.reload();
		
		locked = false;
		modCount = 0;
	}
	
	public abstract A createKey();
}
