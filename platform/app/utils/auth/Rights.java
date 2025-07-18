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

package utils.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Plugin;
import models.enums.UserRole;
import utils.collections.Sets;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

/**
 * class that defines which user role is allowed to do which actions and to query which fields
 *
 */
public class Rights {

	private static Map<String, Map<UserRole, Set<String>>> allowed;

	private static String currentAction;
	private static void action(String actionName) {
		allowed.put(actionName, new HashMap<UserRole, Set<String>>());
		currentAction = actionName;
	}
	
	private static void role(UserRole role, String... fields) {
		allowed.get(currentAction).put(role, Sets.create(fields));
	}
	
	private static void role(UserRole role, Set<String> fields) {
		allowed.get(currentAction).put(role, fields);
	}
		
	static {
		allowed = new HashMap<String, Map<UserRole, Set<String>>>();
		
		action("Users.get");
		role(UserRole.MEMBER, "_id", "role", "firstname", "lastname", "name", "status", "email","searchable", "testUserApp", "testUserCustomer");
		role(UserRole.RESEARCH, "_id", "role", "firstname", "lastname", "name", "status", "email", "searchable", "testUserApp", "testUserCustomer");
		role(UserRole.DEVELOPER, "_id", "role", "firstname", "lastname", "name", "email", "developer", "status", "searchable", "testUserApp", "testUserCustomer");
		role(UserRole.ADMIN, "_id", "address1", "address2", "city", "contractStatus", "agbStatus", "country", "email", "emailLC", "firstname", "gender", "lastname", "login", "mobile", "name", "phone", "registeredAt", "role", "subroles", "security", "status", "zip", "midataID", "birthday", "confirmationCode", "emailStatus", "mobileStatus", "confirmedAt", "reason", "coach", "developer", "language", "searchable", "authType", "notifications", "marketingEmail", "testUserApp", "testUserCustomer");
		role(UserRole.PROVIDER, "_id", "address1", "address2", "city", "contractStatus", "agbStatus", "country", "email", "emailLC", "firstname", "gender", "lastname", "login", "mobile", "name", "phone", "role", "subroles", "status", "zip", "midataID", "birthday", "searchable", "testUserApp", "testUserCustomer");
		
		action("Users.getPROVIDER");
		role(UserRole.MEMBER  , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "phone", "mobile", "name", "role", "subroles", "status", "zip", "testUserApp", "testUserCustomer");
		role(UserRole.PROVIDER, "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "phone", "mobile", "name", "role", "subroles", "status", "zip", "provider", "testUserApp", "testUserCustomer");
		role(UserRole.DEVELOPER, "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "phone", "mobile", "name", "role", "subroles", "status", "zip", "provider","searchable", "developer", "testUserApp", "testUserCustomer");
		role(UserRole.ADMIN, "_id", "address1", "address2", "city", "contractStatus", "agbStatus", "country", "email", "emailLC", "firstname", "gender", "lastname", "login", "mobile", "name", "phone", "registeredAt", "role", "subroles", "security", "status", "zip", "midataID", "birthday", "confirmationCode", "emailStatus", "mobileStatus", "confirmedAt", "reason", "coach", "developer", "language", "searchable", "authType", "notifications", "marketingEmail", "testUserApp", "testUserCustomer");		
		
		action("Users.getRESEARCH");
		role(UserRole.MEMBER, "_id", "role", "firstname", "lastname", "name", "status", "email", "searchable", "testUserApp", "testUserCustomer");
		role(UserRole.RESEARCH, "_id", "role", "firstname", "lastname", "name", "status", "email", "organization", "searchable", "testUserApp", "testUserCustomer");
		role(UserRole.DEVELOPER, "_id", "role", "firstname", "lastname", "name", "status", "email", "searchable", "developer", "testUserApp", "testUserCustomer");		
		
		action("Users.getSelf");
		role(UserRole.MEMBER, "_id", "address1", "address2", "apps", "city", "emailStatus", "mobileStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "birthday", "midataID", "news", "partInterest", "ssn", "language", "searchable", "confirmedAt", "developer", "authType", "notifications", "marketingEmail", "flags", "termsAgreed", "testUserApp", "testUserCustomer");
		role(UserRole.RESEARCH, "_id", "address1", "address2", "apps", "city", "emailStatus", "mobileStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "birthday", "midataID", "language", "searchable", "confirmedAt", "organization", "developer", "authType", "notifications", "marketingEmail", "flags", "termsAgreed", "testUserApp", "testUserCustomer"); // TODO Remove midataID 
		role(UserRole.PROVIDER, "_id", "address1", "address2", "apps", "city", "emailStatus", "mobileStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "birthday", "midataID", "language", "searchable", "confirmedAt", "developer", "authType", "notifications", "marketingEmail", "flags", "termsAgreed", "testUserApp", "testUserCustomer"); // TODO Remove midataID
		role(UserRole.DEVELOPER, "_id", "address1", "address2", "apps", "city", "emailStatus", "mobileStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "birthday", "midataID", "language", "searchable", "confirmedAt", "developer", "authType", "notifications", "marketingEmail", "flags", "termsAgreed", "testUserApp", "testUserCustomer"); // TODO Remove midataID
		role(UserRole.ADMIN, "_id", "address1", "address2", "apps", "city", "emailStatus", "mobileStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "birthday", "midataID", "language", "searchable", "confirmedAt", "developer", "authType", "notifications", "marketingEmail", "flags", "termsAgreed", "confirmationCode", "testUserApp", "testUserCustomer"); // TODO Remove midataID
		
		action("HealthProvider.search");
		role(UserRole.MEMBER  , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.RESEARCH, "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.PROVIDER, "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.ADMIN   , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		
		action("Studies.search");
		role(UserRole.RESEARCH, "_id", "name", "type", "joinMethods", "code", "owner", "createdBy", "createdAt", "description", "infos", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "anonymous", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "groups", "termsOfUse", "requirements", "startDate", "endDate", "dataCreatedBefore", "autoJoinGroup", "autoJoinTestGroup", "leavePolicy", "rejoinPolicy");
		role(UserRole.ADMIN, "_id", "name", "type", "joinMethods", "code", "owner", "createdBy", "createdAt", "description", "infos", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "anonymous", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "groups", "termsOfUse", "requirements", "startDate", "endDate", "dataCreatedBefore", "autoJoinGroup", "autoJoinTestGroup", "leavePolicy", "rejoinPolicy");
		role(UserRole.PROVIDER, "_id", "name", "type", "joinMethods", "code", "owner", "createdAt", "description", "infos", "studyKeywords", "recordQuery", "requiredInformation", "anonymous", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "termsOfUse", "requirements", "startDate", "endDate", "dataCreatedBefore", "leavePolicy", "rejoinPolicy");
		role(UserRole.MEMBER, "_id", "name", "type", "joinMethods", "code", "owner", "createdAt", "description", "infos", "studyKeywords", "recordQuery", "requiredInformation", "anonymous", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "termsOfUse", "requirements", "startDate", "endDate", "dataCreatedBefore", "leavePolicy", "rejoinPolicy");
		
		action("getRecords");
		role(UserRole.ANY, "_id", "id", "owner" , "ownerName", "creatorName", "format", "subformat", "content", "code", "group", "app", "creator", "creatorOrg", "created", "lastUpdated", "version", "name", "description", "tags", "data", "created-after", "created-before", "max-age", "group-strict", "group-exclude", "limit");
		
		action("Circles.listConsents");
		role(UserRole.RESEARCH, "_id", "owner", "name", "authorized", "entityType", "type", "status", "ownerName", "member", "records", "passcode", "createdBefore", "createdAfter", "validUntil", "sharingQuery", "externalOwner", "externalAuthorized", "dateOfCreation", "sharingQuery.link-study", "sharingQuery.link-study-group", "writes", "allowedReshares", "querySignature", "basedOn", "testUserApp", "reportedStatus");
		role(UserRole.DEVELOPER, "_id", "owner", "name", "authorized", "entityType", "type", "status", "ownerName", "member", "records", "passcode", "createdBefore", "createdAfter", "validUntil", "sharingQuery", "externalOwner", "externalAuthorized", "dateOfCreation", "sharingQuery.link-study", "sharingQuery.link-study-group", "writes", "allowedReshares", "querySignature", "basedOn", "testUserApp", "reportedStatus");
		role(UserRole.ADMIN, "_id", "owner", "name", "authorized", "entityType", "type", "status", "ownerName", "member", "records", "passcode", "createdBefore", "createdAfter", "validUntil", "sharingQuery", "externalOwner", "externalAuthorized", "dateOfCreation", "sharingQuery.link-study", "sharingQuery.link-study-group", "writes", "allowedReshares", "querySignature", "basedOn", "testUserApp", "reportedStatus");
		role(UserRole.ANY, "_id", "owner", "name", "authorized", "entityType", "type", "status", "ownerName", "member", "records", "passcode", "createdBefore", "createdAfter", "validUntil", "sharingQuery", "externalOwner", "externalAuthorized", "dateOfCreation", "writes", "applicationId","allowedReshares", "querySignature", "basedOn", "testUserApp", "reportedStatus");		
		
		action("Plugins.get");
		role(UserRole.ANY, Plugin.ALL_PUBLIC);
		role(UserRole.DEVELOPER, Plugin.ALL_DEVELOPER);
		role(UserRole.ADMIN, Plugin.ALL_DEVELOPER);
		
		action("UserGroups.search");
		role(UserRole.ANY, "_id", "creator", "name", "registeredAt", "searchable", "status", "type", "member", "role", "setup");

		action("UserGroups.searchOwn");
		role(UserRole.ANY, "_id", "creator", "name", "registeredAt", "searchable", "status", "type", "member", "role", "active", "setup");
	}
	
	public static void chk(String action, UserRole role, Map<String, Object> props, Set<String> fields) throws InternalServerException, AuthException {
		chk(action, role, props.keySet());
		chk(action, role, fields);
	}
	
    public static void chk(String action, UserRole role, Map<String, Object> props) throws InternalServerException, AuthException {
		chk(action, role, props.keySet());
	}
    
    public static boolean existsAction(String action, UserRole role) {
    	return allowed.containsKey(action) && allowed.get(action).containsKey(role);
    }
	
	public static void chk(String action, UserRole role, Set<String> fields) throws InternalServerException, AuthException {
		Map<UserRole, Set<String>> ac = allowed.get(action);
		if (ac == null) throw new InternalServerException("error.internal", "Action '"+action+"' does not exist");
		
		Set<String> isallowed = ac.get(role);
		if (isallowed == null) isallowed = ac.get(UserRole.ANY);
		if (isallowed == null) throw new AuthException("error.notauthorized.action", "Role is not allowed to perform action.");
		
		if (!isallowed.containsAll(fields)) {		
			Set<String> missing = new HashSet<String>(fields);
			missing.removeAll(isallowed);
			throw new AuthException("error.notauthorized.fields", "Security: Role is not allowed to query provided fields: "+missing.toString());
		}
	}

}
