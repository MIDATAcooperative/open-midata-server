package utils.access;

import models.MidataId;
import utils.exceptions.AppException;

public abstract class AccessContext {
	
	protected AccessContext parent;
	protected APSCache cache;
	
	AccessContext(APSCache cache, AccessContext parent) {
		this.cache = cache;
		this.parent = parent;
	}

	public abstract boolean mayCreateRecord(DBRecord record) throws AppException;
	
	public abstract boolean isIncluded(DBRecord record) throws AppException;
	
	public abstract boolean mayUpdateRecord();
	
	public abstract boolean mustPseudonymize();
	
	public abstract boolean mayAccess(String content, String format) throws AppException;
	
	public boolean produceHistory() {
		return true;
	}
	
	public abstract MidataId getSelf();
	
	public abstract MidataId getTargetAps();
	
	public abstract MidataId getOwner();
	
	public abstract MidataId getOwnerPseudonymized();
	
	public abstract String getOwnerName();
	
	public AccessContext getParent() {
		return parent;
	}
	
	public APSCache getCache() {
		return cache;
	}
}
