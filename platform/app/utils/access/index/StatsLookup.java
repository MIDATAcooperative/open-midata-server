/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access.index;

import java.util.Collections;
import java.util.Set;

import models.MidataId;
import utils.access.DBRecord;

public class StatsLookup extends BaseLookup<StatsIndexKey>{

	private Set<MidataId> owner;	
	private Set<String> studyGroup;
	private Set<String> format;
	private Set<String> content;
	private Set<String> group;
	private Set<MidataId> app;
	private MidataId aps;
	private MidataId stream;
	
	public StatsLookup() {}
	
	public StatsLookup(DBRecord rec) {
		 
		 format = Collections.singleton((String) rec.meta.get("format"));
		 content = Collections.singleton((String) rec.meta.get("content"));
		 group = Collections.singleton(rec.group);
		 owner = Collections.singleton(rec.owner);
		 		   
		 String app1 = (String) rec.meta.get("app"); 
		 if (app1 != null) app = Collections.singleton(new MidataId(app1));
		  		 
	}
		
	public MidataId getAps() {
		return aps;
	}

	public void setAps(MidataId aps) {
		this.aps = aps;
	}
	
	public MidataId getStream() {
		return stream;
	}

	public void setStream(MidataId stream) {
		this.stream = stream;
	}

	public Set<MidataId> getOwner() {
		return owner;
	}

	public void setOwner(Set<MidataId> owner) {
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

	public Set<MidataId> getApp() {
		return app;
	}

	public void setApp(Set<MidataId> app) {
		this.app = app;
	}

	@Override
	public boolean conditionCompare(StatsIndexKey inkey) {
		if (aps != null && !aps.equals(inkey.aps)) return false;
		if (stream != null && !stream.equals(inkey.stream)) return false;
		if (owner != null && !owner.contains(inkey.owner)) return false;
		if (format != null && !format.contains(inkey.format)) return false;
		if (content != null && !content.contains(inkey.content)) return false;
		if (group != null && !group.contains(inkey.group)) return false;
		if (app != null && !app.contains(inkey.app)) return false;
		if (studyGroup != null && (inkey.studyGroup==null || !studyGroup.contains(inkey.studyGroup))) return false;
		return true;
	}

	@Override
	public boolean conditionCompare(StatsIndexKey lk, StatsIndexKey hk) {
		if (aps != null) {
			if ((lk==null || aps.compareTo(lk.aps) >= 0) && (hk==null || aps.compareTo(hk.aps) <= 0)) return true;
			return false;
		} 
		return true;
	}
	
	private String nonull(String what, Object in) {
		return in!=null ? ("what: "+in.toString()) : "";
	}
	
	public String toString() {
		return "{ statslookup "+nonull("owner",owner)+nonull("format", format)+nonull("content", content)+nonull("group",group)+nonull("app",app)+nonull("aps",aps)+nonull("stream",stream)+" }";
	}

}
