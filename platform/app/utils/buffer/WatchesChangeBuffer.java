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

package utils.buffer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.access.DBRecord;
import utils.access.RecordLifecycle;

/**
 * Cache for changing watching APS of records
 *
 */
public class WatchesChangeBuffer {

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
			AccessLog.log("Removing watches for records #recs="+removeWatchingAps.size());
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
