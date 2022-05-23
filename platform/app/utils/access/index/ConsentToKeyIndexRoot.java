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

public class ConsentToKeyIndexRoot extends BaseIndexRoot<ConsentToKeyIndexKey,ConsentToKeyIndexKey> {

	private IndexDefinition model;
	
	public int MIN_DEGREE() { return 10000; };
		
	public ConsentToKeyIndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		
		this.key = key;
		this.model = def;
		this.created = def.creation;
		this.rootPage = new IndexPage(this.key, def, this, 1);		
		if (isnew) {
			locked = true;
			this.rootPage.initAsRootPage();
		}
		this.btree = new BTree(this, this.rootPage);
		this.loadedPages = new HashMap<MidataId, IndexPage<ConsentToKeyIndexKey,ConsentToKeyIndexKey>>();
	}
			
	public IndexDefinition getModel() {
		return model;
	}
    		

	public void addEntry(ConsentToKeyIndexKey key) throws InternalServerException, LostUpdateException {
		modCount++;		
		btree.insert(key);						
	}
	
	public void removeEntry(ConsentToKeyIndexKey key) throws InternalServerException, LostUpdateException {
		modCount++;		
		btree.delete(key);
	}
	
	public byte[] getKey(MidataId aps) throws LostUpdateException, InternalServerException {
		ConsentToKeyIndexKey result = btree.search(new ConsentToKeyIndexKey(aps));
		return result != null ? result.key : null;
	}
	
	@Override
	public ConsentToKeyIndexKey createKey() {
		return new ConsentToKeyIndexKey();
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