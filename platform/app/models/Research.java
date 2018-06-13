package models;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model class for a research organization
 *
 */
@JsonFilter("Research")
public class Research extends Model {

	 private static final String collection = "research";
	 
	 @NotMaterialized
	 public final static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "name", "description")); 
	
	 /**
	  * name of the research organization
	  */
	 public String name;
	 
	 /**
	  * description text of the research organization
	  */
	 public String description;
	 
	 public static void add(Research research) throws InternalServerException {
			Model.insert(collection, research);
  	 }
	 
	 public static void delete(MidataId userId) throws InternalServerException {			
			Model.delete(Research.class, collection, CMaps.map("_id", userId));
	 }
	 
	 public static boolean existsByName(String name) throws InternalServerException {
		 return Model.exists(Research.class, collection, CMaps.map("name", name));
	 }
	 
	 public static boolean existsByName(String name, MidataId exclude) throws InternalServerException {
		 Research r = Model.get(Research.class, collection, CMaps.map("name", name), Sets.create("_id"));
		 return r != null && !r._id.equals(exclude);
	 }
	 
	 public static Research getById(MidataId researchid, Set<String> fields) throws InternalServerException {
			return Model.get(Research.class, collection, CMaps.map("_id", researchid), fields);
	 }
	 
	 public void set(String field, Object value) throws InternalServerException {
			Model.set(this.getClass(), collection, this._id, field, value);
	 }
	 
}
