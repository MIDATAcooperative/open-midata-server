package models;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

public class RecordsInfo {
	public int count;
	public Date oldest;
	public Date newest;
	public Date calculated;
	public Set<String> formats;
	public Set<String> contents;
	public Set<String> groups;
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
