package utils.access;

import models.MidataId;
import utils.exceptions.AppException;

public class DummyAccessContext extends AccessContext {

	private MidataId selfUser;
	
	
	DummyAccessContext(APSCache cache) {
		super(cache, null);
		selfUser = cache.getAccountOwner();
	}
	
	DummyAccessContext(APSCache cache, MidataId selfUser) {
		super(cache, null);
		this.selfUser = selfUser;
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {		
		return false;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {		
		return false;
	}

	@Override
	public boolean mayUpdateRecord() {		
		return false;
	}

	@Override
	public boolean mustPseudonymize() {		
		return false;
	}

	@Override
	public MidataId getTargetAps() {
		return cache.getExecutor();
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
		return selfUser;
	}

	@Override
	public boolean mayAccess(String content, String format) throws AppException {		
		return false;
	}

}
