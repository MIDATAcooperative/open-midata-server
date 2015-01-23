package models;

import java.util.Map;

import models.enums.FilterRuleType;

public class FilterRule {			
	public FilterRuleType type; //Does this filter select data records or members from the database?
	public String className; //Name of filter class implementation to use in order to evaluate this filter
    public Map<String, Object> params; //Parameters to be used by filter class. Semantic depends on used class
	public String group; //Name of result group, selected records or members will belong to. Required for studies with multiple groups of participants.
}
