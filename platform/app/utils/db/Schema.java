package utils.db;

public class Schema {

	public static void init() {
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_USER, new String[] {"users", "researchusers", "hpusers", "admins", "providers", "memberkeys", "research", "studykeywords", "tags", "apps", "visualizations", "news", "studies", "codes", "formats", "formatgroups"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_MAPPING, new String[] {"aps"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_RECORD, new String[] {"records", "messages"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_ACCESS, new String[] {"circles", "spaces", "participation", "keys"});	  
	}
}
