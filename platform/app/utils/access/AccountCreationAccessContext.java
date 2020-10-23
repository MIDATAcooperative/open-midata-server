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

import models.MidataId;
import models.Record;
import utils.exceptions.AppException;

public class AccountCreationAccessContext extends AccessContext {
	
	public AccountCreationAccessContext(AccessContext parent) throws AppException {
	  super(parent.getCache(), parent);
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
	public MidataId getTargetAps() {
		return parent.getTargetAps();
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
		return parent.getOwner();
	}

	@Override
	public MidataId getOwnerPseudonymized() throws AppException  {		
		return parent.getOwnerPseudonymized();
	}

	@Override
	public MidataId getSelf() {
		return parent.getSelf();
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

}
