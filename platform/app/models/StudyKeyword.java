package models;

public class StudyKeyword extends Model {
	
	private static final String collection = "studykeywords";
	
	public String name; //unique. Keyword
	public String description; //Description to be shown to users to make choice easier.
}
