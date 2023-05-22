package utils.context;

import models.MidataId;
import models.Record;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class RepresentativeAccessContext extends AccessContext {

		
	public RepresentativeAccessContext(APSCache cache, AccessContext parent) {
		super(cache, parent);	   
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return parent.mayCreateRecord(record);
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) throws InternalServerException {
		return parent.mayUpdateRecord(stored, newVersion);
	}
	
		
	@Override
	public boolean mustPseudonymize() {
		return parent.mustPseudonymize();
	}
	
	@Override
	public boolean mustRename() {
		return false;
	}

	@Override
	public MidataId getTargetAps() {
		return cache.getAccountOwner();
	}
	
	@Override
	public String getOwnerName() throws AppException {		
		return null;
	}
	@Override
	public MidataId getOwner() {
		return cache.getAccountOwner();
	}
	
	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
		return cache.getAccountOwner();
	}
	
	
	
	@Override
	public MidataId getSelf() {
		return parent.getSelf();
	}
	
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return true;
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return false;
	}
	
	@Override
	public String toString() {
		return "representative("+cache.getAccountOwner()+", "+parentString()+")";
	}
	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {		
		return null;
	}
	@Override
	public String getContextName() {
		return "Representative Access for other person";
	}

}
