package utils.access;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import models.StudyParticipation;
import utils.AccessLog;
import utils.access.ProcessingTools.BlockwiseLoad;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class Feature_Pseudonymization extends Feature {

	private Feature next;

	public Feature_Pseudonymization(Feature next) {
		this.next = next;
	}

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		// For researchers
		if (q.restrictedBy("study")) {
			q = new Query(q, CMaps.map()).setFromRecord(q.getFromRecord());
			MidataId study = q.getMidataIdRestriction("study").iterator().next();

			q.getProperties().put("usergroup", study);
			
			if (q.getContext().mustPseudonymize()) {
				Map<String, Object> newprops = new HashMap<String, Object>();
				newprops.putAll(q.getProperties());
				if (!pseudonymizedIdRestrictions(q, q.getCache().getAccountOwner(), newprops)) return ProcessingTools.empty();
				q = new Query(q, newprops);
			}

		}				

		DBIterator<DBRecord> result = next.iterator(q);

		// moved to ProcessFilters
		/*boolean oname = q.returns("ownerName");
		if (q.returns("owner") || oname || q.returns("data") || q.returns("name")) {
			return new PseudonymIterator(result, q, oname);		
		}*/

		return result;
	}
	
	public static boolean pseudonymizedIdRestrictions(Query q, MidataId group, Map<String,Object> newprops) throws AppException {
		if (q.restrictedBy("owner")) {
			   Set<StudyParticipation> parts = StudyParticipation.getActiveOrRetreatedParticipantsByStudyAndGroupsAndIds(q.restrictedBy("study") ? q.getMidataIdRestriction("study") : null, q.getRestrictionOrNull("study-group"), group, q.getMidataIdRestriction("owner"), Sets.create("name", "order", "owner", "ownerName", "type"));
			   Set<String> owners = new HashSet<String>();
			   for (StudyParticipation part : parts) {
				  owners.add(part.owner.toString());
			   }
			   newprops.put("owner", owners);
			   if (owners.isEmpty()) return false;//return ProcessingTools.empty();
	    }		
		return true;
	}
	
	public static class PseudonymIterator implements DBIterator<DBRecord> {

		private DBIterator<DBRecord> chain;
		private Query q;
		private boolean oname;
		
		PseudonymIterator(DBIterator<DBRecord> chain, Query q, boolean oname) {
			this.chain = chain;
			this.q = q;
			this.oname = oname;
		}
		
		@Override
		public boolean hasNext() throws AppException {
			return chain.hasNext();
		}

		@Override
		public DBRecord next() throws AppException {
			DBRecord r = chain.next();
			if (r.context != null) {
				if (r.context.mustPseudonymize()) {
					if (r.meta != null) {
						r.owner = r.context.getOwnerPseudonymized();
		
						String name = r.context.getOwnerName();
						if (oname && name != null) {															
							r.meta.put("ownerName", name);
						}
		
						// Bugfix for older records
						String creator = r.meta.getString("creator");
						if (creator != null && creator.equals(r.context.getOwner().toString())) {
							r.meta.remove("creator");
						}
		
						String ct = r.meta.getString("content");
														
						if (ct.equals("Patient"))
							r.meta = null;
					}
				} else {		
					if (!r.context.mayContainRecordsFromMultipleOwners() || r.owner==null) {
					  r.owner = r.context.getOwner();
					} 
				}
			}

			return r;
		}
		
		@Override
		public String toString() {		
			return "pseudonymize("+chain.toString()+")";
		}
		
	}	
	

}
