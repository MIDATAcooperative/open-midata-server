/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
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
}
