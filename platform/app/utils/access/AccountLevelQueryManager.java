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
import utils.exceptions.ModelException;


import models.Circle;
import models.Consent;
import models.Record;
import models.StudyParticipation;

public class AccountLevelQueryManager extends QueryManager {

	private QueryManager next;

	public AccountLevelQueryManager(QueryManager next) {
		this.next = next;
	}

	@Override
	protected List<Record> lookup(List<Record> record, Query q) throws AppException {
		return next.lookup(record, q);
	}

	@Override
	protected List<Record> query(Query q) throws AppException {

		if (q.getApsId().equals(q.getCache().getOwner())) {
			Set<String> sets = q.restrictedBy("owner") ? q.getRestriction("owner") : Collections.singleton("all");
			Set<ObjectId> studies = q.restrictedBy("study") ? q.getObjectIdRestriction("study") : null;

			List<Record> result = null;

			if (studies != null) {
				result = new ArrayList<Record>();

				Set<StudyParticipation> consents = new HashSet<StudyParticipation>();
				for (ObjectId studyId : studies)
					consents.addAll(StudyParticipation.getParticipantsByStudy(studyId, Sets.create("pstatus", "ownerName")));

				for (StudyParticipation sp : consents) {
					List<Record> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), sp._id));

					if (q.returns("owner") || q.returns("ownerName")) {
						for (Record record : consentRecords) {
							record.owner = sp._id;
							record.ownerName = sp.ownerName;
						}
					}
					if (q.returns("id")) {
						for (Record record : consentRecords)
							record.id = record._id.toString() + "." + sp._id.toString();
					}

					result.addAll(consentRecords);
				}
			} else {

				if (sets.contains("self") || sets.contains("all")) {
					result = next.query(q);
					if (q.returns("id")) {
						for (Record record : result)
							record.id = record._id.toString() + "." + q.getApsId().toString();
					}
				} else
					result = new ArrayList<Record>();

				if (sets.contains("all") || sets.contains("other") || sets.contains("members")) {
					Set<Consent> consents = null;
					if (sets.contains("shared"))
						consents = new HashSet<Consent>(Circle.getAllByMember(q.getCache().getOwner()));
					else
						consents = Consent.getAllByAuthorized(q.getCache().getOwner());
					for (Consent circle : consents) {
						List<Record> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), circle._id));

						if (q.returns("id")) {
							for (Record record : consentRecords)
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
						List<Record> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), circle._id));

						if (q.returns("id")) {
							for (Record record : consentRecords)
								record.id = record._id.toString() + "." + circle._id.toString();
						}

						result.addAll(consentRecords);

					}

				}
			}

			return result;
		} else {
			List<Record> result = next.query(q);

			if (q.returns("id")) {
				for (Record record : result)
					record.id = record._id.toString() + "." + q.getApsId().toString();
			}
			return result;
		}
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q) throws AppException {
		return next.postProcess(records, q);

	}

}
