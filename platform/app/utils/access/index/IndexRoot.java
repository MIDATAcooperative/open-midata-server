/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access.index;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import utils.AccessLog;
import utils.access.DBRecord;
import utils.access.op.Condition;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;
import utils.stats.Stats;

/**
 * Manages one index
 *
 */
public class IndexRoot extends TsBaseIndexRoot<IndexKey,IndexMatch> {

				
	public IndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		super(key,def,isnew);
		this.key = key;		
		this.rootPage = new IndexPage(this.key, def, this, 1);		
		if (isnew) {
			locked = true;
			this.rootPage.initAsRootPage();
		}
		this.btree = new BTree(this, this.rootPage);
		this.loadedPages = new HashMap<MidataId, IndexPage<IndexKey,IndexMatch>>();		
	}
					
	public List<String> getFormats() {		
		return model.formats;
	}
	
	class EntryInfo {
		MidataId aps;
		DBRecord record;
		Comparable<Object>[] key;
	}

	public void addEntry(MidataId aps, DBRecord record) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();
		if (record.data == null) throw new NullPointerException();
		EntryInfo inf = new EntryInfo();
		inf.aps = aps;
		inf.record = record;
		inf.key = new Comparable[model.fields.size()];		
		extract(0, inf, null,null, null, 0, false);				
		
	}
	
	public void removeEntry(DBRecord record) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();
		if (record.data == null) throw new NullPointerException();
		EntryInfo inf = new EntryInfo();		
		inf.record = record;
		inf.aps = record.consentAps;
		inf.key = new Comparable[model.fields.size()];		
		extract(0, inf, null,null, null, 0, true);						
	}
	
	


	private void extract(int keyIdx, EntryInfo inf, BSONObject data, String path, String[] allpath, int pathIdx, boolean remove) throws InternalServerException, LostUpdateException {
		if (data == null) {
			if (allpath != null) {
				if (pathIdx < allpath.length) {			
				  path = allpath[pathIdx];
				  pathIdx++;
				  data = inf.record.data;
				  if (path.equals("null")) {
					  inf.key[keyIdx] = (Comparable) null;
					  extract(keyIdx+1, inf, null, null, null, 0, remove);  
				  }
				} else return;
			} else {
			
				if (keyIdx >= model.fields.size()) {
					if (remove) {
					  btree.delete(new IndexKey(inf.key, inf.record._id, inf.aps));
					  
					} else {
					  btree.insert(new IndexKey(inf.key, inf.record._id, inf.aps));					  
					}
					return;
				}			
				data = inf.record.data;
				allpath = model.getFieldsSplit().get(keyIdx);
				path = allpath[0];
				pathIdx = 1;
			}
			
		}
		
		int i = path.indexOf('.');
		if (i > 0) {
			String prefix = path.substring(0, i);
			String remain = path.substring(i+1);
			Object access = data.get(prefix);
			if (access instanceof BasicBSONList) {
				BasicBSONList lst = (BasicBSONList) access;
				if (lst.size() == 0) return;
				for (Object obj : lst) {
					if (obj != null && obj instanceof BSONObject) {
						extract(keyIdx, inf, (BSONObject) obj, remain, allpath, pathIdx, remove);
					}
				}				
			} else if (access instanceof BasicBSONObject) {
			   extract(keyIdx, inf, (BasicBSONObject) access, remain, allpath, pathIdx, remove);
			} else extract(keyIdx, inf, null, null, allpath, pathIdx, remove);;
		} else {
			Object res = data.get(path);
			if (res instanceof BasicBSONList) {
				BasicBSONList lst = (BasicBSONList) res;
				if (lst.size() == 0) return;
				for (Object obj : lst) {
					if (obj instanceof Comparable) {
					  if (obj instanceof String) inf.key[keyIdx] = (Comparable) ((String) obj).toUpperCase();
					  else if (obj instanceof Integer) inf.key[keyIdx] = (Comparable) new Double(((Integer) obj).doubleValue());
					  else inf.key[keyIdx] = (Comparable) obj;
					  extract(keyIdx+1, inf, null, null, null, 0, remove);
					} else {
					  AccessLog.log("Cannot extract path:"+path);
					}					
				}				
			} else if (res != null) {
			  if (res instanceof String) inf.key[keyIdx] = (Comparable) ((String) res).toUpperCase();
			  else if (res instanceof Integer) inf.key[keyIdx] = (Comparable) new Double(((Integer) res).doubleValue());
			  else if (res instanceof Comparable) inf.key[keyIdx] = (Comparable) res;
			  else inf.key[keyIdx] = null;
			  extract(keyIdx+1, inf, null, null, null, 0, remove);
			} else {
			  extract(keyIdx, inf, null, null, allpath, pathIdx, remove);
			}
		}
	}



	@Override
	public IndexKey createKey() {
		return new IndexKey();
	}
	
	
	
}
