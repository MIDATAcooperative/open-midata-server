package utils.db;

/**
 * Defines which data model class is stored in which database
 *
 */
public class Schema {

	public static void init() {
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_USER, new String[] {"users", "researchusers", "hpusers", "admins", "providers", "research", "studykeywords", "tags", "plugins", "news", "studies", "codes", "formatinfo", "contentinfo", "formatgroups", "coding", "loinc", "usergroups", "devstats", "termsofuse", "pluginicons", "instancestats", "studyapplink"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_MAPPING, new String[] {"aps", "apslist", "sessions" });
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_RECORD, new String[] {"records", "messages", "tasks", "vrecords", "indexes"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_ACCESS, new String[] {"circles", "spaces", "participation", "keys", "keysext", "futurelogins", "consents", "groupmember", "auditevents", "subscriptions"});
	}
}
