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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.Consent;
import models.MidataId;
import models.enums.ConsentStatus;
import utils.AccessLog;
import utils.QueryTagTools;
import utils.access.Feature_AccountQuery.ConsentIterator;
import utils.access.Feature_AccountQuery.IdAndConsentFieldIterator;
import utils.access.ProcessingTools.FilterIterator;
import utils.access.op.AndCondition;
import utils.access.op.CompareCaseInsensitive;
import utils.access.op.Condition;
import utils.access.op.FieldAccess;
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.CodeGenerator;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.exceptions.RequestTooLargeException;

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
				AccessLog.log("must pseudonymize");
				Map<String, Object> newprops = new HashMap<String, Object>();
				newprops.putAll(q.getProperties());
				if (!pseudonymizedIdRestrictions(q, next, q.getCache().getAccountOwner(), newprops)) return ProcessingTools.empty();
				q = new Query(q, "pseudonym", newprops).setFromRecord(q.getFromRecord());
			} 

		}
		
		if (q.restrictedBy("_id")) {
		    Set<MidataId> ids = q.getMidataIdRestriction("_id");
		    Set<String> pseudonymizedIds = null;
		    for (MidataId id : ids) {
		        if (id.isPseudonymized()) {
		            if (pseudonymizedIds == null) pseudonymizedIds = new HashSet<String>();
		            pseudonymizedIds.add(id.toString());
		        }
		    }
		    
		    if (pseudonymizedIds != null) {
		        Condition cond = AndCondition.parseRemaining(CMaps.map("id", pseudonymizedIds)).optimize();
                Condition indexCond1 = cond.indexExpression();
                Condition dataCondition = AndCondition.and(cond, AndCondition.parseRemaining(q.getProperties().get("data")));
                Condition indexCondition = AndCondition.and(indexCond1, AndCondition.parseRemaining(q.getProperties().get("index")));
                Query q2 = new Query(q, "pseudo-ids", CMaps.map("data", dataCondition).map("index", indexCondition));
                q2.getProperties().remove("_id");
		        if (pseudonymizedIds.size() == ids.size()) {
		            return next.iterator(q2);		          
		        } else {
		            ids.removeAll(pseudonymizedIds);
		            Query qneu = new Query(q, "normal-ids", CMaps.map("_id", ids));
		            return new IdsThenPseudonymized(next, qneu, q2);
		        }
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
					AccessLog.log("UNPSEUDONYMIZE ", id.toString(), " to ", targetId.toString());
					owners.add(targetId.toString());
				} else {
					AccessLog.log("CANNOT UNPSEUDONYMIZE ", id.toString());
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
				if (tags.contains(QueryTagTools.SECURITY_NOT_PSEUDONYMISABLE)) {
					r.data = null; 
					r.meta = null;
					return;
				}
			}
			String format = (String) r.meta.get("format");
			if (!format.equals("fhir/Patient")) {
			  String hash = createHash(r.context, r._id.toString());
			  //AccessLog.log("MAP FROM "+r._id.toString()+" TO "+hash);
			  r.data.put("id", hash);
			  r.pseudoId = new MidataId(createHash(r.context, r._id.toString()));
			}
			if (r4) FhirPseudonymizer.forR4().pseudonymize(r, r.context.getProjectDataFilters());
			else FhirPseudonymizer.forSTU3().pseudonymize(r, r.context.getProjectDataFilters());
		}
	}
	
	public static String createSHAHash(String input) {

	      try {
		      MessageDigest md = MessageDigest.getInstance("SHA-256");
		      byte[] messageDigest =
		              md.digest(input.getBytes(StandardCharsets.UTF_8));
	          return Hex.encodeHexString(messageDigest);//Base64.getEncoder().encodeToString(messageDigest);
	      } catch (NoSuchAlgorithmException e) {
	    	  throw new NullPointerException();
	      }
	}

	public static String getSalt(AccessContext context) throws AppException {
		APS aps = context.getCache().getAPS(context.getAccessor(), context.getAccessor());
		BSONObject obj = aps.getMeta("salt");
		if (obj != null && obj.containsField("v")) {
			return obj.get("v").toString();
		} else {
			String salt = CodeGenerator.generatePassphrase();
			aps.setMeta("salt", CMaps.map("v", salt));
			return salt;
		}
	}
	
	public static String createHash(AccessContext context, String input) throws AppException {
	    return "P"+createSHAHash(context.getSalt()+"-"+context.getAccessor()+"-"+input).substring(0,40);
	}
	
	public static String pseudonymizeRefOrNull(AccessContext useContext, String ref) throws AppException {
	    String parts[] = ref.split("/");
	    
	    if (parts.length == 2 && MidataId.isValid(parts[1])) {
	        if (!parts[0].equals("Patient")) {
    	        DBRecord other = QueryEngine.contextLookup(useContext.internal(), "fhir/"+parts[0], MidataId.from(parts[1]));
    	        if (other == null || other.context.mustPseudonymize()) {
    	            String hash = createHash(useContext, parts[1]);
    	            return parts[0]+"/"+hash;
    	        }
	        }
	    }
	    
	    return null;
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
							r.ownerType = r.context.getOwnerType();
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
					  r.ownerType = r.context.getOwnerType();
					} 
				}
			}

			return r;
		}
		
		@Override
		public String toString() {		
			return "pseudonymize("+chain.toString()+")";
		}
		
		@Override
		public void close() {
			chain.close();			
		}
		
	}	
	
	static class IdsThenPseudonymized extends Feature.MultiSource<Integer> {
        
        private Feature next;  
        private Query q2;
        private boolean pseudo;
        
        IdsThenPseudonymized(Feature next, Query q1, Query q2) throws AppException {                        
            this.next = next;
            this.query = q1;
            this.q2 = q2;
            
            Integer[] steps = {1,2};
            init(ProcessingTools.dbiterator("",  Arrays.asList(steps).iterator()));
        }
        
        @Override
        public DBIterator<DBRecord> advance(Integer step) throws AppException {
            if (step == 1) {
                return next.iterator(query);
            } else if (step == 2) {                                         
                pseudo = true;
                return next.iterator(q2);                
            }
            return null;
        }

        @Override
        public String toString() {          
            return (pseudo ? "pseudo-ids(" : "normal-ids(")+"["+passed+"] "+current.toString()+")";
        }
                        
    }
	
	public static class PseudonymIdOnlyIterator implements DBIterator<DBRecord> {

        private DBIterator<DBRecord> chain;       
        
        PseudonymIdOnlyIterator(DBIterator<DBRecord> chain) {
            this.chain = chain;             
        }
        
        @Override
        public boolean hasNext() throws AppException {
            return chain.hasNext();
        }

        @Override
        public DBRecord next() throws AppException {
            DBRecord r = chain.next();
            
            if (r.context != null && r.context.mustPseudonymize() && !("fhir/Patient").equals(r.getFormatOrNull())) {
              r.pseudoId = new MidataId(createHash(r.context, r._id.toString()));
            }                  

            return r;
        }
        
        @Override
        public String toString() {      
            return "add-pseudo-id("+chain.toString()+")";
        }
        
        @Override
        public void close() {
            chain.close();          
        }
        
    }   
	
	private final static Set<String> FIELDS_FOR_PSEUDONYMIZATION = Collections.unmodifiableSet(Sets.create("_id","format"));
	
	public static Pair<MidataId,String> pseudonymizeUser(AccessContext context, Consent consent) throws AppException {
		if (consent.getOwnerName() != null && !consent.getOwnerName().equals("?")) return Pair.of(consent._id,consent.ownerName);
		if (!consent.isSharingData()) return Pair.of(consent._id, "???");
		BasicBSONObject patient = (BasicBSONObject) RecordManager.instance.getMeta(context, consent._id, "_patient");
		if (patient != null) {
			MidataId pseudoId = new MidataId(patient.getString("id"));
			String pseudoName = patient.getString("name");
			return Pair.of(pseudoId, pseudoName);
		}
		return Pair.of(new MidataId(), "ERROR!");
		//throw new InternalServerException("error.internal", "Cannot pseudonymize");
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
		AccessLog.log(consent._id.toString()," ow=",consent.owner.toString()," executor=",cache.getAccessor().toString()," acowner=",cache.getAccountOwner().toString());
		
		//return Pair.of(new MidataId(), "ERROR!");
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
		AccessLog.log("FOUND USER RECORDS=", Integer.toString(rec.size()));		
		throw new InternalServerException("error.internal", "Cannot unpseudonymize");
	}
	
	public static void addPseudonymization(AccessContext context, MidataId consentId, MidataId pseudoId, String pseudoName) throws AppException {
		AccessLog.log("add pseudonymization");
		RecordManager.instance.setMeta(context, consentId, "_patient", CMaps.map("id", pseudoId.toString()).map("name", pseudoName));		
	}
	   

}
