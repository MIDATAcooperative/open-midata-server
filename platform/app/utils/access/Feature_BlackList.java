package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Record;

import org.bson.BasicBSONObject;

import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * access permission sets may contain a blacklist of record IDs that are never contained in this APS.
 * (makes only sense with query based access permission sets)
 *
 */
public class Feature_BlackList extends Feature {

    private Feature next;
    private Set<String> blacklist; 
	
	public Feature_BlackList(Query q, Feature next) throws AppException {
		this.next = next;
		initBlacklist(q);
	}
	
	private void initBlacklist(Query q) throws AppException {
		blacklist = new HashSet<String>();
		BasicBSONObject list = q.getCache().getAPS(q.getApsId()).getMeta("_exclude");
		if (list != null) {
			Collection idlist = (Collection) list.get("ids");
			for (Object id : idlist) {
				blacklist.add(id.toString());
			}
		}
	}
	
	
	@Override
	protected List<DBRecord> lookup(List<DBRecord> record, Query q)
			throws AppException {
		List<DBRecord> result = next.lookup(record, q);
		return filter(result);
	}
	
	private List<DBRecord> filter(List<DBRecord> input) {		
		if (blacklist.isEmpty()) return input;
		if (AccessLog.detailedLog) AccessLog.logBegin("Begin apply blacklist #recs="+input.size());
		List<DBRecord> filtered = new ArrayList<DBRecord>(input.size());
		for (DBRecord record : input) {
			if (!blacklist.contains(record._id.toString())) filtered.add(record);
		}
		if (AccessLog.detailedLog) AccessLog.logEnd("End apply blacklist #recs="+filtered.size());
		return filtered;
	}

	@Override
	protected List<DBRecord> query(Query q) throws AppException {		
		List<DBRecord> result = next.query(q);
		return filter(result);
	}
	

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q)
			throws AppException {
		return next.postProcess(records, q);		
	}

	

}
