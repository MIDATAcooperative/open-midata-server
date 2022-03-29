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

package utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import models.MidataId;
import models.Plugin;
import models.enums.PluginStatus;
import utils.exceptions.InternalServerException;

public class PluginLoginCache {
	
	private static Map<MidataId, Plugin> cache = new ConcurrentHashMap<MidataId, Plugin>();
	private static Map<String, Plugin> cacheFilename = new ConcurrentHashMap<String, Plugin>();

	public static Plugin getByFilename(String name) throws InternalServerException {
		if (name == null) return null;
		Plugin result = cacheFilename.get(name);
		if (result == null) {
			result = Plugin.getByFilename(name, Plugin.FOR_LOGIN);
			if (result != null) {
			  cache.put(result._id, result);
			  cacheFilename.put(result.filename, result);
			}
		}
		if (result != null && result.status != PluginStatus.DELETED) return result;
		return null;
	}
	
	public static Plugin getById(MidataId id) throws InternalServerException {
		if (id == null) return null;
		
		Plugin result = cache.get(id);
		if (result != null) return result;
		
		result = Plugin.getById(id, Plugin.FOR_LOGIN);
		if (result == null) return null;
		cache.put(id, result);
		cacheFilename.put(result.filename, result);
		return result;		
	}
	
	public static void clear() {
		cache.clear();
		cacheFilename.clear();
	}
}
