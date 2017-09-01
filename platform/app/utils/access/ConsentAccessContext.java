package utils.access;

import java.util.Collections;

import models.Consent;
import models.enums.WritePermissionType;
import utils.exceptions.AppException;

public class ConsentAccessContext extends AccessContext{

	private Consent consent;
	private APSCache cache;
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (consent.writes == null) return true; // old compatibility
		if (!consent.writes.isCreateAllowed()) return false;
		if (consent.writes.isUnrestricted()) return true;
		return !QueryEngine.listFromMemory(cache, consent.sharingQuery, Collections.singletonList(record)).isEmpty();
		
	}

	@Override
	public boolean mayUpdateRecord() {
		if (!consent.writes.isUpdateAllowed()) return false;
		if (parent != null) return parent.mayUpdateRecord();
		return true;
	}

	@Override
	public boolean mustPseudonymize() {
		// TODO Auto-generated method stub
		return false;
	}

	

}
