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

package utils.access.index;

import java.util.ArrayList;
import java.util.List;
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
 * Data model of an index definition 
 *
 */
public class IndexDefinition extends IndexPageModel {
    public @NotMaterialized static final Set<String> ALL = Sets.create("owner", "formats", "fields", "pseudonymize", "enc", "encTs", "version", "rev", "created");
	
	
	public String owner;
	
		
	/**
	 * For which record formats does this index apply?
	 */
	public List<String> formats;
	
	/**
	 * Which fields are included in the index?
	 */
	public List<String> fields;
	
	public boolean pseudonymize;
	
	public byte[] encTs;
	
	private @NotMaterialized List<String[]> fieldsSplit;
	
	public List<String[]> getFieldsSplit() {
		if (fieldsSplit == null) {
		  fieldsSplit = new ArrayList<String[]>(fields.size());
		  for (String f : fields) fieldsSplit.add(f.split("\\|"));
		}
		return fieldsSplit;
	}
		
	public static Set<IndexDefinition> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(IndexDefinition.class, collection, properties, fields);
	}
	
	public static void delete(MidataId id) throws InternalServerException {		
		Model.delete(IndexDefinition.class, collection, CMaps.map("_id", id));
	}
	
	public static IndexDefinition getById(MidataId pageId) throws InternalServerException {
		return Model.get(IndexDefinition.class, collection, CMaps.map("_id", pageId), ALL);
	}
	
	public void updateTs() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "lockTime", "encTs");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal.db", e);
		}
	}
		
}
