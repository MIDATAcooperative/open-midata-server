package models;

/**
 * Tags that describe studies. Currently not used
 *
 */
public class StudyKeyword extends Model {
	
	//private static final String collection = "studykeywords";
	
	/**
	 * name of unique keyword
	 */
	public String name; 
	
	/**
	 * textual description of the meaning of the keyword
	 */
	public String description; 
}
