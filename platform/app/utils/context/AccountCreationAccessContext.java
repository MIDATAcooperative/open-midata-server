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

package utils.context;

import models.MidataId;
import models.Record;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.exceptions.AppException;

public class AccountCreationAccessContext extends AccessContext {
		
	public AccountCreationAccessContext(AccessContext parent, MidataId newAccountId) throws AppException {
	    super(new APSCache(newAccountId, newAccountId), parent);
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {		
		return true;
	}

	@Override
	public boolean mustPseudonymize() {
		return false;		
	}
	
	@Override
	public boolean mustRename() {
		return false;		
	}

	@Override
	public MidataId getTargetAps() {
		return getCache().getAccountOwner();
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}
		

	public String getOwnerName() {
		return null;
	}

	@Override
	public MidataId getOwner() {
		return getCache().getAccountOwner();
	}

	@Override
	public MidataId getOwnerPseudonymized() throws AppException  {		
		return getCache().getAccountOwner();
	}

	@Override
	public MidataId getSelf() {
		return getCache().getAccountOwner();
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
	public Object getAccessRestriction(String content, String format, String field) throws AppException {		
		return null;
	}

	@Override
	public String getContextName() {
		return "New user account";
	}

	@Override
	public MidataId getActor() {
		return parent != null ? parent.getActor() : getCache().getAccountOwner();
	}

	@Override
	public MidataId getAccessor() {
		return getCache().getAccessor();
	}
	
	public void close() throws AppException {
		getCache().finishTouch();
	}
	
	@Override
	public String toString() {
		return "account-create("+cache.getAccessor()+","+parentString()+")";
	}

}
