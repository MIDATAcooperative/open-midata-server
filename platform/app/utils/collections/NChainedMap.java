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
