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

import java.util.List;

import models.MidataId;
import models.Record;
import models.UserGroupMember;
import models.enums.EntityType;
import models.enums.Permission;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.access.Feature_UserGroups;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class UserGroupAccessContext extends AccessContext {

	private UserGroupMember ugm;
	
	public UserGroupAccessContext(UserGroupMember ugm, APSCache cache, AccessContext parent) {
		super(cache, parent);
	    this.ugm = ugm;	
	}
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return ugm.getConfirmedRole().mayWriteData() && parent.mayCreateRecord(record);
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion)  throws InternalServerException {
		return ugm.getConfirmedRole().mayWriteData() && parent.mayUpdateRecord(stored, newVersion);
	}
	
	@Override
	public String getAccessInfo(DBRecord rec) throws AppException {
		return "\n -Is current user role allowed to write data? ["+ugm.getRole().mayWriteData()+"]";
	}
	
	@Override
	public boolean mustPseudonymize() {
		return ugm.getConfirmedRole().pseudonymizedAccess();
	}
	
	@Override
	public boolean mustRename() {
		return false;
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
	public String getOwnerType() {		
		return "Group";
	}
	
	@Override
	public MidataId getOwner() {
		return getAccessor();// parent.getOwner();
	}
	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
		return getAccessor();//  parent.getOwnerPseudonymized();
	}
	@Override
	public MidataId getSelf() {
		return parent.getSelf();
	}
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return parent.mayAccess(content, format);
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
		return parent.getAccessRestriction(content, format, field);
	}
	@Override
	public String getContextName() {
		return "User Group/Project access with role '"+ugm.getRole().roleName+"'";
	}
	
	@Override
	public boolean isUserGroupContext() {		
		return true;
	}
	
	@Override
	public EntityType getAccessorEntityType() throws InternalServerException {
		return EntityType.USERGROUP;
	}
	
	public UserGroupAccessContext forUserGroup(UserGroupMember ugm) throws AppException {
		if (ugm != null && ugm.userGroup.equals(this.ugm.userGroup)) return this;
		return super.forUserGroup(ugm);
	}
	
	public UserGroupAccessContext forUserGroup(MidataId userGroup, Permission permission) throws AppException {
		if (userGroup.equals(ugm.userGroup)) return this;
		return super.forUserGroup(userGroup, permission);		
	}
	
	public boolean canCreateActiveConsentsFor(MidataId owner) {
		if (owner.equals(ugm.userGroup)) return true;
		return super.canCreateActiveConsentsFor(owner);
	}
	
	@Override
	protected AccessContext getRootContext() {	
		if (parent == null) return this;
		return new UserGroupAccessContext(this.ugm, cache, parent.getRootContext());
	}
	
	
}
