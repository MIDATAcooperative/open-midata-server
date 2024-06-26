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

import java.util.HashMap;

import models.MidataId;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class TimestampIndexRoot extends BaseIndexRoot<TimestampIndexKey,TimestampIndexKey> {

	private IndexDefinition model;
	
	public int MIN_DEGREE() { return 10000; };
	
	public TimestampIndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		this.key = key;
		this.model = def;
		this.created = def.creation;
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
	
	@Override
	public BaseIndexPageModel createPage() {
		IndexPageModel page = new IndexPageModel();
		page._id = new MidataId();
		page.rev = getRev();
		page.creation = getCreated();
		return page;
	}
	
	
	
	
}
