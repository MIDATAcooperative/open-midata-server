package utils.access;

import java.util.Collections;

import models.MidataId;
import models.MobileAppInstance;
import utils.exceptions.AppException;

public class AppAccessContext extends AccessContext {

	private MobileAppInstance instance;
	
	public AppAccessContext(MobileAppInstance instance, APSCache cache, AccessContext parent) {
		super(cache, parent);
		this.instance = instance;
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (instance.writes == null) return parent==null || parent.mayCreateRecord(record);
		if (!instance.writes.isCreateAllowed()) return false;
		
		if (instance.writes.isUnrestricted()) return parent==null || parent.mayCreateRecord(record);
		return !QueryEngine.listFromMemory(cache, instance.sharingQuery, Collections.singletonList(record)).isEmpty() && (parent==null || parent.mayCreateRecord(record));
		
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

	@Override
	public MidataId getTargetAps() {
		return instance._id;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return !QueryEngine.listFromMemory(cache, instance.sharingQuery, Collections.singletonList(record)).isEmpty();
	}
	

}
