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

import java.util.Collections;
import java.util.Set;

import models.enums.AuditEventType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;

public class BackgroundAction extends Model {

	@NotMaterialized
	private static final String collection = "bactions";
	
	@NotMaterialized
	public static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "targetId", "type", "created", "owner", "session", "resource"));
	
	/**
	 * which entity needs to be processed
	 */
	public MidataId targetId;
	
	public AuditEventType type;
	
	public long created;
	
	public String resource;
	
	/**
	 * Session information
	 */
	public byte[] session;
	
	/**
	 * Session owner information
	 */
	public MidataId owner;
	
	public static Set<BackgroundAction> getAll() throws AppException {
		return Model.getAll(BackgroundAction.class, collection, CMaps.map(), ALL);
	}
	
	public void add() throws AppException {
		Model.insert(collection, this);
	}
	
	public void delete() throws AppException {
		Model.delete(BackgroundAction.class, collection, CMaps.map("_id", this._id));
	}
}
