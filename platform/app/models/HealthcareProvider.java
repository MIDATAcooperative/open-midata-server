package models;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

public class HealthcareProvider extends Model {
	
	private static final String collection = "providers";
	
	public String name;
	
	public static void add(HealthcareProvider provider) throws InternalServerException {
		Model.insert(collection, provider);
	 }
 
	public static HealthcareProvider getById(ObjectId id) throws InternalServerException {
		return Model.get(HealthcareProvider.class, collection, CMaps.map("_id", id), Sets.create("name"));
	}
	
    public static boolean existsByName(String name) throws InternalServerException {
	   return Model.exists(HealthcareProvider.class, collection, CMaps.map("name", name));
    }

}
