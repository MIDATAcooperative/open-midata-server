package models;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import utils.RuntimeConstants;
import utils.access.DBRecord;
import utils.access.RecordConversion;
import utils.access.index.StatsIndexKey;

/**
 * result of a summary query.
 *
 */
public class RecordsInfo {
	
	/**
	 * number of records summarized in this entry
	 */
	public int count;
	
	/**
	 * creation time of oldest record summarized in this entry
	 */
	public Date oldest;
	
	/**
	 * creation time of newest record summarized in this entry
	 */
	public Date newest;
	
	/**
	 * calculation timestamp.
	 */
	public Date calculated;
	
	/**
	 * set of all record formats of all records summarized in this entry
	 */
	public Set<String> formats;
	
	/**
	 * set of all record content types of all records summarized in this entry
	 */
	public Set<String> contents;
	
	/**
	 * set of all groups of all records summarized in this entry
	 */
	public Set<String> groups;
	
	/**
	 * set of all apps of all records summarized in this entry
	 */
	public Set<MidataId> apps;
	
	/**
	 * set of all owner ids of all records summarized in this entry
	 */
	public Set<String> owners;
	
	/**
	 * set of all owner names of all records summarized in this entry
	 */
	public Set<String> ownerNames;
	
	/**
	 * _id of newest record summarized in this entry
	 */
	public MidataId newestRecord;	
	
	public Record newestRecordContent;

	public MidataId aps;
	
    public RecordsInfo() {
    	formats = new HashSet<String>();
    	contents = new HashSet<String>();
    	groups = new HashSet<String>();
    	owners = new HashSet<String>();
    	apps =new HashSet<MidataId>();
    	
    }
    
    public RecordsInfo(Record rec) {
    	this(RecordConversion.instance.toDB(rec));
    }
    
    public RecordsInfo(DBRecord rec) {
    	formats = new HashSet<String>();
    	contents = new HashSet<String>();
    	groups = new HashSet<String>();
    	owners = new HashSet<String>();
    	apps =new HashSet<MidataId>();
    	
    	
    	formats.add((String) rec.meta.get("format"));
    	contents.add((String) rec.meta.get("content"));
    	groups.add(rec.group);
    	MidataId app = MidataId.from(rec.meta.get("app")); 
    	if (app != null) apps.add(app); else apps.add(RuntimeConstants.instance.portalPlugin);
    	if (rec.owner != null) owners.add(rec.owner.toString());
    	
    	
    	count = 1;
    	oldest = rec._id.getCreationDate(); //meta.get("created");
    	newest = oldest;
    	newestRecord = rec._id;
    	
    	calculated = new Date(System.currentTimeMillis());
    }
		
    
	
	public void merge(RecordsInfo item) {
		this.count += item.count;
		if (item.newest.after(this.newest)) {
			this.newest = item.newest;
			this.newestRecord = item.newestRecord;
		}
		if (item.oldest.before(this.oldest)) {
			this.oldest = item.oldest;
		}
		this.groups.addAll(item.groups);
		this.contents.addAll(item.contents);
		this.formats.addAll(item.formats);
		this.owners.addAll(item.owners);
		this.apps.addAll(item.apps);
		

	}
	
	public void merge(StatsIndexKey item) {
		this.count += item.count;
		if (item.newest > this.newest.getTime()) {
			this.newest = new Date(item.newest);
			this.newestRecord = item.newestRecord;
		}
		if (item.oldest < this.oldest.getTime()) {
			this.oldest = new Date(item.oldest);
		}
		this.groups.add(item.group);
		this.contents.add(item.content);
		this.formats.add(item.format);
		if (item.owner != null) this.owners.add(item.owner.toString());
		if (item.app != null) this.apps.add(item.app);
		

	}

}
