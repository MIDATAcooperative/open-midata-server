package utils.access;

import java.util.Date;

import org.bson.BasicBSONObject;

import controllers.Circles;
import utils.AccessLog;
import utils.exceptions.AppException;

public class Feature_ConsentRestrictions extends Feature {

	private Feature next;
	
	public Feature_ConsentRestrictions(Feature next) {
		this.next = next;
	}

	

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		BasicBSONObject filter = q.getCache().getAPS(q.getApsId()).getMeta("_filter");
		if (filter != null) {			
		  if (filter.containsField("valid-until")) {
			  Date until = filter.getDate("valid-until");
			  if (until.before(new Date(System.currentTimeMillis()))) {
				  AccessLog.log("consent not valid anymore");			
				  Circles.consentExpired(q.getCache().getExecutor(), q.getApsId());
				  return ProcessingTools.empty();
			  }
		  }
		  Date historyDate = filter.getDate("history-date");
		  if (historyDate != null && historyDate.after(new Date())) filter.remove("history-date");
		  if (!filter.isEmpty()) {
			 AccessLog.log("Applying consent filter");
			 return QueryEngine.combineIterator(q, filter.toMap(), new Feature_ProcessFilters(next));
		  }			
		} 
		return next.iterator(q);
	}
	
	
}
