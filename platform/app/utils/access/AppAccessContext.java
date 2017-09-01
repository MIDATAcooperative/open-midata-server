package utils.access;

import java.util.Collections;

import models.MobileAppInstance;
import utils.exceptions.AppException;

public class AppAccessContext extends AccessContext {

	private MobileAppInstance instance;
	private APSCache cache;
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (instance.writes == null) return true;
		if (!instance.writes.isCreateAllowed()) return false;
		
		if (instance.writes.isUnrestricted()) return true;
		return !QueryEngine.listFromMemory(cache, instance.sharingQuery, Collections.singletonList(record)).isEmpty();
		
	}

	@Override
	public boolean mayUpdateRecord() {
		if (!instance.writes.isUpdateAllowed()) return false;
		if (parent != null) return parent.mayUpdateRecord();
		return true;
	}

	@Override
	public boolean mustPseudonymize() {
		// TODO Auto-generated method stub
		return false;
	}
	

}
