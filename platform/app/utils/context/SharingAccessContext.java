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
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import models.Space;
import models.enums.UserRole;
import utils.AccessLog;
import utils.ConsentQueryTools;
import utils.ErrorReporter;
import utils.RuntimeConstants;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.collections.RequestCache;
import utils.exceptions.AppException;

public class SharingAccessContext extends AccessContext {
	private UserRole accessorRole;	
	private MidataId legacyOwner;	
	private RequestCache requestCache;
	
	public SharingAccessContext(AccessContext from) {
		super(from.getRootCache(), null);
		this.accessorRole = from.getAccessorRole();
		this.legacyOwner = from.getLegacyOwner();
		this.requestCache = from.getRequestCache();
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
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
	public MidataId getOwner() {
		return cache.getAccountOwner();
	}

	@Override
	public MidataId getOwnerPseudonymized() {
		return cache.getAccountOwner();
	}

	@Override
	public MidataId getSelf() {
		return cache.getAccountOwner();
	}
	
	@Override
	public String getOwnerName() throws AppException {
		return null;
	}

	@Override
	public String getContextName() {		
		return "Session for "+accessorRole.toString();
	}

	@Override
	public RequestCache getRequestCache() {
		return requestCache;
	}
		
	@Override
	public MidataId getLegacyOwner() {
		return legacyOwner;
	}

	@Override
	public MidataId getTargetAps() {
		return cache.getAccountOwner();
	}
	
	@Override
	public UserRole getAccessorRole() {		
		return accessorRole;
	}
	
	/**
	 * create child context that uses a specific app for accessing
	 * @param app
	 * @return
	 * @throws AppException
	 */
	public AccessContext forApp(MobileAppInstance app) throws AppException {	
		ConsentQueryTools.getSharingQuery(app, false);
		Plugin plugin = Plugin.getById(app.applicationId);
		return new AppAccessContext(app, plugin, getCache(), this);		
	}
	
	/**
	 * create child context that uses a specific Space for accessing
	 * @param space
	 * @param self
	 * @return
	 */
	public AccessContext forSpace(Space space, MidataId self) {		
		return new SpaceAccessContext(space, getCache(), this, self);		
	}
	
	@Override
	public String toString() {
		return "sharing(accessor="+cache.getAccessor()+")";
	}
		

	@Override
	public MidataId getUsedPlugin() {
		return RuntimeConstants.instance.portalPlugin;
	}

	@Override
	public void cleanup() {
		AccessLog.log("[session] close");
		try {
			getCache().finishTouch();
		} catch (AppException e) {
		 	AccessLog.logException("clearCache", e);
		 	ErrorReporter.report("context clean up", null, e);
		}
		try {
		   requestCache.save();
		} catch (AppException e) {
		   AccessLog.logException("requestCache", e);
		   ErrorReporter.report("context clean up", null, e);
		}
	}
	
	
	
	
}
