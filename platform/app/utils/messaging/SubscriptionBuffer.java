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

package utils.messaging;

import java.util.HashMap;
import java.util.Map;

import models.MidataId;

public class SubscriptionBuffer {

	 private Map<MidataId, ResourceChange> cache = new HashMap<MidataId, ResourceChange>();
	 
	 public void add(ResourceChange change) {
		 cache.put(change.getResource()._id, change);
	 }
	 
	 public void remove(MidataId resourceId) {
		 cache.remove(resourceId);
	 }
	 
	 public void flush() {
		 for (ResourceChange change : cache.values()) {
			 SubscriptionManager.resourceChange(change);
		 }
	 }
	 
	 
}
