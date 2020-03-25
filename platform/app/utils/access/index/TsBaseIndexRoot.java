package utils.access.index;

import models.MidataId;
import utils.AccessLog;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public abstract class TsBaseIndexRoot<A extends BaseIndexKey<A,B>,B> extends BaseIndexRoot<A,B> {

	private TimestampIndexRoot ts;
	
	public TsBaseIndexRoot(byte[] key, IndexDefinition def, boolean isnew) throws InternalServerException {
		this.ts = new TimestampIndexRoot(key, def, isnew);
	}
	
	public long getVersion(MidataId aps) throws InternalServerException {
		try {
		  return ts.getValue(aps.toString());
		} catch (LostUpdateException e) {
			reload();
			ts.reload();
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
			ts.reload();
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
		rootPage.reload();
		ts.reload();
		locked = false;
		modCount = 0;
	}
}
