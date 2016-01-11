package utils.access;

import java.util.ArrayList;
import java.util.List;

import utils.exceptions.InternalServerException;

import models.Record;

/**
 * query a preselected list of records in memory
 *
 */
public class Feature_InMemoryQuery extends Feature {
	
	private List<DBRecord> contents;
	
	public Feature_InMemoryQuery(List<DBRecord> contents) {
		this.contents = contents;
	}
			
	@Override
	protected List<DBRecord> lookup(List<DBRecord> input, Query q)
			throws InternalServerException {
		List<DBRecord> result = new ArrayList<DBRecord>();
		for (DBRecord record : input) {
			if (contents.contains(record)) result.add(record);
		}
		return result;
	}

	@Override
	protected List<DBRecord> query(Query q) throws InternalServerException {	
		return contents;
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q)
			throws InternalServerException {
		return records;
	}

	

}
