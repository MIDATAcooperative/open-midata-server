package models;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.bson.types.ObjectId;

public class RecordsInfo {
	public int count;
	public Date oldest;
	public Date newest;
	public Set<String> formats;
	public Set<String> contents;
	public ObjectId newestRecord;
	public String group;

	@Override
	public boolean equals(Object obj) {
		return group.equals(((RecordsInfo) obj).group);
	}

	@Override
	public int hashCode() {
		return group.hashCode();
	}
	
	public static RecordsInfo merge(Collection<RecordsInfo> items) {
		RecordsInfo result = null;
		for (RecordsInfo item : items) {
			if (result == null) {
				result = item;
			} else {
				result.count += item.count;
				if (item.newest.after(result.newest)) {
					result.newest = item.newest;
					result.newestRecord = item.newestRecord;
				}
				if (item.oldest.before(result.oldest)) {
					result.oldest = item.oldest;
				}
			}
		}
		return result;
	}

}
