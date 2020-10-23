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

package utils.auth;

import java.util.Set;

import models.JsonSerializable;
import models.MidataId;
import models.Model;
import models.PersistedSession;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

public class FutureLogin extends Model {
	
	private static final String collection = "futurelogins";
	private static final Set<String> ALL = Sets.create("user", "intPart", "extPartEnc");
	
	public MidataId user;
	
	public byte[] intPart;
	
	public byte[] extPartEnc;
	
	public void set() throws InternalServerException {
		Model.upsert(collection, this);
	}
	
	public static FutureLogin getById(MidataId user) throws InternalServerException {
		return Model.get(FutureLogin.class, collection, CMaps.map("_id", user), ALL);
	}
	
	public static void delete(MidataId user) throws InternalServerException {
		Model.delete(PersistedSession.class, collection, CMaps.map("_id", user));
	}		
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
	
}
