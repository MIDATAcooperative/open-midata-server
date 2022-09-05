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
import models.enums.UserRole;
import utils.AccessLog;
import utils.ConsentQueryTools;
import utils.ErrorReporter;
import utils.RuntimeConstants;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.collections.RequestCache;
import utils.exceptions.AppException;

public class PreLoginAccessContext extends AccessContext {

	private UserRole role;
	private RequestCache requestCache = new RequestCache();
	private MidataId usedPlugin;
	
	public PreLoginAccessContext(APSCache cache, UserRole role, MidataId usedPlugin) {
	  	super(cache, null);
	  	this.role = role;
	  	this.usedPlugin = usedPlugin;
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return record.meta.getString("format").equals("fhir/Patient");
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return false;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {		
		return stored.meta.getString("format").equals("fhir/Patient");
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
	public MidataId getTargetAps() {
		throw new NullPointerException();
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
		return "Pre-Login access";
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
		ConsentQueryTools.getSharingQuery(app, false);
		Plugin plugin = Plugin.getById(app.applicationId);
		return new AppAccessContext(app, plugin, getCache(), this);		
	}
	
	@Override
	public RequestCache getRequestCache() {
		return requestCache;
	}
	
	public MidataId getUsedPlugin() {
		if (usedPlugin != null) return usedPlugin;
		return RuntimeConstants.instance.portalPlugin; 
	}
	
	@Override
	public void cleanup() {
		AccessLog.log("[pre-login session] close");
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
