package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;


import models.Circle;
import models.Consent;
import models.Record;
import models.StudyParticipation;

/**
 * queries made to the access permission set of a user 
 * also query his other access permission sets based on the query parameters.
 *
 */
public class Feature_AccountQuery extends Feature {

	private Feature next;

	public Feature_AccountQuery(Feature next) {
		this.next = next;
	}

	@Override
	protected List<DBRecord> lookup(List<DBRecord> record, Query q) throws AppException {
		return next.lookup(record, q);
	}

	@Override
	protected List<DBRecord> query(Query q) throws AppException {

		if (q.getApsId().equals(q.getCache().getOwner())) {
			if (AccessLog.detailedLog) AccessLog.logBegin("Begin process owner aps");
			Set<String> sets = q.restrictedBy("owner") ? q.getRestriction("owner") : Collections.singleton("all");
			Set<ObjectId> studies = q.restrictedBy("study") ? q.getObjectIdRestriction("study") : null;

			List<DBRecord> result = null;

			if (studies != null) {
				result = new ArrayList<DBRecord>();

				Set<StudyParticipation> consents = new HashSet<StudyParticipation>();
				for (ObjectId studyId : studies)
					consents.addAll(StudyParticipation.getParticipantsByStudy(studyId, Sets.create("pstatus", "ownerName")));

				for (StudyParticipation sp : consents) {
					List<DBRecord> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), sp._id));

					if (q.returns("owner") || q.returns("ownerName")) {
						for (DBRecord record : consentRecords) {
							record.owner = sp._id;
							record.meta.put("ownerName", sp.ownerName);
						}
					}
					if (q.returns("id")) {
						for (DBRecord record : consentRecords)
							record.id = record._id.toString() + "." + sp._id.toString();
					}

					result.addAll(consentRecords);
				}
			} else {

				if (sets.contains("self") || sets.contains("all") || sets.contains(q.getApsId().toString())) {
					result = next.query(q);
					if (q.returns("id")) {
						for (DBRecord record : result)
							record.id = record._id.toString() + "." + q.getApsId().toString();
					}
				} else
					result = new ArrayList<DBRecord>();

				if (sets.contains("all") || sets.contains("other") || sets.contains("shared")) {
					Set<Consent> consents = null;
					if (sets.contains("shared"))
						consents = new HashSet<Consent>(Circle.getAllByMember(q.getCache().getOwner()));
					else
						consents = Consent.getAllByAuthorized(q.getCache().getOwner());
					for (Consent circle : consents) {
						List<DBRecord> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), circle._id));

						if (q.returns("id")) {
							for (DBRecord record : consentRecords)
								record.id = record._id.toString() + "." + circle._id.toString();
						}

						result.addAll(consentRecords);

					}
				} else {
					Set<ObjectId> owners = new HashSet<ObjectId>();
					for (String owner : sets) {
						if (ObjectId.isValid(owner))
							owners.add(new ObjectId(owner));
					}

					Set<Consent> consents = Consent.getAllByAuthorizedAndOwners(q.getCache().getOwner(), owners);
					for (Consent circle : consents) {
						List<DBRecord> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), circle._id));

						if (q.returns("id")) {
							for (DBRecord record : consentRecords)
								record.id = record._id.toString() + "." + circle._id.toString();
						}

						result.addAll(consentRecords);

					}

				}
			}
			if (AccessLog.detailedLog) AccessLog.logEnd("End process owner aps");
			return result;
		} else {
			List<DBRecord> result = next.query(q);

			if (q.returns("id")) {
				for (DBRecord record : result)
					record.id = record._id.toString() + "." + q.getApsId().toString();
			}
			return result;
		}
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q) throws AppException {
		return next.postProcess(records, q);

	}

}
