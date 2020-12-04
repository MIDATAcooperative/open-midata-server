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

package utils.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.Consent;
import models.MidataId;
import models.StudyParticipation;
import utils.AccessLog;
import utils.access.ProcessingTools.BlockwiseLoad;
import utils.access.op.CompareCaseInsensitive;
import utils.access.op.CompareCondition;
import utils.access.op.Condition;
import utils.access.op.FieldAccess;
import utils.access.pseudo.FhirPseudonymizer;
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
			q = new Query(q, "pseudonym-from", CMaps.map()).setFromRecord(q.getFromRecord());
			MidataId study = q.getMidataIdRestriction("study").iterator().next();

			q.getProperties().put("usergroup", study);
			
			if (q.getContext().mustPseudonymize() || q.getContext().mustRename()) {
				Map<String, Object> newprops = new HashMap<String, Object>();
				newprops.putAll(q.getProperties());
				if (!pseudonymizedIdRestrictions(q, next, q.getCache().getAccountOwner(), newprops)) return ProcessingTools.empty();
				q = new Query(q, "pseudonym", newprops).setFromRecord(q.getFromRecord());
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
	
	public static boolean pseudonymizedIdRestrictions(Query q, Feature next, MidataId group, Map<String,Object> newprops) throws AppException {
		if (q.restrictedBy("owner")) {
			Set<MidataId> ids = q.getMidataIdRestriction("owner");
			Set<String> owners = new HashSet<String>(ids.size());
			for (MidataId id : ids) {
				MidataId targetId = Feature_Pseudonymization.unpseudonymizeUser(q, next, id);
				if (targetId!=null) {
					AccessLog.log("UNPSEUDONYMIZE "+id+" to "+targetId);
					owners.add(targetId.toString());
				}
			}			  
			newprops.put("owner", owners);
			if (owners.isEmpty()) return false;//return ProcessingTools.empty();
	    }		
		return true;
	}
	
	public static void pseudonymize(DBRecord r) throws AppException {
		if (r.data != null && r.meta != null && r.context.mustPseudonymize()) {
			BasicBSONList tags = (BasicBSONList) r.meta.get("tags");
			boolean r4 = false;
			if (tags != null) {		
				r4 = tags.contains("fhir:r4");							
			}
			if (r4) FhirPseudonymizer.forR4().pseudonymize(r);
			else FhirPseudonymizer.forSTU3().pseudonymize(r);
		}
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
				if (r.context.mustPseudonymize() || r.context.mustRename()) {
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
					
					// Not needed. Is always done immediately on decryption.
					// pseudonymize(r);					
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
	
	private final static Set<String> FIELDS_FOR_PSEUDONYMIZATION = Collections.unmodifiableSet(Sets.create("_id","format"));
	
	public static Pair<MidataId,String> pseudonymizeUser(MidataId executor, Consent consent) throws AppException {
		if (consent.getOwnerName() != null && !consent.getOwnerName().equals("?")) return Pair.of(consent._id,consent.ownerName);
		BasicBSONObject patient = (BasicBSONObject) RecordManager.instance.getMeta(executor, consent._id, "_patient");
		if (patient != null) {
			MidataId pseudoId = new MidataId(patient.getString("id"));
			String pseudoName = patient.getString("name");
			return Pair.of(pseudoId, pseudoName);
		}
		
		throw new InternalServerException("error.internal", "Cannot pseudonymize");
	}
	
	public static Pair<MidataId,String> pseudonymizeUser(APSCache cache, Consent consent) throws AppException {
		if (consent.getOwnerName() != null && !consent.getOwnerName().equals("?")) return Pair.of(consent._id,consent.ownerName);
		if (consent.getOwnerName() == null) return Pair.of(consent.owner, null);
		BasicBSONObject patient = Feature_UserGroups.findApsCacheToUse(cache, consent._id).getAPS(consent._id, consent.owner).getMeta("_patient");
		if (patient != null) {
			MidataId pseudoId = new MidataId(patient.getString("id"));
			String pseudoName = patient.getString("name");
			return Pair.of(pseudoId, pseudoName);
		}
		AccessLog.log(consent._id+" ow="+consent.owner+" executor="+cache.getExecutor()+" acowner="+cache.getAccountOwner());
		throw new InternalServerException("error.internal", "Cannot pseudonymize");
	}
	
	public static MidataId unpseudonymizeUser(Query q, Feature next, MidataId pseudonymizedUser) throws AppException {
		if (q.getCache().getAccountOwner().equals(pseudonymizedUser)) return pseudonymizedUser;
		AccessLog.logBeginPath("unpseudonymize", "user="+pseudonymizedUser);
		Condition cnd = FieldAccess.path("id", new CompareCaseInsensitive((Comparable) pseudonymizedUser.toString(), CompareCaseInsensitive.CompareCaseInsensitiveOperator.EQUALS)).optimize();
	    Object study = q.getProperties().get("study");	   
		List<DBRecord> rec = ProcessingTools.collect(new Feature_ProcessFilters(next).iterator(new Query("unpseudonymize",CMaps.mapNotEmpty("study", study).map("format","fhir/Patient").map("content","PseudonymizedPatient").map("data", cnd).map("index", cnd.indexExpression()).map("fast-index","true"), Sets.create("_id"),q.getCache(),q.getApsId(),q.getContext(),q)));
		AccessLog.logEndPath("#found="+rec.size());
		if (rec.size()==1) {
			return rec.get(0).context.getOwner();
		}
		if (rec.size()==0) return null;		
		AccessLog.log("FOUND USER RECORDS="+rec.size());		
		throw new InternalServerException("error.internal", "Cannot unpseudonymize");
	}
	
	public static void addPseudonymization(MidataId executorId, MidataId consentId, MidataId pseudoId, String pseudoName) throws AppException {
		RecordManager.instance.setMeta(executorId, consentId, "_patient", CMaps.map("id", pseudoId.toString()).map("name", pseudoName));		
	}

}
