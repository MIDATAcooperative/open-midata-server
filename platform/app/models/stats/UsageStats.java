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

package models.stats;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.DuplicateKeyException;

import models.MidataId;
import models.Model;
import models.enums.UsageAction;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class UsageStats extends Model {

	
	protected @NotMaterialized static final String collection = "usagestats";
	/**
	 * constant set containing all fields of this class
	 */
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "version", "date", "object", "objectName", "detail", "action", "count");

	public long version;
	
	public String date;
	
	/**
	 * ID of object
	 */
	public MidataId object;
	
	/**
	 * ID of detail-object or null
	 */
	public MidataId detail;
	
	/**
	 * Name of object
	 */
	public String objectName;
	
	/**
	 * Action executed.
	 */
	public UsageAction action;
	
		
	/**
	 * Number of times query has been run
	 */
	public int count;
	
		
	public static UsageStats get(String date, MidataId object, MidataId detail, UsageAction action) throws InternalServerException {
		return Model.get(UsageStats.class, collection, CMaps.map("date", date).map("object",object).map("detail",detail).map("action", action), ALL);
	}
	
	public static List<UsageStats> getByDate(String date) throws InternalServerException {
		return Model.getAllList(UsageStats.class, collection, CMaps.map("date", date), ALL, 2000, "action", 1);
	}
	
	public static List<UsageStats> getByPlugin(MidataId plugin) throws InternalServerException {
		return Model.getAllList(UsageStats.class, collection, CMaps.map("object", plugin), ALL, 2000, "date", 1);
	}
	
	public static Set<UsageStats> getAll(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.getAll(UsageStats.class, collection, properties, ALL);
	}
	
	public void add() throws InternalServerException {		
		try {
			UsageStats fromDB = UsageStats.get(this.date, this.object, this.detail, this.action);
			if (fromDB != null) {
				fromDB.count+=this.count;
				DBLayer.secureUpdate(fromDB, collection, "version", "count");
			} else {
				this._id = new MidataId();
				DBLayer.insert(collection, this);
			}						
		} catch (LostUpdateException e) {
			add();
		} catch (DuplicateKeyException e3) {
			add();
		} catch (DatabaseException e2) {
			throw new InternalServerException("error.internal", e2);
		}
	}
			
}
