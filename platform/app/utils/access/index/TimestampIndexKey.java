/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
