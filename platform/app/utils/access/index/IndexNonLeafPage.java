package utils.access.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

import utils.AccessLog;
import utils.access.op.Condition;
import utils.access.op.EqualsSingleValueCondition;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class IndexNonLeafPage extends IndexPage {

	private Map<ObjectId, IndexPage> loadedChilds;
	
	public IndexNonLeafPage(byte[] key, IndexPageModel model) throws InternalServerException {
		model.enc = null; 
		this.key = key;
		this.model = model;
		loadedChilds = new HashMap<ObjectId, IndexPage>();
	}
	
	public static IndexNonLeafPage split(byte[] enckey, IndexPage oldPage) throws InternalServerException {
		
		try {
			AccessLog.logBegin("start index split");
			
			IndexPageModel old = oldPage.model;
			IndexNonLeafPage splitted = new IndexNonLeafPage(enckey, old);
			//List<>
			
			// Create list with ordered map key -> entries
			// split map in X parts
			SortedMap<IndexKey, BasicBSONList> entries = new TreeMap<IndexKey, BasicBSONList>();
						
			BasicBSONList lst = (BasicBSONList) old.unencrypted.get("e");
			
			AccessLog.log("lst size="+lst.size());
			
			for (Object entry : lst) {
				BasicBSONObject row = (BasicBSONObject) entry;
				BasicBSONList rowkey = (BasicBSONList) row.get("k");
				entries.put(new IndexKey(rowkey), (BasicBSONList) row.get("e"));
			}
			
			int total = entries.size();
			IndexPage current = new IndexPage(enckey);
			splitted.loadedChilds.put(current.model._id, current);
			BasicBSONList low = null;
			int idx = 0;
			int limit = total / 10;
			
			AccessLog.log("total = "+total+" limit="+limit);
			
			splitted.model.unencrypted.put("p", new BasicBSONList());
			
			for (Map.Entry<IndexKey, BasicBSONList> entry : entries.entrySet()) {
				idx++;
				if (idx > limit) {
					splitted.addEntry(low, entry.getKey().getKey(), current.model._id);
					current.setRange(low, entry.getKey().getKey());
					current = new IndexPage(enckey);
					splitted.loadedChilds.put(current.model._id, current);
					low = entry.getKey().getKey();
					idx = 0;
				}
								
				current.addEntry(entry.getKey().getKey().toArray()).put("e", entry.getValue());				
			}
			splitted.addEntry(low, null, current.model._id);
			current.setRange(low, null);
			
			splitted.model.unencrypted.removeField("e");
			
			AccessLog.log("pages in memory:"+splitted.loadedChilds.size());
			splitted.flush();
			
			AccessLog.logEnd("end index split");
			
			return splitted;
		} catch (LostUpdateException e) {
			oldPage.reload();
			return split(enckey, oldPage);
		}
		
	}
	
   public void splitInto(BasicBSONList plowkey, BasicBSONList phighkey, IndexPage oldPage) throws InternalServerException {
		
		try {
			AccessLog.logBegin("start index split");
			
			IndexPageModel old = oldPage.model;
			
			//List<>
			
			// Create list with ordered map key -> entries
			// split map in X parts
			SortedMap<IndexKey, BasicBSONList> entries = new TreeMap<IndexKey, BasicBSONList>();
						
			BasicBSONList lst = (BasicBSONList) old.unencrypted.get("e");
			
			AccessLog.log("lst size="+lst.size());
			
			for (Object entry : lst) {
				BasicBSONObject row = (BasicBSONObject) entry;
				BasicBSONList rowkey = (BasicBSONList) row.get("k");
				entries.put(new IndexKey(rowkey), (BasicBSONList) row.get("e"));
			}
			
			int total = entries.size();
			IndexPage current = new IndexPage(this.key);
			loadedChilds.put(current.model._id, current);
			BasicBSONList low = plowkey;
			int idx = 0;
			int limit = total / 10;
			
			AccessLog.log("total = "+total+" limit="+limit);
			
						
			for (Map.Entry<IndexKey, BasicBSONList> entry : entries.entrySet()) {
				idx++;
				if (idx > limit) {
					addEntry(low, entry.getKey().getKey(), current.model._id);
					current = new IndexPage(this.key);
					loadedChilds.put(current.model._id, current);
					low = entry.getKey().getKey();
					idx = 0;
				}
								
				current.addEntry(entry.getKey().getKey().toArray()).put("e", entry.getValue());				
			}
			addEntry(low, phighkey, current.model._id);
			
            BasicBSONList pages = (BasicBSONList) model.unencrypted.get("p");
            Iterator<Object> it = pages.iterator();
			while (it.hasNext()) {				
			    Object entry = it.next();
				BasicBSONObject row = (BasicBSONObject) entry;
				if (row.getString("c").equals(oldPage.model._id.toString())) it.remove();				
			}
												
			flush();
			
			AccessLog.logEnd("end index split");
						
		} catch (LostUpdateException e) {
			reload();
			oldPage.reload();
			splitInto(plowkey, phighkey, oldPage);
		}
		
	}
	
	private void addEntry(BasicBSONList lk, BasicBSONList hk, ObjectId target) {
		BasicBSONList lst = (BasicBSONList) model.unencrypted.get("p");
		BasicBSONObject entry = new BasicBSONObject();		
		entry.put("lk", lk);
		entry.put("hk", hk);
		entry.put("c", target.toString());
		lst.add(entry);				
	}

	
	
	public void addEntry(Comparable<Object>[] key, ObjectId aps, ObjectId target) throws InternalServerException {
		if (key[0] == null) return;
				
		EqualsSingleValueCondition[] cond = new EqualsSingleValueCondition[key.length];
		for (int i=0;i<key.length;i++) cond[i] = new EqualsSingleValueCondition(key[i]);
		
		Collection<IndexMatch> results = null;
		Collection<ObjectId> targets = findEntries(cond);
		for (ObjectId targetPage : targets) {
			IndexPage ip = access(targetPage); 
			ip.addEntry(key, aps, target);
			/*if (ip.needsSplit()) {
				splitInto((BasicBSONList) ip.model.unencrypted.get("lk"), (BasicBSONList) ip.model.unencrypted.get("hk"), ip);
			}*/
		}
						
	}
	
	public Collection<IndexMatch> lookup(Condition[] key) throws InternalServerException {
		Collection<IndexMatch> results = null;
		Collection<ObjectId> targets = findEntries(key);
		for (ObjectId target : targets) {
			Collection<IndexMatch> partResult = access(target).lookup(key);
			if (results == null) results = partResult;
			else results.addAll(partResult);
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
		for (IndexPage page : loadedChilds.values()) page.flush();
	}
	
	public void reload() throws InternalServerException {
		model = IndexPageModel.getById(model._id);
		decrypt();
	}
	
	public void init() {		
	}
	
	protected void removeFromEntries(Condition[] key, Set<String> ids) throws InternalServerException {
		
		Collection<ObjectId> targets = findEntries(key);
		for (ObjectId target : targets) {
			access(target).removeFromEntries(key, ids);
		}
				
	}
	
	private BasicBSONObject findEntry(Comparable<Object>[] key) {
		EqualsSingleValueCondition[] cond = new EqualsSingleValueCondition[key.length];
		for (int i=0;i<key.length;i++) cond[i] = new EqualsSingleValueCondition(key[i]);
		
		BasicBSONList lst = (BasicBSONList) model.unencrypted.get("p");
		for (Object entry : lst) {
			BasicBSONObject row = (BasicBSONObject) entry;
			BasicBSONList lowkey = (BasicBSONList) row.get("lk");
			BasicBSONList highkey = (BasicBSONList) row.get("hk");
			boolean match = conditionCompare(cond, lowkey, highkey);						
			if (match) return row;
		}
		return null;
	}
	
	private boolean conditionCompare(Condition[] cond, BasicBSONList lowkey, BasicBSONList highkey) {
		for (int i=0;i<cond.length;i++) {
			if (!cond[i].isInBounds(lowkey==null ? null : lowkey.get(i),  highkey == null ? null: highkey.get(i)))  return false;
		}		
		return true;
	}
	
	private Collection<ObjectId> findEntries(Condition[] key) {
		Collection<ObjectId> result = new ArrayList<ObjectId>();
		
		BasicBSONList lst = (BasicBSONList) model.unencrypted.get("p");
		for (Object entry : lst) {
			BasicBSONObject row = (BasicBSONObject) entry;
			BasicBSONList lowkey = (BasicBSONList) row.get("lk");
			BasicBSONList highkey = (BasicBSONList) row.get("hk");
			boolean match = conditionCompare(key, lowkey, highkey);						
			if (match) {
				ObjectId childpage = new ObjectId(row.get("c").toString());
				result.add(childpage);				
			}
		}
		return result;
	}
	
	private IndexPage access(ObjectId pageId) throws InternalServerException {
		IndexPage ip = loadedChilds.get(pageId);
		if (ip != null) return ip;
		ip = new IndexPage(this.key, IndexPageModel.getById(pageId));
		loadedChilds.put(pageId, ip);
		
		return ip;
	}
	
	public boolean needsSplit() {
		return false;
	}
}