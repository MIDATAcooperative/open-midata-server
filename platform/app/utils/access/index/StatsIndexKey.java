package utils.access.index;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Set;


import models.MidataId;
import models.Record;
import models.RecordsInfo;
import utils.access.DBRecord;

public class StatsIndexKey extends BaseIndexKey<StatsIndexKey,StatsIndexKey> implements Comparable<StatsIndexKey> {

    public MidataId aps;	
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
	}


	@Override
	public boolean matches(StatsIndexKey match) {
		return this.equals(match);
	}


	@Override
	public void writeObject(ObjectOutputStream s, StatsIndexKey last) throws IOException {
		s.writeUTF(aps.toString());	
		s.writeUTF(format);		
		s.writeUTF(content);
		s.writeUTF(group);
		s.writeUTF(app.toString());		
		s.writeUTF(owner.toString());
		s.writeUTF(ownerName);
		s.writeUTF(newestRecord.toString());	
		s.writeInt(count);	
		s.writeLong(oldest);		
		s.writeLong(newest);		
		s.writeLong(calculated);		
	}


	@Override
	public void readObject(ObjectInputStream s, StatsIndexKey last) throws IOException, ClassNotFoundException {
		aps = new MidataId(s.readUTF());	
		format = s.readUTF();		
		content = s.readUTF();
		group = s.readUTF();
		app = new MidataId(s.readUTF());		
		owner = new MidataId(s.readUTF());
		ownerName = s.readUTF();
		newestRecord = new MidataId(s.readUTF());	
		count = s.readInt();	
		oldest = s.readLong();		
		newest = s.readLong();		
		calculated = s.readLong();		
	}

	
}
