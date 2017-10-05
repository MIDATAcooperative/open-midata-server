package utils.access.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import utils.AccessLog;
import utils.access.EncryptionUtils;
import utils.access.op.Condition;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Manages a single page of an index
 *
 */
public class IndexPage {

	protected IndexPageModel model;
	protected IndexRoot root;
	protected byte[] key;
	protected boolean changed;
	protected boolean childsChanged;
	
	protected boolean mIsLeaf;
    protected int mCurrentKeyNum;
    protected IndexKey mKeys[];  
    protected MidataId mChildren[];
    protected Map<String, Long> ts;
    
    	
	
	protected IndexPage() {}
	
	public IndexPage(byte[] key, IndexPageModel model, IndexRoot root) throws InternalServerException {
		this.key = key;
		this.model = model;
		this.root = root;
		if (model.enc != null) decrypt();
	}
	
	
	public IndexPage(byte[] key, IndexRoot root) throws InternalServerException {
		this.key = key;
		this.model = new IndexPageModel();
		this.model._id = new MidataId();
		this.root = root;
		
		init();				
		encrypt();
		
		IndexPageModel.add(this.model);
	}
	
	public void copyFrom(IndexPage other) {
		this.mIsLeaf = other.mIsLeaf;
	    this.mCurrentKeyNum = other.mCurrentKeyNum;
	    System.arraycopy(other.mKeys, 0, this.mKeys, 0, other.mKeys.length);
	    System.arraycopy(other.mChildren, 0, this.mChildren, 0, other.mChildren.length);    
	    this.changed = true;
	}
	
	public IndexPage getChild(int idx) throws InternalServerException, LostUpdateException {
		MidataId child = mChildren[idx];
		if (child == null) return null;
		
		
		IndexPage loaded = root.loadedPages.get(child);
		if (loaded != null) return loaded;
				
		loaded = new IndexPage(this.key, IndexPageModel.getById(child), root);
		if (loaded.model.lockTime > root.getVersion()) throw new LostUpdateException();
		root.loadedPages.put(child, loaded);
		return loaded;
	}
	
	public MidataId getId() {
		return model._id;
	}
		
	/*
	public void setRange(BasicBSONList lk, BasicBSONList hk) {
		this.model.unencrypted.put("lk", lk);
		this.model.unencrypted.put("hk", hk);
	}
	*/
	
	public long getVersion() {
		return model.version;
	}
	
	/*
	public void addEntry(Comparable<Object>[] key, MidataId aps, MidataId target) throws InternalServerException {
		//if (key[0] == null) return;
		
	    BasicBSONObject entry = findEntry(key);
	    if (entry == null) {
	    	entry = addEntry(key);
	    }
	    //AccessLog.log("add to="+entry.toString()+" key="+key[0].toString());
	    if (!containsRecord(entry, target, aps)) {
	    	addRecord(entry, aps, target);
	    	changed = true;
	    }
	}
	
	public void removeEntry(Comparable<Object>[] key, MidataId target, MidataId aps) throws InternalServerException {
		if (key[0] == null) return;
		
	    BasicBSONObject entry = findEntry(key);
	    if (entry == null) return;
	    	
	    if (removeRecord(entry, target, aps)) {
	      ((BasicBSONList) model.unencrypted.get("e")).remove(entry);
	    }
	}
	*/
	
	public Collection<IndexMatch> lookup(Condition[] key) throws InternalServerException, LostUpdateException  {
        long t = System.currentTimeMillis();				
		Collection<IndexKey> entries = findEntries(key);
		if (entries == null) return null;
		
		Collection<IndexMatch> results = new ArrayList<IndexMatch>(entries.size());
		for (IndexKey o : entries) {
			results.add(new IndexMatch(MidataId.from(o.getId()), MidataId.from(o.value)));			
		}
		AccessLog.log("lookup:"+(System.currentTimeMillis() - t));
		return results;
	}

	public boolean flush() throws InternalServerException, LostUpdateException {
		if (changed) {
			AccessLog.log("Flushing index page");
			encrypt();
			model.update();			
			changed = false;
			return true;
		}
		return false;
		
	}
	
	public void reload() throws InternalServerException {
		model = IndexPageModel.getById(model._id);
		decrypt();
		
	}
	
	public void init() {
		if (mKeys == null) {
		  mIsLeaf = true;
          mCurrentKeyNum = 0;
          mKeys = new IndexKey[IndexRoot.UPPER_BOUND_KEYNUM];
          mChildren = new MidataId[IndexRoot.UPPER_BOUND_KEYNUM + 1];        
		  changed = true;
		}
	}
	
	public void initAsRootPage() {
		init();
		this.ts = new HashMap<String, Long>();
	}
	
	public void initNonLeaf() {		
		mIsLeaf = false;
        mCurrentKeyNum = 0;
        mKeys = new IndexKey[IndexRoot.UPPER_BOUND_KEYNUM];
        mChildren = new MidataId[IndexRoot.UPPER_BOUND_KEYNUM + 1];        
		changed = true;		
	}
	
	protected void encrypt() throws InternalServerException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			AccessLog.log("encrypt:"+mIsLeaf+" "+mCurrentKeyNum+" ts="+ts);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeBoolean(mIsLeaf);
			oos.writeInt(mCurrentKeyNum);			
			for (int i=0;i<mCurrentKeyNum;i++) {
				oos.writeObject(mKeys[i]);
			}
			if (!mIsLeaf) {
				for (int i=0;i<=mCurrentKeyNum;i++) {
					oos.writeUTF(mChildren[i].toString());
				}
			}
			
