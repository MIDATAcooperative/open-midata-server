package utils.access;

import java.util.ArrayList;
import java.util.List;

import utils.exceptions.ModelException;

import models.Record;

public class InMemoryQM extends QueryManager {
	
	private List<Record> contents;
	
	public InMemoryQM(List<Record> contents) {
		this.contents = contents;
	}
			
	@Override
	protected List<Record> lookup(List<Record> input, Query q)
			throws ModelException {
		List<Record> result = new ArrayList<Record>();
		for (Record record : input) {
			if (contents.contains(record)) result.add(record);
		}
		return result;
	}

	@Override
	protected List<Record> query(Query q) throws ModelException {	
		return contents;
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q)
			throws ModelException {
		return records;
	}

	

}
