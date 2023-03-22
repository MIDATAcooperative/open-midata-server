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

public class AccountReuseAccessContext extends AccountCreationAccessContext {
	
	private Record patientRecord;
	
	public AccountReuseAccessContext(AccessContext parent, MidataId newAccountId, Record patientRecord) throws AppException {
		super(new APSCache(parent.getAccessor(), newAccountId), parent);
		this.patientRecord = patientRecord;
	}
	
	protected AccountReuseAccessContext(APSCache cache, AccessContext parent) {
	    super(cache, parent);
	}
	
	@Override
	public boolean canCreateActiveConsents() {
		return false;
	}
		
	@Override
	public String getContextName() {
		return "Re-use user account";
	}

	@Override
	public String toString() {
		return "account-reuse("+cache.getAccountOwner()+","+parentString()+")";
	}
	
	@Override
	public MidataId getPatientRecordId() {
		return patientRecord._id;
	}
	
	@Override
	public AccessContext forAccountReshare() {
		return new AccountReuseAccessContext(getCache(), null);		
	}
	
	@Override
	public boolean isUserGroupContext() {
		return false;
	}

}
