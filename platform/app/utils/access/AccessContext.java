/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	
	public abstract boolean mustRename();
	
	public abstract boolean mayContainRecordsFromMultipleOwners();
	
	public abstract boolean mayAccess(String content, String format) throws AppException;
	
	public abstract Object getAccessRestriction(String content, String format, String field) throws AppException; 
	
	public boolean produceHistory() {
		return true;
	}
	
	public abstract MidataId getSelf();
	
	public abstract MidataId getTargetAps();
	
	public abstract MidataId getOwner();
	
	public abstract MidataId getOwnerPseudonymized() throws AppException;
	
	public abstract String getOwnerName() throws AppException;
	
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
	
	public abstract String getContextName();
	
	public String getAccessInfo(DBRecord rec) throws AppException {
		return "";
	}
	
	
	
	public String getMayUpdateReport(DBRecord stored, Record newVersion) throws AppException {
		boolean result = mayUpdateRecord(stored, newVersion);
		String report = getContextName()+" "+getAccessInfo(stored)+": result mayUpdate="+result;
		if (parent != null) return parent.getMayUpdateReport(stored, newVersion)+"\n"+report;
		return report;
	}
	
	public String getMayCreateRecordReport(DBRecord record) throws AppException {
		boolean result = mayCreateRecord(record);
		String report = getContextName()+" "+getAccessInfo(record)+": result mayCreate="+result;
		if (parent != null) return parent.getMayCreateRecordReport(record)+"\n"+report;
		return report;
	}
	
}
