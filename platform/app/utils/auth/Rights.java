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
		role(UserRole.MEMBER, "_id", "role", "firstname", "lastname", "name", "status");
		role(UserRole.RESEARCH, "_id", "role", "firstname", "lastname", "name", "status");
		role(UserRole.DEVELOPER, "_id", "role", "firstname", "lastname", "name", "email", "developer", "status");
		role(UserRole.ADMIN, "_id", "address1", "address2", "city", "contractStatus", "agbStatus", "country", "email", "emailLC", "firstname", "gender", "history", "lastname", "login", "mobile", "name", "phone", "registeredAt", "role", "subroles", "security", "status", "zip", "midataID", "birthday", "confirmationCode", "emailStatus", "confirmedAt", "reason", "coach", "developer");
		role(UserRole.PROVIDER, "_id", "address1", "address2", "city", "contractStatus", "agbStatus", "country", "email", "emailLC", "firstname", "gender", "lastname", "login", "mobile", "name", "phone", "role", "subroles", "status", "zip", "midataID", "birthday");
		
		action("Users.getPROVIDER");
		role(UserRole.MEMBER  , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "phone", "mobile", "name", "role", "subroles", "status", "zip");
		
		action("Users.getSelf");
		role(UserRole.MEMBER, "_id", "address1", "address2", "apps", "city", "emailStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "birthday", "midataID", "news", "partInterest", "ssn", "language", "searchable", "confirmedAt");
		role(UserRole.RESEARCH, "_id", "address1", "address2", "apps", "city", "emailStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "midataID", "language", "searchable", "confirmedAt"); // TODO Remove midataID 
		role(UserRole.PROVIDER, "_id", "address1", "address2", "apps", "city", "emailStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "midataID", "language", "searchable", "confirmedAt"); // TODO Remove midataID
		role(UserRole.DEVELOPER, "_id", "address1", "address2", "apps", "city", "emailStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "midataID", "language", "searchable", "confirmedAt"); // TODO Remove midataID
		role(UserRole.ADMIN, "_id", "address1", "address2", "apps", "city", "emailStatus", "contractStatus", "agbStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "subroles", "security", "status", "visualizations", "zip", "midataID", "language", "searchable", "confirmedAt"); // TODO Remove midataID
		
		action("HealthProvider.search");
		role(UserRole.MEMBER  , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.RESEARCH, "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.PROVIDER, "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.ADMIN   , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		
		action("Studies.search");
		role(UserRole.RESEARCH, "_id", "name", "code", "owner", "createdBy", "createdAt", "description", "infos", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "history", "groups");
		role(UserRole.ADMIN, "_id", "name", "code", "owner", "createdBy", "createdAt", "description", "infos", "studyKeywords", "participantRules",  "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "history", "groups");
		role(UserRole.PROVIDER, "_id", "name", "code", "owner", "createdAt", "description", "infos", "studyKeywords", "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "history");
		role(UserRole.MEMBER, "_id", "name", "code", "owner", "createdAt", "description", "infos", "studyKeywords", "recordQuery", "requiredInformation", "assistance", "validationStatus", "participantSearchStatus", "executionStatus", "history");
		
		action("getRecords");
		role(UserRole.ANY, "_id", "id", "owner" , "ownerName", "creatorName", "format", "subformat", "content", "code", "group", "app", "creator", "created", "lastUpdated", "version", "name", "description", "tags", "data", "created-after", "created-before", "max-age", "group-strict", "group-exclude", "limit");
		
		action("Circles.listConsents");
		role(UserRole.ANY, "_id", "owner", "name", "authorized", "entityType", "type", "status", "ownerName", "member", "records", "passcode", "createdBefore", "validUntil");
		
		action("Plugins.get");
		role(UserRole.ANY, Plugin.ALL_PUBLIC);
		role(UserRole.DEVELOPER, Plugin.ALL_DEVELOPER);
		role(UserRole.ADMIN, Plugin.ALL_DEVELOPER);
		
		action("UserGroups.search");
		role(UserRole.ANY, "_id", "creator", "history", "name", "registeredAt", "searchable", "status", "type", "member");

		action("UserGroups.searchOwn");
		role(UserRole.ANY, "_id", "creator", "history", "name", "registeredAt", "searchable", "status", "type", "member");
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
