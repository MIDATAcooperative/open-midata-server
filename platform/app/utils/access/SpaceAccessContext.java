package utils.access;

import utils.exceptions.AppException;

public class SpaceAccessContext extends AccessContext {

	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord() {
		return true;
	}

	@Override
	public boolean mustPseudonymize() {	
		return false;
	}

}
