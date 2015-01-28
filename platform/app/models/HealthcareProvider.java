package models;

import utils.collections.CMaps;

public class HealthcareProvider extends Model {
	
	private static final String collection = "providers";
	
	public String name;
	
	public static void add(HealthcareProvider provider) throws ModelException {
		Model.insert(collection, provider);
	 }
 
    public static boolean existsByName(String name) throws ModelException {
	   return Model.exists(collection, CMaps.map("name", name));
    }

}
