package utils.access;

import models.MidataId;
import utils.exceptions.AppException;

public class AccountAccessContext extends AccessContext {

	public AccountAccessContext(APSCache cache, AccessContext parent) {
		super(cache, parent);
	}
	
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

	@Override
	public MidataId getTargetAps() {		
		return cache.getAccountOwner();
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public String getOwnerName() {		
		return null;
	}

	@Override
	public MidataId getOwner() {
		return cache.getAccountOwner();
	}

	@Override
	public MidataId getOwnerPseudonymized() {
		return cache.getAccountOwner();
	}

}
