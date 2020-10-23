/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.Consent;
import models.MidataId;
import models.enums.ConsentStatus;
import utils.AccessLog;
import utils.access.index.ConsentToKeyIndexKey;
import utils.access.index.ConsentToKeyIndexRoot;
import utils.collections.CMaps;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;

public class FasterDecryptTool {

	public static void accelerate(APSCache cache, List<Consent> consents) throws AppException {
		AccessLog.logBegin("start accelerate consent access");
		MidataId owner = cache.getAccountOwner();
		APS main = cache.getAPS(owner);
		
		BasicBSONObject obj = main.getMeta("_consents");
		MidataId idxId = null;
		if (obj == null) {
			idxId = new MidataId();														
			main.setMeta("_consents", CMaps.map("id", idxId.toString()));						
		} else {
			idxId = MidataId.from(obj.getString("id"));			
		}
		
		ConsentToKeyIndexRoot root = cache.getConsentKeyIndexRoot(idxId);
						
		Map<MidataId, byte[]> keys = new HashMap<MidataId, byte[]>(consents.size());
		Set<Consent> missing = new HashSet<Consent>();
		for (Consent c : consents) {	
			try {
				byte[] key = root.getKey(c._id);
				if (key != null) {
					keys.put(c._id, key);
				} else {
					if (c.status==ConsentStatus.ACTIVE && c.status==ConsentStatus.FROZEN) missing.add(c);
				}
			} catch (LostUpdateException e) {
				root.reload();
			}			
		}
		cache.prefetch(consents, keys);
		
		AccessLog.logEnd("end accelerate consent access");
		if (missing.isEmpty()) return;
		AccessLog.logBegin("start add missing acceleration keys size="+missing.size());
		try {
			for (Consent c : missing) {
			  APS targetAPS = cache.getAPS(c._id);
			  if (targetAPS.isAccessible()) {
				  byte[] newkey = ((APSImplementation) targetAPS).eaps.getAPSKey();
				  root.addEntry(new ConsentToKeyIndexKey(c._id, newkey));			  
			  }
			}
			root.flush();
		} catch (LostUpdateException e) {}

		AccessLog.logEnd("end add missing acceleration keys");
	}
}
