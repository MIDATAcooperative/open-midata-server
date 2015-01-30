package models;

import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.search.Search;
import utils.search.SearchException;
import utils.search.Search.Type;

public class Research extends Model {

	 private static final String collection = "research";
	
	 public String name;
	 public String description;
	 
	 public static void add(Research research) throws ModelException {
			Model.insert(collection, research);
  	 }
	 
	 public static boolean existsByName(String name) throws ModelException {
		 return Model.exists(collection, CMaps.map("name", name));
	 }
	 
	 public static Research getById(ObjectId researchid, Set<String> fields) throws ModelException {
			return Model.get(Research.class, collection, CMaps.map("_id", researchid), fields);
		}
	 
}
