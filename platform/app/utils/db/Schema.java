package utils.db;

/**
 * Defines which data model class is stored in which database
 *
 */
public class Schema {

	public static void init() {
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_USER, new String[] {"users", "researchusers", "hpusers", "admins", "providers", "research", "studykeywords", "tags", "plugins", "pluginreviews", "news", "studies", "codes", "formatinfo", "contentinfo", "formatgroups", "coding", "loinc", "usergroups", "devstats", "termsofuse", "pluginicons", "instancestats", "studyapplink", "usagestats", "changelog"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_MAPPING, new String[] {"aps", "apslist", "sessions", "securitytokens" });
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_RECORD, new String[] {"records", "messages", "tasks", "vrecords", "indexes", "unlinkedbinaries"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_ACCESS, new String[] {"circles", "spaces", "participation", "keys", "keysext", "keyrecover", "keyprocess", "futurelogins", "consents", "groupmember", "auditevents", "subscriptions", "testcalls"});
	}
}
