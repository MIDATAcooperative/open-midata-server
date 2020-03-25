package utils.access.index;

import models.MidataId;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public interface BaseIndexPageModel {

	public long getVersion();
	
	public void setLockTime(long lockTime);
	
	public long getLockTime();
	
	public void setEnc(byte[] enc);
	
	public byte[] getEnc();
	
	public void update() throws InternalServerException, LostUpdateException;
			
	public void updateLock() throws InternalServerException, LostUpdateException;		
	
	public MidataId getId();
	
	public BaseIndexPageModel reload() throws InternalServerException;
}
