package utils.access;

import java.util.List;

import models.ModelException;
import models.Record;

public class InMemoryQM extends QueryManager {
	
	private List<Record> contents;
	
	public InMemoryQM(List<Record> contents) {
		this.contents = contents;
	}
	
	@Override
	protected boolean lookupSingle(Record record, Query q)
			throws ModelException {		
		return contents.contains(record);
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
