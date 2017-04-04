package utils.collections;

import java.util.HashMap;
import java.util.Map;

import models.MidataId;
import models.User;
import utils.exceptions.AppException;

public class RequestCache {

	private Map<MidataId, User> userCache;
	
	public User getUserById(MidataId userId) throws AppException {
		User result = null;
		if (userCache == null) {
			userCache = new HashMap<MidataId, User>();
		} else {
			result = userCache.get(userId);
		}
		if (result == null) {
			result = User.getById(userId, User.PUBLIC);
			userCache.put(userId, result);
		}
		return result;
	}
}
