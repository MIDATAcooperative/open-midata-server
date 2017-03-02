package utils.collections;

import java.util.HashMap;
import java.util.Map;

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
	
	public NChainedMap<K, V> map(Map<K, V> properties) {		
		putAll(properties);
		return this;
	}
	
	public NChainedMap<K, V> mapNotEmpty(K key, V value) {				
		if (value != null && !"".equals(value.toString())) put(key, value);
		return this;
	}
	
	public NChainedMap<K, V> removeKey(K key) {
		remove(key);
		return this;
	}
		
	
}