			oos.writeObject(ts);
			oos.close();
		} catch (IOException e) {
			
		}
		model.enc = EncryptionUtils.encrypt(key, bos.toByteArray());
	}
	
	protected void decrypt() throws InternalServerException {
		byte[] data = EncryptionUtils.decrypt(key, model.enc);
		try {
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
			mIsLeaf = in.readBoolean();
			mCurrentKeyNum = in.readInt();
			mKeys = new IndexKey[IndexRoot.UPPER_BOUND_KEYNUM];
	        mChildren = new MidataId[IndexRoot.UPPER_BOUND_KEYNUM + 1];       
			
			for (int i=0;i<mCurrentKeyNum;i++) {
				mKeys[i] = (IndexKey) in.readObject();
			}
			
			if (!mIsLeaf) {
				for (int i=0;i<=mCurrentKeyNum;i++) {
					mChildren[i] = MidataId.from(in.readUTF());
				}
			}
			
			this.ts = (Map<String, Long>) in.readObject();
			
			AccessLog.log("decrypt:"+mIsLeaf+" "+mCurrentKeyNum+" ts="+ts);
		} catch (IOException e) {
			AccessLog.logException("IOException", e);
			throw new InternalServerException("error.internal", "IOException"); }
		catch (ClassNotFoundException e2) { throw new InternalServerException("error.internal", "ClassNotFoundException");}
	}
			
	protected Collection<IndexKey> findEntries(Condition[] key) throws InternalServerException, LostUpdateException {
		Collection<IndexKey> result = new ArrayList<IndexKey>();
		
		
		if (mIsLeaf) {
			for (int i=0;i<mCurrentKeyNum;i++)  {			
				boolean match = conditionCompare(key, mKeys[i].getKey());						
				if (match) {
					result.add(mKeys[i]);
				}			
			}
		} else {
				
			for (int i=0;i<=mCurrentKeyNum;i++) {	
				
				boolean match = conditionCompare(key, i==0 ? null : mKeys[i-1].getKey(), i == mCurrentKeyNum ? null : mKeys[i].getKey());						
				if (match) {
					IndexPage c = getChild(i);
					result.addAll(c.findEntries(key));				
				}
			}
		}
		return result;
	}
		
	
	private boolean keyCompare(Object[] key, Comparable[] idxKey) {
		for (int i=0;i<key.length;i++) {
			if ((key[i] != null) ? (!key[i].equals(idxKey[i])) : (idxKey[i] != null)) return false;
		}		
		return true;
	}
	
	private boolean conditionCompare(Condition[] key, Comparable[] idxKey) {
		for (int i=0;i<key.length;i++) {
			if (!key[i].satisfiedBy(idxKey[i])) return false;
		}		
		return true;
	}
	
	private boolean conditionCompare(Condition[] cond, Comparable[] lowkey, Comparable[] highkey) {
		for (int i=0;i<cond.length;i++) {
			if (!cond[i].isInBounds(lowkey==null ? null : lowkey[i],  highkey == null ? null: highkey[i]))  return false;
		}		
		return true;
	}
	
	/*
	private boolean containsRecord(BasicBSONObject row, MidataId target, MidataId aps) {
	   BasicBSONList entries = (BasicBSONList) row.get("e");
	   if (entries == null) return false;
	   String targetStr = target.toString();
	   String apsStr = aps.toString();
	   for (Object entry : entries) {
		   if (((BasicBSONObject) entry).get("t").equals(targetStr) &&
			  ((BasicBSONObject) entry).get("a").equals(apsStr)) return true;
	   }
	   return false;
	}
	
	protected BasicBSONObject addEntry(Object[] key) {
		BasicBSONList lst = (BasicBSONList) model.unencrypted.get("e");
		BasicBSONObject entry = new BasicBSONObject();
		BasicBSONList idxkey = new BasicBSONList();
		entry.put("k", idxkey);
		for (Object o : key) idxkey.add(o);
		lst.add(entry);		
		return entry;
	}
	
	private void addRecord(BasicBSONObject row, MidataId aps, MidataId target) {
			  
	  BasicBSONList entries = (BasicBSONList) row.get("e");
	  if (entries == null) {
		  entries = new BasicBSONList();
		  row.put("e", entries);
	  }
	  BasicBSONObject entry = new BasicBSONObject();
	  entry.put("t", target.toString());
	  entry.put("a", aps.toString());
	  entries.add(entry);
	  
	  model.unencrypted.put("size", ((BasicBSONObject) model.unencrypted).getInt("size") + 1);
	}
	
	private boolean removeRecord(BasicBSONObject row, MidataId target, MidataId aps) {
		BasicBSONList objs = (BasicBSONList) row.get("e");
		if (objs == null) return false;
		String t = target.toString();
		String a = aps.toString();
		for (int i=0;i<objs.size();i++) {
			BasicBSONObject e = (BasicBSONObject) objs.get(i);
			if (t.equals(e.getString("t")) && a.equals(e.getString("a"))) {
				objs.remove(i);		
				changed = true;
				return objs.size() == 0;					
			}
		}	
		return false;
	}
	
	protected void setTimestamp(String key, long value) {
	   BasicBSONObject tslist = (BasicBSONObject) model.unencrypted.get("ts");
	   tslist.put(key, value);
	   changed = true;
	}
	
	protected long getTimestamp(String key) {
		BasicBSONObject tslist = (BasicBSONObject) model.unencrypted.get("ts");				
		Object v = tslist.get(key);
		if (v == null) return -1;
		return ((Long) v);
	}
		
	
	*/
}
