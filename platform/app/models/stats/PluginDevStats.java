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

package models.stats;

import java.util.Map;
import java.util.Set;

import models.MidataId;
import models.Model;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Statistical developer information about plugin query execution
 *
 */
public class PluginDevStats extends Model {

	
	protected @NotMaterialized static final String collection = "devstats";
	/**
	 * constant set containing all fields of this class
	 */
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "action", "comments", "count", "lastExecTime", "firstrun", "lastrun", "params", "plugin", "resultCount", "queryCount", "totalExecTime", "conflicts", "db");

	
	/**
	 * ID of plugin
	 */
	public MidataId plugin;
	
	/**
	 * Action executed. (fhir method+path)
	 */
	public String action;
	
	/**
	 * Parameters
	 */
	public String params;
	
	/**
	 * Timestamp of first protokolled query
	 */
	public long firstrun;	
	
	/**
	 * Timestamp query last run
	 */
	public long lastrun;	
	
	/**
	 * Number of times query has been run
	 */
	public int count;
	
	/**
	 * Number of conflicts
	 */
	public int conflicts;
	
	/**
	 * Number of internal queries
	 */
	public int db;
	
	/**
	 * Comments from system
	 */
	public Set<String> comments;
	
	/**
	 * Request Results
	 */
	public Map<String, Integer> resultCount;
	
	/**
	 * Query count
	 */
	public Map<String, Integer> queryCount;	
	
	/**
	 * Sum of all execution times
	 */
	public long totalExecTime;
	
	/**
	 * Last execution time
	 */
	public long lastExecTime;
	
	public static PluginDevStats getByRequest(MidataId plugin, String action, String params) throws InternalServerException {
		return Model.get(PluginDevStats.class, collection, CMaps.map("plugin", plugin.toDb()).map("action", action).map("params", params), ALL);
	}
	
	public void update() throws InternalServerException, LostUpdateException {		
		try {
			DBLayer.upsert(collection, this);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	public static Set<PluginDevStats> getByPlugin(MidataId plugin, Set<String> fields) throws InternalServerException {
		return Model.getAll(PluginDevStats.class, collection, CMaps.map("plugin", plugin.toDb()), ALL);
	}
	
	public static void deleteByPlugin(MidataId plugin) throws InternalServerException {
		Model.delete(PluginDevStats.class, collection, CMaps.map("plugin", plugin.toDb()));
	}
}
