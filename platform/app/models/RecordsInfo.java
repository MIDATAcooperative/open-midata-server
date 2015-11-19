package models;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

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
	 * _id of newest record summarized in this entry
	 */
	public ObjectId newestRecord;	

	
    public RecordsInfo() {
    	formats = new HashSet<String>();
    	contents = new HashSet<String>();
    	groups = new HashSet<String>();
    }
    
    public RecordsInfo(Record rec) {
    	formats = new HashSet<String>();
    	contents = new HashSet<String>();
    	groups = new HashSet<String>();
    	
    	formats.add(rec.format);
    	contents.add(rec.content);
    	groups.add(rec.group);
    	
    	count = 1;
    	oldest = rec.created;
    	newest = rec.created;
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

	}

}
