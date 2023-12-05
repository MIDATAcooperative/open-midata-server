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

package utils.access.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.MidataId;
import utils.AccessLog;
import utils.access.EncryptionUtils;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

/**
 * Manages a single page of an index
 *
 */
public class IndexPage<A extends BaseIndexKey<A,B>,B> {

	protected BaseIndexPageModel model;
	protected BaseIndexRoot<A,B> root;
	protected byte[] key;
	protected boolean changed;
	protected boolean childsChanged;
	
	protected boolean mIsLeaf;
    protected int mCurrentKeyNum;
    protected A mKeys[];  
    protected MidataId mChildren[];
    //protected Map<String, Long> ts;
    protected int depth;
    	
	
	protected IndexPage() {}
	
	public IndexPage(byte[] key, BaseIndexPageModel model, BaseIndexRoot<A,B> root, int depth) throws InternalServerException {
		this.key = key;
		this.model = model;
		this.root = root;
		this.depth = depth;
		if (root.maxDepth < depth) root.maxDepth = depth;
		if (model.getEnc() != null) decrypt();
	}
	
	
	public IndexPage(byte[] key, BaseIndexRoot<A,B> root) throws InternalServerException {
		this.key = key;
		
		this.model = root.createPage();		
		this.root = root;
		this.depth = 1;
		if (root.maxDepth < depth) root.maxDepth = depth;
		init();				
		encrypt();
		
		this.model.add();
	}
	
	public void copyFrom(IndexPage<A,B> other) {
		this.mIsLeaf = other.mIsLeaf;
	    this.mCurrentKeyNum = other.mCurrentKeyNum;
	    System.arraycopy(other.mKeys, 0, this.mKeys, 0, other.mKeys.length);
	    System.arraycopy(other.mChildren, 0, this.mChildren, 0, other.mChildren.length);    
	    this.changed = true;
	}
	
	public IndexPage<A,B> getChild(int idx) throws InternalServerException, LostUpdateException {
		MidataId child = mChildren[idx];
		if (child == null) return null;
		
		
		IndexPage<A,B> loaded = root.loadedPages.get(child);
		if (loaded != null) return loaded;
				
		loaded = new IndexPage(this.key, model.loadChildById(child), root, this.depth + 1);
		if (loaded.model.getLockTime() > root.getVersion()) throw new LostUpdateException();
		root.loadedPages.put(child, loaded);
		
		return loaded;
	}
	
	public void loadMultipleChilds(Collection<Integer> idxs) throws InternalServerException, LostUpdateException {
		if (idxs.size() < 2) return;
		Set<MidataId> toload = new HashSet<MidataId>(idxs.size());
		for (Integer id : idxs) {
			MidataId child = mChildren[id];
			if (child == null) continue;
			if (!root.loadedPages.containsKey(child)) toload.add(child);
		}
		if (toload.isEmpty()) return;
		Set<? extends BaseIndexPageModel> result = model.getMultipleById(toload);
		for (BaseIndexPageModel r : result) {
			IndexPage<A,B> loaded = new IndexPage<A,B>(this.key, r, root, this.depth + 1);
			if (loaded.model.getLockTime() > root.getVersion() && loaded.model.getLockTime() > System.currentTimeMillis() - 1000l * 60l * 5l ) {
				AccessLog.log("LOST UPDATE: "+loaded.model.getLockTime()+" vs "+root.getVersion());
				throw new LostUpdateException();
			}
			root.loadedPages.put(r.getId(), loaded);		
		}
	}
	
	public MidataId getId() {
		return model.getId();
	}
			
	
	public long getVersion() {
		return model.getVersion();
	}
		
	
	public Collection<B> lookup(BaseLookup<A> key) throws InternalServerException, LostUpdateException  {
        long t = System.currentTimeMillis();				
		Collection<A> entries = findEntries(key);
		if (entries == null) return null;
		
		Collection<B> results = new ArrayList<B>(entries.size());
		for (A o : entries) {
			results.add(o.toMatch());			
		}		
		return results;
	}
	
	public Collection<B> lookup(BaseLookup<A> key, MidataId targetAps) throws InternalServerException, LostUpdateException  {
        long t = System.currentTimeMillis();				
		Collection<A> entries = findEntries(key);
		if (entries == null) return null;
		
		Collection<B> results = new ArrayList<B>(entries.size());
		for (A o : entries) {
			if (((IndexKey) o).value.equals(targetAps)) results.add(o.toMatch());			
		}		
		return results;
	}

