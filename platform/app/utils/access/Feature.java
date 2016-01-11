package utils.access;

import java.util.List;
import java.util.Set;

import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import models.Record;
import models.RecordsInfo;

/**
 * abstract super-class for additional features of the query engine
 *
 */
public abstract class Feature {

		
	protected abstract List<DBRecord> lookup(List<DBRecord> record, Query q) throws AppException;
	
	protected abstract List<DBRecord> query(Query q) throws AppException;
	
	protected abstract List<DBRecord> postProcess(List<DBRecord> records, Query q) throws AppException;
		
}
