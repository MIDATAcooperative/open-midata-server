package utils.access;

import utils.exceptions.AppException;

public class AccountAccessContext extends AccessContext {

	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (parent != null) return parent.mayCreateRecord(record);
		return true;
	}

	@Override
	public boolean mayUpdateRecord() {
		if (parent != null) return parent.mayUpdateRecord();
		return true;
	}

	@Override
	public boolean mustPseudonymize() {
		if (parent != null) return parent.mustPseudonymize();
		return false;
	}

}
