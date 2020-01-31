package utils.access;

import models.MidataId;
import models.Record;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.exceptions.AppException;

public class PublicAccessContext extends AccessContext {
	
	public PublicAccessContext(APSCache cache, AccessContext parent) {
		super(cache, parent);	    
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {		
		return parent.mayCreateRecord(record) && record.owner.equals(RuntimeConstants.instance.publicUser);
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {		
		return newVersion.tags != null && newVersion.tags.contains("security:public") &&
			   (newVersion.creator != null && newVersion.creator.toString().equals(stored.meta.getString("creator"))
			   || (newVersion.app != null && newVersion.app.toString().equals(stored.meta.getString("app"))));
	}

	@Override
	public boolean mustPseudonymize() {
		return false;
	}

	@Override
	public MidataId getTargetAps() {
		return RuntimeConstants.instance.publicUser;
	}
	
	@Override
	public String getOwnerName() {		
		return "Public";
	}
	@Override
	public MidataId getOwner() {
		return RuntimeConstants.instance.publicUser;
	}
	@Override
	public MidataId getOwnerPseudonymized() {
		return RuntimeConstants.instance.publicUser;
	}
	@Override
	public MidataId getSelf() {
		return RuntimeConstants.instance.publicUser;
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
