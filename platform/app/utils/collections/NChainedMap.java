package utils.collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NChainedMap<K, V> extends HashMap<K, V> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1399019935309992878L;

	public NChainedMap() {		
	}

	public NChainedMap<K, V> map(K key, V value) {
		put(key, value);
		return this;
	}
		
	
}
