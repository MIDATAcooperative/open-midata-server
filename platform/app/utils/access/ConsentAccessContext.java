package utils.access;

import java.util.Collections;

import models.Consent;
import models.MidataId;
import models.enums.WritePermissionType;
import utils.exceptions.AppException;

public class ConsentAccessContext extends AccessContext{

	private Consent consent;	
	
	public ConsentAccessContext(Consent consent, APSCache cache, AccessContext parent) {
		super(cache, parent);
		this.consent = consent;
	}
	
	public ConsentAccessContext(Consent consent, AccessContext parent) {
		super(parent.getCache(), parent);
		this.consent = consent;
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (consent.writes == null) return parent==null || parent.mayCreateRecord(record);
		if (!consent.writes.isCreateAllowed()) return false;
		if (consent.writes.isUnrestricted()) return parent==null || parent.mayCreateRecord(record);
		return !QueryEngine.listFromMemory(cache, consent.sharingQuery, Collections.singletonList(record)).isEmpty() && (parent==null || parent.mayCreateRecord(record));
		
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

	@Override
	public MidataId getTargetAps() {
		return consent._id;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return !QueryEngine.listFromMemory(cache, consent.sharingQuery, Collections.singletonList(record)).isEmpty();
	}

	

}
