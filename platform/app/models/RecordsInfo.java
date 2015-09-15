package models;

import java.util.Date;
import java.util.Set;

public class RecordsInfo {
	public int count;
	public Date oldest;
	public Date newest;
	public Set<String> formats;
	public Set<String> contents;
	public String group;

	@Override
	public boolean equals(Object obj) {
		return group.equals(((RecordsInfo) obj).group);
	}

	@Override
	public int hashCode() {
		return group.hashCode();
	}

}
