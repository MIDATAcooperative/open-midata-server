package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import controllers.RecordSharing;

import models.Circle;
import models.Consent;
import models.ModelException;
import models.Record;

public class AccountLevelQueryManager extends QueryManager {

	private QueryManager next;
	
	public AccountLevelQueryManager(QueryManager next) {
		this.next = next;
	}
	
	@Override
	protected List<Record> lookup(List<Record> record, Query q)
			throws ModelException {
		return next.lookup(record, q);
	}
		

	@Override
	protected List<Record> query(Query q) throws ModelException {
		
		if (q.getApsId().equals(q.getCache().getOwner())) {
			Set<String> sets = q.restrictedBy("owner") ? q.getRestriction("owner") : Collections.singleton("all");	
			
			List<Record> result = null;
			if (sets.contains("self") || sets.contains("all")) {
				result = next.query(q);
				if (q.returns("id")) {
					for (Record record : result) record.id = record._id.toString()+"."+q.getApsId().toString();
				}
			} else result = new ArrayList<Record>();
			
			if (sets.contains("all")) {
				Set<Consent> consents = Consent.getAllByAuthorized(q.getCache().getOwner());
				for (Consent circle : consents) {
					List<Record> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), circle._id));
					
					if (q.returns("id")) {
						for (Record record : consentRecords) record.id = record._id.toString()+"."+circle._id.toString();
					}
					
					result.addAll(consentRecords);
										
				}
			} else {
				Set<ObjectId> owners = new HashSet<ObjectId>();
				for (String owner : sets) { 
					if (ObjectId.isValid(owner)) owners.add(new ObjectId(owner));										
				}		
				
				Set<Consent> consents = Consent.getAllByAuthorizedAndOwners(q.getCache().getOwner(), owners);
				for (Consent circle : consents) {
					List<Record> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), circle._id));
					
					if (q.returns("id")) {
						for (Record record : consentRecords) record.id = record._id.toString()+"."+circle._id.toString();
					}
					
					result.addAll(consentRecords);
										
				}
				
			}
						
		    return result;
		} else {
			List<Record> result = next.query(q);
			
			if (q.returns("id")) {
				for (Record record : result) record.id = record._id.toString()+"."+q.getApsId().toString();
			}
			return result;
		}
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q)
			throws ModelException {
		return next.postProcess(records, q);
		
	}

	

	
}
