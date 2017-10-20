package utils.access;

import java.util.Collections;

import models.Consent;
import models.MidataId;
import models.enums.ConsentType;
import utils.exceptions.AppException;

public class AccountCreationAccessContext extends AccessContext {
	
	public AccountCreationAccessContext(AccessContext parent) throws AppException {
	  super(parent.getCache(), parent);
	}
	
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

	@Override
	public MidataId getTargetAps() {
		return parent.getTargetAps();
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}
		

	public String getOwnerName() {
		return null;
	}

	@Override
	public MidataId getOwner() {
		return parent.getOwner();
	}

}
