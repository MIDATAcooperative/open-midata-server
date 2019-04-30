package utils.access;

import models.MidataId;
import models.UserGroupMember;
import utils.exceptions.AppException;

public class PublicAccessContext extends AccessContext {
	
	public PublicAccessContext(APSCache cache, AccessContext parent) {
		super(cache, parent);	    
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord() {
		return false; // XXXXXXXXXXXXXXXXXX creator only
	}

	@Override
	public boolean mustPseudonymize() {
		return false;
	}

	@Override
	public MidataId getTargetAps() {
		return cache.getAccountOwner();
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
	@Override
	public MidataId getSelf() {
		return cache.getAccountOwner();
	}
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return true;
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return false;
	}

}
