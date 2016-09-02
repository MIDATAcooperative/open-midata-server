package utils.access.index;

import org.bson.types.BasicBSONList;

public class IndexKey implements Comparable<IndexKey> {

	private BasicBSONList key;
	
	public IndexKey(BasicBSONList keyarray) {
		this.key = keyarray;		
	}

	@Override
	public int compareTo(IndexKey arg0) {
		for (int idx=0;idx<key.size();idx++) {
			int comp = ((Comparable) key.get(idx)).compareTo(arg0.key.get(idx));
			if (comp != 0) return comp;
		}
		return 0;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof IndexKey) {
			IndexKey other = (IndexKey) arg0;
			
			for (int idx=0;idx<key.size();idx++) {
				if (! key.get(idx).equals(other.key.get(idx))) return false;
			}
			
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hk = 0;
		for (Object o: key) { hk += o.hashCode(); }
		return hk;
	}
	
	public BasicBSONList getKey() {
		return key;
	}
	
	
}
