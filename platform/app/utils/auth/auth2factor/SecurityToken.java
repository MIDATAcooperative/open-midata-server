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
