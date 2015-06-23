package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.Sets;
import utils.rules.FormatRule;
import utils.rules.Rule;

import models.FilterRule;
import models.Member;
import models.ModelException;
import models.Record;

public class RuleApplication {

	public static RuleApplication instance = new RuleApplication();
	
	public Map<String, Rule> rulecache;
	
	public RuleApplication() {
		rulecache = new HashMap<String, Rule>();
		rulecache.put("format", new FormatRule());
	}
	
	public boolean qualifiesFor(Record record, FilterRule filterRule) throws ModelException {
		Rule rule = rulecache.get(filterRule.name);
		if (rule == null) throw new ModelException("Unknown rule: "+filterRule.name);
		return rule.qualifies(record, filterRule.params);
	}

	public void applyRules(ObjectId userId, List<FilterRule> filterRules, ObjectId sourceaps, ObjectId targetaps, boolean ownerInformation) throws ModelException {
		Collection<Record> records = RecordSharing.instance.list(userId, sourceaps, RecordSharing.FULLAPS, RecordSharing.COMPLETE_META);
	    Set<ObjectId> result = new HashSet<ObjectId>();
	    
		// TODO Apply correctly
		for (Record record : records) {
			boolean qualifies = true;
			
			for (FilterRule rule : filterRules) {
				if (!qualifiesFor(record, rule)) qualifies = false; 
			}
			
			if (qualifies) { 
				result.add(record._id); 
			}
		}
		
		RecordSharing.instance.share(userId, sourceaps, targetaps, result, ownerInformation);
	}
	
	public void applyRules(ObjectId userId, Record record) throws ModelException {
		Member member = Member.getById(userId, Sets.create("rules"));
		if (member.rules!=null) {
			for (String key : member.rules.keySet()) {
				List<FilterRule> rules = member.rules.get(key);
				applyRules(userId, rules, userId, new ObjectId(key), true);
			}
		}
	}
	
	public void setupRules(ObjectId userId, List<FilterRule> filterRules, ObjectId sourceaps, ObjectId targetaps, boolean ownerInformation) throws ModelException {
		
		Member member = Member.getById(userId, Sets.create("rules"));
		
		if (member.rules == null) member.rules = new HashMap<String, List<FilterRule>>();		
		
		for (FilterRule filterRule : filterRules) { 
			filterRule.aps = targetaps;		
		}		
		member.rules.put(targetaps.toString(), filterRules);
        Member.set(userId, "rules", member.rules);			
			
	}
	
    public void setupRulesForSpace(ObjectId userId, List<FilterRule> filterRules, ObjectId sourceaps, ObjectId targetaps, boolean ownerInformation) throws ModelException {
				
		Map<String, Object> query = new HashMap<String, Object>();
		
		for (FilterRule filterRule : filterRules) {
			Rule rule = rulecache.get(filterRule.name);
			if (rule == null) throw new ModelException("Unknown rule: "+filterRule.name);
			rule.setup(query, filterRule.params);
		}
		
		RecordSharing.instance.shareByQuery(userId, sourceaps, targetaps, query);
	}

	public List<FilterRule> createRulesFromQuery(Map<String, Object> query) {
		List<FilterRule> rules = new ArrayList<FilterRule>();
		if (query.containsKey("format")) {
			FilterRule fr = new FilterRule();
			fr.name = "format";
			fr.params = (List) query.get("format"); 
		}
		return rules;
	}
		
}
