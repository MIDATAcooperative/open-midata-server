package utils.access.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import utils.AccessLog;
import utils.access.EncryptionUtils;
import utils.access.op.Condition;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

/**
 * Manages a single page of an index
 *
 */
public class IndexPage {

	protected IndexPageModel model;
	protected byte[] key;
	protected boolean changed;
	
	public final static int PAGE_LIMIT = 10000;
	
	protected IndexPage() {}
	
	public IndexPage(byte[] key, IndexPageModel model) throws InternalServerException {
		this.key = key;
		this.model = model;
		if (model.enc != null) decrypt();
	}
	
	
	public IndexPage(byte[] key) throws InternalServerException {
		this.key = key;
		this.model = new IndexPageModel();
		this.model._id = new MidataId();
		
		init();				
		encrypt();
		
		IndexPageModel.add(this.model);
	}
	
	public void setRange(BasicBSONList lk, BasicBSONList hk) {
		this.model.unencrypted.put("lk", lk);
		this.model.unencrypted.put("hk", hk);
	}
	
	public long getVersion() {
		return model.version;
	}
	
	public void addEntry(Comparable<Object>[] key, MidataId aps, MidataId target) throws InternalServerException {
		//if (key[0] == null) return;
		
	    BasicBSONObject entry = findEntry(key);
	    if (entry == null) {
	    	entry = addEntry(key);
	    }
	    //AccessLog.log("add to="+entry.toString()+" key="+key[0].toString());
	    if (!containsRecord(entry, target)) {
	    	addRecord(entry, aps, target);
	    	changed = true;
	    }
	}
	
	public void removeEntry(Comparable<Object>[] key, MidataId target) throws InternalServerException {
		if (key[0] == null) return;
		
	    BasicBSONObject entry = findEntry(key);
	    if (entry == null) return;
	    	
	    if (removeRecord(entry, target)) {
	      ((BasicBSONList) model.unencrypted.get("e")).remove(entry);
	    }
	}
	
	public Collection<IndexMatch> lookup(Condition[] key) throws InternalServerException  {
				
		BasicBSONList entries = findEntries(key);
		if (entries == null) return null;
		
		Collection<IndexMatch> results = new ArrayList<IndexMatch>();
		for (Object o : entries) {
			BasicBSONObject obj = (BasicBSONObject) o;
			IndexMatch match = new IndexMatch();
			match.apsId = new MidataId(obj.get("a").toString());
			match.recordId = new MidataId(obj.get("t").toString());
			results.add(match);
		}
		return results;
	}
	
	public void flush() throws InternalServerException, LostUpdateException {
		if (changed) {
			AccessLog.log("Flushing index");
			encrypt();
			model.update();			
			changed = false;
		}
	}
	
	public void reload() throws InternalServerException {
		model = IndexPageModel.getById(model._id);
		decrypt();
	}
	
	public void init() {
		if (model.unencrypted == null) {
			model.unencrypted = new BasicBSONObject();
			BasicBSONList entries = new BasicBSONList();
			model.unencrypted.put("e", entries);
			model.unencrypted.put("size", 0);
			model.unencrypted.put("ts", new BasicBSONObject());
			changed = true;
		}
	}
	
	protected void encrypt() throws InternalServerException {
		model.enc = EncryptionUtils.encryptBSON(key, model.unencrypted);
	}
	
	protected void decrypt() throws InternalServerException {
		model.unencrypted = EncryptionUtils.decryptBSON(key, model.enc);
		if (model.unencrypted.get("ts") == null) throw new InternalServerException("error.internal", "Failed to load index");		
	}
	
	private BasicBSONObject findEntry(Object[] key) {
		BasicBSONList lst = (BasicBSONList) model.unencrypted.get("e");
		for (Object entry : lst) {
			BasicBSONObject row = (BasicBSONObject) entry;
			BasicBSONList rowkey = (BasicBSONList) row.get("k");
			boolean match = keyCompare(key, rowkey);						
			if (match) return row;
		}
		return null;
	}
	
	private BasicBSONList findEntries(Condition[] key) {
		BasicBSONList result = null;
		boolean cloned = false;
		
		BasicBSONList lst = (BasicBSONList) model.unencrypted.get("e");
		for (Object entry : lst) {
			BasicBSONObject row = (BasicBSONObject) entry;
			BasicBSONList rowkey = (BasicBSONList) row.get("k");
			boolean match = conditionCompare(key, rowkey);						
			if (match) {
				if (result == null) {
					result = (BasicBSONList) row.get("e");
				} else if (cloned) {
					result.addAll((BasicBSONList) row.get("e"));
				} else {
					BasicBSONList old = result;
					result = new BasicBSONList();
					result.addAll(old);
					result.addAll((BasicBSONList) row.get("e"));
					cloned = true;
				}
			}
		}
		return result;
	}
	
	protected void removeFromEntries(Condition[] key, Set<String> ids) throws InternalServerException {
				
		BasicBSONList lst = (BasicBSONList) model.unencrypted.get("e");
		for (Object entry : lst) {
			BasicBSONObject row = (BasicBSONObject) entry;
			BasicBSONList rowkey = (BasicBSONList) row.get("k");
			boolean match = conditionCompare(key, rowkey);						
			if (match) {
				BasicBSONList objs = (BasicBSONList) row.get("e");
				
				for (int i=0;i<objs.size();i++) {
					BasicBSONObject e = (BasicBSONObject) objs.get(i);
					if (ids.contains(e.getString("t"))) {
						objs.remove(i);
						i--;
						changed = true;
					}
				}
			}
		}		
	}
	
	private boolean keyCompare(Object[] key, BasicBSONList idxKey) {
		for (int i=0;i<key.length;i++) {
			if ((key[i] != null) ? (!key[i].equals(idxKey.get(i))) : (idxKey.get(i) != null)) return false;
		}		
		return true;
	}
	
	private boolean conditionCompare(Condition[] key, BasicBSONList idxKey) {
		for (int i=0;i<key.length;i++) {
			if (!key[i].satisfiedBy(idxKey.get(i))) return false;
		}		
		return true;
	}
	
	private boolean containsRecord(BasicBSONObject row, MidataId target) {
	   BasicBSONList entries = (BasicBSONList) row.get("e");
	   if (entries == null) return false;
	   String targetStr = target.toString();
	   for (Object entry : entries) {
		   if (((BasicBSONObject) entry).get("t").equals(targetStr)) return true;
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
	
	private boolean removeRecord(BasicBSONObject row, MidataId target) {
		BasicBSONList objs = (BasicBSONList) row.get("e");
		if (objs == null) return false;
		String t = target.toString();
		for (int i=0;i<objs.size();i++) {
			BasicBSONObject e = (BasicBSONObject) objs.get(i);
			if (t.equals(e.getString("t"))) {
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
	
	public boolean needsSplit() {
		return ((BasicBSONObject) model.unencrypted).getInt("size") > PAGE_LIMIT
				&& ((BasicBSONList) model.unencrypted.get("e")).size() >= 10;
	}
	
	public boolean isNonLeaf() {
		return model.unencrypted.get("p") != null;
	}
	
	
}
