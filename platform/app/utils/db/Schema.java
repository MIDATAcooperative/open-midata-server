package utils.db;

/**
 * Defines which data model class is stored in which database
 *
 */
public class Schema {

	public static void init() {
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_USER, new String[] {"users", "researchusers", "hpusers", "admins", "providers", "research", "studykeywords", "tags", "plugins", "news", "studies", "codes", "formatinfo", "contentinfo", "formatgroups"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_MAPPING, new String[] {"aps"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_RECORD, new String[] {"records", "messages", "tasks"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_ACCESS, new String[] {"circles", "spaces", "participation", "keys", "consents"});	  
	}
}
