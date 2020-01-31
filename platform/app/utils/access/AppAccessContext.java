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
		return false;
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
}
