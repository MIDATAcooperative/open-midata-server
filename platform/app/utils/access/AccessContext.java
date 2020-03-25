package utils.access;

import java.util.Map;

import models.MidataId;
import models.Record;
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
	
	public abstract boolean mayUpdateRecord(DBRecord stored, Record newVersion);
	
	public abstract boolean mustPseudonymize();
	
	public abstract boolean mayContainRecordsFromMultipleOwners();
	
	public abstract boolean mayAccess(String content, String format) throws AppException;
	
	public boolean produceHistory() {
		return true;
	}
	
	public abstract MidataId getSelf();
	
	public abstract MidataId getTargetAps();
	
	public abstract MidataId getOwner();
	
	public abstract MidataId getOwnerPseudonymized();
	
	public abstract String getOwnerName();
	
	public Map<String, Object> getQueryRestrictions() {
		if (parent != null) return parent.getQueryRestrictions(); else return null;
	}
	
	public AccessContext getParent() {
		return parent;
	}
	
	public APSCache getCache() {
		return cache;
	}

	public MidataId getNewRecordCreator() {
		if (parent != null) return parent.getNewRecordCreator();
		return cache.getExecutor();
	}
	
	protected String parentString() {
		if (parent==null) return "";
		return parent.toString();
	}
}
