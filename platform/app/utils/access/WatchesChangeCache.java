package utils.access;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import utils.AccessLog;
import utils.ErrorReporter;

/**
 * Cache for changing watching APS of records
 *
 */
public class WatchesChangeCache {

	private Map<DBRecord, Set<MidataId>> addWatchingAps;
	private Map<DBRecord, Set<MidataId>> removeWatchingAps;
	
	/**
	 * flush all changes to DB
	 */
	public void save() {
		if (addWatchingAps != null) {
			AccessLog.log("Adding watches for records #recs="+addWatchingAps.size());
			for (Map.Entry<DBRecord, Set<MidataId>> entry : addWatchingAps.entrySet()) {
				try {
				  RecordLifecycle.addWatchingAps(entry.getKey(), entry.getValue());
				} catch (Exception e) {
					ErrorReporter.report("permission cache flush", null, e);					
				}
			}
			addWatchingAps = null;
		}
		if (removeWatchingAps != null) {
			AccessLog.log("Removing watches for records #recs="+addWatchingAps.size());
			for (Map.Entry<DBRecord, Set<MidataId>> entry : removeWatchingAps.entrySet()) {
				try {
				   RecordLifecycle.removeWatchingAps(entry.getKey(), entry.getValue());
				} catch (Exception e) {
					ErrorReporter.report("permission cache flush", null, e);					
				}
			}
			removeWatchingAps = null;
		}				
	}
	
	/**
	 * Add a watching APS for a record
	 * @param rec
	 * @param aps
	 */
	public void addWatchingAps(DBRecord rec, MidataId aps) {
		if (removeWatchingAps != null) save();
		if (addWatchingAps == null) addWatchingAps = new HashMap<DBRecord, Set<MidataId>>();
		Set<MidataId> apses = addWatchingAps.get(rec);
		if (apses == null) {
			apses = new HashSet<MidataId>();
			addWatchingAps.put(rec, apses);
		}
		apses.add(aps);
	}
	
	/**
	 * Remove a watching APS for a record
	 * @param rec
	 * @param aps
	 */
	public void removeWatchingAps(DBRecord rec, MidataId aps)  {
		if (addWatchingAps != null) save();
		if (removeWatchingAps == null) removeWatchingAps = new HashMap<DBRecord, Set<MidataId>>();
		Set<MidataId> apses = removeWatchingAps.get(rec);
		if (apses == null) {
			apses = new HashSet<MidataId>();
			removeWatchingAps.put(rec, apses);
		}
		apses.add(aps);
	}
}
