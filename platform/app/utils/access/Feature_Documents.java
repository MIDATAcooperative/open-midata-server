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
/*
	@Override
	protected List<DBRecord> lookup(List<DBRecord> records, Query q) throws AppException {

		List<DBRecord> result = next.lookup(records, q);

		if (result.size() < records.size()) {
			for (DBRecord rec : records) {
				if (rec.security == null && rec.document != null && rec.stream == null) {
					List<DBRecord> docs = QueryEngine.listInternal(q.getCache(), q.getApsId(), CMaps.map("_id", rec.document), Sets.create("stream"));
					if (docs.size() == 1) {
						DBRecord doc = docs.get(0);
						rec.key = doc.key;
						rec.security = doc.security;
						rec.owner = doc.owner;
						rec.meta.put("format", doc.meta.get("format")); //TODO or it is filtered out by QueryRedirect
						rec.meta.put("content", doc.meta.get("content")); //TODO or it is filtered out by QueryRedirect
						if (doc.meta.containsField("subformat")) rec.meta.put("subformat", doc.meta.get("subformat")); //TODO or it is filtered out by QueryRedirect
						result.add(rec);
					}
				}
			}
		}

		return result;
	}
*/
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		return next.query(q);
	}
	

	
}
