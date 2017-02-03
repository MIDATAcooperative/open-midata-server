package models;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

/**
 * data model for healthcare providers. This class is not for the individual person but for the clinic
 * 
 *
 */
public class HealthcareProvider extends Model {
	
	private static final String collection = "providers";
	
	/**
	 * the name of the healthcare provider (clinic)
	 */
	public String name;
	
	public static void add(HealthcareProvider provider) throws InternalServerException {
		Model.insert(collection, provider);
	}
	
	public static void delete(MidataId userId) throws InternalServerException {			
		Model.delete(HealthcareProvider.class, collection, CMaps.map("_id", userId));
    }
 
	public static HealthcareProvider getById(MidataId id) throws InternalServerException {
		return Model.get(HealthcareProvider.class, collection, CMaps.map("_id", id), Sets.create("name"));
	}
	
    public static boolean existsByName(String name) throws InternalServerException {
	   return Model.exists(HealthcareProvider.class, collection, CMaps.map("name", name));
    }

}
