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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.EntityType;
import models.enums.UserStatus;
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
	
	public static final @NotMaterialized Set<String> NON_DELETED = Collections.unmodifiableSet(Sets.create(UserStatus.ACTIVE.toString(), UserStatus.NEW.toString(), UserStatus.BLOCKED.toString(), UserStatus.TIMEOUT.toString(), null));
	
	@NotMaterialized
	 public final static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "name","description","url","parent","status","city", "zip", "country", "address1", "address2", "phone", "mobile")); 
		
	/**
	 * the name of the healthcare provider (clinic)
	 */
	public String name;
	public String description;
	public String url;
	public MidataId parent;
	public MidataId managerId;
	public EntityType managerType;
	public UserStatus status;
	
	/**
	 * additional identifiers
	 */
	@NotMaterialized public List<String> identifiers;
	
	/**
	 * City of clinic address
	 */
	public String city;	 
	
	/**
	 * Zip code of clinic address
	 */
	public String zip;	 
	
	/**
	 * Country of clinic address
	 */
	public String country;
	
	/**
	 * Address line 1 (Street) of clinic address
	 */
	public String address1;
	
	/**
	 * Address line 2 of clinic address
	 */
	public String address2;
	
	/**
	 * Phone number of clinic
	 */
	public String phone;
	
	/**
	 * Mobile phone number of clinic
	 */
	public String mobile;
	
	public static void add(HealthcareProvider provider) throws InternalServerException {
		Model.insert(collection, provider);
	}
	
	public static void delete(MidataId userId) throws InternalServerException {			
		Model.delete(HealthcareProvider.class, collection, CMaps.map("_id", userId));
    }
 
	public static HealthcareProvider getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(HealthcareProvider.class, collection, CMaps.map("_id", id).map("status", NON_DELETED), fields);
	}
	
	public static HealthcareProvider getByIdAlsoDeleted(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(HealthcareProvider.class, collection, CMaps.map("_id", id), fields);
	}
	
    public static boolean existsByName(String name) throws InternalServerException {
	   return Model.exists(HealthcareProvider.class, collection, CMaps.map("name", name).map("status", NON_DELETED));
    }
    
    public static HealthcareProvider getByName(String name) throws InternalServerException {
 	   return Model.get(HealthcareProvider.class, collection, CMaps.map("name", name).map("status", NON_DELETED), ALL);
     }
    
    public static boolean existsByName(String name, MidataId exclude) throws InternalServerException {
    	 HealthcareProvider r = Model.get(HealthcareProvider.class, collection, CMaps.map("name", name).map("status", NON_DELETED), Sets.create("_id"));
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
