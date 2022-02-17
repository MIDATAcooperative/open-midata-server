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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import models.MidataId;

public class StatsIndexKey extends BaseIndexKey<StatsIndexKey,StatsIndexKey> implements Comparable<StatsIndexKey> {

    public MidataId aps;	
    public MidataId stream;
    public String studyGroup;
	public String format;		
	public String content;
	public String group;
	public MidataId app;		
	public MidataId owner;
	public String ownerName;
	
	public MidataId newestRecord;	
	public int count;	
	public long oldest;		
	public long newest;		
	public long calculated;
	
	 
	@Override
	public int compareTo(StatsIndexKey other) {
		int b = aps.compareTo(other.aps);
		if (b!=0) return b;		
		b = (stream==null) ? (other.stream==null? 0 : 1) : (other.stream==null ? -1 : stream.compareTo(other.stream));
		if (b!=0) return b;
		b = group.compareTo(other.group);
		if (b!=0) return b;
		b = content.compareTo(other.content);
		if (b!=0) return b;
		b = format.compareTo(other.format);
		if (b!=0) return b;
		b = app.compareTo(other.app);
		if (b!=0) return b;
		b = owner.compareTo(other.owner);
		if (b!=0) return b;
		
		return 0;
	}


	@Override
	public StatsIndexKey toMatch() {
		return this;
		
		/*
		RecordsInfo res = new RecordsInfo();
		
		res.formats.add(this.format);	
		res.contents.add(this.content);
		res.groups.add(this.group);
		res.apps.add(this.app);
		res.owners.add(this.owner);
		res.ownerNames.add(this.ownerName);
		res.newest = new Date(this.newest);
		res.oldest = new Date(this.oldest);
		res.count = this.count;
		res.newestRecord = this.newestRecord;	
		res.calculated = new Date(this.calculated);
			*/	
		//return res;
	}


	@Override
	public StatsIndexKey copy() {
		StatsIndexKey result = new StatsIndexKey();
		result.aps = this.aps;	
		result.stream = this.stream;
		result.studyGroup = this.studyGroup;
		result.format = this.format;		
		result.content = this.content;
		result.group = this.group;
		result.app = this.app;
		result.owner = this.owner;
		result.ownerName = this.ownerName;
		result.newestRecord = this.newestRecord;
		result.count = this.count;
		result.oldest = this.oldest;
		result.newest = this.newest;
		result.calculated = this.calculated;
		return result;
	}


	@Override
	public void fetchValue(StatsIndexKey otherKey) {
		this.newestRecord = otherKey.newestRecord;
		this.count = otherKey.count;
		this.oldest = otherKey.oldest;
		this.newest = otherKey.newest;
		this.calculated = otherKey.calculated;		
		this.studyGroup = otherKey.studyGroup;
	}


	@Override
	public boolean matches(StatsIndexKey match) {
		return this.equals(match);
	}


	@Override
	public void writeObject(ObjectOutputStream s, StatsIndexKey last) throws IOException {
		s.writeObject(aps.toString());
		s.writeObject(stream != null ? stream.toString() : null);
		s.writeObject(studyGroup);
		s.writeObject(format);		
		s.writeObject(content);
		s.writeObject(group);
		s.writeObject(app.toString());		
		s.writeObject(owner.toString());
		s.writeObject(ownerName);
		s.writeObject(newestRecord.toString());	
		s.writeInt(count);	
		s.writeLong(oldest);		
		s.writeLong(newest);		
		s.writeLong(calculated);		
	}


	@Override
	public void readObject(ObjectInputStream s, StatsIndexKey last) throws IOException, ClassNotFoundException {
		aps = MidataId.from((String) s.readObject());
		stream = MidataId.from((String) s.readObject());
		studyGroup = (String) s.readObject();
		format = (String) s.readObject();		
		content = (String) s.readObject();
		group = (String) s.readObject();
		app = MidataId.from((String) s.readObject());		
		owner = MidataId.from((String) s.readObject());
		ownerName = (String) s.readObject();
		newestRecord = MidataId.from((String) s.readObject());	
		count = s.readInt();	
		oldest = s.readLong();		
		newest = s.readLong();		
		calculated = s.readLong();		
	}

	
}
