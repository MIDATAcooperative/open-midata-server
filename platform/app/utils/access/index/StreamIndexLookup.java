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

import java.util.Set;

import models.MidataId;

public class StreamIndexLookup extends BaseLookup<StreamIndexKey>{

	private MidataId id;
	private String format;
	private Set<String> content;
	private Set<MidataId> owner;
	private Set<MidataId> app;
	private boolean onlyStreams;
	private long minCreated;
	private long maxCreated;
	
	public StreamIndexLookup() {	
	}
	
		
	public MidataId getId() {
		return id;
	}


	public void setId(MidataId id) {
		this.id = id;
	}


	public String getFormat() {
		return format;
	}


	public void setFormat(String format) {
		this.format = format;
	}


	public Set<String> getContent() {
		return content;
	}


	public void setContent(Set<String> content) {
		this.content = content;
	}


	public Set<MidataId> getApp() {
		return app;
	}


	public void setApp(Set<MidataId> app) {
		this.app = app;
	}
	

	public Set<MidataId> getOwner() {
		return owner;
	}


	public void setOwner(Set<MidataId> owner) {
		this.owner = owner;
	}


	public boolean isOnlyStreams() {
		return onlyStreams;
	}


	public void setOnlyStreams(boolean onlyStreams) {
		this.onlyStreams = onlyStreams;
	}
	
	
	public long getMinCreated() {
		return minCreated;
	}


	public void setMinCreated(long minCreated) {
		this.minCreated = minCreated;
	}


	public long getMaxCreated() {
		return maxCreated;
	}


	public void setMaxCreated(long maxCreated) {
		this.maxCreated = maxCreated;
	}


	@Override
	public boolean conditionCompare(StreamIndexKey inkey) {		
         if (id != null && !id.equals(inkey.getId())) return false;
         if (minCreated != 0 && inkey.getCreated() < minCreated && inkey.getIsstream()==null) return false;
         if (maxCreated != 0 && inkey.getCreated() > maxCreated && inkey.getIsstream()==null) return false;
         if (format != null && !format.equals(inkey.getFormat())) return false;
         if (content != null && !content.contains(inkey.getContent())) return false;
         if (app != null && inkey.getApp() !=null && !app.contains(inkey.getApp())) return false;
         if (owner != null && !owner.contains(inkey.getOwner())) return false;
         if (onlyStreams && inkey.getIsstream()==null) return false;
		return true;
	}

	@Override
	public boolean conditionCompare(StreamIndexKey lk, StreamIndexKey hk) {
		if (format != null) {
			if ((lk==null || format.compareTo(lk.getFormat()) >= 0) && (hk==null || format.compareTo(hk.getFormat())<=0)) {
				if (id != null) {
					if ((lk==null || id.compareTo(lk.getId()) >= 0) && (hk==null || id.compareTo(hk.getId())<=0)) return true;
					else return false;
				} 
				return true;
			} else return false;
		} else if (id != null && lk!=null && hk!=null && lk.getFormat().equals(hk.getFormat())) {			
			if (id.compareTo(lk.getId()) >= 0 && id.compareTo(hk.getId())<=0) return true;
			else return false;			
		}
		return true;
	}


	@Override
	public String toString() {
		return "aps-lookup({"
	           +(id!=null?(" id:"+id):"")
	           +(format!=null?(" format:"+format.toString()):"")
	           +(content!=null?(" content:"+content.toString()):"")
	           +(owner!=null?(" owner:"+owner.toString()):"")
	           +(app!=null?(" app:"+app.toString()):"")
	           +(minCreated!=0?(" min:"+minCreated):"")
	           +(maxCreated!=0?(" max:"+maxCreated):"")
	           +(onlyStreams?" streams":"")	           
	           +"})";		
	}
	
	

}
