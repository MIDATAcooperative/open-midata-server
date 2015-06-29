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

import utils.collections.Sets;
import utils.rules.FormatRule;
import utils.rules.Rule;

import models.APSNotExistingException;
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
		Set<ObjectId> result = applyRules(records, filterRules);
		
		RecordSharing.instance.share(userId, sourceaps, targetaps, result, ownerInformation);
	}
	
	protected Set<ObjectId> applyRules(Collection<Record> records, List<FilterRule> filterRules) throws ModelException {
        Set<ObjectId> result = new HashSet<ObjectId>();
	    		
		for (Record record : records) {
						
			if (applyRules(record, filterRules)) { 
				result.add(record._id); 
			}
		}
		
		return result;
	}
	
	protected boolean applyRules(Record record, List<FilterRule> filterRules) throws ModelException {
		// TODO Apply correctly
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
						/*if (e.getAps().toString().equals(key)) {
							member.rules.remove(key);
							Member.set(member._id, "rules", member.rules);
						}*/
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
			Object formats = query.get("format");
			fr.name = "format";
			fr.params = formats instanceof List ? (List) formats : Collections.<String>singletonList(formats.toString());
			rules.add(fr);
		}
		return rules;
	}
		
}
