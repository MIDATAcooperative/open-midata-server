package utils.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import utils.AccessLog;
import utils.access.index.StatsIndexKey;
import utils.access.index.StatsIndexRoot;
import utils.access.op.AlwaysTrueCondition;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.IndexAccessContext;
import utils.exceptions.AppException;

public class Feature_ManyUserNoRestriction extends Feature {

	private Feature next;
	
	public Feature_ManyUserNoRestriction(Feature next) {
		this.next = next;
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.getApsId().equals(q.getCache().getAccountOwner()) &&
		    !q.restrictedBy("index") &&
			!q.restrictedBy("_id") &&
			(q.restrictedBy("format") || q.restrictedBy("content") || q.restrictedBy("app")) &&
		    !q.restrictedBy("created-after") &&
			!q.restrictedBy("updated-after") &&
			!q.restrictedBy("shared-after") &&
			!q.restrictedBy("consent-after") &&
	        !q.restrictedBy("study-related")) { 				
				String mode = q.getStringRestriction("public");
				Set<String> sets = q.restrictedBy("owner") ? q.getRestriction("owner") : Collections.singleton("all");
				if (!("only".equals(mode)) && sets.contains("all")) {
					/*StatsIndexRoot index = q.getCache().getStatsIndexRoot(false);
					StatsIndexRoot index2 = q.getCache().getStatsIndexRoot(true);
					if (index!=null) AccessLog.log("INDEX 1 = "+index.getModel().getId().toString()+" "+index.getModel().pseudonymize);
					if (index2!=null) AccessLog.log("INDEX 2 = "+index2.getModel().getId().toString()+" "+index2.getModel().pseudonymize);
					if (index != null || index2 != null) {*/						
						Feature calcStats = new Feature_Stats(new Feature_ProcessFilters(new Feature_FormatGroups(new Feature_AccountQuery(new Feature_ConsentRestrictions(new Feature_Consents(new Feature_Streams()))))));
						Query q2 = new Query(q.onlyRestrictions(Sets.create("format","content","app","owner","public","deleted","group","group-system","code","group-exclude","force-local","usergroup","study","consent-limit")), "find-many", CMaps.map("fast-stats", true).map("no-postfilter-streams", true), q.getApsId(), new IndexAccessContext(q.getCache(), false, null));						
						DBIterator<DBRecord> recs = calcStats.iterator(q2);		
						Set<String> owner = new HashSet<String>();
						int count = 0;
						while (recs.hasNext()) {
							DBRecord record = recs.next();
							StatsIndexKey inf2 = (StatsIndexKey) record.attached;
							if (owner.add(inf2.owner.toString())) count++;
							// cancel on too many
							if (count > 100) return next.iterator(q);
						}
						AccessLog.log("use many user index; users="+owner.size());	
						if (owner.isEmpty()) return ProcessingTools.empty();						
						return next.iterator(new Query(q,"many-users",CMaps.map("owner", owner)));						
					//}														
				}
		} 
		return next.iterator(q);
	}

}
