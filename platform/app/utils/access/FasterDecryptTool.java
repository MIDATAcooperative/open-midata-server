package utils.access;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.Consent;
import models.MidataId;
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
					missing.add(c);
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
