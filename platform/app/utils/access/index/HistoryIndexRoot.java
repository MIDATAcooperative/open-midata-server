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
import utils.AccessLog;
import utils.access.EncryptedAPS;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class HistoryIndexRoot extends BaseIndexRoot<HistoryIndexKey, HistoryIndexKey> {

	private EncryptedAPS model;
	private byte[] key; 
	
	public int MIN_DEGREE() { return 2000; };
		
	public HistoryIndexRoot(byte[] key, EncryptedAPS model) throws AppException {
	    this.key = key;
		this.model = model;
		HistoryIndexPageModel ai = new HistoryIndexPageModel(model);
		this.rootPage = new IndexPage(this.key, ai, this, 1);		
		if (ai.getEnc()==null) {
			locked = true;
			this.rootPage.initAsRootPage();
		}
		this.btree = new BTree(this, this.rootPage);
		this.loadedPages = new HashMap<MidataId, IndexPage<HistoryIndexKey,HistoryIndexKey>>();
		
	}
		
	
	/*public IndexDefinition getModel() {
		return model;
	}*/
    				
	
	/*
	public List<String> getFormats() {		
		return model.formats;
	}
	*/
		

	public void addEntry(HistoryIndexKey history) throws InternalServerException, LostUpdateException {
		modCount++;
		AccessLog.log("history-add: "+history.toString());
		//if (modCount > 100) lockIndex();						
		btree.insert(history);						
	}
	
	public boolean removeEntry(HistoryIndexKey history) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();		
		return btree.delete(history) != null;
	}
	


	@Override
	public HistoryIndexKey createKey() {
		return new HistoryIndexKey();
	}
	
	@Override
	public BaseIndexPageModel createPage() {
		HistoryExtraIndexPageModel page = new HistoryExtraIndexPageModel();
		page._id = new MidataId();
		page.rev = getRev();		
		return page;
	}
	
	
	
}
