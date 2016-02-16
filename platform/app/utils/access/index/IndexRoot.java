package utils.access.index;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

import utils.access.DBRecord;
import utils.access.op.Condition;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

/**
 * Manages one index
 *
 */
public class IndexRoot {

	private IndexDefinition model;
	private IndexPage rootPage;
	private SecretKey key;
	
	public IndexRoot(SecretKey key, IndexDefinition def, boolean isnew) throws InternalServerException {
		this.key = key;
		this.model = def;
		this.rootPage = new IndexPage(this.key, def);		
		if (isnew) {
			this.rootPage.init();
		}
	}
	
	public long getVersion() {
		return rootPage.getVersion();
	}
	
	public long getVersion(ObjectId aps) {
		return rootPage.getTimestamp(aps.toString());
	}
	
	public void setVersion(ObjectId aps, long now) {
		rootPage.setTimestamp(aps.toString(), now);	
	}
	
	public void flush() throws InternalServerException, LostUpdateException {
		rootPage.flush();
	}
	
	public void reload() throws InternalServerException {
		rootPage.reload();
	}

	public List<String> getFormats() {		
		return model.formats;
	}

	public boolean restrictedToSelf() {
		return model.selfOnly;
	}

	public void addEntry(ObjectId aps, DBRecord record) {
		Object[] key = new Object[model.fields.size()];
		int idx = 0;
		for (String path : model.fields) {
			key[idx] = extract(record.data, path);
			idx++;
		}
		rootPage.addEntry(key, aps, record._id);
	}


	private Object extract(BSONObject data, String path) {
		int i = path.indexOf('.');
		if (i > 0) {
			String prefix = path.substring(0, i);
			String remain = path.substring(i+1);
			Object access = data.get(prefix);
			if (access instanceof BasicBSONList) {
				BasicBSONList lst = (BasicBSONList) access;
				if (lst.size() == 0) return null;
				if (lst.size() == 1) {
					return extract((BSONObject) lst.get(0), remain);
				}
				
				// TODO Not Implemented for multiple entries
                return null;
			} else if (access instanceof BasicBSONObject) {
			   return extract((BasicBSONObject) access, remain);
			} else return null;
		} else {
			return data.get(path);
		}
	}
	
	public Collection<IndexMatch> lookup(Condition[] key) {
		return rootPage.lookup(key);
	}

	public void removeRecords(Condition[] key, Set<String> ids) {
		rootPage.removeFromEntries(key, ids);				
	}

	
}
