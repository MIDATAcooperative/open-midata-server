package utils.access.index;

import java.util.Set;

import org.bson.BSONObject;

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
 * Data model for an index page
 *
 */
public class IndexPageModel extends Model {

	protected @NotMaterialized static final String collection = "indexes";
	private final static Set<String> ALL_PAGE = Sets.create("version", "enc");
	
	/**
	 * Last updated version number to prevent lost updates
	 */
	public long version;
	
	/**
	 * encrypted data
	 */
	public byte[] enc;
	
	/**
	 * Unencrypted data of index
	 */
	public @NotMaterialized BSONObject unencrypted;
	
	public static void add(IndexPageModel def) throws InternalServerException {
		Model.insert(collection, def);				
	}
	
	public static IndexPageModel getById(MidataId pageId) throws InternalServerException {
		return Model.get(IndexPageModel.class, collection, CMaps.map("_id", pageId), ALL_PAGE);
	}
	
	public void update() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "enc");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal.db", e);
		}
	}
	
	
	// array of { key : array , entries : [ { rec :   , consent :  } ] or page : IndexPageId } 
}
