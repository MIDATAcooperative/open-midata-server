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

import java.util.HashSet;
import java.util.Set;

import com.mongodb.BasicDBList;

import models.MidataId;
import utils.AccessLog;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;


/**
 * Functions supporting the record lifecycle
 *
 */
public class RecordLifecycle {
	
	/**
	 * Adds an APS to the watch list of a record
	 * @param rec record where watch should be added
	 * @param aps id of aps that wants to watch for changes
	 * @throws AppException
	 */
	public static void addWatchingAps(DBRecord rec, Set<MidataId> apses) throws AppException {
		if (apses.isEmpty()) return;
		DBRecord record = DBRecord.getById(rec._id, Sets.create("encWatches"));
		if (record == null) throw new InternalServerException("error.internal", "Record with id "+rec._id.toString()+" not found in database. Account or consent needs repair?");
		record.key = rec.key;
		record.security = rec.security;
		record.meta = null;
		RecordEncryption.decryptRecord(record);
		if (record.watches == null) record.watches = new BasicDBList();
		boolean changed = false;
		for (MidataId aps : apses) {
			if (!record.watches.contains(aps.toString())) {
				record.watches.add(aps.toString());
				changed = true;
			}
		}
		if (changed) {
			RecordEncryption.encryptRecord(record);
			DBRecord.set(record._id, "encWatches", record.encWatches);
		}
	}
	
	/**
	 * Removes an APS from the watch list of a record
	 * @param rec record where watch should be removed
	 * @param aps id of aps that no longer wants to watch for changes
	 * @throws AppException
	 */
	public static void removeWatchingAps(DBRecord rec, Set<MidataId> apses) throws AppException  {
		if (apses.isEmpty()) return;
		DBRecord record = DBRecord.getById(rec._id, Sets.create("encWatches"));
		record.key = rec.key;
		record.security = rec.security;
		record.meta = null;
		if (record.encWatches == null) return;
		RecordEncryption.decryptRecord(record);
		boolean changed = false;
		for (MidataId aps : apses) {
			if (record.watches.contains(aps.toString())) {
				record.watches.remove(aps.toString());
				changed = true;
			}
		}
		if (changed) {
			RecordEncryption.encryptRecord(record);
			DBRecord.set(record._id, "encWatches", record.encWatches);
		}
	}
	
	/**
	 * Touch all watching APS of a record
	 * @param rec record that has been changed
	 * @param cache APS cache for request
	 * @throws AppException
	 */
	public static void notifyOfChange(DBRecord rec, APSCache cache) throws AppException {
		if (rec.stream != null) cache.touchAPS(rec.stream);
		if (rec.watches == null) {
			AccessLog.log("no watches registered for notify of change");
			return;	
		}
		AccessLog.log("notify of change");
		
		for (Object watch : rec.watches) cache.touchConsent(MidataId.from(watch));
		
	}
		
		

}
