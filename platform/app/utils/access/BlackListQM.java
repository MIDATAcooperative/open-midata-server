package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Record;

import org.bson.BasicBSONObject;

public class BlackListQM extends QueryManager {

    private QueryManager next;
    private Set<String> blacklist; 
	
	public BlackListQM(Query q, QueryManager next) throws ModelException {
		this.next = next;
		initBlacklist(q);
	}
	
	private void initBlacklist(Query q) throws ModelException {
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
	protected List<Record> lookup(List<Record> record, Query q)
			throws ModelException {
		List<Record> result = next.lookup(record, q);
		return filter(result);
	}
	
	private List<Record> filter(List<Record> input) {		
		if (blacklist.isEmpty()) return input;
		List<Record> filtered = new ArrayList<Record>(input.size());
		for (Record record : input) {
			if (!blacklist.contains(record._id.toString())) filtered.add(record);
		}
		return filtered;
	}

	@Override
	protected List<Record> query(Query q) throws ModelException {		
		List<Record> result = next.query(q);
		return filter(result);
	}
	

	@Override
	protected List<Record> postProcess(List<Record> records, Query q)
			throws ModelException {
		return next.postProcess(records, q);		
	}

	

}
