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

import java.util.Date;
import java.util.List;

import org.bson.BasicBSONObject;

import models.MidataId;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.DummyAccessContext;
import utils.exceptions.AppException;

public class Feature_Expiration {

	public static void setup(APS apswrapper) throws AppException {
		BasicBSONObject query = apswrapper.getMeta("_query");
		if (query != null && query.containsField("keep-only")) {
			apswrapper.setMeta("_expire", CMaps.map("count", query.get("keep-only")));
		}
	}
	
	public static void check(APSCache cache, APS apswrapper) throws AppException {
		BasicBSONObject query = apswrapper.getMeta("_expire");
		if (query != null) {
			int count = query.getInt("count");
			Date last = query.getDate("last-done");
			if (last == null || last.before(new Date(System.currentTimeMillis() - 1000l * 60l * 60l * 24l))) {
				expireOldRecords(cache, apswrapper.getId(), count);
				apswrapper.setMeta("_expire", CMaps.map("count", count).map("last-done", new Date()));
			}
		}
	}
	
	public static void expireOldRecords(APSCache cache, MidataId aps, int count) throws AppException {
    	List<DBRecord> result = QueryEngine.listInternal(cache, aps, new DummyAccessContext(cache), CMaps.map("sort", "lastUpdated desc"), Sets.create("_id"));
    	if (result.size() > count) {
    		result.subList(0, count).clear();
    		APS target = cache.getAPS(aps);            
            target.removePermission(result);            
    	}
    }
}
