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
import java.util.List;

import models.MidataId;
import utils.access.DBRecord;
import utils.access.EncryptedAPS;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class StreamIndexRoot extends BaseIndexRoot<StreamIndexKey,DBRecord> {

	private EncryptedAPS model;
	private byte[] key; 
	
	public int MIN_DEGREE() { return 200; };
		
	public StreamIndexRoot(byte[] key, EncryptedAPS model) throws AppException {
	    this.key = key;
		this.model = model;
		ApsIndexPageModel ai = new ApsIndexPageModel(model);
		this.rootPage = new IndexPage(this.key, ai, this, 1);		
		if (ai.getEnc()==null) {
			locked = true;
			this.rootPage.initAsRootPage();
		}
		this.btree = new BTree(this, this.rootPage);
		this.loadedPages = new HashMap<MidataId, IndexPage<StreamIndexKey,DBRecord>>();
		
	}
		
	
	/*public IndexDefinition getModel() {
		return model;
	}*/
    				
	
	/*
	public List<String> getFormats() {		
		return model.formats;
	}
	*/
		

	public void addEntry(DBRecord record) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();
		if (record._id==null) throw new NullPointerException();
		
		StreamIndexKey key = new StreamIndexKey(record);
		btree.insert(key);						
	}
	
	public boolean removeEntry(DBRecord record) throws InternalServerException, LostUpdateException {
		modCount++;
		//if (modCount > 100) lockIndex();
		//if (record.data == null) throw new NullPointerException();
		
		StreamIndexKey key = new StreamIndexKey(record);
		return btree.delete(key) != null;
	}
	


	@Override
	public StreamIndexKey createKey() {
		return new StreamIndexKey();
	}
	
	@Override
	public BaseIndexPageModel createPage() {
		ApsExtraIndexPageModel page = new ApsExtraIndexPageModel();
		page._id = new MidataId();
		page.rev = getRev();		
		return page;
	}
	
	
	
}
