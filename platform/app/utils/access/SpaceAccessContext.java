package utils.access;

import models.MidataId;
import models.Space;
import utils.exceptions.AppException;

public class SpaceAccessContext extends AccessContext {

	private Space space;
	private MidataId self;
	
	public SpaceAccessContext(Space space, APSCache cache, AccessContext parent, MidataId self) {
		super(cache, parent);
		this.space = space;
		this.self = self;
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
		return record.owner.equals(cache.getAccountOwner());
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
		return self;
	}
	
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return false;
	}
	
	public Space getInstance() {
		return space;
	}

}
