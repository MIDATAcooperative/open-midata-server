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
import utils.collections.CMaps;
import utils.exceptions.AppException;

public class FasterDecryptTool {

	public static void accelerate(Query q, List<Consent> consents) throws AppException {
		AccessLog.logBegin("start accelerate consent access");
		MidataId owner = q.getCache().getAccountOwner();
		APS main = q.getCache().getAPS(owner);
		APS map = null;
		BasicBSONObject obj = main.getMeta("_consents");
		if (obj == null) {
			MidataId apsId = new MidataId();
			RecordManager.instance.createPrivateAPS(owner, apsId);
			main.setMeta("_consents", CMaps.map("id", apsId.toString()));			
			map = q.getCache().getAPS(apsId, owner);
		} else {
			MidataId apsId = MidataId.from(obj.getString("id"));
			map = q.getCache().getAPS(apsId, owner);
		}
		
		BasicBSONObject bs = map.getMeta("consents");
		Map<String, Object> entries = bs != null ? bs.toMap() : new HashMap<String, Object>(consents.size());
		Map<MidataId, byte[]> keys = new HashMap<MidataId, byte[]>(consents.size());
		Set<Consent> missing = new HashSet<Consent>();
		for (Consent c : consents) {
			byte[] key = (byte[]) entries.get(c._id.toString());
			if (key != null) {
				keys.put(c._id, key);
			} else {
				missing.add(c);
			}
			
		}
		q.getCache().prefetch(consents, keys);
		
		AccessLog.logEnd("end accelerate consent access");
		if (missing.isEmpty()) return;
		AccessLog.logBegin("start add missing acceleration keys size="+missing.size());
		for (Consent c : missing) {
		  APS targetAPS = q.getCache().getAPS(c._id);
		  if (targetAPS.isAccessible()) {
			  byte[] newkey = ((APSImplementation) targetAPS).eaps.getAPSKey();
			  entries.put(c._id.toString(), newkey);
		  }
		}
		
		map.setMeta("consents", entries);
		AccessLog.logEnd("end add missing acceleration keys");
	}
}
