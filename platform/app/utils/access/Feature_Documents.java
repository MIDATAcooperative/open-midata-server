package utils.access;

import java.util.List;

import models.Record;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class Feature_Documents extends Feature {

	private Feature next;

	public Feature_Documents(Feature next) throws AppException {
		this.next = next;
	}

	@Override
	protected List<Record> lookup(List<Record> records, Query q) throws AppException {

		List<Record> result = next.lookup(records, q);

		if (result.size() < records.size()) {
			for (Record rec : records) {
				if (rec.key == null && rec.document != null && rec.stream == null) {
					List<Record> docs = QueryEngine.listInternal(q.getCache(), q.getApsId(), CMaps.map("_id", rec.document), Sets.create("stream"));
					if (docs.size() == 1) {
						Record doc = docs.get(0);
						rec.key = doc.key;
						rec.owner = doc.owner;
						rec.format = doc.format; //TODO or it is filtered out by QueryRedirect
						rec.content = doc.content; //TODO or it is filtered out by QueryRedirect
						result.add(rec);
					}
				}
			}
		}

		return result;
	}

	@Override
	protected List<Record> query(Query q) throws AppException {
		return next.query(q);
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q) throws AppException {
		return next.postProcess(records, q);
	}

}
