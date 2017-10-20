package utils.access;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.MidataId;
import models.StudyParticipation;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class Feature_Pseudonymization extends Feature {

	private Feature next;

	public Feature_Pseudonymization(Feature next) {
		this.next = next;
	}

	@Override
	protected List<DBRecord> query(Query q) throws AppException {

		// For researchers
		if (q.restrictedBy("study")) {
			q = new Query(q, CMaps.map());
			MidataId study = q.getMidataIdRestriction("study").iterator().next();
			
			q.getProperties().put("usergroup", study);
				   
		}		
		
		List<DBRecord> result = next.query(q);

		boolean oname = q.returns("ownerName");
		if (q.returns("owner") || oname) {
			for (DBRecord r : result) {
				AccessLog.log("onyes="+oname+" must="+r.context.mustPseudonymize()+" on="+r.context.getOwnerName());
				if (r.context.mustPseudonymize()) {
					r.owner = r.context.getTargetAps();

					String name = r.context.getOwnerName();
					if (oname && name != null) {
						QueryEngine.fetchFromDB(q, r);
						RecordEncryption.decryptRecord(r);
						r.meta.put("ownerName", name);

						// Bugfix for older records
						String creator = r.meta.getString("creator");
						if (creator != null && creator.equals(r.owner.toString()))
							r.meta.remove("creator");
					}

				} else r.owner = r.context.getOwner();
				
				AccessLog.log("on2="+r.meta.get("ownerName"));
			}
		}

		return result;
	}

}
