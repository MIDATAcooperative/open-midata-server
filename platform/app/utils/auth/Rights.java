package utils.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import utils.collections.Sets;
import utils.exceptions.AuthException;
import utils.exceptions.ModelException;

import models.enums.UserRole;

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
		
	static {
		allowed = new HashMap<String, Map<UserRole, Set<String>>>();
		
		action("Users.get");
		role(UserRole.MEMBER, "_id", "role", "firstname", "lastname", "name");
		role(UserRole.RESEARCH, "_id", "role", "firstname", "lastname", "name");
		role(UserRole.DEVELOPER, "_id", "role", "firstname", "lastname", "name");
		role(UserRole.ADMIN, "_id", "address1", "address2", "city", "contractStatus", "country", "email", "firstname", "gender", "history", "lastname", "login", "mobile", "name", "phone", "registeredAt", "role", "security", "status", "zip", "midataID", "birthday");
		role(UserRole.PROVIDER, "_id", "address1", "address2", "city", "contractStatus", "country", "email", "firstname", "gender", "lastname", "login", "mobile", "name", "phone", "role", "status", "zip", "midataID", "birthday");
		
		action("Users.getPROVIDER");
		role(UserRole.MEMBER  , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "phone", "mobile", "name", "role", "status", "zip");
		
		action("Users.getSelf");
		role(UserRole.MEMBER, "_id", "address1", "address2", "apps", "city", "contractStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "security", "status", "visualizations", "zip", "birthday", "midataID", "news", "partInterest", "ssn");
		role(UserRole.RESEARCH, "_id", "address1", "address2", "apps", "city", "contractStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "security", "status", "visualizations", "zip", "midataID"); // TODO Remove midataID 
		role(UserRole.PROVIDER, "_id", "address1", "address2", "apps", "city", "contractStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "security", "status", "visualizations", "zip", "midataID"); // TODO Remove midataID
		role(UserRole.DEVELOPER, "_id", "address1", "address2", "apps", "city", "contractStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "security", "status", "visualizations", "zip", "midataID"); // TODO Remove midataID
		role(UserRole.ADMIN, "_id", "address1", "address2", "apps", "city", "contractStatus", "country", "email", "firstname", "name", "lastname", "gender", "history", "login", "messages", "mobile", "phone", "registeredAt", "role", "security", "status", "visualizations", "zip", "midataID"); // TODO Remove midataID
		
		action("HealthProvider.search");
		role(UserRole.MEMBER  , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.RESEARCH, "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.PROVIDER, "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
		role(UserRole.ADMIN   , "_id", "address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "name", "role", "status", "zip");
	}
	
	public static void chk(String action, UserRole role, Map<String, Object> props, Set<String> fields) throws ModelException, AuthException {
		chk(action, role, props.keySet());
		chk(action, role, fields);
	}
	
    public static void chk(String action, UserRole role, Map<String, Object> props) throws ModelException, AuthException {
		chk(action, role, props.keySet());
	}
    
    public static boolean existsAction(String action, UserRole role) {
    	return allowed.containsKey(action) && allowed.get(action).containsKey(role);
    }
	
	public static void chk(String action, UserRole role, Set<String> fields) throws ModelException, AuthException {
		Map<UserRole, Set<String>> ac = allowed.get(action);
		if (ac == null) throw new ModelException("error.internal", "Action does not exist");
		
		Set<String> isallowed = ac.get(role);
		if (isallowed == null) throw new AuthException("error.notauthorized", "Role is not allowed to perform action.");
		
		if (!isallowed.containsAll(fields)) {		
			Set<String> missing = new HashSet<String>(fields);
			missing.removeAll(isallowed);
			throw new AuthException("error.notauthorized.field", "Security: Role is not allowed to query provided fields: "+missing.toString());
		}
	}

}
