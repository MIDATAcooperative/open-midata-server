package utils.collections;

import java.util.HashMap;
import java.util.Map;

import models.MidataId;
import models.User;
import utils.auth.ExecutionInfo;
import utils.buffer.StudyPublishBuffer;
import utils.exceptions.AppException;

public class RequestCache {

	private Map<MidataId, User> userCache;
	private StudyPublishBuffer studyPublishBuffer;
	
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
	
	public StudyPublishBuffer getStudyPublishBuffer() {
		if (studyPublishBuffer == null) {
			studyPublishBuffer = new StudyPublishBuffer();
		}
		return studyPublishBuffer;
	}
	
	public void save() throws AppException {
		if (studyPublishBuffer != null) studyPublishBuffer.save();
		studyPublishBuffer = null;
	}
}
