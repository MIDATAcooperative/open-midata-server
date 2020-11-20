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

import models.MidataId;
import models.Record;
import models.UserGroupMember;
import utils.exceptions.AppException;

public class UserGroupAccessContext extends AccessContext {

	private UserGroupMember ugm;
	
	public UserGroupAccessContext(UserGroupMember ugm, APSCache cache, AccessContext parent) {
		super(cache, parent);
	    this.ugm = ugm;	
	}
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return ugm.getRole().mayWriteData() && parent.mayCreateRecord(record);
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		return ugm.getRole().mayWriteData() && parent.mayUpdateRecord(stored, newVersion);
	}
	
	@Override
	public String getAccessInfo(DBRecord rec) throws AppException {
		return "[ allowWrite="+ugm.getRole().mayWriteData()+" ]";
	}
	
	@Override
	public boolean mustPseudonymize() {
		return ugm.getRole().pseudonymizedAccess();
	}

	@Override
	public MidataId getTargetAps() {
		return cache.getAccountOwner();
	}
	
	@Override
	public String getOwnerName() throws AppException {		
		return parent.getOwnerName();
	}
	@Override
	public MidataId getOwner() {
		return parent.getOwner();
	}
	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
		return parent.getOwnerPseudonymized();
	}
	@Override
	public MidataId getSelf() {
		return parent.getSelf();
	}
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return false;
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return false;
	}
	
	@Override
	public String toString() {
		return "usergroup("+ugm.userGroup+" "+parentString()+")";
	}
	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {		
		return null;
	}
	@Override
	public String getContextName() {
		return "User Group/Project";
	}

}
