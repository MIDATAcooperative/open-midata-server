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

package models;

import java.util.Set;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class PersistedSession extends Model {
	
	private static final String collection = "sessions";
	private static final Set<String> ALL = Sets.create("timeout", "splitkey","user");
	
	public long timeout;
	public byte[] splitkey;
	public MidataId user;
	
	@NotMaterialized
	private String handle;	
	
	@Override
	public Object to_db_id() {
		return handle;
	}
	
	@Override
	public void set_id(Object _id) {
		this.handle = _id.toString();
	}
	
	public static PersistedSession getById(String handle) throws InternalServerException {
		return Model.get(PersistedSession.class, collection, CMaps.map("_id", handle), ALL);
	}
	
	public static void delete(String handle) throws InternalServerException {
		Model.delete(PersistedSession.class, collection, CMaps.map("_id", handle));
	}
	
	public static void deleteExpired() throws InternalServerException {
		Model.delete(PersistedSession.class, collection, CMaps.map("timeout", CMaps.map("$lt", System.currentTimeMillis())));
	}
	
	public void add() throws InternalServerException {
		Model.upsert(collection, this);
	}
	

}
