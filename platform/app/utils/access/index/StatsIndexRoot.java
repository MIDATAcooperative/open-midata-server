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
import models.RecordsInfo;
import utils.AccessLog;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class StatsIndexRoot extends TsBaseIndexRoot<StatsIndexKey,StatsIndexKey> {

	private IndexDefinition model;
	
	public int MIN_DEGREE() { return 1000; };
	
	
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
		if (key.group==null) throw new NullPointerException();
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
