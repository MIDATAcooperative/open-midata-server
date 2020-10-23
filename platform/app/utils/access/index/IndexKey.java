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
import java.io.Serializable;

import models.MidataId;

public class IndexKey extends BaseIndexKey<IndexKey,IndexMatch> implements Comparable<IndexKey> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7778089868162498692L;
	
	private Comparable key[];
	protected MidataId id;
	protected MidataId value;
	
	public IndexKey(Comparable[] key, MidataId id, MidataId value) {
		this.key = new Comparable[key.length]; 
		System.arraycopy(key, 0, this.key, 0, key.length);
		this.id = id;
		this.value = value;
	}
	
	public IndexKey(IndexKey other) {
		this.key = other.key;
		this.id = other.id;
		this.value = other.value;
	}
	
	// For deserialization
	public IndexKey() {}

	@Override
	public int compareTo(IndexKey arg0) {
		for (int idx=0;idx<key.length;idx++) {
			if (key[idx] != null) {
			  if (arg0.key[idx] == null) return 1;
			  int comp = key[idx].compareTo(arg0.key[idx]);
			  if (comp != 0) return comp;
			} else {
				if (arg0.key[idx] != null) return -1;
			}
		}
		int r = id.compareTo(arg0.id);
		return r == 0 ? value.compareTo(arg0.value) : r;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof IndexKey) {
			IndexKey other = (IndexKey) arg0;
			
			for (int idx=0;idx<key.length;idx++) {
				if (key[idx] != null) {
				  if (! key[idx].equals(other.key[idx])) return false;
				} else if (other.key[idx] != null) return false;
			}
			
			return id.equals(other.id) && value.equals(other.value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hk = 0;
		for (Object o: key) { if (o!=null) hk += o.hashCode(); }
		hk += id.hashCode() + value.hashCode();
		return hk;
	}
	
	public Comparable[] getKey() {
		return key;
	}
	
	public MidataId getId() {
		return id;
	}
	
	public MidataId getValue() {
		return value;
	}
	
	public void writeObject(ObjectOutputStream s, IndexKey last) throws IOException {
		int writeout = 0;
		if (last!=null) {
		  for (int i=0;i<key.length;i++) {
			  if (key[i]==null || !key[i].equals(last.key[i])) writeout |= (2 << i);
		  }
		} else {
           writeout = (2 << key.length) - 1;					
		}
		s.writeByte(key.length);
		s.writeInt(writeout);
	    s.writeUTF(id.toString());
	    s.writeUTF(value.toString());
	    for (int i=0;i<key.length;i++) if ((writeout & (2<<i))>0) s.writeObject(key[i]);
	}
		 
	public void readObject(ObjectInputStream s, IndexKey last) throws IOException, ClassNotFoundException {
		int len = s.readByte();
		key = new Comparable[len];
		int writeout = s.readInt();		
		id = MidataId.from(s.readUTF());
		value = MidataId.from(s.readUTF());
		for (int i=0;i<len;i++) if ((writeout & (2<<i))>0) key[i] = (Comparable) s.readObject(); else key[i] = last.key[i];				
	}

	@Override
	public IndexMatch toMatch() {
		return new IndexMatch(id, value);
	}

	@Override
	public IndexKey copy() {
		return new IndexKey(key,id,value);
	}
	
	

	@Override
	public void fetchValue(IndexKey otherKey) {
		this.value = otherKey.value;		
	}

	@Override
	public boolean matches(IndexMatch match) {
		return match.recordId.equals(id) && match.apsId.equals(value);
	}

	/*
	@Override
	public String toString() {
		return "("+key[0].toString()+","+id.toString()+","+value.toString()+")";
	}
	*/
	
	
}
