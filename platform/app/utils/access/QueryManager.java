package utils.access;

import java.util.List;

import models.ModelException;
import models.Record;

public abstract class QueryManager {

		
	protected abstract List<Record> lookup(List<Record> record, Query q) throws ModelException;
	
	protected abstract List<Record> query(Query q) throws ModelException;
	
	protected abstract List<Record> postProcess(List<Record> records, Query q) throws ModelException;
}
