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

package utils.collections;

import java.util.HashMap;
import java.util.Map;

import models.MidataId;
import models.ServiceInstance;
import models.User;
import models.UserGroup;
import models.enums.UserStatus;
import utils.buffer.StudyPublishBuffer;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.messaging.SubscriptionBuffer;

public class RequestCache {

	private Map<MidataId, User> userCache;
	private Map<MidataId, UserGroup> userGroupCache;
	private Map<MidataId, ServiceInstance> siCache;
	private StudyPublishBuffer studyPublishBuffer;
	private SubscriptionBuffer subscriptionBuffer;
	
	public User getUserById(MidataId userId) throws InternalServerException {
		return getUserById(userId, false);
	}
	
	public User getUserById(MidataId userId, boolean alsoDeleted) throws InternalServerException {
		User result = null;
		if (userCache == null) {
			userCache = new HashMap<MidataId, User>();
		} else {
			result = userCache.get(userId);
		}
		if (result == null) {
			result = User.getByIdAlsoDeleted(userId, User.PUBLIC);
			userCache.put(userId, result);
		}
		if (!alsoDeleted && result != null && result.status.isDeleted()) return null;
		return result;
	}
	
	public UserGroup getUserGroupById(MidataId userGroupId) throws InternalServerException {
		UserGroup result = null;
		if (userGroupCache == null) {
			userGroupCache = new HashMap<MidataId, UserGroup>();
		} else {
			result = userGroupCache.get(userGroupId);
		}
		if (result == null) {
			result = UserGroup.getById(userGroupId, UserGroup.ALL);
			userGroupCache.put(userGroupId, result);
		}		
		return result;
	}
	
	public void update(UserGroup grp) {
		if (userGroupCache != null) userGroupCache.put(grp._id, grp);
	}
	
	public ServiceInstance getServiceInstanceById(MidataId serviceInstanceId) throws InternalServerException {
		ServiceInstance result = null;
		if (siCache == null) {
			siCache = new HashMap<MidataId, ServiceInstance>();
		} else {
			result = siCache.get(serviceInstanceId);
		}
		if (result == null) {
			result = ServiceInstance.getById(serviceInstanceId, ServiceInstance.ALL);
			siCache.put(serviceInstanceId, result);
		}		
		return result;
	}
	
	public StudyPublishBuffer getStudyPublishBuffer() {
		if (studyPublishBuffer == null) {
			studyPublishBuffer = new StudyPublishBuffer();
		}
		return studyPublishBuffer;
	}
	
	public void bufferResourceChanges() {
		if (subscriptionBuffer == null) subscriptionBuffer = new SubscriptionBuffer();
	}
	
	public SubscriptionBuffer getSubscriptionBuffer() {
		return subscriptionBuffer;
	}
	
	public void save() throws AppException {
		if (studyPublishBuffer != null) studyPublishBuffer.save();
		studyPublishBuffer = null;
		if (subscriptionBuffer != null) subscriptionBuffer.flush();
		subscriptionBuffer = null;
	}
}
