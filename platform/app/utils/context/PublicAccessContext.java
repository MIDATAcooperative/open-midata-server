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

import java.util.Collections;

import models.MidataId;
import models.Record;
import models.enums.Permission;
import models.enums.UserRole;
import models.enums.WritePermissionType;
import utils.RuntimeConstants;
import utils.UserGroupTools;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.access.QueryEngine;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class PublicAccessContext extends AccessContext {
	
	public PublicAccessContext(APSCache cache, AccessContext parent) {
		super(cache, parent);	    
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {		
		return parent.mayCreateRecord(record) && record.owner.equals(RuntimeConstants.instance.publicUser);
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) throws InternalServerException {	
		if (UserGroupTools.isGroupManaged(newVersion.format, newVersion.content)) {
			return newVersion.tags != null && newVersion.tags.contains("security:public") && (UserGroupTools.accessorIsMemberOfGroup(parent, stored._id, Permission.SETUP) || getAccessorRole() == UserRole.ADMIN);
		}
		return newVersion.tags != null && newVersion.tags.contains("security:public") &&
			   (newVersion.creator != null && newVersion.creator.toString().equals(stored.meta.getString("creator"))
			   || (newVersion.app != null && newVersion.app.toString().equals(stored.meta.getString("app"))));
	}
	
	@Override
	public String getMayUpdateReport(DBRecord stored, Record newVersion) throws AppException {
		boolean result = mayUpdateRecord(stored, newVersion);
		boolean isGrpManaged = UserGroupTools.isGroupManaged(newVersion.format, newVersion.content);
		boolean hasPublicTag = newVersion.tags != null && newVersion.tags.contains("security:public");
		String ai = "\n- Resource has public tag? ["+hasPublicTag+"]\n- Resource is group managed? ["+isGrpManaged+"]";
		if (isGrpManaged) {
			boolean accessorIsMemberOfGroup = (UserGroupTools.accessorIsMemberOfGroup(parent, stored._id, Permission.SETUP) || getAccessorRole() == UserRole.ADMIN);
			ai+="\n- Accessor has permissions for group? ["+accessorIsMemberOfGroup+"]";
		} else {
			boolean hasSameCreator = (newVersion.creator != null && newVersion.creator.toString().equals(stored.meta.getString("creator")));
			boolean hasSameApp = (newVersion.app != null && newVersion.app.toString().equals(stored.meta.getString("app")));
			ai+="\n- Resource has same creator? ["+hasSameCreator+"]\n- Resource was created by same app? ["+hasSameApp+"]";
		}
				
		String report = getContextName()+ai+"\n- Conclusion: is update allowed for this entry of the chain? ["+result+"]\n";
		if (parent != null) return parent.getMayUpdateReport(stored, newVersion)+"\n"+report;
		return report;
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
		return RuntimeConstants.instance.publicUser;
	}
	
	@Override
	public String getOwnerName() {		
		return "Public";
	}
	@Override
	public MidataId getOwner() {
		return RuntimeConstants.instance.publicUser;
	}
	@Override
	public MidataId getOwnerPseudonymized() {
		return RuntimeConstants.instance.publicUser;
	}
	@Override
	public MidataId getSelf() {
		return RuntimeConstants.instance.publicUser;
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
	public String toString() {
		return "public("+parentString()+")";
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {		
		return null;
	}

	@Override
	public String getContextName() {
		return "Public Data Access";
	}

	@Override
	public AccessContext forPublic() throws AppException {
		return this;
	}
	
	@Override
	public boolean isUserGroupContext() {		
		return false;
	}
		
	

}
