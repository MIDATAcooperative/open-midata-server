package utils.auth.auth2factor;

import java.util.Set;

import models.MidataId;
import models.Model;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Security Token for 2-factor authentication
 *
 */
public class SecurityToken extends Model {

	@NotMaterialized
	private static final String collection = "securitytokens";
	
	@NotMaterialized
	private static final Set<String> ALL = Sets.create("token", "created", "failedAttempts");
	
	/**
	 * the token
	 */
	public String token;
	
	/**
	 * time of creation
	 */
	public long created;
	
	/**
	 * Number of failed input attempts
	 */
	public int failedAttempts;
	
	public void add() throws InternalServerException {
		Model.upsert(collection, this);
	}
	
	public static SecurityToken getById(MidataId id) throws InternalServerException {
		return Model.get(SecurityToken.class, collection, CMaps.map("_id", id), ALL);
	}
	
	public static void delete(MidataId id) throws InternalServerException {
		Model.delete(SecurityToken.class, collection, CMaps.map("_id", id));
	}
	
	public void failedAttempt() throws InternalServerException {
		this.failedAttempts++;
		Model.set(SecurityToken.class, collection, this._id, "failedAttempts", this.failedAttempts);
	}
}
