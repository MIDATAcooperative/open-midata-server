package utils.access;

import utils.exceptions.AppException;

public abstract class AccessContext {
	
	protected AccessContext parent;

	public abstract boolean mayCreateRecord(DBRecord record) throws AppException;
	
	public abstract boolean mayUpdateRecord();
	
	public abstract boolean mustPseudonymize();
	
	public AccessContext getParent() {
		return parent;
	}
}
