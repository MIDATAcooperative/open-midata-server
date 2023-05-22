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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model for healthcare providers. This class is not for the individual person but for the clinic
 * 
 *
 */
public class HealthcareProvider extends Model {
	
	private static final String collection = "providers";
	
	@NotMaterialized
	 public final static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "name","description","url","parent")); 
		
	/**
	 * the name of the healthcare provider (clinic)
	 */
	public String name;
	public String description;
	public String url;
	public MidataId parent;
	
	public static void add(HealthcareProvider provider) throws InternalServerException {
		Model.insert(collection, provider);
	}
	
	public static void delete(MidataId userId) throws InternalServerException {			
		Model.delete(HealthcareProvider.class, collection, CMaps.map("_id", userId));
    }
 
	public static HealthcareProvider getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(HealthcareProvider.class, collection, CMaps.map("_id", id), fields);
	}
	
    public static boolean existsByName(String name) throws InternalServerException {
	   return Model.exists(HealthcareProvider.class, collection, CMaps.map("name", name));
    }
    
    public static boolean existsByName(String name, MidataId exclude) throws InternalServerException {
    	HealthcareProvider r = Model.get(HealthcareProvider.class, collection, CMaps.map("name", name), Sets.create("_id"));
		 return r != null && !r._id.equals(exclude);
	 }
    
    public void set(String field, Object value) throws InternalServerException {
		Model.set(this.getClass(), collection, this._id, field, value);
    }
    
    public void setMultiple(Collection<String> fields) throws InternalServerException {
    	this.setMultiple(collection, fields);
    }
    
    public static Set<HealthcareProvider> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		 return Model.getAll(HealthcareProvider.class, collection, properties, fields);
	 }

}
