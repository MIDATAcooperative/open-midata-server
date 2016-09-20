package models;

import java.util.Set;

import models.MidataId;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.exceptions.InternalServerException;
import utils.search.Search;
import utils.search.SearchException;
import utils.search.Search.Type;

/**
 * data model class for a research organization
 *
 */
@JsonFilter("Research")
public class Research extends Model {

	 private static final String collection = "research";
	
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
	 
	 public static boolean existsByName(String name) throws InternalServerException {
		 return Model.exists(Research.class, collection, CMaps.map("name", name));
	 }
	 
	 public static Research getById(MidataId researchid, Set<String> fields) throws InternalServerException {
			return Model.get(Research.class, collection, CMaps.map("_id", researchid), fields);
		}
	 
}
