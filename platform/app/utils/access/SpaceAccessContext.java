package utils.access;

import java.util.Collections;

import models.MidataId;
import models.Space;
import utils.exceptions.AppException;

public class SpaceAccessContext extends AccessContext {

	private Space space;
	
	public SpaceAccessContext(Space space, APSCache cache, AccessContext parent) {
		super(cache, parent);
		this.space = space;
	}
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return parent==null || parent.mayCreateRecord(record);
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
		return space._id;
	}
	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return false;
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
