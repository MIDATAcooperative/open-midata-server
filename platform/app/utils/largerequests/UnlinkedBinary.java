package utils.largerequests;

import java.util.List;
import java.util.Set;

import models.AccessPermissionSet;
import models.MidataId;
import models.Model;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * A binary file that has been uploaded but is not linked from any record
 *
 */
public class UnlinkedBinary extends Model {
	
	private static final String collection = "unlinkedbinaries";
	public @NotMaterialized static final long EXPIRATION = 1000 * 60 * 60 * 8; 
	public @NotMaterialized static final Set<String> ALL_FIELDS = Sets.create("_id", "created", "owner");
	

	/* Id is same as id of file */
	
	/**
	 * Time of creation
	 */
	public long created;
	
	/**
	 * Owner of file
	 */
	public MidataId owner;
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static UnlinkedBinary getById(MidataId id) throws InternalServerException {
		return Model.get(UnlinkedBinary.class, collection, CMaps.map("_id", id), ALL_FIELDS);
	}
	
	public void delete() throws InternalServerException {
		Model.delete(UnlinkedBinary.class, collection, CMaps.map("_id", this._id));
	}
	
	public static List<UnlinkedBinary> getExpired() throws InternalServerException {
		return Model.getAllList(UnlinkedBinary.class, collection, CMaps.map("created", CMaps.map("$lt", System.currentTimeMillis() - EXPIRATION)), ALL_FIELDS, 1000);
	}
	
	public boolean isExpired() {
		return this.created < System.currentTimeMillis() - EXPIRATION;
	}
}
