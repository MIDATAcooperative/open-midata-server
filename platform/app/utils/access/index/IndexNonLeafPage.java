package utils.access.index;

import java.util.ArrayList;
import java.util.Collection;
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

	public IndexNonLeafPage(byte[] key, IndexPageModel model) throws InternalServerException {
		super(key, model);					
	}
	
	public static IndexNonLeafPage split(byte[] enckey, IndexPage oldPage) throws InternalServerException {
		
		try {
			IndexPageModel old = oldPage.model;
			IndexNonLeafPage splitted = new IndexNonLeafPage(enckey, old);
			//List<>
			
			// Create list with ordered map key -> entries
			// split map in X parts
			SortedMap<IndexKey, BasicBSONList> entries = new TreeMap<IndexKey, BasicBSONList>();
			
			
			BasicBSONList lst = (BasicBSONList) old.unencrypted.get("e");
			
			for (Object entry : lst) {
				BasicBSONObject row = (BasicBSONObject) entry;
				BasicBSONList rowkey = (BasicBSONList) row.get("k");
				entries.put(new IndexKey(rowkey), (BasicBSONList) row.get("e"));
			}
			
			int total = entries.size();
			IndexPage current = new IndexPage(enckey, new IndexPageModel());
			splitted.loadedChilds.put(current.model._id, current);
			BasicBSONList low = null;
			int idx = 0;
			int limit = total / 10;
			
			for (Map.Entry<IndexKey, BasicBSONList> entry : entries.entrySet()) {
				idx++;
				if (idx > limit) {
					splitted.addEntry(low, entry.getKey().getKey(), current.model._id);
					current = new IndexPage(enckey, new IndexPageModel());
					splitted.loadedChilds.put(current.model._id, current);
					low = entry.getKey().getKey();
					idx = 0;
				}
								
				current.addEntry(entry.getKey().getKey().toArray()).put("e", entry.getValue());				
			}
			splitted.addEntry(low, null, current.model._id);
			
			splitted.flush();
			
			return splitted;
		} catch (LostUpdateException e) {
			oldPage.reload();
			return split(enckey, oldPage);
		}
		
	}
	
	private void addEntry(BasicBSONList lk, BasicBSONList hk, ObjectId target) {
		BasicBSONList lst = (BasicBSONList) model.unencrypted.get("e");
		BasicBSONObject entry = new BasicBSONObject();		
		entry.put("lk", lk);
		entry.put("hk", hk);
		entry.put("c", target.toString());
		lst.add(entry);				
	}

	private Map<ObjectId, IndexPage> loadedChilds;
	
	public void addEntry(Comparable<Object>[] key, ObjectId aps, ObjectId target) throws InternalServerException {
		if (key[0] == null) return;
				
		EqualsSingleValueCondition[] cond = new EqualsSingleValueCondition[key.length];
		for (int i=0;i<key.length;i++) cond[i] = new EqualsSingleValueCondition(key[i]);
		
		Collection<IndexMatch> results = null;
		Collection<ObjectId> targets = findEntries(cond);
		for (ObjectId targetPage : targets) {
			access(targetPage).addEntry(key, aps, target);			
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
			if (!cond[i].isInBounds(lowkey.get(i),  highkey.get(i)))  return false;
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
}
