package models;

import java.util.Collection;
import java.util.Collections;
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
	 public final static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "name","description","url")); 
		
	/**
	 * the name of the healthcare provider (clinic)
	 */
	public String name;
	public String description;
	public String url;
	
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

}
