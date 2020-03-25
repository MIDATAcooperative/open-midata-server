package utils.access;

import models.MidataId;
import models.Record;
import models.UserGroupMember;
import utils.exceptions.AppException;

public class UserGroupAccessContext extends AccessContext {

	private UserGroupMember ugm;
	
	public UserGroupAccessContext(UserGroupMember ugm, APSCache cache, AccessContext parent) {
		super(cache, parent);
	    this.ugm = ugm;	
	}
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return ugm.getRole().mayWriteData() && parent.mayCreateRecord(record);
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		return ugm.getRole().mayWriteData() && parent.mayUpdateRecord(stored, newVersion);
	}

	@Override
	public boolean mustPseudonymize() {
		return ugm.getRole().pseudonymizedAccess();
	}

	@Override
	public MidataId getTargetAps() {
		return cache.getAccountOwner();
	}
	
	@Override
	public String getOwnerName() {		
		return parent.getOwnerName();
	}
	@Override
	public MidataId getOwner() {
		return parent.getOwner();
	}
	@Override
	public MidataId getOwnerPseudonymized() {
		return parent.getOwnerPseudonymized();
	}
	@Override
	public MidataId getSelf() {
		return parent.getSelf();
	}
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return false;
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return false;
	}
	
	@Override
	public String toString() {
		return "usergroup("+ugm.userGroup+" "+parentString()+")";
	}

}
