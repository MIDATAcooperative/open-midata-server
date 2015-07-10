package utils.access;

import java.util.List;

import models.FormatInfo;
import models.ModelException;
import models.Record;

public class FormatGroupHandling extends QueryManager {

	private QueryManager next;
	
	public FormatGroupHandling(QueryManager next) {
		this.next = next;
	}
	
	@Override
	protected boolean lookupSingle(Record record, Query q)
			throws ModelException {
		
		boolean result = next.lookupSingle(record, q);
		if (result && q.returns("group")) {
			FormatInfo fi = FormatInfo.getByName(record.format);
    		record.group = fi.group;
		}
		
		return result;
	}

	@Override
	protected List<Record> query(Query q) throws ModelException {
				
        List<Record> result = next.query(q);
        
        if (q.returns("group")) {
        	for (Record r : result) {
        		FormatInfo fi = FormatInfo.getByName(r.format);
        		r.group = fi.group;
        	}
        }
		return result;
	}

}
