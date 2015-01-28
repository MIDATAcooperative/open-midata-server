package utils.db;

public class Schema {

	public static void init() {
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_USER, new String[] {"users", "researchusers", "hpusers", "admins", "providers", "memberkeys", "research", "studykeywords", "tags", "apps", "visualizations", "news"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_MAPPING, new String[] {"test"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_RECORD, new String[] {"studies", "records", "participation", "codes", "messages"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_ACCESS, new String[] {"circles", "spaces"});	  
	}
}