	public boolean flush() throws InternalServerException, LostUpdateException {
		if (changed) {
			//AccessLog.log("Flushing index page");
			encrypt();
			model.update();			
			changed = false;
			return true;
		}
		return false;
		
	}
	
	public void reload() throws InternalServerException {
		model = model.reload();
		decrypt();
		
	}
	
	public void init() {
		if (mKeys == null) {
		  mIsLeaf = true;
          mCurrentKeyNum = 0;
          mKeys = (A[]) new BaseIndexKey[root.UPPER_BOUND_KEYNUM()];
          mChildren = new MidataId[root.UPPER_BOUND_KEYNUM() + 1];        
		  changed = true;
		}
	}
	
	public void initAsRootPage() {
		init();	
	}
	
	public void initNonLeaf() {		
		mIsLeaf = false;
        mCurrentKeyNum = 0;
        mKeys = (A[]) new BaseIndexKey[root.UPPER_BOUND_KEYNUM()];
        mChildren = new MidataId[root.UPPER_BOUND_KEYNUM() + 1];        
		changed = true;		
	}
	
	protected void encrypt() throws InternalServerException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {			
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeBoolean(mIsLeaf);			
			oos.writeInt(mCurrentKeyNum);
			A last = null;
			for (int i=0;i<mCurrentKeyNum;i++) {
				mKeys[i].writeObject(oos, last);
				last = mKeys[i];
			}
			if (!mIsLeaf) {
				for (int i=0;i<=mCurrentKeyNum;i++) {
					oos.writeUTF(mChildren[i].toString());
				}
			}						
			oos.close();
			bos.close();
			model.setEnc(model.encrypt(key, bos.toByteArray()));
		} catch (IOException e) {
			throw new InternalServerException("error.internal", e);
		}
		
	}
	
	protected void decrypt() throws InternalServerException {
		byte[] data = model.decrypt(key, model.getEnc());
		try {
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
			mIsLeaf = in.readBoolean();
			mCurrentKeyNum = in.readInt();		
			mKeys = (A[]) new BaseIndexKey[root.UPPER_BOUND_KEYNUM()];
	        mChildren = new MidataId[root.UPPER_BOUND_KEYNUM() + 1];       
			A last = null;
			for (int i=0;i<mCurrentKeyNum;i++) {
				mKeys[i] = root.createKey();
				mKeys[i].readObject(in, last);
				last = mKeys[i];				
			}
			
			if (!mIsLeaf) {
				for (int i=0;i<=mCurrentKeyNum;i++) {
					mChildren[i] = MidataId.from(in.readUTF());
				}
			}
			in.close();
			
		} catch (IOException e) {
			AccessLog.logException("IOException", e);
			throw new InternalServerException("error.internal", "IOException"); }
		catch (ClassNotFoundException e2) { throw new InternalServerException("error.internal", "ClassNotFoundException");}
	}
			
	protected int getEstimatedIndexCoverage(BaseLookup<A> key) {
		if (mIsLeaf || mCurrentKeyNum == 0) return 0;
		int work = 0;
		for (int i=0;i<=mCurrentKeyNum;i++) {				
			if (key.conditionCompare(i==0 ? null : mKeys[i-1], i == mCurrentKeyNum ? null : mKeys[i])) work++;									
		}
		return 100 * work / mCurrentKeyNum;
	}
	
	protected Collection<A> findEntries(BaseLookup<A> key) throws InternalServerException, LostUpdateException {
		Collection<A> result = new ArrayList<A>();
		
		
	
			
			for (int i=0;i<mCurrentKeyNum;i++)  {			
				boolean match = key.conditionCompare(mKeys[i]);						
				if (match) {					
					result.add(mKeys[i]);
				} 
			}
		
		  if (!mIsLeaf) {
			List<Integer> ids = null;
			int matchId = -1;
			
			for (int i=0;i<=mCurrentKeyNum;i++) {	
			  	
				boolean match = key.conditionCompare(i==0 ? null : mKeys[i-1], i == mCurrentKeyNum ? null : mKeys[i]);						
				if (match) {
			
					if (matchId == -1) matchId = i;
					else {
						if (ids == null) {
							ids = new ArrayList<Integer>();
							ids.add(matchId);
						}
						ids.add(i);
					}							
				}
			}
			if (ids != null) {
				loadMultipleChilds(ids);
				for (Integer id : ids) result.addAll(getChild(id).findEntries(key));
			} else if (matchId >= 0) result.addAll(getChild(matchId).findEntries(key));
		}
		return result;
	}
							
}
