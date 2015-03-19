package models;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import utils.db.NotMaterialized;

import models.enums.FilterRuleType;

public class FilterRule implements JsonSerializable {			
	public FilterRuleType type; //Does this filter select data records or members from the database?
	public String name; //Name of filter class implementation to use in order to evaluate this filter
    public List<Object> params; //Parameters to be used by filter class. Semantic depends on used class
	public String group; //Name of result group, selected records or members will belong to. Required for studies with multiple groups of participants.
	public ObjectId aps; //APS to which this rule provides records 	
}
