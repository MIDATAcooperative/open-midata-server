package utils.access.index;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import models.MidataId;

public class TimestampIndexKey extends BaseIndexKey<TimestampIndexKey,TimestampIndexKey> implements Comparable<TimestampIndexKey> {

    public String what;	
	public long value;	
	
	public TimestampIndexKey() {}
	
	public TimestampIndexKey(String what, long value) {
		this.what = what;
		this.value = value;
	}
	 
	@Override
	public int compareTo(TimestampIndexKey other) {
		return what.compareTo(other.what);		
	}


	@Override
	public TimestampIndexKey toMatch() {
		return this;				
	}


	@Override
	public TimestampIndexKey copy() {
		TimestampIndexKey result = new TimestampIndexKey();
		result.what = this.what;
		result.value = this.value;
		return result;
	}


	@Override
	public void fetchValue(TimestampIndexKey otherKey) {
		this.value = otherKey.value;			
	}


	@Override
	public boolean matches(TimestampIndexKey match) {
		return this.equals(match);
	}


	@Override
	public void writeObject(ObjectOutputStream s, TimestampIndexKey last) throws IOException {
		s.writeUTF(what);	
		s.writeLong(value);					
	}

	@Override
	public void readObject(ObjectInputStream s, TimestampIndexKey last) throws IOException, ClassNotFoundException {			
		what = s.readUTF();
		value = s.readLong();
	}
	
}
