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
import models.User;
import models.enums.UserStatus;
import utils.buffer.StudyPublishBuffer;
import utils.exceptions.AppException;

public class RequestCache {

	private Map<MidataId, User> userCache;
	private StudyPublishBuffer studyPublishBuffer;
	
	public User getUserById(MidataId userId) throws AppException {
		return getUserById(userId, false);
	}
	
	public User getUserById(MidataId userId, boolean alsoDeleted) throws AppException {
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
		if (!alsoDeleted && result != null && result.status == UserStatus.DELETED) return null;
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
