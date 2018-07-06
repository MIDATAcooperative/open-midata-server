package utils.access.index;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import models.MidataId;

public class IndexKey implements Comparable<IndexKey>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7778089868162498692L;
	
	private Comparable key[];
	protected MidataId id;
	protected MidataId value;
	
	public IndexKey(Comparable[] key, MidataId id, MidataId value) {
		this.key = key;
		this.id = id;
		this.value = value;
	}
	
	public IndexKey(IndexKey other) {
		this.key = other.key;
		this.id = other.id;
		this.value = other.value;
	}

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
	
	private void writeObject(ObjectOutputStream s) throws IOException {
	    s.writeUTF(id.toString());
	    s.writeUTF(value.toString());
	    s.writeObject(key);
	}
		 
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		id = MidataId.from(s.readUTF());
		value = MidataId.from(s.readUTF());
		key = (Comparable[]) s.readObject();
	}
	
}
