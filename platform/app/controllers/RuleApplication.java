package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.access.AccessLog;
import utils.collections.Sets;
import utils.exceptions.ModelException;

import utils.rules.Rule;

import models.APSNotExistingException;
import models.FilterRule;
import models.Member;
import models.Record;

public class RuleApplication {

	public static RuleApplication instance = new RuleApplication();
	
	public Map<String, Rule> rulecache;
	
	public RuleApplication() {
		rulecache = new HashMap<String, Rule>();		
	}
	
	/*
	public boolean qualifiesFor(Record record, FilterRule filterRule) throws ModelException {
		Rule rule = rulecache.get(filterRule.name);
		if (rule == null) throw new ModelException("Unknown rule: "+filterRule.name);
		return filterRule.negate ? !rule.qualifies(record, filterRule.params) : rule.qualifies(record, filterRule.params);
	}

	public void applyRules(ObjectId userId, List<FilterRule> filterRules, ObjectId sourceaps, ObjectId targetaps, boolean ownerInformation) throws ModelException {
		AccessLog.debug("BEGIN APPLY RULES");
		Collection<Record> records = RecordSharing.instance.list(userId, sourceaps, RecordSharing.FULLAPS_FLAT_OWNER, RecordSharing.COMPLETE_META);
		Set<ObjectId> result = applyRules(records, filterRules);		
		RecordSharing.instance.share(userId, sourceaps, targetaps, result, ownerInformation);
		
		Collection<Record> streams = RecordSharing.instance.list(userId, targetaps, RecordSharing.STREAMS_ONLY_OWNER, RecordSharing.COMPLETE_META);
		AccessLog.debug("UNSHARE STREAMS CANDIDATES = "+streams.size());
		Set<ObjectId> remove = new HashSet<ObjectId>();
		for (Record stream : streams) {
			if (!applyRules(stream, filterRules)) remove.add(stream._id);
		}
		AccessLog.debug("UNSHARE STREAMS QUALIFIED = "+remove.size());
		RecordSharing.instance.unshare(userId, targetaps, remove);
		AccessLog.debug("END APPLY RULES");
		
	}
	
	public Set<ObjectId> applyRules(Collection<Record> records, List<FilterRule> filterRules) throws ModelException {
        Set<ObjectId> result = new HashSet<ObjectId>();
	    		
		for (Record record : records) {
						
			if (applyRules(record, filterRules)) { 
				result.add(record._id); 
			}
		}
		
		return result;
	}
	
	public boolean applyRules(Record record, List<FilterRule> filterRules) throws ModelException {
		// TODO Apply correctly
		if (filterRules == null || filterRules.size() == 0) return false;
		
		boolean qualifies = true;
		
		for (FilterRule rule : filterRules) {
			if (!qualifiesFor(record, rule)) qualifies = false;			
		}
	
		return qualifies;
	}
	
	public void applyRules(ObjectId executingPerson, ObjectId userId, Record record, ObjectId useAps) throws ModelException {
		Member member = Member.getById(userId, Sets.create("rules"));
		if (member.rules!=null) {
			for (String key : member.rules.keySet()) {
				List<FilterRule> rules = member.rules.get(key);
				if (applyRules(record, rules)) {
					try {
					  RecordSharing.instance.share(executingPerson, useAps, new ObjectId(key), Collections.singleton(record._id), true);
					} catch (APSNotExistingException e) {
						
					}
				}
			}
		}
	}
	
	public void merge(List<FilterRule> target , List<FilterRule> add_rules) throws ModelException {
		for (FilterRule rule : add_rules) {
			for (FilterRule srule : target) {
				if (rule.name.equals(srule.name)) {
					Rule r = rulecache.get(srule.name);
					if (r == null) throw new ModelException("Unknown rule: "+srule.name);
				    r.merge(srule.params, rule.params);
				}
			}
		}
	}
	
	public void setupRules(ObjectId userId, List<FilterRule> filterRules, ObjectId sourceaps, ObjectId targetaps, boolean ownerInformation) throws ModelException {
		if (filterRules.size() == 0) {
			removeRules(userId, targetaps);
			return;
		}
		
		Member member = Member.getById(userId, Sets.create("rules"));
		
		if (member.rules == null) member.rules = new HashMap<String, List<FilterRule>>();		
		
		for (FilterRule filterRule : filterRules) { 
			filterRule.aps = targetaps;		
		}		
		member.rules.put(targetaps.toString(), filterRules);
        Member.set(userId, "rules", member.rules);			
			
	}
	
	public List<FilterRule> getRules(ObjectId userId, ObjectId apsId) throws ModelException {
		Member member = Member.getById(userId, Sets.create("rules"));
		if (member.rules!=null) return member.rules.get(apsId.toString());
		return null;
	}
	
	public void removeRules(ObjectId userId, ObjectId targetaps) throws ModelException {
        Member member = Member.getById(userId, Sets.create("rules"));
		
		if (member.rules == null) return;
		 
		String key = targetaps.toString();
	    if (member.rules.containsKey(key)) {
	    	member.rules.remove(key);
	    	Member.set(userId, "rules", member.rules);
	    }
	}
	
    public void setupRulesForSpace(ObjectId userId, List<FilterRule> filterRules, ObjectId sourceaps, ObjectId targetaps, boolean ownerInformation) throws ModelException {				
		Map<String, Object> query = queryFromRules(filterRules);						
		RecordSharing.instance.shareByQuery(userId, sourceaps, targetaps, query);
	}

	public List<FilterRule> createRulesFromQuery(Map<String, Object> query) {
		List<FilterRule> rules = new ArrayList<FilterRule>();
		createRulesFromQuery(query, rules, false);
		if (query.containsKey("_exclude")) {
			createRulesFromQuery((Map<String, Object>) query.get("_exclude"), rules, true);
		}
		return rules;
	}
	
	private void createRulesFromQuery(Map<String, Object> query, List<FilterRule> rules, boolean negate) {	
		if (query.containsKey("format")) {
			FilterRule fr = new FilterRule();
			Object formats = query.get("format");
			fr.name = "format";
			fr.params = formats instanceof Collection ? new ArrayList((Collection) formats) : Collections.<String>singletonList(formats.toString());
			fr.negate = negate;
			rules.add(fr);
		}
		if (query.containsKey("content")) {
			FilterRule fr = new FilterRule();
			Object contents = query.get("content");
			fr.name = "content";
			fr.params = contents instanceof Collection ? new ArrayList((Collection) contents) : Collections.<String>singletonList(contents.toString());
			fr.negate = negate;
			rules.add(fr);
		}
		if (query.containsKey("group")) {
			FilterRule fr = new FilterRule();
			Object groups = query.get("group");
			fr.name = "group";
			fr.params = groups instanceof Collection ? new ArrayList((Collection) groups) : Collections.<String>singletonList(groups.toString());
			fr.negate = negate;
			rules.add(fr);
		}
		if (query.containsKey("_id")) {
			FilterRule fr = new FilterRule();
			Object ids = query.get("_id");
			fr.name = "_id";
			fr.params = ids instanceof Collection ? new ArrayList((Collection) ids) : Collections.<String>singletonList(ids.toString());
			fr.negate = negate;
			rules.add(fr);
		}	
	}
	
	public Map<String, Object> queryFromRules(List<FilterRule> filterRules) throws ModelException {
        Map<String, Object> query = new HashMap<String, Object>();
        Map<String, Object> exclude = null;
		
		for (FilterRule filterRule : filterRules) {
			Rule rule = rulecache.get(filterRule.name);
			if (rule == null) throw new ModelException("Unknown rule: "+filterRule.name);
			
			if (filterRule.negate) {
			   if (exclude==null) {
				   exclude = new HashMap<String, Object>();
				   query.put("_exclude", exclude);
			   }				
			   rule.setup(exclude, filterRule.params);	
			} else {
			  rule.setup(query, filterRule.params);
			}
		}
		
		return query;
	}
		*/
		
}
