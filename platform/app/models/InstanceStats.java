package models;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Instance usage statistics for one day
 *
 */
public class InstanceStats extends Model {
	
	private static final String collection = "instancestats";
	public @NotMaterialized static final Set<String> ALL_FIELDS = Collections.unmodifiableSet(Sets.create("_id", "date", "recordCount", "vRecordCount", "appCount", "userCount", "consentCount", "runningStudyCount", "groupCount", "auditEventCount", "indexCount", "indexPageCount", "languages")); 
	
	/**
	 * date of statistic
	 */
	public Date date;
	
	/**
	 * total number of records in database
	 */
	public long recordCount;
	
	/**
	 * total number of archived records
	 */
	public long vRecordCount;
	
	/**
	 * total number of apps registered
	 */
	public long appCount; 
	
	/**
	 * total number of studies running
	 */
	public long runningStudyCount;
	
	/**
	 * total number of groups
	 */
	public long groupCount;
	
	/**
	 * total number of audit events
	 */
	public long auditEventCount;
	
	/**
	 * total number of indexes
	 */
	public long indexCount;
	
	/**
	 * total number of index pages
	 */
	public long indexPageCount;
	
	/**
	 * distribution of language use
	 */
	public Map<String, Long> languages;
    
	/**
	 * total number of users per role
	 */
	public Map<String, Long> userCount;
	
	/**
	 * total number of consents per type
	 */
	public Map<String, Long> consentCount;
		
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static Set<InstanceStats> getAll(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.getAll(InstanceStats.class, collection, properties, ALL_FIELDS);
	}

}
