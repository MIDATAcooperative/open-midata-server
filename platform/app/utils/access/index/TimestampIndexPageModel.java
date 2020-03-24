package utils.access.index;

import models.MidataId;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class TimestampIndexPageModel implements BaseIndexPageModel {

	private IndexDefinition def;
	
	public TimestampIndexPageModel(IndexDefinition def) {
		this.def = def;
	}
		
	@Override
	public long getVersion() {
		return def.version;
	}

	@Override
	public void setLockTime(long lockTime) {
		def.lockTime = lockTime;		
	}

	@Override
	public long getLockTime() {
		return def.lockTime;
	}

	@Override
	public void setEnc(byte[] enc) {
		def.encTs = enc;		
	}

	@Override
	public byte[] getEnc() {
		return def.encTs;
	}

	@Override
	public void update() throws InternalServerException, LostUpdateException {
		def.updateTs();		
	}

	@Override
	public void updateLock() throws InternalServerException, LostUpdateException {
		def.updateLock();		
	}

	@Override
	public MidataId getId() {
		return def._id;
	}

	@Override
	public BaseIndexPageModel reload() throws InternalServerException {
		return new TimestampIndexPageModel(IndexDefinition.getById(def._id));
	}

}
