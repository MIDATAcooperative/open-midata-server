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

import java.util.Collections;

import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import utils.exceptions.AppException;

public class AppAccessContext extends AccessContext {

	private MobileAppInstance instance;
	private Plugin plugin;
		
	public AppAccessContext(MobileAppInstance instance, Plugin plugin, APSCache cache, AccessContext parent) {
		super(cache, parent);
		this.instance = instance;
		this.plugin = plugin;
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (instance.writes == null) return parent==null || parent.mayCreateRecord(record);
		if (!instance.writes.isCreateAllowed()) return false;
		
		if (instance.writes.isUnrestricted()) return parent==null || parent.mayCreateRecord(record);
		return !QueryEngine.listFromMemory(this, instance.sharingQuery, Collections.singletonList(record)).isEmpty() && (parent==null || parent.mayCreateRecord(record));
		
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		if (instance.writes == null) return true;
		if (!instance.writes.isUpdateAllowed()) return false;
		if (parent != null) return parent.mayUpdateRecord(stored, newVersion);
		return true;
	}

	@Override
	public boolean mustPseudonymize() {		
		return plugin.pseudonymize;
	}

	@Override
	public MidataId getTargetAps() {
		return instance._id;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		if (instance.writes == null) return false;
		return !QueryEngine.listFromMemory(this, instance.sharingQuery, Collections.singletonList(record)).isEmpty();
	}

	@Override
	public String getOwnerName() {		
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
	public boolean mayAccess(String content, String format) throws AppException {
		return Feature_FormatGroups.mayAccess(instance.sharingQuery, content, format);		
	}
	
	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {
		return Feature_FormatGroups.getAccessRestriction(instance.sharingQuery, content, format, field);
	}
	
    public MobileAppInstance getAppInstance() {
    	return instance;
    }

	@Override
	public boolean produceHistory() {
		return !plugin.noUpdateHistory;
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return true;
	}

	public MidataId getNewRecordCreator() {
		
		if (plugin.type.equals("external")) return plugin._id;		
		return cache.getExecutor();
	}
	
	@Override
	public String toString() {
		return "app("+instance._id+" "+parentString()+")";
	}

	
}
