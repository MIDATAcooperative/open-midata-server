package utils.access.index;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import models.MidataId;
import utils.RuntimeConstants;
import utils.access.DBRecord;

public class StatsLookup extends BaseLookup<StatsIndexKey>{

	private Set<String> owner;	
	private Set<String> studyGroup;
	private Set<String> format;
	private Set<String> content;
	private Set<String> group;
	private Set<String> app;
	private MidataId aps;
	
	public StatsLookup() {}
	
	public StatsLookup(DBRecord rec) {
		 
		 format = Collections.singleton((String) rec.meta.get("format"));
		 content = Collections.singleton((String) rec.meta.get("content"));
		 group = Collections.singleton(rec.group);
		 owner = Collections.singleton(rec.owner.toString());
		 		   
		 String app1 = (String) rec.meta.get("app"); 
		 if (app1 != null) app = Collections.singleton(app1);
		  		 
	}
		
	public MidataId getAps() {
		return aps;
	}

	public void setAps(MidataId aps) {
		this.aps = aps;
	}

	public Set<String> getOwner() {
		return owner;
	}

	public void setOwner(Set<String> owner) {
		this.owner = owner;
	}

	public Set<String> getStudyGroup() {
		return studyGroup;
	}

	public void setStudyGroup(Set<String> studyGroup) {
		this.studyGroup = studyGroup;
	}

	public Set<String> getFormat() {
		return format;
	}

	public void setFormat(Set<String> format) {
		this.format = format;
	}

	public Set<String> getContent() {
		return content;
	}

	public void setContent(Set<String> content) {
		this.content = content;
	}

	public Set<String> getGroup() {
		return group;
	}

	public void setGroup(Set<String> group) {
		this.group = group;
	}

	public Set<String> getApp() {
		return app;
	}

	public void setApp(Set<String> app) {
		this.app = app;
	}

	@Override
	public boolean conditionCompare(StatsIndexKey inkey) {
		if (aps != null && !aps.equals(inkey.aps)) return false;
		if (owner != null && !owner.contains(inkey.owner)) return false;
		if (format != null && !format.contains(inkey.format)) return false;
		if (content != null && !content.contains(inkey.content)) return false;
		if (group != null && !group.contains(inkey.group)) return false;
		if (app != null && !app.contains(inkey.app)) return false;
		return true;
	}

	@Override
	public boolean conditionCompare(StatsIndexKey lk, StatsIndexKey hk) {
		if (aps != null) {
			if (aps.compareTo(lk.aps) >= 0 && aps.compareTo(hk.aps) <= 0) return true;
			return false;
		} 
		return true;
	}
	
	private String nonull(String what, Object in) {
		return in!=null ? ("what: "+in.toString()) : "";
	}
	
	public String toString() {
		return "{ statslookup "+nonull("owner",owner)+nonull("format", format)+nonull("content", content)+nonull("group",group)+nonull("app",app)+nonull("aps",aps)+" }";
	}

}
