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

import models.MidataId;
import utils.AccessLog;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public abstract class TsBaseIndexRoot<A extends BaseIndexKey<A,B>,B> extends BaseIndexRoot<A,B> {

	private TimestampIndexRoot ts;
	protected IndexDefinition model;
	
	public TsBaseIndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		this.model = def;
		this.created = def.creation;
		this.ts = new TimestampIndexRoot(key, def, isnew);
	}
	
	public IndexDefinition getModel() {
		return model;
	}
    	
	
	public long getVersion(MidataId aps) throws InternalServerException {
		try {
		  return ts.getValue(aps.toString());
		} catch (LostUpdateException e) {
			reload();			
			return getVersion(aps);
		}
	}
	
	public void setVersion(MidataId aps, long now) throws LostUpdateException, InternalServerException {
		rootPage.changed = true;
		ts.setValue(aps.toString(), now);		
	}
	
	public long getAllVersion() throws InternalServerException {
		try {
		  return ts.getValue("all");
		} catch (LostUpdateException e) {
			reload();			
			return getAllVersion();
		}
	}
	
	public void setAllVersion(long now) throws LostUpdateException, InternalServerException {
		AccessLog.log("setAllVersion="+now);
		rootPage.changed = true;
		ts.setValue("all", now);					
	}
	
    public void flush() throws InternalServerException, LostUpdateException {
    	super.flush();
    	ts.flush();
    }
    
    public void prepareToCreate() throws InternalServerException {
		rootPage.encrypt();
		ts.prepareToCreate();
		rootPage.changed = false;
	}
	
				
	public void reload() throws InternalServerException {
		loadedPages.clear();
		model = IndexDefinition.getById(model._id);
		rootPage = new IndexPage(this.key, model, this, 1);	
		ts = new TimestampIndexRoot(key, model, false);		
		locked = false;
		modCount = 0;
	}
}
