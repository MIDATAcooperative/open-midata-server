package utils.access;

import models.MidataId;
import utils.exceptions.AppException;

public class UserGroupAccessContext extends AccessContext {

	public UserGroupAccessContext(APSCache cache, AccessContext parent) {
		super(cache, parent);
		
	}
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return parent.mayCreateRecord(record);
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord() {
		return parent.mayUpdateRecord();
	}

	@Override
	public boolean mustPseudonymize() {
		return parent.mustPseudonymize();
	}

	@Override
	public MidataId getTargetAps() {
		return cache.getAccountOwner();
	}

}
