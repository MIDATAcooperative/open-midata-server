package utils.access;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import models.MidataId;
import models.StudyParticipation;
import utils.AccessLog;
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
	protected Iterator<DBRecord> iterator(Query q) throws AppException {
		// For researchers
		if (q.restrictedBy("study")) {
			q = new Query(q, CMaps.map()).setFromRecord(q.getFromRecord());
			MidataId study = q.getMidataIdRestriction("study").iterator().next();

			q.getProperties().put("usergroup", study);

		}

		Iterator<DBRecord> result = next.iterator(q);

		boolean oname = q.returns("ownerName");
		if (q.returns("owner") || oname || q.returns("data") || q.returns("name")) {
			return new PseudonymIterator(result, q, oname);		
		}

		return result;
	}
	
	static class PseudonymIterator implements Iterator<DBRecord> {

		private Iterator<DBRecord> chain;
		private Query q;
		private boolean oname;
		
		PseudonymIterator(Iterator<DBRecord> chain, Query q, boolean oname) {
			this.chain = chain;
			this.q = q;
			this.oname = oname;
		}
		
		@Override
		public boolean hasNext() {
			return chain.hasNext();
		}

		@Override
		public DBRecord next() {
			DBRecord r = chain.next();
			if (r.context.mustPseudonymize()) {

				r.owner = r.context.getOwnerPseudonymized();

				String name = r.context.getOwnerName();
				if (oname && name != null) {
					try {
					  QueryEngine.fetchFromDB(q, r);
					  RecordEncryption.decryptRecord(r);
					} catch (AppException e) {
						throw new RuntimeException(e);
					}
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

			} else {
				r.owner = r.context.getOwner();
			}

			return r;
		}
		
		@Override
		public String toString() {		
			return "pseudonymize("+chain.toString()+")";
		}
		
	}	
	

}
