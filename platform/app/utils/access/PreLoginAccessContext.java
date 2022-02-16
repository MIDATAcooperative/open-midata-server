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
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import models.enums.UserRole;
import utils.ConsentQueryTools;
import utils.exceptions.AppException;

public class PreLoginAccessContext extends AccessContext {

	private UserRole role;
	
	public PreLoginAccessContext(APSCache cache, UserRole role) {
	  	super(cache, null);
	  	this.role = role;
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return false;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return false;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {		
		return false;
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
	public boolean mayContainRecordsFromMultipleOwners() {
		return true;
	}

	@Override
	public boolean mayAccess(String content, String format) throws AppException {		
		return false;
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {		
		return null;
	}

	@Override
	public MidataId getSelf() {
		throw new NullPointerException();
	}

	@Override
	public MidataId getTargetAps() {
		throw new NullPointerException();
	}

	@Override
	public MidataId getOwner() {
		throw new NullPointerException();
	}

	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
		throw new NullPointerException();
	}

	@Override
	public String getOwnerName() throws AppException {
		throw new NullPointerException();
	}

	@Override
	public String getContextName() {
		return "pre-login";
	}
	
	@Override
	public String toString() {
		return "pre-login("+cache.getAccessor()+")";
	}

	@Override
	public UserRole getAccessorRole() {
		return role;
	}
	
	/**
	 * create child context that uses a specific app for accessing
	 * @param app
	 * @return
	 * @throws AppException
	 */
	public AccessContext forApp(MobileAppInstance app) throws AppException {	
		ConsentQueryTools.getSharingQuery(app, true);
		Plugin plugin = Plugin.getById(app.applicationId);
		return new AppAccessContext(app, plugin, getCache(), null);		
	}
			
}
