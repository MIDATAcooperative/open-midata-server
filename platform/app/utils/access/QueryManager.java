package utils.access;

import java.util.List;

import models.ModelException;
import models.Record;

public abstract class QueryManager {

	protected abstract boolean lookupSingle(Record record, Query q) throws ModelException;
	
	protected abstract List<Record> query(Query q) throws ModelException;
	
	protected abstract void postProcess(List<Record> records, Query q) throws ModelException;
}
