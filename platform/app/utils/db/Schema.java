/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.db;

/**
 * Defines which data model class is stored in which database
 *
 */
public class Schema {

	public static void init() {
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_USER, new String[] {"users", "researchusers", "hpusers", "admins", "providers", "research", "studykeywords", "tags", "plugins", "pluginreviews", "news", "studies", "codes", "formatinfo", "contentinfo", "formatgroups", "groupcontent", "coding", "loinc", "usergroups", "devstats", "termsofuse", "pluginicons", "instancestats", "studyapplink", "usagestats", "changelog", "bulkmails", "deployreport", "ratelimited", "csvdef", "monitorstats", "monitoringevent" });
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_MAPPING, new String[] {"aps", "apslist", "apsextra", "apshistory", "sessions", "securitytokens", "bactions" });
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_RECORD, new String[] {"records", "messages", "tasks", "vrecords", "indexes", "unlinkedbinaries"});
	    DBLayer.setCollectionsForDatabase(DBLayer.DB_ACCESS, new String[] {"circles", "spaces", "participation", "keys", "keysext", "keyrecover", "keyprocess", "futurelogins", "consents", "vconsents", "consentreshare", "groupmember", "auditevents", "subscriptions", "testcalls", "licenses", "serviceinstances", "usedcodes"});
	}
}
