package models.stats;

import java.util.Date;
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
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "version", "date", "object", "objectName", "action", "count");

	public long version;
	
	public String date;
	
	/**
	 * ID of object
	 */
	public MidataId object;
	
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
	
		
	public static UsageStats get(String date, MidataId object, UsageAction action) throws InternalServerException {
		return Model.get(UsageStats.class, collection, CMaps.map("date", date).map("object",object).map("action", action), ALL);
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
			UsageStats fromDB = UsageStats.get(this.date, this.object, this.action);
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
