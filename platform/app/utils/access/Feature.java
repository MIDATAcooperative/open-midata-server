package utils.access;

import java.util.List;

import utils.exceptions.AppException;

/**
 * abstract super-class for additional features of the query engine
 *
 */
public abstract class Feature {
				
	protected abstract List<DBRecord> query(Query q) throws AppException;
					
}
