package utils.access;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.BasicBSONObject;

import utils.AccessLog;
import utils.exceptions.AppException;

public class Feature_ConsentRestrictions extends Feature {

	private Feature next;
	
	public Feature_ConsentRestrictions(Feature next) {
		this.next = next;
	}


	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		BasicBSONObject filter = q.getCache().getAPS(q.getApsId()).getMeta("_filter");
		if (filter != null) {			
		  if (filter.containsField("valid-until")) {
			  Date until = filter.getDate("valid-until");
			  if (until.before(new Date(System.currentTimeMillis()))) {
				  AccessLog.log("consent not valid anymore");				  
				  return new ArrayList<DBRecord>();
			  }
		  }
		  if (!filter.isEmpty()) {
			 AccessLog.log("Applying consent filter");
			 return QueryEngine.combine(q, filter.toMap(), new Feature_ProcessFilters(next));
		  }			
		} 
		return next.query(q);		
	}
	
	
}
